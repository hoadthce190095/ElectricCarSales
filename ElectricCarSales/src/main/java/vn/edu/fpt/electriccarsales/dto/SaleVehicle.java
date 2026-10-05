package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;

public class SaleVehicle {
    private int carId;
    private String vin;
    private int modelId;
    private String modelName;
    private String versionName;
    private BigDecimal salesPrice;

    public SaleVehicle() {
    }

    public SaleVehicle(int carId, String vin, int modelId, String modelName, String versionName,
        BigDecimal salesPrice) {
        this.carId = carId;
        this.vin = vin;
        this.modelId = modelId;
        this.modelName = modelName;
        this.versionName = versionName;
        this.salesPrice = salesPrice;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
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

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public BigDecimal getSalesPrice() {
        return salesPrice;
    }

    public void setSalesPrice(BigDecimal salesPrice) {
        this.salesPrice = salesPrice;
    }
}
