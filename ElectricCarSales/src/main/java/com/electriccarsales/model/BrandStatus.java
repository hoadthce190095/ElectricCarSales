package com.electriccarsales.model;

public class BrandStatus {
    private int brandStatusID;
    private String statusName;

    public  BrandStatus(){
    }


    public BrandStatus(int brandStatusID, String statusName) {
        this.brandStatusID = brandStatusID;
        this.statusName = statusName;
    }

    public int getBrandStatusID() {
        return brandStatusID;
    }

    public void setBrandStatusID(int brandStatusID) {
        this.brandStatusID = brandStatusID;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    @Override
    public String toString() {
        return "BrandStatus{" +
                "brandStatusID=" + brandStatusID +
                ", statusName='" + statusName + '\'' +
                '}';
    }
}
