package com.electriccarsales.model;

import java.sql.Timestamp;

public class OrderStatusHistory {
    private int id;
    private int orderId;
    private int orderStatusId;
    private Integer employeeId;
    private String oldStatus;
    private String newStatus;
    private String status;
    private String statusName;
    private String changedBy;
    private Integer changedById;
    private String changedByName;
    private Timestamp changedAt;
    private String note;

    public OrderStatusHistory() {
    }

    public int getHistoryId() {
        return id;
    }

    public void setHistoryId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getOrderStatusId() {
        return orderStatusId;
    }

    public void setOrderStatusId(int orderStatusId) {
        this.orderStatusId = orderStatusId;
        if (this.status == null && orderStatusId > 0) {
            this.status = String.valueOf(orderStatusId);
        }
    }

    public int getStatusId() {
        return orderStatusId;
    }

    public void setStatusId(int statusId) {
        setOrderStatusId(statusId);
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
        this.changedById = employeeId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus != null ? newStatus : status;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
        if (this.status == null) {
            this.status = newStatus;
        }
    }

    public String getStatus() {
        return status != null ? status : newStatus;
    }

    public void setStatus(String status) {
        this.status = status;
        if (this.newStatus == null) {
            this.newStatus = status;
        }
        if (status != null) {
            try {
                this.orderStatusId = Integer.parseInt(status.trim());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public String getStatusName() {
        return statusName != null ? statusName : (newStatus != null ? newStatus : status);
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedById = changedBy;
        this.employeeId = changedBy;
        if (changedBy != null) {
            this.changedBy = String.valueOf(changedBy);
        }
    }

    public Integer getChangedById() {
        return changedById != null ? changedById : employeeId;
    }

    public void setChangedById(Integer changedById) {
        this.changedById = changedById;
        this.employeeId = changedById;
    }

    public String getChangedByName() {
        return changedByName != null ? changedByName : changedBy;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp changedAt) {
        this.changedAt = changedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
