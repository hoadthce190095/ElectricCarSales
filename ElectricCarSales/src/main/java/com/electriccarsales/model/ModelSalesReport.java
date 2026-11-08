package com.electriccarsales.model;

import java.math.BigDecimal;

public class ModelSalesReport {
    private int modelId;
    private String modelName;
    private long vehiclesSold;
    private BigDecimal salesAmount;

    public ModelSalesReport() {
    }

    public ModelSalesReport(int modelId, String modelName, long vehiclesSold, BigDecimal salesAmount) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.vehiclesSold = vehiclesSold;
        this.salesAmount = salesAmount;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public long getVehiclesSold() {
        return vehiclesSold;
    }

    public void setVehiclesSold(long vehiclesSold) {
        this.vehiclesSold = vehiclesSold;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }
}
