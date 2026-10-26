package com.electriccarsales.model;

import java.time.LocalDateTime;

public class OTPVerification {
    private int otpVerificationID;
    private int userID;
    private String otpCode;
    private String purpose;
    private LocalDateTime createdDate;
    private LocalDateTime expiredDate;
    private LocalDateTime verifiedDate;
    private String status;

    public OTPVerification() {
    }

    public OTPVerification(int otpVerificationID, int userID, String otpCode, String purpose, LocalDateTime createdDate, LocalDateTime expiredDate, LocalDateTime verifiedDate, String status) {
        this.otpVerificationID = otpVerificationID;
        this.userID = userID;
        this.otpCode = otpCode;
        this.purpose = purpose;
        this.createdDate = createdDate;
        this.expiredDate = expiredDate;
        this.verifiedDate = verifiedDate;
        this.status = status;
    }

    public int getOtpVerificationID() {
        return otpVerificationID;
    }

    public void setOtpVerificationID(int otpVerificationID) {
        this.otpVerificationID = otpVerificationID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(LocalDateTime expiredDate) {
        this.expiredDate = expiredDate;
    }

    public LocalDateTime getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(LocalDateTime verifiedDate) {
        this.verifiedDate = verifiedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "OTPVerification{" +
                "otpVerificationID=" + otpVerificationID +
                ", userID=" + userID +
                ", otpCode='" + otpCode + '\'' +
                ", purpose='" + purpose + '\'' +
                ", createdDate=" + createdDate +
                ", expiredDate=" + expiredDate +
                ", verifiedDate=" + verifiedDate +
                ", status='" + status + '\'' +
                '}';
    }
}
