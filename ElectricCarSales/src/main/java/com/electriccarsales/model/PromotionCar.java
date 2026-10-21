package com.electriccarsales.model;

public class PromotionCar {
    private int promotionCarID;
    private int promotionID;
    private int modelID;

    public PromotionCar() {
    }

    public PromotionCar(int promotionCarID, int promotionID, int modelID) {
        this.promotionCarID = promotionCarID;
        this.promotionID = promotionID;
        this.modelID = modelID;
    }

    public int getPromotionCarID() {
        return promotionCarID;
    }

    public void setPromotionCarID(int promotionCarID) {
        this.promotionCarID = promotionCarID;
    }

    public int getPromotionID() {
        return promotionID;
    }

    public void setPromotionID(int promotionID) {
        this.promotionID = promotionID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }


    @Override
    public String toString() {
        return "PromotionCar{" +
            "promotionCarID=" + promotionCarID +
            ", promotionID=" + promotionID +
            ", modelID=" + modelID +
            '}';
    }
}
