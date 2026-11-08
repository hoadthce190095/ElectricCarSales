package com.electriccarsales.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AdminUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String email;
    private String phone;
    private int roleId;
    private String role;
    private int statusId;
    private String status;
    private LocalDateTime createdDate;
    private Integer employeeId;
    private String employeeName;

    public AdminUser() {
    }

    public AdminUser(int userId, String username, String email, String phone, int roleId, String role,
                     int statusId, String status, LocalDateTime createdDate, Integer employeeId, String employeeName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.roleId = roleId;
        this.role = role;
        this.statusId = statusId;
        this.status = status;
        this.createdDate = createdDate;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
