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
import com.bank.trading.model.*;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cancels a live resting order or marks an order as REJECTED.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>O(1) lookup in the global {@code orderNodeMap}.</li>
 *   <li>Remove the node from its price level's doubly-linked list (O(1)).</li>
 *   <li>Remove empty price levels from the TreeMap.</li>
 *   <li>Release the fund / holding reservation.</li>
 *   <li>Persist: UPDATE orders, INSERT order_event, INSERT audit_log — all in
 *       one transaction.</li>
 * </ol>
 *
 * <p>Cache is only modified after the DB commit.</p>
 */
public final class CancelOrderService {

    private final CacheManager       cache;
    private final ReservationService reservationService;
    private final OrderBookService   orderBookService;

    private final OrderDao     orderDao;
    private final OrderEventDao orderEventDao;
    private final AuditLogDao  auditLogDao;

    public CancelOrderService(CacheManager cache,
                              ReservationService reservationService,
                              OrderBookService orderBookService) {
        this.cache              = cache;
        this.reservationService = reservationService;
        this.orderBookService   = orderBookService;
        this.orderDao           = new OrderDaoImpl();
        this.orderEventDao      = new OrderEventDaoImpl();
        this.auditLogDao        = new AuditLogDaoImpl();
    }

    /**
     * Cancel a live order by ID.  The order must currently be resting in the
     * order book (PENDING or PARTIALLY_FILLED).
     *
     * @param orderId     the order to cancel
     * @param actorUserId the user requesting the cancellation (for audit trail)
     * @throws IllegalArgumentException if the order is not in the book
     */
    public void cancel(long orderId, long actorUserId) {
        OrderNode node = cache.getOrderNode(orderId);
        if (node == null) {
            throw new IllegalArgumentException(
                "Order " + orderId + " is not in the order book (already filled, cancelled, or unknown).");
        }
        Order order = node.getOrder();
        persistCancellation(order, OrderStatus.CANCELLED, actorUserId);
        // Only mutate cache after successful DB commit:
        applyCancel(order, orderId);
    }

    /**
     * Mark an order as REJECTED (called when validation fails after insertion,
     * or for FOK orders that cannot be fully filled).  The order is NOT in the
     * order book at this point.
     *
     * @param order the newly inserted order to reject
     */
    public void reject(Order order) {
        persistCancellation(order, OrderStatus.REJECTED, order.getTraderId());
        // Release any reservation that may have been set before the reject decision.
        reservationService.release(order);
        order.setStatus(OrderStatus.REJECTED);
    }

    /**
     * Cancel only the remaining quantity of a partially-matched order.
     * Used for MARKET and IOC orders whose remainder cannot rest in the book.
     *
     * @param order the partially-filled incoming order
     */
    public void cancelRemainder(Order order) {
        if (order.getRemainingQty() == 0) return;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderStatus newStatus = OrderStatus.CANCELLED;

                orderDao.updateStatus(order.getOrderId(), newStatus,
                    order.getRemainingQty(), conn);

                insertCancelEvent(order, newStatus, order.getTraderId(), conn);
                insertCancelAudit(order, order.getTraderId(), conn);

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                throw new RuntimeException("cancelRemainder failed: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("cancelRemainder DB connection error: " + ex.getMessage(), ex);
        }

        // Release the unfilled reservation portion
        reservationService.release(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setRemainingQty(0);
    }

    /**
     * Called by {@link ExpiryService} for orders that expire end-of-day.
     * Behaves identically to {@link #cancel} but uses a system actor (null).
     */
    public void expire(Order order) {
        if (!cache.hasOpenOrder(order.getOrderId())) {
            return;  // already removed (concurrent cancel — defensive only)
        }
        persistCancellation(order, OrderStatus.CANCELLED, -1L);
        applyExpiry(order);
    }

    // ------------------------------------------------------------------ //
    //  Private persistence                                                //
    // ------------------------------------------------------------------ //

    private void persistCancellation(Order order, OrderStatus newStatus, long actorUserId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDao.updateStatus(order.getOrderId(), newStatus,
                    order.getRemainingQty(), conn);
                insertCancelEvent(order, newStatus, actorUserId, conn);
                insertCancelAudit(order, actorUserId, conn);
                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                throw new RuntimeException("Cancellation DB failed: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Cancellation DB connection error: " + ex.getMessage(), ex);
        }
    }

    private void insertCancelEvent(Order order, OrderStatus newStatus,
                                   long actorUserId, Connection conn) throws SQLException {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getOrderId());
        event.setEventType(newStatus == OrderStatus.REJECTED
            ? OrderEventType.REJECTED : OrderEventType.CANCELLED);
        event.setPreviousStatus(order.getStatus());
        event.setNewStatus(newStatus);
        event.setQuantityChanged(order.getRemainingQty());
        event.setActorUserId(actorUserId > 0 ? actorUserId : null);
        orderEventDao.insert(event, conn);
    }

    private void insertCancelAudit(Order order, long actorUserId,
                                   Connection conn) throws SQLException {
        AuditLog audit = new AuditLog();
        audit.setActorUserId(actorUserId > 0 ? actorUserId : null);
        audit.setActionType(ActionType.ORDER_CANCELLED);
        audit.setEntityType("orders");
        audit.setEntityId(order.getOrderId());
        audit.setDetails("{\"symbol\":\"" + order.getSymbol()
            + "\",\"remainingQty\":" + order.getRemainingQty() + "}");
        auditLogDao.insert(audit, conn);
    }

    // ------------------------------------------------------------------ //
    //  Cache mutations (post-commit)                                      //
    // ------------------------------------------------------------------ //

    private void applyCancel(Order order, long orderId) {
        reservationService.release(order);
        orderBookService.removeOrderFromBook(orderId);
        order.setStatus(OrderStatus.CANCELLED);
    }

    private void applyExpiry(Order order) {
        reservationService.release(order);
        orderBookService.removeOrderFromBook(order.getOrderId());
        order.setStatus(OrderStatus.CANCELLED);
    }
}
