package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class InstallmentPayment {
    private int installmentPaymentID;
    private int scheduleID;
    private BigDecimal paymentAmount;
    private Timestamp paymentDate;
    private int paymentMethodID;
    private int paymentStatusID;
    private String referenceCode;
    private String note;

    public InstallmentPayment() {
    }

    public InstallmentPayment(int installmentPaymentID, int scheduleID, BigDecimal paymentAmount,
                              Timestamp paymentDate, int paymentMethodID, int paymentStatusID,
                              String referenceCode, String note) {
        this.installmentPaymentID = installmentPaymentID;
        this.scheduleID = scheduleID;
        this.paymentAmount = paymentAmount;
        this.paymentDate = paymentDate;
        this.paymentMethodID = paymentMethodID;
        this.paymentStatusID = paymentStatusID;
        this.referenceCode = referenceCode;
        this.note = note;
    }

    public int getInstallmentPaymentID() { return installmentPaymentID; }
    public void setInstallmentPaymentID(int installmentPaymentID) { this.installmentPaymentID = installmentPaymentID; }
    public int getScheduleID() { return scheduleID; }
    public void setScheduleID(int scheduleID) { this.scheduleID = scheduleID; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public Timestamp getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Timestamp paymentDate) { this.paymentDate = paymentDate; }
    public int getPaymentMethodID() { return paymentMethodID; }
    public void setPaymentMethodID(int paymentMethodID) { this.paymentMethodID = paymentMethodID; }
    public int getPaymentStatusID() { return paymentStatusID; }
    public void setPaymentStatusID(int paymentStatusID) { this.paymentStatusID = paymentStatusID; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
