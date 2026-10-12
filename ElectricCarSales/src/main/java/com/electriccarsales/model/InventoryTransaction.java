package com.electriccarsales.model;

import java.sql.Timestamp;

public class InventoryTransaction {
    private int inventoryTransactionID;
    private int carID;
    private int employeeID;
    private Integer supplierID;
    private String transactionType;
    private Timestamp transactionDate;
    private String note;

    public InventoryTransaction() {}

    public InventoryTransaction(int inventoryTransactionID, int carID, int employeeID, Integer supplierID, String transactionType, Timestamp transactionDate, String note) {
        this.inventoryTransactionID = inventoryTransactionID;
        this.carID = carID;
        this.employeeID = employeeID;
        this.supplierID = supplierID;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.note = note;
    }

    public int getInventoryTransactionID() { return inventoryTransactionID; }
    public void setInventoryTransactionID(int inventoryTransactionID) { this.inventoryTransactionID = inventoryTransactionID; }

    public int getCarID() { return carID; }
    public void setCarID(int carID) { this.carID = carID; }

    public int getEmployeeID() { return employeeID; }
    public void setEmployeeID(int employeeID) { this.employeeID = employeeID; }

    public Integer getSupplierID() { return supplierID; }
    public void setSupplierID(Integer supplierID) { this.supplierID = supplierID; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Timestamp getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Timestamp transactionDate) { this.transactionDate = transactionDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}