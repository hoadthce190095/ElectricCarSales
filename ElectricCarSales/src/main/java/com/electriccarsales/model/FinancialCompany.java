package com.electriccarsales.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public class FinancialCompany {
    private int companyID;
    private String companyName;
    private String phone;
    private String email;
    private String address;
    private String description;
    private Integer paymentDueDay;
    private LocalTime paymentCutoffTime;
    private BigDecimal lateFeeDailyPercent;
    private BigDecimal earlySettlementFeePercent;

    public FinancialCompany() {
    }

    public FinancialCompany(int companyID, String companyName, String phone,
                            String email, String address, String description) {
        this.companyID = companyID;
        this.companyName = companyName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.description = description;
    }

    public int getCompanyID() { return companyID; }
    public void setCompanyID(int companyID) { this.companyID = companyID; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPaymentDueDay() { return paymentDueDay; }
    public void setPaymentDueDay(Integer paymentDueDay) { this.paymentDueDay = paymentDueDay; }
    public LocalTime getPaymentCutoffTime() { return paymentCutoffTime; }
    public void setPaymentCutoffTime(LocalTime paymentCutoffTime) { this.paymentCutoffTime = paymentCutoffTime; }
    public BigDecimal getLateFeeDailyPercent() { return lateFeeDailyPercent; }
    public void setLateFeeDailyPercent(BigDecimal lateFeeDailyPercent) { this.lateFeeDailyPercent = lateFeeDailyPercent; }
    public BigDecimal getEarlySettlementFeePercent() { return earlySettlementFeePercent; }
    public void setEarlySettlementFeePercent(BigDecimal earlySettlementFeePercent) { this.earlySettlementFeePercent = earlySettlementFeePercent; }
}
