package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SaleReport {
    private int orderId;
    private LocalDateTime orderDate;
    private String status;
    private String customerName;
    private String employeeName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String contractNumber;
    private String invoiceNumber;
    private String deliveryStatus;
    private boolean eligible;
    private List<SaleVehicle> vehicles;

    public SaleReport() {
    }

    public SaleReport(int orderId, LocalDateTime orderDate, String status, String customerName, String employeeName,
        BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingAmount, String contractNumber,
        String invoiceNumber, String deliveryStatus, boolean eligible, List<SaleVehicle> vehicles) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.status = status;
        this.customerName = customerName;
        this.employeeName = employeeName;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.remainingAmount = remainingAmount;
        this.contractNumber = contractNumber;
        this.invoiceNumber = invoiceNumber;
        this.deliveryStatus = deliveryStatus;
        this.eligible = eligible;
        this.vehicles = vehicles;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<SaleVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<SaleVehicle> vehicles) {
        this.vehicles = vehicles;
    }
}
