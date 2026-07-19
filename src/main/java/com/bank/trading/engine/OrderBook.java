package com.bank.trading.engine;

import com.bank.trading.model.Order;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Order book for a single instrument.
 *
 * buyBook  — descending by price (best bid = first entry)
 * sellBook — ascending  by price (best ask = first entry)
 *
 * Only TreeMap is used from java.util collections.
 * All FIFO queues within each price level are custom DoublyLinkedList / PriceLevel.
 */
public class OrderBook {
    private final String symbol;
    private final TreeMap<BigDecimal, PriceLevel> buyBook;
    private final TreeMap<BigDecimal, PriceLevel> sellBook;
    private final DoublyLinkedList listHelper;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyBook  = new TreeMap<>(Comparator.reverseOrder());
        this.sellBook = new TreeMap<>();
        this.listHelper = new DoublyLinkedList();
    }

    /**
     * Add an order to the appropriate side of the book.
     * Returns the OrderNode created, so the caller (CacheManager) can store it
     * in the global orderNodeMap for O(1) lookup.
     * O(log P) where P = number of distinct price levels.
     */
    public OrderNode addOrder(Order order) {
        TreeMap<BigDecimal, PriceLevel> book = (order.getSide() == com.bank.trading.model.Side.BUY) ? buyBook : sellBook;
        BigDecimal price = order.getPrice();
        PriceLevel level = book.computeIfAbsent(price, PriceLevel::new);
        OrderNode node = new OrderNode(order);
        listHelper.addToTail(level, node);
        return node;
    }

    /**
     * Remove an order from the book by its OrderNode.
     * The node must be looked up externally (via CacheManager.getOrderNode) before calling this.
     * Removes empty price levels.
     * O(1).
     */
    public void removeOrder(OrderNode node) {
        PriceLevel level = node.getPriceLevel();
        if (level == null) return;

        listHelper.remove(level, node);
        if (level.isEmpty()) {
            // remove from the correct side
            BigDecimal price = level.getPrice();
            buyBook.remove(price);
            sellBook.remove(price);
        }
    }

    /** Best bid (highest buy price). O(1). Returns null if buy book is empty. */
    public Map.Entry<BigDecimal, PriceLevel> getBestBid() {
        return buyBook.isEmpty() ? null : buyBook.firstEntry();
    }

    /** Best ask (lowest sell price). O(1). Returns null if sell book is empty. */
    public Map.Entry<BigDecimal, PriceLevel> getBestAsk() {
        return sellBook.isEmpty() ? null : sellBook.firstEntry();
    }

    public String getSymbol() { return symbol; }
    public TreeMap<BigDecimal, PriceLevel> getBuyBook() { return buyBook; }
    public TreeMap<BigDecimal, PriceLevel> getSellBook() { return sellBook; }
}
