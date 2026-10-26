package com.electriccarsales.service;

import com.electriccarsales.model.EmployeeSalesReport;
import com.electriccarsales.model.ModelSalesReport;
import com.electriccarsales.model.MonthlyReport;
import com.electriccarsales.model.ReportData;
import com.electriccarsales.model.ReportSummary;
import com.electriccarsales.model.SaleReport;
import com.electriccarsales.model.SaleVehicle;
import com.electriccarsales.model.StatusTotal;

import java.io.IOException;
import java.io.Writer;

/**
 * UTF-8 CSV readable by Excel; text cells cannot become executable spreadsheet formulas.
 */
public final class ReportCsv {
    private ReportCsv() {
    }

    public static void write(ReportData report, Writer writer) throws IOException {
        writer.write('\uFEFF');
        row(writer, "Section", "Date", "ID", "Label", "Count", "Amount", "PaidAmount", "RemainingAmount", "Status");
        row(writer, "Period", report.getPeriod().getFrom(), report.getPeriod().getTo(), "Inclusive", "", "", "", "",
            "");
        if (report.getDefinitions().containsKey("demo")) {
            row(writer, "Source", "", "demo", report.getDefinitions().get("demo"), "", "", "", "", "");
        }
        ReportSummary summary = report.getSummary();
        row(writer, "Summary", "", "", "Cash receipts (Payments only)", summary.getCompletedPayments(),
            summary.getRevenue(), "", "", "");
        row(writer, "Summary", "", "", "Sales value", summary.getVehiclesSold(), summary.getSalesAmount(), "",
            summary.getOutstandingAmount(), "");
        row(writer, "Summary", "", "", "Installment receipts (separate)", "", summary.getInstallmentReceipts(), "",
            summary.getOverdueAmount(), "");
        for (MonthlyReport month : report.getMonthly()) {
            row(writer, "Monthly", month.getMonth(), "", "Cash receipts", month.getOrders(), month.getRevenue(), "", "",
                "");
            row(writer, "Monthly", month.getMonth(), "", "Sales value", month.getVehiclesSold(), month.getSalesAmount(),
                "", "", "");
        }
        for (ModelSalesReport model : report.getModels()) {
            row(writer, "Model", "", model.getModelId(), model.getModelName(), model.getVehiclesSold(),
                model.getSalesAmount(), "", "", "");
        }
        for (EmployeeSalesReport employee : report.getEmployees()) {
            row(writer, "Employee", "", employee.getEmployeeId(), employee.getEmployeeName(),
                employee.getVehiclesSold(), employee.getSalesAmount(), "", "", "");
        }
        for (StatusTotal status : report.getOrderStatuses()) {
            row(writer, "OrderStatus", "", "", "", status.getCount(), status.getAmount(), "", "", status.getStatus());
        }
        for (StatusTotal status : report.getPaymentStatuses()) {
            row(writer, "PaymentStatus", "", "", "", status.getCount(), status.getAmount(), "", "", status.getStatus());
        }
        for (StatusTotal status : report.getInventory()) {
            row(writer, "CurrentInventory", "", "", "", status.getCount(), status.getAmount(), "", "",
                status.getStatus());
        }
        for (SaleReport sale : report.getSales()) {
            row(writer, "Order", sale.getOrderDate(), sale.getOrderId(), sale.getCustomerName(),
                sale.getVehicles().size(), sale.getTotalAmount(), sale.getPaidAmount(), sale.getRemainingAmount(),
                sale.getStatus());
            for (SaleVehicle vehicle : sale.getVehicles()) {
                row(writer, "Vehicle", sale.getOrderDate(), vehicle.getCarId(),
                    vehicle.getModelName() + " / " + vehicle.getVin(), 1, vehicle.getSalesPrice(), "", "",
                    sale.getStatus());
            }
        }
        for (var definition : report.getDefinitions().entrySet()) {
            row(writer, "Definition", "", "", definition.getKey() + ": " + definition.getValue(), "", "", "", "", "");
        }
    }

    public static String cell(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = value.toString();
        if (!(value instanceof Number)) {
            String significant = text.stripLeading();
            if (!significant.isEmpty() && "=+-@".indexOf(significant.charAt(0)) >= 0
                || !text.isEmpty() && "\t\r\n".indexOf(text.charAt(0)) >= 0) {
                text = "'" + text;
            }
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static void row(Writer writer, Object... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i != 0) {
                writer.write(',');
            }
            writer.write(cell(cells[i]));
        }
        writer.write("\r\n");
    }
}
