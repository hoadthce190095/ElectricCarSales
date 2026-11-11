package com.electriccarsales.model;

public class InventoryItem {
    private int modelId;
    private String modelName;
    private String color;
    private String location;
    private String statusName;
    private int quantity;

    public InventoryItem() {}

    public int getModelId() { return modelId; }
    public void setModelId(int modelId) { this.modelId = modelId; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
