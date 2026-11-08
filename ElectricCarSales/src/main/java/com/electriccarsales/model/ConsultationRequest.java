package com.electriccarsales.model;

import java.sql.Timestamp;

/** Request row joined with Customers, CarModels and ConsultationStatuses. */
public class ConsultationRequest {
    private int id;
    private int customerId;
    private int staffId;
    private int modelId;
    private int statusId;
    private String content;
    private String status;
    private Timestamp createdAt;
    private String customerName, customerPhone, customerEmail, modelName, employeeName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int v) { customerId = v; }
    public int getStaffId() { return staffId; }
    public void setStaffId(int v) { staffId = v; }
    public int getModelId() { return modelId; }
    public void setModelId(int v) { modelId = v; }
    public int getStatusId() { return statusId; }
    public void setStatusId(int v) { statusId = v; }
    public String getStatusName() { return status; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp v) { createdAt = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { customerName = v; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { customerPhone = v; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String v) { customerEmail = v; }
    public String getModelName() { return modelName; }
    public void setModelName(String v) { modelName = v; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String v) { employeeName = v; }
}
