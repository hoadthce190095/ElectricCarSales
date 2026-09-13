/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Hoa Hà
 */
public class Cars {
    private int carID;
    private int modelID;
    private String vin;
    private String color;
    private int manufacturingYear;
    private String carStatus;

    public Cars() {
    }

    public Cars(int carID, int modelID, String vin, String color, int manufacturingYear, String carStatus) {
        this.carID = carID;
        this.modelID = modelID;
        this.vin = vin;
        this.color = color;
        this.manufacturingYear = manufacturingYear;
        this.carStatus = carStatus;
    }

    public int getCarID() {
        return carID;
    }

    public void setCarID(int carID) {
        this.carID = carID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(int manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public String getCarStatus() {
        return carStatus;
    }

    public void setCarStatus(String carStatus) {
        this.carStatus = carStatus;
    }

    @Override
    public String toString() {
        return "Cars{" + "carID=" + carID + ", modelID=" + modelID + ", vin=" + vin + ", color=" + color + ", manufacturingYear=" + manufacturingYear + ", carStatus=" + carStatus + '}';
    }
    
    
}
