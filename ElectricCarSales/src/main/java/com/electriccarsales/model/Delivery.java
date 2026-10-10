package com.electriccarsales.model;

import java.sql.Timestamp;

public class Delivery {
    private int deliveryID;
    private int orderID;
    private int customerID;
    private int employeeID;
    private int carID;
    private Timestamp deliveryDate;
    private String deliveryAddress;
    private String deliveryStatus;
    private String note;

    public Delivery() {}

    public Delivery(int deliveryID, int orderID, int customerID, int employeeID, int carID, Timestamp deliveryDate, String deliveryAddress, String deliveryStatus, String note) {
        this.deliveryID = deliveryID;
        this.orderID = orderID;
        this.customerID = customerID;
        this.employeeID = employeeID;
        this.carID = carID;
        this.deliveryDate = deliveryDate;
        this.deliveryAddress = deliveryAddress;
        this.deliveryStatus = deliveryStatus;
        this.note = note;
    }

    public int getDeliveryID() { return deliveryID; }
    public void setDeliveryID(int deliveryID) { this.deliveryID = deliveryID; }
    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }
    public int getCustomerID() { return customerID; }
    public void setCustomerID(int customerID) { this.customerID = customerID; }
    public int getEmployeeID() { return employeeID; }
    public void setEmployeeID(int employeeID) { this.employeeID = employeeID; }
    public int getCarID() { return carID; }
    public void setCarID(int carID) { this.carID = carID; }
    public Timestamp getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Timestamp deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
