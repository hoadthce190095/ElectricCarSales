package com.electriccarsales.model;

import java.time.LocalDateTime;

public class VehicleTCO {
    private int tcoID;
    private int modelID;
    private double vehiclePrice;
    private double energyCost;
    private double maintenanceCost;
    private double insuranceCost;
    private double registrationCost;
    private double incentiveAmount;
    private double totalCost;
    private int periodMonths;
    private String description;
    private LocalDateTime createDate;

    public VehicleTCO() {
    }

    public VehicleTCO(int tcoID, int modelID, double vehiclePrice, double energyCost, double maintenanceCost, double insuranceCost, double registrationCost, double incentiveAmount, double totalCost, int periodMonths, String description, LocalDateTime createDate) {
        this.tcoID = tcoID;
        this.modelID = modelID;
        this.vehiclePrice = vehiclePrice;
        this.energyCost = energyCost;
        this.maintenanceCost = maintenanceCost;
        this.insuranceCost = insuranceCost;
        this.registrationCost = registrationCost;
        this.incentiveAmount = incentiveAmount;
        this.totalCost = totalCost;
        this.periodMonths = periodMonths;
        this.description = description;
        this.createDate = createDate;
    }

    public int getTcoID() {
        return tcoID;
    }

    public void setTcoID(int tcoID) {
        this.tcoID = tcoID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public double getVehiclePrice() {
        return vehiclePrice;
    }

    public void setVehiclePrice(double vehiclePrice) {
        this.vehiclePrice = vehiclePrice;
    }

    public double getEnergyCost() {
        return energyCost;
    }

    public void setEnergyCost(double energyCost) {
        this.energyCost = energyCost;
    }

    public double getMaintenanceCost() {
        return maintenanceCost;
    }

    public void setMaintenanceCost(double maintenanceCost) {
        this.maintenanceCost = maintenanceCost;
    }

    public double getInsuranceCost() {
        return insuranceCost;
    }

    public void setInsuranceCost(double insuranceCost) {
        this.insuranceCost = insuranceCost;
    }

    public double getRegistrationCost() {
        return registrationCost;
    }

    public void setRegistrationCost(double registrationCost) {
        this.registrationCost = registrationCost;
    }

    public double getIncentiveAmount() {
        return incentiveAmount;
    }

    public void setIncentiveAmount(double incentiveAmount) {
        this.incentiveAmount = incentiveAmount;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public int getPeriodMonths() {
        return periodMonths;
    }

    public void setPeriodMonths(int periodMonths) {
        this.periodMonths = periodMonths;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "VehicleTCO{" +
            "tcoID=" + tcoID +
            ", modelID=" + modelID +
            ", vehiclePrice=" + vehiclePrice +
            ", energyCost=" + energyCost +
            ", maintenanceCost=" + maintenanceCost +
            ", insuranceCost=" + insuranceCost +
            ", registrationCost=" + registrationCost +
            ", incentiveAmount=" + incentiveAmount +
            ", totalCost=" + totalCost +
            ", periodMonths=" + periodMonths +
            ", description='" + description + '\'' +
            ", createDate=" + createDate +
            '}';
    }
}
