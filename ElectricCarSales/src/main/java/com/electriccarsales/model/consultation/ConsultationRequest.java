package com.electriccarsales.model;

import java.sql.Timestamp;

public class ConsultationRequest {
    private int id;
    private int customerId;
    private int staffId;
    private Integer modelId;
    private String content;
    private String status;
    private Timestamp createdAt;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String modelName;
    private String staffName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }
    public Integer getModelId() { return modelId; }
    public void setModelId(Integer modelId) { this.modelId = modelId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getVersionInfo() {
        if (content == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(?:Mẫu|Xe):.*?(?:\\| Bản:|\\| Phiên bản:)\\s*([^|\\]]+)").matcher(content);
        return m.find() ? m.group(1).trim() : "";
    }

    public String getColorInfo() {
        if (content == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(?:Mẫu|Xe):.*?(?:\\| Màu:)\\s*([^|\\]]+)").matcher(content);
        return m.find() ? m.group(1).trim() : "";
    }

    public String getPaymentPreferenceInfo() {
        if (content == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[Phương án:\\s*([^|\\]]+)\\]").matcher(content);
        return m.find() ? m.group(1).trim() : "";
    }

    public String getCleanMessage() {
        if (content == null) return "";
        String cleaned = content;
        cleaned = cleaned.replaceAll("\\[(?:Mẫu|Xe):[^\\]]+\\]\\s*", "");
        cleaned = cleaned.replaceAll("\\[Phương án:[^\\]]+\\]\\s*", "");
        return cleaned.trim();
    }
}
