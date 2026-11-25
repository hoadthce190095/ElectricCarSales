package com.electriccarsales.model;

public class ComparisonItem {

    private int comparisonItemID;
    private int comparisonSessionID;
    private int modelID;
    private String comparisonKey;

    public ComparisonItem() {
    }

    public ComparisonItem(int comparisonItemID, int comparisonSessionID, int modelID) {
        this.comparisonItemID = comparisonItemID;
        this.comparisonSessionID = comparisonSessionID;
        this.modelID = modelID;
    }

    public int getComparisonItemID() {
        return comparisonItemID;
    }

    public void setComparisonItemID(int comparisonItemID) {
        this.comparisonItemID = comparisonItemID;
    }

    public int getComparisonSessionID() {
        return comparisonSessionID;
    }

    public void setComparisonSessionID(int comparisonSessionID) {
        this.comparisonSessionID = comparisonSessionID;
    }

    public int getModelID() {
        return modelID;
    }

    public void setModelID(int modelID) {
        this.modelID = modelID;
    }

    public String getComparisonKey() {
        return comparisonKey;
    }

    public void setComparisonKey(String comparisonKey) {
        this.comparisonKey = comparisonKey;
    }

    @Override
    public String toString() {
        return "ComparisonItem{" + "comparisonItemID=" + comparisonItemID + ", comparisonSessionID=" + comparisonSessionID + ", modelID=" + modelID + ", comparisonKey='" + comparisonKey + '\'' + '}';
    }


}
