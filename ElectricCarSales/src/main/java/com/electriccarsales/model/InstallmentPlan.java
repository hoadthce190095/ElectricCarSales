package com.electriccarsales.model;

import java.math.BigDecimal;

public class InstallmentPlan {
    private int planID;
    private String planName;
    private int loanTermMonths;
    private BigDecimal interestRate;
    private String description;

    public InstallmentPlan() {
    }

    public InstallmentPlan(int planID, String planName, int loanTermMonths,
                           BigDecimal interestRate, String description) {
        this.planID = planID;
        this.planName = planName;
        this.loanTermMonths = loanTermMonths;
        this.interestRate = interestRate;
        this.description = description;
    }

    public int getPlanID() { return planID; }
    public void setPlanID(int planID) { this.planID = planID; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getLoanTermMonths() { return loanTermMonths; }
    public void setLoanTermMonths(int loanTermMonths) { this.loanTermMonths = loanTermMonths; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
