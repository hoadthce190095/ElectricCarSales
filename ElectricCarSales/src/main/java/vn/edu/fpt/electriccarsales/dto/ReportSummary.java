package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;

public class ReportSummary {
    private BigDecimal revenue;
    private BigDecimal salesAmount;
    private long vehiclesSold;
    private long totalOrders;
    private long completedOrders;
    private long cancelledOrders;
    private BigDecimal outstandingAmount;
    private long completedPayments;
    private long contracts;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private long installmentContracts;
    private BigDecimal overdueAmount;
    private BigDecimal installmentReceipts;
    private long inconsistentOrders;

    public ReportSummary() {
    }

    public ReportSummary(BigDecimal revenue, BigDecimal salesAmount, long vehiclesSold, long totalOrders,
        long completedOrders, long cancelledOrders, BigDecimal outstandingAmount, long completedPayments,
        long contracts, BigDecimal discountAmount, BigDecimal taxAmount, long installmentContracts,
        BigDecimal overdueAmount, BigDecimal installmentReceipts, long inconsistentOrders) {
        this.revenue = revenue;
        this.salesAmount = salesAmount;
        this.vehiclesSold = vehiclesSold;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        this.outstandingAmount = outstandingAmount;
        this.completedPayments = completedPayments;
        this.contracts = contracts;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.installmentContracts = installmentContracts;
        this.overdueAmount = overdueAmount;
        this.installmentReceipts = installmentReceipts;
        this.inconsistentOrders = inconsistentOrders;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public long getVehiclesSold() {
        return vehiclesSold;
    }

    public void setVehiclesSold(long vehiclesSold) {
        this.vehiclesSold = vehiclesSold;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public long getCompletedPayments() {
        return completedPayments;
    }

    public void setCompletedPayments(long completedPayments) {
        this.completedPayments = completedPayments;
    }

    public long getContracts() {
        return contracts;
    }

    public void setContracts(long contracts) {
        this.contracts = contracts;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public long getInstallmentContracts() {
        return installmentContracts;
    }

    public void setInstallmentContracts(long installmentContracts) {
        this.installmentContracts = installmentContracts;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public BigDecimal getInstallmentReceipts() {
        return installmentReceipts;
    }

    public void setInstallmentReceipts(BigDecimal installmentReceipts) {
        this.installmentReceipts = installmentReceipts;
    }

    public long getInconsistentOrders() {
        return inconsistentOrders;
    }

    public void setInconsistentOrders(long inconsistentOrders) {
        this.inconsistentOrders = inconsistentOrders;
    }
}
