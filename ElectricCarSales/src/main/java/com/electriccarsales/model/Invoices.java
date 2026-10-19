package com.electriccarsales.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Invoices {
    private int invoiceID;
    private int orderID;
    private int customerID;
    private int employeeID;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String status;
    private String note;

    public Invoices() {
    }

    public Invoices(int invoiceID, int orderID, int customerID, int employeeID, String invoiceNumber, LocalDateTime invoiceDate, BigDecimal subTotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal totalAmount, String status, String note) {
        this.invoiceID = invoiceID;
        this.orderID = orderID;
        this.customerID = customerID;
        this.employeeID = employeeID;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.subTotal = subTotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.status = status;
        this.note = note;
    }

    public int getInvoiceID() {
        return invoiceID;
    }

    public void setInvoiceID(int invoiceID) {
        this.invoiceID = invoiceID;
    }

    public int getOrderID() {
        return orderID;
    }

    public void setOrderID(int orderID) {
        this.orderID = orderID;
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }

    public int getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(int employeeID) {
        this.employeeID = employeeID;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "Invoices{" +
                "invoiceID=" + invoiceID +
                ", orderID=" + orderID +
                ", customerID=" + customerID +
                ", employeeID=" + employeeID +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", invoiceDate=" + invoiceDate +
                ", subTotal=" + subTotal +
                ", discountAmount=" + discountAmount +
                ", taxAmount=" + taxAmount +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
