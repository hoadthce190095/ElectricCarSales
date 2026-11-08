package com.electriccarsales.model;

public class ConsultationStatus {
    private int statusId;
    private String statusCode;
    public ConsultationStatus() { }
    public ConsultationStatus(int id, String name) { statusId=id; statusCode=name; }
    public int getId() { return statusId; }
    public int getStatusId() { return statusId; }
    public void setStatusId(int id) { statusId=id; }
    public String getStatusName() { return statusCode; }
    public String getStatusCode() { return statusCode; }
    public String getDescription() { return statusCode; }
    public void setStatusName(String v) { statusCode=v; }
}
