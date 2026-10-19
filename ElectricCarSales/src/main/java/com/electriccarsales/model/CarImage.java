package com.electriccarsales.model;

import java.time.LocalDateTime;

public class CarImage {
    private int carImageID;
    private int modelID;
    private String imageURL;
    private String imageType;
    private boolean isPrimary;
    private int displayOrder;
    private LocalDateTime createDate;

    public CarImage() {
    }

    public CarImage(int carImageID, int modelID, String imageURL, String imageType, boolean isPrimary, int displayOrder, LocalDateTime createDate) {
        this.carImageID = carImageID;
        this.modelID = modelID;
        this.imageURL = imageURL;
        this.imageType = imageType;
        this.isPrimary = isPrimary;
        this.displayOrder = displayOrder;
        this.createDate = createDate;
    }

    public int getCarImageID() {
        return carImageID;
    }

    public void setCarImageID(int carImageID) {
        this.carImageID = carImageID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public boolean isIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }


    @Override
    public String toString() {
        return "CarImage{" +
            "carImageID=" + carImageID +
            ", modelID=" + modelID +
            ", imageURL='" + imageURL + '\'' +
            ", imageType='" + imageType + '\'' +
            ", isPrimary=" + isPrimary +
            ", displayOrder=" + displayOrder +
            ", createDate=" + createDate +
            '}';
    }
}
