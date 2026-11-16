package com.electriccarsales.model;

import com.electriccarsales.util.FormatUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order {
    private int id;
    private int customerId;
    private Integer employeeId;
    private int paymentMethodId;
    private BigDecimal originalAmount; // Tiền nghiệp vụ thực tế, TUYỆT ĐỐI KHÔNG GHI ĐÈ
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String status;
    private Timestamp orderDate;
    private String note;
    private String vehicleName;
    private String paymentMethodName;
    private String contractStatus;

    // Các trường phục vụ theo dõi công nợ, đối tác tài chính và trạng thái thanh toán
    private BigDecimal paidAmount;
    private Integer companyId;
    private String companyName;

    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public int getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public BigDecimal getPaidAmount() { return paidAmount != null ? paidAmount : BigDecimal.ZERO; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    // Helpers chuẩn hóa tiền tệ và mã đơn hàng theo RDS/SDS
    public String getOrderCode() { return "ECS" + id; }

    public String getFormattedOriginalAmount() {
        return FormatUtils.formatMoney(originalAmount);
    }

    public String getFormattedDepositAmount() {
        return FormatUtils.formatMoney(depositAmount);
    }

    public String getFormattedRemainingAmount() {
        return FormatUtils.formatMoney(remainingAmount);
    }

    public String getFormattedPaidAmount() {
        return FormatUtils.formatMoney(getPaidAmount());
    }

    public boolean isFullyPaid() {
        return remainingAmount != null && remainingAmount.signum() == 0
                && (paidAmount != null && originalAmount != null && paidAmount.compareTo(originalAmount) >= 0);
    }

    public boolean isDepositPaid() {
        if ("DEPOSITED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {
            return true;
        }
        return paidAmount != null && depositAmount != null && paidAmount.compareTo(depositAmount) >= 0;
    }

    public boolean isInstallment() {
        return "INSTALLMENT".equalsIgnoreCase(paymentMethodName) || (companyId != null && companyId > 0);
    }
}
