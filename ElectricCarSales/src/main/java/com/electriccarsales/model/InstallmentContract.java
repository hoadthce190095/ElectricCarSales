package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class InstallmentContract {
    private int installmentContractID;
    private int orderID;
    private int companyID;
    private int planID;
    private BigDecimal vehiclePrice;
    private BigDecimal downPayment;
    private BigDecimal loanAmount;
    private int loanTermMonths;
    private BigDecimal interestRate;
    private BigDecimal totalInterest;
    private BigDecimal totalInstallmentAmount;
    private BigDecimal periodicPayment;
    private Timestamp contractDate;
    private String status;
    private String companyName;
    private String planName;
    private String customerName;

    public InstallmentContract() {
    }

    public InstallmentContract(int installmentContractID, int orderID, int companyID, int planID,
                               BigDecimal vehiclePrice, BigDecimal downPayment, BigDecimal loanAmount,
                               int loanTermMonths, BigDecimal interestRate, BigDecimal totalInterest,
                               BigDecimal totalInstallmentAmount, BigDecimal periodicPayment,
                               Timestamp contractDate, String status) {
        this.installmentContractID = installmentContractID;
        this.orderID = orderID;
        this.companyID = companyID;
        this.planID = planID;
        this.vehiclePrice = vehiclePrice;
        this.downPayment = downPayment;
        this.loanAmount = loanAmount;
        this.loanTermMonths = loanTermMonths;
        this.interestRate = interestRate;
        this.totalInterest = totalInterest;
        this.totalInstallmentAmount = totalInstallmentAmount;
        this.periodicPayment = periodicPayment;
        this.contractDate = contractDate;
        this.status = status;
    }

    public int getInstallmentContractID() { return installmentContractID; }
    public void setInstallmentContractID(int installmentContractID) { this.installmentContractID = installmentContractID; }
    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }
    public int getCompanyID() { return companyID; }
    public void setCompanyID(int companyID) { this.companyID = companyID; }
    public int getPlanID() { return planID; }
    public void setPlanID(int planID) { this.planID = planID; }
    public BigDecimal getVehiclePrice() { return vehiclePrice; }
    public void setVehiclePrice(BigDecimal vehiclePrice) { this.vehiclePrice = vehiclePrice; }
    public BigDecimal getDownPayment() { return downPayment; }
    public void setDownPayment(BigDecimal downPayment) { this.downPayment = downPayment; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
    public int getLoanTermMonths() { return loanTermMonths; }
    public void setLoanTermMonths(int loanTermMonths) { this.loanTermMonths = loanTermMonths; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
    public BigDecimal getTotalInstallmentAmount() { return totalInstallmentAmount; }
    public void setTotalInstallmentAmount(BigDecimal totalInstallmentAmount) { this.totalInstallmentAmount = totalInstallmentAmount; }
    public BigDecimal getPeriodicPayment() { return periodicPayment; }
    public void setPeriodicPayment(BigDecimal periodicPayment) { this.periodicPayment = periodicPayment; }
    public Timestamp getContractDate() { return contractDate; }
    public void setContractDate(Timestamp contractDate) { this.contractDate = contractDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
