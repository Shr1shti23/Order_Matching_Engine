package com.bank.trading.engine;

import com.bank.trading.model.Order;

/** Node in the doubly-linked list at a PriceLevel. One node per live order. */
public class OrderNode {
    private Order order;
    private OrderNode prev;
    private OrderNode next;
    private PriceLevel priceLevel;

    public OrderNode(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderNode getPrev() {
        return prev;
    }

    public void setPrev(OrderNode prev) {
        this.prev = prev;
    }

    public OrderNode getNext() {
        return next;
    }

    public void setNext(OrderNode next) {
        this.next = next;
    }

    public PriceLevel getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel;
    }
}
