package com.bank.trading.engine;

import java.math.BigDecimal;

/** All resting orders at a single price, maintained in FIFO order. */
public class PriceLevel {
    private final BigDecimal price;
    private OrderNode head;      // oldest order (first to match)
    private OrderNode tail;      // newest order
    private int orderCount;
    private long totalQuantity;  // sum of remainingQty across all orders

    public PriceLevel(BigDecimal price) {
        this.price = price;
        this.head = null;
        this.tail = null;
        this.orderCount = 0;
        this.totalQuantity = 0;
    }

    /** Append an order to the tail (newest). O(1). */
    public void addToTail(OrderNode node) {
        node.setPriceLevel(this);
        if (tail == null) {
            head = node;
            tail = node;
            node.setPrev(null);
            node.setNext(null);
        } else {
            tail.setNext(node);
            node.setPrev(tail);
            node.setNext(null);
            tail = node;
        }
        orderCount++;
        totalQuantity += node.getOrder().getRemainingQty();
    }

    /** Remove a specific node. O(1). */
    public void remove(OrderNode node) {
        if (node.getPrev() != null) {
            node.getPrev().setNext(node.getNext());
        } else {
            head = node.getNext();
        }

        if (node.getNext() != null) {
            node.getNext().setPrev(node.getPrev());
        } else {
            tail = node.getPrev();
        }

        node.setPrev(null);
        node.setNext(null);
        node.setPriceLevel(null);
        orderCount--;
        totalQuantity -= node.getOrder().getRemainingQty();
    }

    public boolean isEmpty() { 
        return orderCount == 0; 
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public OrderNode getHead() {
        return head;
    }
    
    public void setHead(OrderNode head) {
        this.head = head;
    }
    
    public OrderNode getTail() {
        return tail;
    }
    
    public void setTail(OrderNode tail) {
        this.tail = tail;
    }
    
    public int getOrderCount() {
        return orderCount;
    }
    
    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }
    
    public long getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
}
