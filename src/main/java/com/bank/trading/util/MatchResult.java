package com.bank.trading.util;

import java.math.BigDecimal;

/**
 * Utility for holding match results.
 */
public class MatchResult {
    private final long buyOrderId;
    private final long sellOrderId;
    private final BigDecimal executionPrice;
    private final long quantity;
    private final long restingOrderId;

    public MatchResult(long buyOrderId, long sellOrderId, BigDecimal executionPrice, long quantity, long restingOrderId) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.executionPrice = executionPrice;
        this.quantity = quantity;
        this.restingOrderId = restingOrderId;
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }

    public BigDecimal getExecutionPrice() {
        return executionPrice;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getRestingOrderId() {
        return restingOrderId;
    }
}
