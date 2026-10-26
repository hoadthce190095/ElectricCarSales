package com.electriccarsales.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public catalogue projection. It excludes vehicle identifiers and business records.
 */
public class VehicleModel {
    private int modelId;
    private String modelName;
    private String carLineName;
    private String brandName;
    private String description;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int availableCars;
    private List<String> versions = List.of();
    private List<String> colors = List.of();
    private VehicleSpecifications specifications = new VehicleSpecifications();

    public VehicleModel() {
    }

    public VehicleModel(int modelId, String modelName, String carLineName, String brandName, String description,
        BigDecimal minPrice, BigDecimal maxPrice, int availableCars, List<String> versions, List<String> colors,
        VehicleSpecifications specifications) {
        setModelId(modelId);
        setModelName(modelName);
        setCarLineName(carLineName);
        setBrandName(brandName);
        setDescription(description);
        setMinPrice(minPrice);
        setMaxPrice(maxPrice);
        setAvailableCars(availableCars);
        setVersions(versions);
        setColors(colors);
        setSpecifications(specifications);
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getCarLineName() {
        return carLineName;
    }

    public void setCarLineName(String carLineName) {
        this.carLineName = carLineName;
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

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getAvailableCars() {
        return availableCars;
    }

    public void setAvailableCars(int availableCars) {
        this.availableCars = availableCars;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions == null ? List.of() : List.copyOf(versions);
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors == null ? List.of() : List.copyOf(colors);
    }

    public VehicleSpecifications getSpecifications() {
        return specifications;
    }

    public void setSpecifications(VehicleSpecifications specifications) {
        this.specifications = specifications == null ? new VehicleSpecifications() : specifications;
    }
}
