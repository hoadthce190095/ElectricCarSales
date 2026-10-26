package com.electriccarsales.model;

public class FinancialCompany {
    private int companyID;
    private String companyName;
    private String phone;
    private String email;
    private String address;
    private String description;

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
}
