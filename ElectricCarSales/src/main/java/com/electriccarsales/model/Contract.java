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

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String employeeName;
    private String modelName;
    private String versionName;
    private String vin;
    private String paymentMethodName;
    private String companyName;
    private String orderStatus;

    public Contract() {}

    public Contract(int contractID, int orderID, int customerID, int employeeID, String contractNumber,
                    Timestamp contractDate, double contractAmount, String status, String note) {
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
    public int getContractId() { return contractID; }
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

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
}