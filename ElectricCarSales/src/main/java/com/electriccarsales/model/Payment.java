package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Payment {
    private int paymentID;
    private int orderID;
    private int paymentMethodID;
    private int paymentTypeID;
    private int paymentStatusID;
    private BigDecimal paymentAmount;
    private Timestamp paymentDate;
    private String referenceCode;
    private String note;
    private String paymentMethodName;
    private String paymentTypeName;
    private String paymentStatusCode;
    private String customerName;
    private BigDecimal orderTotalAmount;
    private BigDecimal orderRemainingAmount;

    public Payment() {
    }

    public Payment(int paymentID, int orderID, int paymentMethodID, int paymentTypeID,
                   int paymentStatusID, BigDecimal paymentAmount, Timestamp paymentDate,
                   String referenceCode, String note) {
        this.paymentID = paymentID;
        this.orderID = orderID;
        this.paymentMethodID = paymentMethodID;
        this.paymentTypeID = paymentTypeID;
        this.paymentStatusID = paymentStatusID;
        this.paymentAmount = paymentAmount;
        this.paymentDate = paymentDate;
        this.referenceCode = referenceCode;
        this.note = note;
    }

    public int getPaymentID() { return paymentID; }
    public int getPaymentId() { return paymentID; }
    public void setPaymentID(int paymentID) { this.paymentID = paymentID; }
    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }
    public int getPaymentMethodID() { return paymentMethodID; }
    public void setPaymentMethodID(int paymentMethodID) { this.paymentMethodID = paymentMethodID; }
    public int getPaymentTypeID() { return paymentTypeID; }
    public void setPaymentTypeID(int paymentTypeID) { this.paymentTypeID = paymentTypeID; }
    public int getPaymentStatusID() { return paymentStatusID; }
    public void setPaymentStatusID(int paymentStatusID) { this.paymentStatusID = paymentStatusID; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public Timestamp getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Timestamp paymentDate) { this.paymentDate = paymentDate; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public String getPaymentTypeName() { return paymentTypeName; }
    public String getType() { return paymentTypeName; }
    public void setPaymentTypeName(String paymentTypeName) { this.paymentTypeName = paymentTypeName; }
    public String getPaymentStatusCode() { return paymentStatusCode; }
    public void setPaymentStatusCode(String paymentStatusCode) { this.paymentStatusCode = paymentStatusCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getOrderTotalAmount() { return orderTotalAmount; }
    public void setOrderTotalAmount(BigDecimal orderTotalAmount) { this.orderTotalAmount = orderTotalAmount; }
    public BigDecimal getOrderRemainingAmount() { return orderRemainingAmount; }
    public void setOrderRemainingAmount(BigDecimal orderRemainingAmount) { this.orderRemainingAmount = orderRemainingAmount; }

    public String getOrderCode() {
        return "ECS" + orderID;
    }

    public String getFormattedPaymentAmount() {
        return com.electriccarsales.util.FormatUtils.formatMoney(paymentAmount);
    }

    public String getFormattedOrderTotalAmount() {
        return com.electriccarsales.util.FormatUtils.formatMoney(orderTotalAmount);
    }

    public String getFormattedOrderRemainingAmount() {
        return com.electriccarsales.util.FormatUtils.formatMoney(orderRemainingAmount);
    }

    public String getFormattedType() {
        if ("DEPOSIT".equalsIgnoreCase(paymentTypeName)) return "DEPOSIT_30";
        if ("REMAINING".equalsIgnoreCase(paymentTypeName)) return "REMAINING_70";
        return paymentTypeName != null ? paymentTypeName : "—";
    }

    public boolean isReconciled() {
        return note != null && note.contains("RECONCILED=true;");
    }

    public String getReconciledBy() {
        if (note == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("RECONCILED_BY=([^;]+);").matcher(note);
        return m.find() ? m.group(1) : null;
    }

    public String getReconciledAt() {
        if (note == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("RECONCILED_AT=([^;]+);").matcher(note);
        return m.find() ? m.group(1) : null;
    }
}
