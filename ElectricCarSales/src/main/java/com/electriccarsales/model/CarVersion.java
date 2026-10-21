package com.electriccarsales.model;

public class CarVersion {
    private int versionID;
    private int modelID;
    private  String versionName;
    private String description;

    public CarVersion() {
    }

    public CarVersion(int versionID, int modelID, String versionName, String description) {
        this.versionID = versionID;
        this.modelID = modelID;
        this.versionName = versionName;
        this.description = description;
    }

    public int getVersionID() {
        return versionID;
    }

    public void setVersionID(int versionID) {
        this.versionID = versionID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "CarVersion{" +
            "versionID=" + versionID +
            ", modelID=" + modelID +
            ", versionName='" + versionName + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
