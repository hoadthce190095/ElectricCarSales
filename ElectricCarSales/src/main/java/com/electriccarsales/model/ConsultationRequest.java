package com.electriccarsales.model;

import java.sql.Timestamp;

public class ConsultationRequest {
    private int id;
    private int customerId;
    private int staffId;
    private String content;
    private String status;
    private Timestamp createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}