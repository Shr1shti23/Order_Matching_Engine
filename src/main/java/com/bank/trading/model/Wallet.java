package com.bank.trading.model;

import java.math.BigDecimal;

/**
 * Represents a client's wallet for tracking cash balance.
 */
public class Wallet {
    private long walletId;
    private long clientId;
    private BigDecimal cashBalance;
    private BigDecimal reservedBalance;
    private String currency;
    private int version;

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public void setReservedBalance(BigDecimal reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public BigDecimal getAvailableBalance() {
        if (cashBalance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal reserved = reservedBalance != null ? reservedBalance : BigDecimal.ZERO;
        return cashBalance.subtract(reserved);
    }
}
