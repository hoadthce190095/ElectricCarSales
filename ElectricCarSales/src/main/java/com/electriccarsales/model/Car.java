package com.electriccarsales.model;

public class Car {
    private int carID;
    private String carCode;
    private int modelID;
    private int versionID;
    private int colorID;
    private int carStatusID;
    private String origin;
    private int year;
    private String vin;
    private String engineNumber;
    private String lotCode;
    private double price;

    public Car() {}

    public Car(int carID, String carCode, int modelID, int versionID, int colorID, int carStatusID, String origin, int year, String vin, String engineNumber, String lotCode, double price) {
        this.carID = carID;
        this.carCode = carCode;
        this.modelID = modelID;
        this.versionID = versionID;
        this.colorID = colorID;
        this.carStatusID = carStatusID;
        this.origin = origin;
        this.year = year;
        this.vin = vin;
        this.engineNumber = engineNumber;
        this.lotCode = lotCode;
        this.price = price;
    }

    public int getCarID() { return carID; }
    public void setCarID(int carID) { this.carID = carID; }

    public String getCarCode() { return carCode; }
    public void setCarCode(String carCode) { this.carCode = carCode; }

    public int getModelID() { return modelID; }
    public void setModelID(int modelID) { this.modelID = modelID; }

    public int getVersionID() { return versionID; }
    public void setVersionID(int versionID) { this.versionID = versionID; }

    public int getColorID() { return colorID; }
    public void setColorID(int colorID) { this.colorID = colorID; }

    public int getCarStatusID() { return carStatusID; }
    public void setCarStatusID(int carStatusID) { this.carStatusID = carStatusID; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}