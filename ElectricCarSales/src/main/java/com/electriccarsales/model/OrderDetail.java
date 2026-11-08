package com.electriccarsales.model;

import java.math.BigDecimal;

public class OrderDetail {
    private int id;
    private int orderId;
    private int vehicleId;
    private int quantity = 1;
    private BigDecimal unitPrice;

    // Joined vehicle display info
    private String carCode;
    private String modelName;
    private String versionName;
    private String colorName;
    private String vin;

    public OrderDetail() {
    }

    public OrderDetail(int id, int orderId, int vehicleId, BigDecimal unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.vehicleId = vehicleId;
        this.quantity = 1;
        this.unitPrice = unitPrice;
    }

    public int getOrderDetailId() {
        return id;
    }

    public void setOrderDetailId(int id) {
        this.id = id;
    }

    public BigDecimal getSubtotal() {
        return unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }

    public void setSubtotal(BigDecimal subtotal) {
    }

    public String getVehicleName() {
        return modelName;
    }

    public void setVehicleName(String vehicleName) {
        this.modelName = vehicleName;
    }

    public String getVehicleColor() {
        return colorName;
    }

    public void setVehicleColor(String vehicleColor) {
        this.colorName = vehicleColor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getCarId() {
        return vehicleId;
    }

    public void setCarId(int carId) {
        this.vehicleId = carId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSalesPrice() {
        return unitPrice;
    }

    public void setSalesPrice(BigDecimal salesPrice) {
        this.unitPrice = salesPrice;
    }

    public String getCarCode() {
        return carCode;
    }

    public void setCarCode(String carCode) {
        this.carCode = carCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }
}
