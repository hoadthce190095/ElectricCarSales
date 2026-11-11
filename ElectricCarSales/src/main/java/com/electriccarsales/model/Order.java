package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order {
    private int id;
    private int customerId;
    private Integer employeeId;
    private int paymentMethodId;
    private BigDecimal originalAmount; // Tiền nghiệp vụ thực tế, TUYỆT ĐỐI KHÔNG GHI ĐÈ
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String status;
    private Timestamp orderDate;
    private String note;
    private String vehicleName;

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public int getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
