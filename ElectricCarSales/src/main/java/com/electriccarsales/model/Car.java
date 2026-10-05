package com.electriccarsales.model;

public class Car {
    private int carID;
    private String vin;
    private int modelID;
    private int supplierID;
    private int statusID;
    private double importPrice;
    private String notes;

    public Car() {}

    public Car(int carID, String vin, int modelID, int supplierID, int statusID, double importPrice, String notes) {
        this.carID = carID;
        this.vin = vin;
        this.modelID = modelID;
        this.supplierID = supplierID;
        this.statusID = statusID;
        this.importPrice = importPrice;
        this.notes = notes;
    }

    public int getCarID() { return carID; }
    public void setCarID(int carID) { this.carID = carID; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public int getModelID() { return modelID; }
    public void setModelID(int modelID) { this.modelID = modelID; }

    public int getSupplierID() { return supplierID; }
    public void setSupplierID(int supplierID) { this.supplierID = supplierID; }

    public int getStatusID() { return statusID; }
    public void setStatusID(int statusID) { this.statusID = statusID; }

    public double getImportPrice() { return importPrice; }
    public void setImportPrice(double importPrice) { this.importPrice = importPrice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
