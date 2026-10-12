package com.electriccarsales.model;

import java.sql.Timestamp;

public class Contract {
    private int contractID;
    private int orderID;
    private int customerID;
    private int employeeID;
    private String contractNumber;
    private Timestamp contractDate;
    private double contractAmount;
    private String status;
    private String note;

    public Contract() {}

    public Contract(int contractID, int orderID, int customerID, int employeeID, String contractNumber, Timestamp contractDate, double contractAmount, String status, String note) {
        this.contractID = contractID;
        this.orderID = orderID;
        this.customerID = customerID;
        this.employeeID = employeeID;
        this.contractNumber = contractNumber;
        this.contractDate = contractDate;
        this.contractAmount = contractAmount;
        this.status = status;
        this.note = note;
    }

    public int getContractID() { return contractID; }
    public void setContractID(int contractID) { this.contractID = contractID; }

    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }

    public int getCustomerID() { return customerID; }
    public void setCustomerID(int customerID) { this.customerID = customerID; }

    public int getEmployeeID() { return employeeID; }
    public void setEmployeeID(int employeeID) { this.employeeID = employeeID; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public Timestamp getContractDate() { return contractDate; }
    public void setContractDate(Timestamp contractDate) { this.contractDate = contractDate; }

    public double getContractAmount() { return contractAmount; }
    public void setContractAmount(double contractAmount) { this.contractAmount = contractAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}