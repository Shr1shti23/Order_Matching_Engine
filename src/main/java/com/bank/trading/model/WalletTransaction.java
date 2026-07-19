package com.bank.trading.model;

import java.math.BigDecimal;

/**
 * Represents a transaction affecting a wallet.
 */
public class WalletTransaction {
    private long transactionId;
    private long walletId;
    private WalletTxType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Long tradeId;
    private String reference;

    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public WalletTxType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(WalletTxType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
