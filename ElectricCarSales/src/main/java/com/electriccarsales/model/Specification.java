package com.electriccarsales.model;

public class Specification {
    private int specificationID;
    private int modelID;
    private String dimensions;
    private double batteryCapacity;
    private String wheelType;
    private double energyConsumption;
    private double rangeKM;
    private double maxPower;
    private double maxSpeed;
    private int seatingCapacity;
    private double chargingTime;
    private int airbagCount;
    private String description;

    public Specification() {
    }

    public Specification(int specificationID, int modelID, String dimensions, double batteryCapacity, String wheelType, double energyConsumption, double rangeKM, double maxPower, double maxSpeed, int seatingCapacity, double chargingTime, int airbagCount, String description) {
        this.specificationID = specificationID;
        this.modelID = modelID;
        this.dimensions = dimensions;
        this.batteryCapacity = batteryCapacity;
        this.wheelType = wheelType;
        this.energyConsumption = energyConsumption;
        this.rangeKM = rangeKM;
        this.maxPower = maxPower;
        this.maxSpeed = maxSpeed;
        this.seatingCapacity = seatingCapacity;
        this.chargingTime = chargingTime;
        this.airbagCount = airbagCount;
        this.description = description;
    }

    public int getSpecificationID() {
        return specificationID;
    }

    public void setSpecificationID(int specificationID) {
        this.specificationID = specificationID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public String getWheelType() {
        return wheelType;
    }

    public void setWheelType(String wheelType) {
        this.wheelType = wheelType;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public double getRangeKM() {
        return rangeKM;
    }

    public void setRangeKM(double rangeKM) {
        this.rangeKM = rangeKM;
    }

    public double getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(double maxPower) {
        this.maxPower = maxPower;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public double getChargingTime() {
        return chargingTime;
    }

    public void setChargingTime(double chargingTime) {
        this.chargingTime = chargingTime;
    }

    public int getAirbagCount() {
        return airbagCount;
    }

    public void setAirbagCount(int airbagCount) {
        this.airbagCount = airbagCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return "Specification{" +
                "specificationID=" + specificationID +
                ", modelID=" + modelID +
                ", dimensions='" + dimensions + '\'' +
                ", batteryCapacity=" + batteryCapacity +
                ", wheelType='" + wheelType + '\'' +
                ", energyConsumption=" + energyConsumption +
                ", rangeKM=" + rangeKM +
                ", maxPower=" + maxPower +
                ", maxSpeed=" + maxSpeed +
                ", seatingCapacity=" + seatingCapacity +
                ", chargingTime=" + chargingTime +
                ", airbagCount=" + airbagCount +
                ", description='" + description + '\'' +
                '}';
    }
}
