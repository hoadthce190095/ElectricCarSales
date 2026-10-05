package com.electriccarsales.model;

public class CarLine {
    private int carLineID;
    private int brandID;
    private String carLineName;
    private String description;

    public CarLine() {
    }

    public CarLine(int carLineID, int brandID, String carLineName, String description) {
        this.carLineID = carLineID;
        this.brandID = brandID;
        this.carLineName = carLineName;
        this.description = description;
    }

    public int getCarLineID() {
        return carLineID;
    }

    public void setCarLineID(int carLineID) {
        this.carLineID = carLineID;
    }

    public int getBrandID() {
        return brandID;
    }

    public void setBrandID(int brandID) {
        this.brandID = brandID;
    }

    public String getCarLineName() {
        return carLineName;
    }

    public void setCarLineName(String carLineName) {
        this.carLineName = carLineName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "CarLine{" +
                "carLineID=" + carLineID +
                ", brandID=" + brandID +
                ", carLineName='" + carLineName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
