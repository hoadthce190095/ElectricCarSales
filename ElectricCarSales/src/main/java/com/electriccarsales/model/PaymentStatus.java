package com.electriccarsales.model;

public class PaymentStatus {
    private int paymentStatusID;
    private String statusID;
    private String description;

    public PaymentStatus() {
    }

    public PaymentStatus(int paymentStatusID, String statusID, String description) {
        this.paymentStatusID = paymentStatusID;
        this.statusID = statusID;
        this.description = description;
    }

    public int getPaymentStatusID() { return paymentStatusID; }
    public void setPaymentStatusID(int paymentStatusID) { this.paymentStatusID = paymentStatusID; }
    public String getStatusID() { return statusID; }
    public void setStatusID(String statusID) { this.statusID = statusID; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
