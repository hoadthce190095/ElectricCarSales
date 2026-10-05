package com.electriccarsales.model;

import java.sql.Timestamp;

public class Delivery {
    private int deliveryID;
    private int orderID;
    private int employeeID;
    private Timestamp deliveryDate;
    private String deliveryAddress;
    private String status;
    private String notes;

    public Delivery() {}

    public Delivery(int deliveryID, int orderID, int employeeID, Timestamp deliveryDate, String deliveryAddress, String status, String notes) {
        this.deliveryID = deliveryID;
        this.orderID = orderID;
        this.employeeID = employeeID;
        this.deliveryDate = deliveryDate;
        this.deliveryAddress = deliveryAddress;
        this.status = status;
        this.notes = notes;
    }


    public void setDeliveryID(int deliveryID) { this.deliveryID = deliveryID; }
    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }
    public int getEmployeeID() { return employeeID; }
    public void setEmployeeID(int employeeID) { this.employeeID = employeeID; }
    public Timestamp getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Timestamp deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
