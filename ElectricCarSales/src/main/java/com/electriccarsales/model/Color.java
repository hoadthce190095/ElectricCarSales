package com.electriccarsales.model;

public class Color {
    private  int colorID;
    private  String colorName;
    private String description;

    public Color() {
    }

    public Color(int colorID, String colorName, String description) {
        this.colorID = colorID;
        this.colorName = colorName;
        this.description = description;
    }

    public int getColorID() {
        return colorID;
    }
    public void setColorID(int colorID) {
        this.colorID = colorID;
    }

    public String getColorName() {
        return colorName;
    }
    public void setColorName(String colorName) {
        this.colorName = colorName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Color{" +
            "colorID=" + colorID +
            ", colorName='" + colorName + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
