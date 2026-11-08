package com.electriccarsales.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportData {
    private ReportPeriod period;
    private LocalDateTime generatedAt;
    private Map<String, String> definitions;
    private ReportSummary summary;
    private List<MonthlyReport> monthly;
    private List<ModelSalesReport> models;
    private List<EmployeeSalesReport> employees;
    private List<StatusTotal> orderStatuses;
    private List<StatusTotal> paymentStatuses;
    private List<StatusTotal> inventory;
    private List<SaleReport> sales;

    public ReportData() {
    }

    public ReportData(ReportPeriod period, LocalDateTime generatedAt, Map<String, String> definitions,
        ReportSummary summary, List<MonthlyReport> monthly, List<ModelSalesReport> models,
        List<EmployeeSalesReport> employees, List<StatusTotal> orderStatuses, List<StatusTotal> paymentStatuses,
        List<StatusTotal> inventory, List<SaleReport> sales) {
        this.period = period;
        this.generatedAt = generatedAt;
        this.definitions = definitions;
        this.summary = summary;
        this.monthly = monthly;
        this.models = models;
        this.employees = employees;
        this.orderStatuses = orderStatuses;
        this.paymentStatuses = paymentStatuses;
        this.inventory = inventory;
        this.sales = sales;
    }

    public ReportPeriod getPeriod() {
        return period;
    }

    public void setPeriod(ReportPeriod period) {
        this.period = period;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Map<String, String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, String> definitions) {
        this.definitions = definitions;
    }

    public ReportSummary getSummary() {
        return summary;
    }

    public void setSummary(ReportSummary summary) {
        this.summary = summary;
    }

    public List<MonthlyReport> getMonthly() {
        return monthly;
    }

    public void setMonthly(List<MonthlyReport> monthly) {
        this.monthly = monthly;
    }

    public List<ModelSalesReport> getModels() {
        return models;
    }

    public void setModels(List<ModelSalesReport> models) {
        this.models = models;
    }

    public List<EmployeeSalesReport> getEmployees() {
        return employees;
    }

    public void setEmployees(List<EmployeeSalesReport> employees) {
        this.employees = employees;
    }

    public List<StatusTotal> getOrderStatuses() {
        return orderStatuses;
    }

    public void setOrderStatuses(List<StatusTotal> orderStatuses) {
        this.orderStatuses = orderStatuses;
    }

    public List<StatusTotal> getPaymentStatuses() {
        return paymentStatuses;
    }

    public void setPaymentStatuses(List<StatusTotal> paymentStatuses) {
        this.paymentStatuses = paymentStatuses;
    }

    public List<StatusTotal> getInventory() {
        return inventory;
    }

    public void setInventory(List<StatusTotal> inventory) {
        this.inventory = inventory;
    }

    public List<SaleReport> getSales() {
        return sales;
    }

    public void setSales(List<SaleReport> sales) {
        this.sales = sales;
    }
}
