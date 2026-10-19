package com.electriccarsales.model;

import java.time.LocalDateTime;

public class Promotion {
    private int promotionID;
    private String promotionName;
    private String description;
    private  String discountType;
    private double discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;

    public Promotion() {
    }

    public Promotion(int promotionID, String promotionName, String description, String discountType, double discountValue, LocalDateTime startDate, LocalDateTime endDate, String status) {
        this.promotionID = promotionID;
        this.promotionName = promotionName;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getPromotionID() {
        return promotionID;
    }

    public void setPromotionID(int promotionID) {
        this.promotionID = promotionID;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Promotion{" +
            "promotionID=" + promotionID +
            ", promotionName='" + promotionName + '\'' +
            ", description='" + description + '\'' +
            ", discountType='" + discountType + '\'' +
            ", discountValue=" + discountValue +
            ", startDate=" + startDate +
            ", endDate=" + endDate +
            ", status='" + status + '\'' +
            '}';
    }
}
