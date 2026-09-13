/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Hoa Hà
 */
public class OrderDetail {

    private int orderDetailID;
    private int orderID;
    private int carID;
    private int quantity;
    private double unitPrice;
    private double subTotal;

    private String vin;
    private String brandName;
    private String modelName;
    private String color;
    private int manufacturingYear;
    private double batteryCapacity;
    private int rangeKM;

    public OrderDetail() {
    }

    public OrderDetail(int orderDetailID, int orderID, int carID, int quantity, double unitPrice, double subTotal, String vin, String brandName, String modelName, String color, int manufacturingYear, double batteryCapacity, int rangeKM) {
        this.orderDetailID = orderDetailID;
        this.orderID = orderID;
        this.carID = carID;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subTotal = subTotal;
        this.vin = vin;
        this.brandName = brandName;
        this.modelName = modelName;
        this.color = color;
        this.manufacturingYear = manufacturingYear;
        this.batteryCapacity = batteryCapacity;
        this.rangeKM = rangeKM;
    }

    public int getOrderDetailID() {
        return orderDetailID;
    }

    public void setOrderDetailID(int orderDetailID) {
        this.orderDetailID = orderDetailID;
    }

    public int getOrderID() {
        return orderID;
    }

    public void setOrderID(int orderID) {
        this.orderID = orderID;
    }

    public int getCarID() {
        return carID;
    }

    public void setCarID(int carID) {
        this.carID = carID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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
    
    
}
