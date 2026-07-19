package com.bank.trading.model;

import java.math.BigDecimal;

/**
 * Represents an event associated with an order.
 */
public class OrderEvent {
    private long eventId;
    private long orderId;
    private OrderEventType eventType;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private Long quantityChanged;
    private BigDecimal price;
    private Long actorUserId;
    private String details;

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public OrderEventType getEventType() {
        return eventType;
    }

    public void setEventType(OrderEventType eventType) {
        this.eventType = eventType;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Long getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(Long quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
