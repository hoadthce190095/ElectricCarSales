/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Hoa Hà
 */
public class CarModels {
    private int modelID;
    private int brandID;
    private String modelName;
    private double batteryCapacity;
    private int rangeKM;
    private double price;
    private String description;

    public CarModels() {
    }

    public CarModels(int modelID, int brandID, String modelName, double batteryCapacity, int rangeKM, double price, String description) {
        this.modelID = modelID;
        this.brandID = brandID;
        this.modelName = modelName;
        this.batteryCapacity = batteryCapacity;
        this.rangeKM = rangeKM;
        this.price = price;
        this.description = description;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public int getBrandID() {
        return brandID;
    }

    public void setBrandID(int brandID) {
        this.brandID = brandID;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public int getRangeKM() {
        return rangeKM;
    }

    public void setRangeKM(int rangeKM) {
        this.rangeKM = rangeKM;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    
    
}
