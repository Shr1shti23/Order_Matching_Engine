package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.AuditLogDao;
import com.bank.trading.dao.OrderDao;
import com.bank.trading.dao.OrderEventDao;
import com.bank.trading.dao.impl.AuditLogDaoImpl;
import com.bank.trading.dao.impl.OrderDaoImpl;
import com.bank.trading.dao.impl.OrderEventDaoImpl;
import com.bank.trading.engine.OrderBook;
import com.bank.trading.model.*;
import com.bank.trading.util.MatchResult;
import com.bank.trading.util.OrderPlacementResult;
import com.bank.trading.util.ValidationResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Orchestrator for the full order placement workflow.
 *
 * <h2>Workflow</h2>
 * <pre>
 * placeOrder(order)
 *   1. RiskValidationService.validate()          [cache reads only]
 *   2. orderDao.insert()                         [DB: INSERT orders]
 *   3. Insert ORDER_PLACED audit entry           [DB]
 *   4. ReservationService.reserve()              [cache write]
 *   5. FOK pre-check (if applicable)             [read-only simulation]
 *   6. MatchingEngine.match()                    [read-only simulation]
 *   7. SettlementService.settle()                [DB transaction + cache]
 *   8a. LIMIT / GTC: OrderBookService.addOrderToBook()  [cache]
 *   8b. MARKET / IOC: CancelOrderService.cancelRemainder() [DB + cache]
 * </pre>
 *
 * <p>This class contains zero business logic of its own — it delegates every
 * step to a specialist service.</p>
 */
public final class TradingService {

    private final CacheManager          cache;
    private final RiskValidationService riskValidationService;
    private final ReservationService    reservationService;
    private final MatchingEngine        matchingEngine;
    private final SettlementService     settlementService;
    private final OrderBookService      orderBookService;
    private final CancelOrderService    cancelOrderService;

    private final OrderDao      orderDao;
    private final OrderEventDao orderEventDao;
    private final AuditLogDao   auditLogDao;

    public TradingService(CacheManager cache,
                          RiskValidationService riskValidationService,
                          ReservationService reservationService,
                          MatchingEngine matchingEngine,
                          SettlementService settlementService,
                          OrderBookService orderBookService,
                          CancelOrderService cancelOrderService) {
        this.cache                = cache;
        this.riskValidationService = riskValidationService;
        this.reservationService   = reservationService;
        this.matchingEngine       = matchingEngine;
        this.settlementService    = settlementService;
        this.orderBookService     = orderBookService;
        this.cancelOrderService   = cancelOrderService;
        this.orderDao             = new OrderDaoImpl();
        this.orderEventDao        = new OrderEventDaoImpl();
        this.auditLogDao          = new AuditLogDaoImpl();
    }

    /**
     * Place an order through the full validation → matching → settlement pipeline.
     *
     * @param order the order to place (orderId must be 0; it will be set by this method)
     * @return result indicating success or failure with a descriptive message
     */
    public OrderPlacementResult placeOrder(Order order) {

        // ----------------------------------------------------------------
        // Step 1: Risk validation (cache-only)
        // ----------------------------------------------------------------
        ValidationResult validation = riskValidationService.validate(order, order.getTraderId());
        if (!validation.isValid()) {
            return OrderPlacementResult.failure(validation.getMessage());
        }

        // ----------------------------------------------------------------
        // Step 2: Persist the order (INSERT, status=PENDING)
        // ----------------------------------------------------------------
        long orderId;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                order.setStatus(OrderStatus.PENDING);
                orderId = orderDao.insert(order, conn);
                order.setOrderId(orderId);

                // INSERT initial order_event (CREATED)
                OrderEvent createdEvent = new OrderEvent();
                createdEvent.setOrderId(orderId);
                createdEvent.setEventType(OrderEventType.CREATED);
                createdEvent.setPreviousStatus(null);
                createdEvent.setNewStatus(OrderStatus.PENDING);
                createdEvent.setQuantityChanged(order.getOriginalQty());
                createdEvent.setPrice(order.getPrice());
                createdEvent.setActorUserId(order.getTraderId());
                orderEventDao.insert(createdEvent, conn);

                // INSERT audit log (ORDER_PLACED)
                AuditLog audit = new AuditLog();
                audit.setActorUserId(order.getTraderId());
                audit.setActionType(ActionType.ORDER_PLACED);
                audit.setEntityType("orders");
                audit.setEntityId(orderId);
                audit.setDetails("{\"symbol\":\"" + order.getSymbol()
                    + "\",\"side\":\"" + order.getSide()
                    + "\",\"qty\":" + order.getOriginalQty()
                    + ",\"price\":" + order.getPrice() + "}");
                auditLogDao.insert(audit, conn);

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                return OrderPlacementResult.failure("Failed to persist order: " + ex.getMessage());
            }
        } catch (SQLException ex) {
            return OrderPlacementResult.failure("DB connection error: " + ex.getMessage());
        }

        // ----------------------------------------------------------------
        // Step 3: Reserve funds / holdings in cache
        // ----------------------------------------------------------------
        try {
            reservationService.reserve(order);
        } catch (IllegalStateException ex) {
            return OrderPlacementResult.failure("Reservation failed: " + ex.getMessage());
        }

        // ----------------------------------------------------------------
        // Step 4: Get the order book for this instrument
        // ----------------------------------------------------------------
        OrderBook book = cache.getOrderBook(order.getSymbol());
        if (book == null) {
            reservationService.release(order);
            cancelOrderService.reject(order);
            return OrderPlacementResult.failure("No order book for symbol: " + order.getSymbol());
        }

        // ----------------------------------------------------------------
        // Step 5: FOK pre-check
        // ----------------------------------------------------------------
        if (order.getTimeInForce() == TimeInForce.FOK) {
            if (!matchingEngine.canFullyFill(order, book)) {
                reservationService.release(order);
                cancelOrderService.reject(order);
                return OrderPlacementResult.failure(
                    "FOK order rejected: insufficient liquidity to fill " + order.getOriginalQty() + " units.");
            }
        }

        // ----------------------------------------------------------------
        // Step 6: Match (read-only simulation)
        // ----------------------------------------------------------------
        List<MatchResult> matches = matchingEngine.match(order, book);

        // ----------------------------------------------------------------
        // Step 7: Settle matched fills (one DB transaction)
        // ----------------------------------------------------------------
        if (!matches.isEmpty()) {
            try {
                settlementService.settle(order, matches);
            } catch (RuntimeException ex) {
                // Settlement failed — release reservation; order stays in DB as PENDING
                // but is not in the book.  A compensating cancel is required.
                reservationService.release(order);
                cancelOrderService.reject(order);
                return OrderPlacementResult.failure("Settlement failed: " + ex.getMessage());
            }
        }

        // ----------------------------------------------------------------
        // Step 8: Handle remaining quantity
        // ----------------------------------------------------------------
        if (order.getRemainingQty() > 0) {
            boolean restInBook = order.getOrderType() == OrderType.LIMIT
                && order.getTimeInForce() != TimeInForce.IOC
                && order.getTimeInForce() != TimeInForce.FOK;

            if (restInBook) {
                // LIMIT / GTC / DAY → add to order book
                orderBookService.addOrderToBook(order);
            } else {
                // MARKET, IOC, or FOK remainder → cancel
                cancelOrderService.cancelRemainder(order);
            }
        }

        String fillSummary;
        if (matches.isEmpty()) {
            fillSummary = (order.getRemainingQty() > 0 && order.getStatus() == OrderStatus.PENDING) 
                ? "No fills; order resting in book." 
                : "No fills; order cancelled/rejected.";
        } else {
            fillSummary = matches.size() + " fill(s). Remaining: " + order.getRemainingQty() + 
                (order.getStatus() == OrderStatus.CANCELLED ? " (Remainder cancelled)" : "");
        }

        return OrderPlacementResult.success(orderId, fillSummary);
    }
}
