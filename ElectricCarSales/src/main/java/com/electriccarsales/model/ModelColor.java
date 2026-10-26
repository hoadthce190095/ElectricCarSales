package com.electriccarsales.model;

public class ModelColor {
    private int modelColorID;
    private int modelID;
    private int colorID;

    public ModelColor() {
    }

    public ModelColor(int modelColorID, int modelID, int colorID) {
        this.modelColorID = modelColorID;
        this.modelID = modelID;
        this.colorID = colorID;
    }

    public int getModelColorID() {
        return modelColorID;
    }

    public void setModelColorID(int modelColorID) {
        this.modelColorID = modelColorID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public int getColorID() {
        return colorID;
    }

    public void setColorID(int colorID) {
        this.colorID = colorID;
    }

    @Override
    public String toString() {
        return "ModelColor{" +
            "modelColorID=" + modelColorID +
            ", modelID=" + modelID +
            ", colorID=" + colorID +
            '}';
    }
}
