package com.electriccarsales.model;

public class PaymentType {
    private int paymentTypeID;
    private String typeName;
    private String description;

    public PaymentType() {
    }

    public PaymentType(int paymentTypeID, String typeName, String description) {
        this.paymentTypeID = paymentTypeID;
        this.typeName = typeName;
        this.description = description;
    }

    public int getPaymentTypeID() { return paymentTypeID; }
    public void setPaymentTypeID(int paymentTypeID) { this.paymentTypeID = paymentTypeID; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
