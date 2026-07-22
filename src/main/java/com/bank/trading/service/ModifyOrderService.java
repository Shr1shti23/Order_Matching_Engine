package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.AuditLogDao;
import com.bank.trading.dao.OrderDao;
import com.bank.trading.dao.OrderEventDao;
import com.bank.trading.dao.impl.AuditLogDaoImpl;
import com.bank.trading.dao.impl.OrderDaoImpl;
import com.bank.trading.dao.impl.OrderEventDaoImpl;
import com.bank.trading.engine.OrderNode;
import com.bank.trading.engine.PriceLevel;
import com.bank.trading.model.*;
import com.bank.trading.util.OrderPlacementResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Modifies an existing live order.
 *
 * <h2>Rules (from the design document)</h2>
 * <ul>
 *   <li><strong>Same price + reduced quantity</strong> — modify in place; preserve
 *       FIFO priority.</li>
 *   <li><strong>Any price change or quantity increase</strong> — cancel the existing
 *       order (loses priority) and submit a new one through {@link TradingService}.</li>
 * </ul>
 *
 * <p>In-place modifications are applied to the cache only after the DB update
 * commits.</p>
 */
public final class ModifyOrderService {

    private final CacheManager       cache;
    private final TradingService     tradingService;
    private final CancelOrderService cancelOrderService;
    @SuppressWarnings("unused")
    private final ReservationService reservationService;

    private final OrderDao      orderDao;
    private final OrderEventDao orderEventDao;
    private final AuditLogDao   auditLogDao;

    public ModifyOrderService(CacheManager cache,
                              TradingService tradingService,
                              CancelOrderService cancelOrderService,
                              ReservationService reservationService) {
        this.cache              = cache;
        this.tradingService     = tradingService;
        this.cancelOrderService = cancelOrderService;
        this.reservationService = reservationService;
        this.orderDao           = new OrderDaoImpl();
        this.orderEventDao      = new OrderEventDaoImpl();
        this.auditLogDao        = new AuditLogDaoImpl();
    }

    /**
     * Modify an order.
     *
     * @param orderId      the order to modify
     * @param newPrice     new limit price (null is not permitted for LIMIT orders)
     * @param newQty       new total quantity (must be > 0)
     * @param actorUserId  trader requesting the modification
     * @return result describing whether the modification succeeded
     */
    public OrderPlacementResult modify(long orderId, BigDecimal newPrice,
                                       long newQty, long actorUserId) {
        if (newQty <= 0) {
            return OrderPlacementResult.failure("New quantity must be positive.");
        }

        OrderNode node = cache.getOrderNode(orderId);
        if (node == null) {
            return OrderPlacementResult.failure(
                "Order " + orderId + " is not in the order book.");
        }

        Order order = node.getOrder();

        if (order.getOrderType() == OrderType.MARKET) {
            return OrderPlacementResult.failure(
                "MARKET orders cannot be modified.");
        }

        boolean samePrice    = newPrice != null && newPrice.compareTo(order.getPrice()) == 0;
        boolean reducedQty   = newQty < order.getRemainingQty();
        boolean inPlaceAllowed = samePrice && reducedQty;

        if (inPlaceAllowed) {
            return modifyInPlace(order, node, newQty, actorUserId);
        } else {
            return cancelAndResubmit(order, newPrice, newQty, actorUserId);
        }
    }

    // ------------------------------------------------------------------ //
    //  In-place modification (same price, reduced qty)                   //
    // ------------------------------------------------------------------ //

    private OrderPlacementResult modifyInPlace(Order order, OrderNode node,
                                                long newQty, long actorUserId) {
        long oldRemaining = order.getRemainingQty();
        long qtyDelta     = oldRemaining - newQty;   // positive: we're reducing

        // Persist first
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDao.updateStatus(order.getOrderId(), order.getStatus(), newQty, conn);

                OrderEvent event = new OrderEvent();
                event.setOrderId(order.getOrderId());
                event.setEventType(OrderEventType.MODIFIED);
                event.setPreviousStatus(order.getStatus());
                event.setNewStatus(order.getStatus());
                event.setQuantityChanged(-qtyDelta);   // negative = reduction
                event.setPrice(order.getPrice());
                event.setActorUserId(actorUserId);
                orderEventDao.insert(event, conn);

                AuditLog audit = new AuditLog();
                audit.setActorUserId(actorUserId);
                audit.setActionType(ActionType.UPDATE);
                audit.setEntityType("orders");
                audit.setEntityId(order.getOrderId());
                audit.setDetails("{\"modification\":\"in_place\",\"oldQty\":"
                    + oldRemaining + ",\"newQty\":" + newQty + "}");
                auditLogDao.insert(audit, conn);

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                return OrderPlacementResult.failure("In-place modify DB failed: " + ex.getMessage());
            }
        } catch (SQLException ex) {
            return OrderPlacementResult.failure("In-place modify connection error: " + ex.getMessage());
        }

        // Apply cache update after commit
        PriceLevel level = node.getPriceLevel();
        level.setTotalQuantity(level.getTotalQuantity() - qtyDelta);
        order.setRemainingQty(newQty);
        order.setOriginalQty(Math.min(order.getOriginalQty(), newQty));

        // Release the freed reservation (BUY only; SELL qty reservation reduced)
        if (order.getSide() == Side.BUY) {
            BigDecimal release = order.getPrice().multiply(BigDecimal.valueOf(qtyDelta));
            Wallet wallet = cache.getWallet(order.getClientId());
            if (wallet != null) {
                wallet.setReservedBalance(
                    wallet.getReservedBalance().subtract(release).max(BigDecimal.ZERO));
            }
        } else {
            com.bank.trading.model.Holding holding =
                cache.getHolding(order.getClientId(), order.getInstrumentId());
            if (holding != null) {
                holding.setReservedQuantity(
                    Math.max(0L, holding.getReservedQuantity() - qtyDelta));
            }
        }

        return OrderPlacementResult.success(order.getOrderId(),
            "Order modified in place. New remaining qty: " + newQty);
    }

    // ------------------------------------------------------------------ //
    //  Cancel + resubmit (price change or qty increase)                  //
    // ------------------------------------------------------------------ //

    private OrderPlacementResult cancelAndResubmit(Order original, BigDecimal newPrice,
                                                    long newQty, long actorUserId) {
        // Cancel the existing order (releases reservation, removes from book)
        cancelOrderService.cancel(original.getOrderId(), actorUserId);

        // Build a replacement order from the original
        Order replacement = new Order();
        replacement.setClientId(original.getClientId());
        replacement.setTraderId(original.getTraderId());
        replacement.setInstrumentId(original.getInstrumentId());
        replacement.setSymbol(original.getSymbol());
        replacement.setSide(original.getSide());
        replacement.setOrderType(original.getOrderType());
        replacement.setTimeInForce(original.getTimeInForce());
        replacement.setPrice(newPrice != null ? newPrice : original.getPrice());
        replacement.setOriginalQty(newQty);
        replacement.setRemainingQty(newQty);
        replacement.setStatus(OrderStatus.PENDING);

        return tradingService.placeOrder(replacement);
    }
}
