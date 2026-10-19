package com.electriccarsales.model;

import java.time.LocalDateTime;

public class SystemConfiguration {
    private String key;
    private String name;
    private String value;
    private String type;
    private int min;
    private int max;
    private String description;
    private Integer updatedBy;
    private LocalDateTime updatedDate;

    public SystemConfiguration() {
    }

    public SystemConfiguration(String key, String name, String value, String type, int min, int max, String description,
        Integer updatedBy, LocalDateTime updatedDate) {
        this.key = key;
        this.name = name;
        this.value = value;
        this.type = type;
        this.min = min;
        this.max = max;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedDate = updatedDate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
