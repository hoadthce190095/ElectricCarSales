package com.electriccarsales.model;

import java.sql.Date;

public class Contract {
    private int contractID;
    private int orderID;
    private String contractNumber;
    private Date signDate;
    private String status;
    private String fileUrl;

    public Contract() {}

    public Contract(int contractID, int orderID, String contractNumber, Date signDate, String status, String fileUrl) {
        this.contractID = contractID;
        this.orderID = orderID;
        this.contractNumber = contractNumber;
        this.signDate = signDate;
        this.status = status;
        this.fileUrl = fileUrl;
    }

    public int getContractID() { return contractID; }
    public void setContractID(int contractID) { this.contractID = contractID; }
    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }
    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
    public Date getSignDate() { return signDate; }
    public void setSignDate(Date signDate) { this.signDate = signDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
