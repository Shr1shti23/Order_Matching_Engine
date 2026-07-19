package com.bank.trading.model;

import java.math.BigDecimal;

/**
 * Represents a transaction affecting a holding.
 */
public class HoldingTransaction {
    private long transactionId;
    private long clientId;
    private int instrumentId;
    private Long tradeId;
    private HoldingTxType transactionType;
    private long quantity;
    private BigDecimal price;

    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(int instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public HoldingTxType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(HoldingTxType transactionType) {
        this.transactionType = transactionType;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
