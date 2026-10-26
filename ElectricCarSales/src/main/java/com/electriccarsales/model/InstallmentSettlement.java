package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class InstallmentSettlement {
    private int settlementID;
    private int installmentContractID;
    private Timestamp settlementDate;
    private BigDecimal principalRemaining;
    private BigDecimal accruedInterest;
    private BigDecimal settlementFee;
    private BigDecimal settlementAmount;
    private int paymentMethodID;
    private int employeeID;
    private String referenceCode;
    private String note;

    public InstallmentSettlement() {
    }

    public InstallmentSettlement(int settlementID, int installmentContractID, Timestamp settlementDate,
                                 BigDecimal principalRemaining, BigDecimal accruedInterest,
                                 BigDecimal settlementFee, BigDecimal settlementAmount,
                                 int paymentMethodID, int employeeID, String referenceCode, String note) {
        this.settlementID = settlementID;
        this.installmentContractID = installmentContractID;
        this.settlementDate = settlementDate;
        this.principalRemaining = principalRemaining;
        this.accruedInterest = accruedInterest;
        this.settlementFee = settlementFee;
        this.settlementAmount = settlementAmount;
        this.paymentMethodID = paymentMethodID;
        this.employeeID = employeeID;
        this.referenceCode = referenceCode;
        this.note = note;
    }

    public int getSettlementID() { return settlementID; }
    public void setSettlementID(int settlementID) { this.settlementID = settlementID; }
    public int getInstallmentContractID() { return installmentContractID; }
    public void setInstallmentContractID(int installmentContractID) { this.installmentContractID = installmentContractID; }
    public Timestamp getSettlementDate() { return settlementDate; }
    public void setSettlementDate(Timestamp settlementDate) { this.settlementDate = settlementDate; }
    public BigDecimal getPrincipalRemaining() { return principalRemaining; }
    public void setPrincipalRemaining(BigDecimal principalRemaining) { this.principalRemaining = principalRemaining; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public BigDecimal getSettlementFee() { return settlementFee; }
    public void setSettlementFee(BigDecimal settlementFee) { this.settlementFee = settlementFee; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }
    public int getPaymentMethodID() { return paymentMethodID; }
    public void setPaymentMethodID(int paymentMethodID) { this.paymentMethodID = paymentMethodID; }
    public int getEmployeeID() { return employeeID; }
    public void setEmployeeID(int employeeID) { this.employeeID = employeeID; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
