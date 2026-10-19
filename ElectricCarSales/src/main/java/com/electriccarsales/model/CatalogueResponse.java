package com.electriccarsales.model;

import java.util.List;

/**
 * Public catalogue response for the assistant screen.
 */
public class CatalogueResponse {
    private String mode;
    private List<VehicleModel> models = List.of();

    public CatalogueResponse() {
    }

    public CatalogueResponse(String mode, List<VehicleModel> models) {
        setMode(mode);
        setModels(models);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<VehicleModel> getModels() {
        return models;
    }

    public void setModels(List<VehicleModel> models) {
        this.models = models == null ? List.of() : List.copyOf(models);
    }
}
