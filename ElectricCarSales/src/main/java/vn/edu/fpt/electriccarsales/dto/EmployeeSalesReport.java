package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;

public class EmployeeSalesReport {
    private Integer employeeId;
    private String employeeName;
    private long orders;
    private long vehiclesSold;
    private BigDecimal salesAmount;

    public EmployeeSalesReport() {
    }

    public EmployeeSalesReport(Integer employeeId, String employeeName, long orders, long vehiclesSold,
        BigDecimal salesAmount) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.orders = orders;
        this.vehiclesSold = vehiclesSold;
        this.salesAmount = salesAmount;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public long getOrders() {
        return orders;
    }

    public void setOrders(long orders) {
        this.orders = orders;
    }

    public long getVehiclesSold() {
        return vehiclesSold;
    }

    public void setVehiclesSold(long vehiclesSold) {
        this.vehiclesSold = vehiclesSold;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }
}
