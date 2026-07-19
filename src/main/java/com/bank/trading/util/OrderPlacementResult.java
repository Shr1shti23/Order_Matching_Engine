package com.bank.trading.util;

/**
 * Utility for holding order placement results.
 */
public class OrderPlacementResult {
    private final boolean success;
    private final String message;
    private final long orderId;

    private OrderPlacementResult(boolean success, String message, long orderId) {
        this.success = success;
        this.message = message;
        this.orderId = orderId;
    }

    public static OrderPlacementResult success(long orderId, String message) {
        return new OrderPlacementResult(true, message, orderId);
    }

    public static OrderPlacementResult failure(String message) {
        return new OrderPlacementResult(false, message, -1);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public long getOrderId() {
        return orderId;
    }
}
