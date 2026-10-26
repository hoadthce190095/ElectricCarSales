package com.electriccarsales.model;

import java.math.BigDecimal;

/**
 * Verified specification fields from the vehicle catalogue; unavailable values remain null.
 */
public class VehicleSpecifications {
    private String dimensions;
    private BigDecimal batteryCapacity;
    private String wheelType;
    private BigDecimal energyConsumption;
    private BigDecimal rangeKM;
    private BigDecimal maxPower;
    private BigDecimal maxSpeed;
    private Integer seatingCapacity;
    private BigDecimal chargingTime;
    private Integer airbagCount;
    private String description;

    public VehicleSpecifications() {
    }

    public VehicleSpecifications(String dimensions, BigDecimal batteryCapacity, String wheelType,
        BigDecimal energyConsumption, BigDecimal rangeKM, BigDecimal maxPower, BigDecimal maxSpeed,
        Integer seatingCapacity, BigDecimal chargingTime, Integer airbagCount, String description) {
        setDimensions(dimensions);
        setBatteryCapacity(batteryCapacity);
        setWheelType(wheelType);
        setEnergyConsumption(energyConsumption);
        setRangeKM(rangeKM);
        setMaxPower(maxPower);
        setMaxSpeed(maxSpeed);
        setSeatingCapacity(seatingCapacity);
        setChargingTime(chargingTime);
        setAirbagCount(airbagCount);
        setDescription(description);
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public BigDecimal getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(BigDecimal batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public String getWheelType() {
        return wheelType;
    }

    public void setWheelType(String wheelType) {
        this.wheelType = wheelType;
    }

    public BigDecimal getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(BigDecimal energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public BigDecimal getRangeKM() {
        return rangeKM;
    }

    public void setRangeKM(BigDecimal rangeKM) {
        this.rangeKM = rangeKM;
    }

    public BigDecimal getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(BigDecimal maxPower) {
        this.maxPower = maxPower;
    }

    public BigDecimal getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(BigDecimal maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public BigDecimal getChargingTime() {
        return chargingTime;
    }

    public void setChargingTime(BigDecimal chargingTime) {
        this.chargingTime = chargingTime;
    }

    public Integer getAirbagCount() {
        return airbagCount;
    }

    public void setAirbagCount(Integer airbagCount) {
        this.airbagCount = airbagCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
