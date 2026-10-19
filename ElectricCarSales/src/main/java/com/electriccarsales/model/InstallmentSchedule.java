package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Date;

public class InstallmentSchedule {
    private int scheduleID;
    private int installmentContractID;
    private int periodNumber;
    private Date dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal paymentAmount;
    private BigDecimal paidAmount;
    private String status;

    public InstallmentSchedule() {
    }

    public InstallmentSchedule(int scheduleID, int installmentContractID, int periodNumber,
                               Date dueDate, BigDecimal principalAmount, BigDecimal interestAmount,
                               BigDecimal paymentAmount, BigDecimal paidAmount, String status) {
        this.scheduleID = scheduleID;
        this.installmentContractID = installmentContractID;
        this.periodNumber = periodNumber;
        this.dueDate = dueDate;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.paymentAmount = paymentAmount;
        this.paidAmount = paidAmount;
        this.status = status;
    }

    public int getScheduleID() { return scheduleID; }
    public void setScheduleID(int scheduleID) { this.scheduleID = scheduleID; }
    public int getInstallmentContractID() { return installmentContractID; }
    public void setInstallmentContractID(int installmentContractID) { this.installmentContractID = installmentContractID; }
    public int getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
