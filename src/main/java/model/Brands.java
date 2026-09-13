/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Hoa Hà
 */
public class Brands {
    private int brandID;
    private String brandName;
    private String country;

    public Brands() {
    }

    public Brands(int brandID, String brandName, String country) {
        this.brandID = brandID;
        this.brandName = brandName;
        this.country = country;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "Brands{" + "brandID=" + brandID + ", brandName=" + brandName + ", country=" + country + '}';
    }
    
    
}
