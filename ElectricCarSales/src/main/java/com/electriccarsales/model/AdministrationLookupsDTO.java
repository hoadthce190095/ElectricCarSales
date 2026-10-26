package com.electriccarsales.model;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public class AdministrationLookupsDTO {
    private List<LookupDTO> roles;
    private List<LookupDTO> statuses;
    private List<LookupDTO> models;
    private Map<Integer, BigDecimal> modelMinPrices = Map.of();

    public AdministrationLookupsDTO() {
    }

    public AdministrationLookupsDTO(List<LookupDTO> roles, List<LookupDTO> statuses, List<LookupDTO> models) {
        this.roles = roles;
        this.statuses = statuses;
        this.models = models;
    }

    public List<LookupDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<LookupDTO> roles) {
        this.roles = roles;
    }

    public List<LookupDTO> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<LookupDTO> statuses) {
        this.statuses = statuses;
    }

    public List<LookupDTO> getModels() {
        return models;
    }

    public void setModels(List<LookupDTO> models) {
        this.models = models;
    }

    public Map<Integer, BigDecimal> getModelMinPrices() {
        return modelMinPrices;
    }

    public void setModelMinPrices(Map<Integer, BigDecimal> modelMinPrices) {
        this.modelMinPrices = modelMinPrices;
    }
}
