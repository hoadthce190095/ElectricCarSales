package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;

public class MonthlyReport {
    private String month;
    private BigDecimal revenue;
    private BigDecimal salesAmount;
    private long vehiclesSold;
    private long orders;

    public MonthlyReport() {
    }

    public MonthlyReport(String month, BigDecimal revenue, BigDecimal salesAmount, long vehiclesSold, long orders) {
        this.month = month;
        this.revenue = revenue;
        this.salesAmount = salesAmount;
        this.vehiclesSold = vehiclesSold;
        this.orders = orders;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public long getVehiclesSold() {
        return vehiclesSold;
    }

    public void setVehiclesSold(long vehiclesSold) {
        this.vehiclesSold = vehiclesSold;
    }

    public long getOrders() {
        return orders;
    }

    public void setOrders(long orders) {
        this.orders = orders;
    }
}
