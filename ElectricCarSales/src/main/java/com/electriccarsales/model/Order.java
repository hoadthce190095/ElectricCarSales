package com.electriccarsales.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order {
    private int id;
    private int customerId;
    private BigDecimal originalAmount; // Tiền nghiệp vụ thực tế, TUYỆT ĐỐI KHÔNG GHI ĐÈ
    private String status;
    private Timestamp orderDate;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
}