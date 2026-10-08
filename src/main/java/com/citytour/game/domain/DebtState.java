package com.citytour.game.domain;

public class DebtState {
    private String debtorId;
    private String creditorId;
    private int amount;
    private String reason;

    public DebtState() {
    }

    public DebtState(String debtorId, String creditorId, int amount, String reason) {
        this.debtorId = debtorId;
        this.creditorId = creditorId;
        this.amount = amount;
        this.reason = reason;
    }

    public String getDebtorId() { return debtorId; }
    public void setDebtorId(String debtorId) { this.debtorId = debtorId; }
    public String getCreditorId() { return creditorId; }
    public void setCreditorId(String creditorId) { this.creditorId = creditorId; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
