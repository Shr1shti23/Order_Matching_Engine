package com.bank.trading.model;

/**
 * Represents the assignment of a client to a trader.
 */
public class TraderClientAssignment {
    private long assignmentId;
    private long traderId;
    private long clientId;
    private long assignedBy;
    private boolean active;

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getTraderId() {
        return traderId;
    }

    public void setTraderId(long traderId) {
        this.traderId = traderId;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
