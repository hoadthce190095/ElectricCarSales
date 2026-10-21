package com.electriccarsales.model;

import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Brand {
    private int brandID;
    private String brandName;
    private String description;
    private int brandStatusID;

    public Brand() {
    }

    public Brand(int brandID, String brandName, String description, int brandStatusID) {
        this.brandID = brandID;
        this.brandName = brandName;
        this.description = description;
        this.brandStatusID = brandStatusID;
    }

    public int getBrandID() {
        return brandID;
    }

    public void setBrandID(int brandID) {
        this.brandID = brandID;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBrandStatusID() {
        return brandStatusID;
    }

    public void setBrandStatusID(int brandStatusID) {
        this.brandStatusID = brandStatusID;
    }

    @Override
    public String toString() {
        return "Brand{" +
                "brandID=" + brandID +
                ", brandName='" + brandName + '\'' +
                ", Description='" + description + '\'' +
                ", brandStatusID=" + brandStatusID + '\'' +
                '}';
    }

}
