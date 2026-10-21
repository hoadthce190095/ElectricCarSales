package com.electriccarsales.model;

public class CarModel {
    private int modelID;
    private int carLineID;
    private String modelName;
    private String description;

    public CarModel() {
    }

    public CarModel(int modelID, int carLineID, String modelName, String description) {
        this.modelID = modelID;
        this.carLineID = carLineID;
        this.modelName = modelName;
        this.description = description;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public int getCarLineID() {
        return carLineID;
    }

    public void setCarLineID(int carLineID) {
        this.carLineID = carLineID;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "CarModel{" +
                "modelID=" + modelID +
                ", carLineID=" + carLineID +
                ", modelName='" + modelName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
