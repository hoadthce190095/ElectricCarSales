package com.electriccarsales.model;

public class CarStatus {
    private int carStatusID;
    private String statusName;
    private String description;

    public CarStatus() {}

    public CarStatus(int carStatusID, String statusName, String description) {
        this.carStatusID = carStatusID;
        this.statusName = statusName;
        this.description = description;
    }

    public int getCarStatusID() { return carStatusID; }
    public void setCarStatusID(int carStatusID) { this.carStatusID = carStatusID; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}