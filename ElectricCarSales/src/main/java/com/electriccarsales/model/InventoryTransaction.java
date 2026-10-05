package com.electriccarsales.model;

import java.sql.Timestamp;

public class InventoryTransaction {
    private int transactionID;
    private int carID;
    private String transactionType;
    private Timestamp transactionDate;
    private int employeeID;
    private String notes;

    public InventoryTransaction() {}

    public InventoryTransaction(int transactionID, int carID, String transactionType, Timestamp transactionDate, int employeeID, String notes) {
        this.transactionID = transactionID;
        this.carID = carID;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.employeeID = employeeID;
        this.notes = notes;
    }

    public int getTransactionID() { return transactionID; }
    public void setTransactionID(int transactionID) { this.transactionID = transactionID; }

    public int getCarID() { return carID; }
    public void setCarID(int carID) { this.carID = carID; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Timestamp getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Timestamp transactionDate) { this.transactionDate = transactionDate; }

    public int getEmployeeID() { return employeeID; }
    public void setEmployeeID(int employeeID) { this.employeeID = employeeID; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
