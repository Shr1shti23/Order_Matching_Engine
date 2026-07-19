package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.engine.OrderBook;
import com.bank.trading.engine.OrderNode;
import com.bank.trading.model.Order;

/**
 * Bridges the {@link OrderBook} data structure and the {@link CacheManager}.
 *
 * <p>The order book's {@code addOrder} method returns the newly created
 * {@link OrderNode}.  This service registers that node into the global
 * {@code orderNodeMap} so that cancel and modify operations can find it
 * in O(1) time.</p>
 *
 * <p>Removal keeps the price level clean: empty levels are pruned from the
 * TreeMap automatically by {@link OrderBook#removeOrder(OrderNode)}.</p>
 *
 * <p>No database access.  No business logic.</p>
 */
public final class OrderBookService {

    private final CacheManager cache;

    public OrderBookService(CacheManager cache) {
        this.cache = cache;
    }

    /**
     * Add a resting order to its instrument's order book and register the
     * resulting node for O(1) lookup.
     *
     * @param order the order to add (must have a non-null price — MARKET orders
     *              must never call this method)
     */
    public void addOrderToBook(Order order) {
        if (order.getPrice() == null) {
            throw new IllegalArgumentException(
                "MARKET orders must not rest in the order book. orderId=" + order.getOrderId());
        }
        OrderBook book = cache.getOrderBook(order.getSymbol());
        if (book == null) {
            throw new IllegalStateException(
                "No order book found for symbol " + order.getSymbol());
        }
        OrderNode node = book.addOrder(order);
        cache.putOrderNode(order.getOrderId(), node);
    }

    /**
     * Remove an order from its instrument's order book and deregister its node.
     * Empty price levels are removed automatically.
     *
     * @param orderId the ID of the order to remove
     * @return {@code true} if the order was found and removed;
     *         {@code false} if it was not in the book (already filled / never rested)
     */
    public boolean removeOrderFromBook(long orderId) {
        OrderNode node = cache.getOrderNode(orderId);
        if (node == null) {
            return false;
        }
        OrderBook book = cache.getOrderBook(node.getOrder().getSymbol());
        if (book != null) {
            book.removeOrder(node);
        }
        cache.removeOrderNode(orderId);
        return true;
    }

    /**
     * Check whether an order is currently resting in the book.
     */
    public boolean isInBook(long orderId) {
        return cache.hasOpenOrder(orderId);
    }
}
