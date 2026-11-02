package com.electriccarsales.model;

public class InventoryCarDetail {
    private String vin;
    private String engineNumber;
    private String color;
    private String status;
    private double price;

    public InventoryCarDetail() {}

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
