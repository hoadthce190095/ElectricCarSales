package com.electriccarsales.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.electriccarsales.model.EmployeeSalesReport;
import com.electriccarsales.model.ModelSalesReport;
import com.electriccarsales.model.MonthlyReport;
import com.electriccarsales.model.ReportData;
import com.electriccarsales.model.ReportSummary;
import com.electriccarsales.model.SaleReport;
import com.electriccarsales.model.SaleVehicle;
import com.electriccarsales.model.StatusTotal;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Xuất báo cáo XLSX bằng Apache POI, giữ ô số và ô văn bản riêng.
 */
public final class ReportXlsx {
    private ReportXlsx() {
    }

    public static void write(ReportData report, OutputStream output) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            for (SheetData sheetData : sheets(report)) {
                createSheet(workbook, sheetData, headerStyle, moneyStyle);
            }
            workbook.write(output);
        }
    }

    // Mỗi Object[] là một dòng. Dòng đầu chứa tiêu đề, các dòng sau chứa dữ liệu.
    private static void createSheet(XSSFWorkbook workbook, SheetData sheetData, CellStyle headerStyle,
        CellStyle moneyStyle) {
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(sheetData.getName());
        sheet.createFreezePane(0, 1);
        int columnCount = 0;
        for (int rowIndex = 0; rowIndex < sheetData.getRows().size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            Object[] values = sheetData.getRows().get(rowIndex);
            columnCount = Math.max(columnCount, values.length);
            for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                Object value = values[columnIndex];
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                    if (value instanceof BigDecimal) {
                        cell.setCellStyle(moneyStyle);
                    }
                } else {
                    // setCellValue(String) lưu văn bản; tên khách hàng không trở thành công thức.
                    cell.setCellValue(value == null ? "" : value.toString());
                }
                if (rowIndex == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.setColumnWidth(columnIndex, 24 * 256);
        }
        if (sheetData.getRows().size() > 1 && columnCount > 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, sheetData.getRows().size() - 1, 0, columnCount - 1));
        }
    }

    private static List<SheetData> sheets(ReportData report) {
        ReportSummary s = report.getSummary();
        List<Object[]> summary = new ArrayList<>();
        summary.add(new Object[]{"Chỉ tiêu", "Giá trị"});
        summary.add(new Object[]{"Từ ngày", report.getPeriod().getFrom()});
        summary.add(new Object[]{"Đến ngày (bao gồm)", report.getPeriod().getTo()});
        summary.add(new Object[]{"Thời điểm tạo (Việt Nam)", report.getGeneratedAt()});
        summary.add(new Object[]{"Tiền thực thu (Payments)", s.getRevenue()});
        summary.add(new Object[]{"Giá trị bán hàng", s.getSalesAmount()});
        summary.add(new Object[]{"Xe đã bán", s.getVehiclesSold()});
        summary.add(new Object[]{"Tổng đơn hàng", s.getTotalOrders()});
        summary.add(new Object[]{"Đơn hoàn tất", s.getCompletedOrders()});
        summary.add(new Object[]{"Đơn đã hủy", s.getCancelledOrders()});
        summary.add(new Object[]{"Số tiền chưa thu", s.getOutstandingAmount()});
        summary.add(new Object[]{"Thanh toán thành công", s.getCompletedPayments()});
        summary.add(new Object[]{"Hợp đồng hợp lệ", s.getContracts()});
        summary.add(new Object[]{"Giảm giá", s.getDiscountAmount()});
        summary.add(new Object[]{"Thuế", s.getTaxAmount()});
        summary.add(new Object[]{"Hợp đồng trả góp", s.getInstallmentContracts()});
        summary.add(new Object[]{"Trả góp quá hạn", s.getOverdueAmount()});
        summary.add(new Object[]{"Tiền trả góp (trình bày riêng)", s.getInstallmentReceipts()});
        summary.add(new Object[]{"Đơn có chứng từ không hợp lệ", s.getInconsistentOrders()});

        List<Object[]> orders = new ArrayList<>();
        orders.add(new Object[]{"Mã đơn", "Ngày tạo", "Trạng thái", "Khách hàng", "Nhân viên", "Tổng tiền", "Đã thu",
            "Chưa thu", "Hợp đồng", "Hóa đơn", "Giao hàng", "Hợp lệ", "Số xe"});
        List<Object[]> vehicles = new ArrayList<>();
        vehicles.add(new Object[]{"Mã đơn", "Ngày tạo", "Trạng thái đơn", "Hợp lệ", "Mã xe", "VIN", "Mẫu xe",
            "Phiên bản", "Giá bán lịch sử"});
        for (SaleReport sale : report.getSales()) {
            orders.add(new Object[]{sale.getOrderId(), sale.getOrderDate(), sale.getStatus(), sale.getCustomerName(),
                sale.getEmployeeName(), sale.getTotalAmount(), sale.getPaidAmount(), sale.getRemainingAmount(),
                sale.getContractNumber(), sale.getInvoiceNumber(), sale.getDeliveryStatus(),
                sale.isEligible() ? "Có" : "Không", sale.getVehicles().size()});
            for (SaleVehicle vehicle : sale.getVehicles()) {
                vehicles.add(new Object[]{sale.getOrderId(), sale.getOrderDate(), sale.getStatus(),
                    sale.isEligible() ? "Có" : "Không", vehicle.getCarId(), vehicle.getVin(), vehicle.getModelName(),
                    vehicle.getVersionName(), vehicle.getSalesPrice()});
            }
        }
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"Nhóm", "Nhãn", "Số lượng", "Tiền thu", "Giá trị bán/giá niêm yết", "Số đơn"});
        for (MonthlyReport month : report.getMonthly()) {
            stats.add(new Object[]{"Theo tháng", month.getMonth(), month.getVehiclesSold(), month.getRevenue(),
                month.getSalesAmount(), month.getOrders()});
        }
        for (ModelSalesReport model : report.getModels()) {
            stats.add(new Object[]{"Theo mẫu xe", model.getModelName(), model.getVehiclesSold(), null,
                model.getSalesAmount(), null});
        }
        for (EmployeeSalesReport employee : report.getEmployees()) {
            stats.add(new Object[]{"Theo nhân viên", employee.getEmployeeName(), employee.getVehiclesSold(), null,
                employee.getSalesAmount(), employee.getOrders()});
        }
        for (StatusTotal status : report.getOrderStatuses()) {
            stats.add(
                new Object[]{"Trạng thái đơn", status.getStatus(), status.getCount(), null, status.getAmount(), null});
        }
        for (StatusTotal status : report.getPaymentStatuses()) {
            stats.add(new Object[]{"Trạng thái thanh toán", status.getStatus(), status.getCount(), status.getAmount(),
                null, null});
        }
        for (StatusTotal status : report.getInventory()) {
            stats.add(new Object[]{"Tồn kho hiện tại", status.getStatus(), status.getCount(), null, status.getAmount(),
                null});
        }
        List<Object[]> definitions = new ArrayList<>();
        definitions.add(new Object[]{"Chỉ tiêu", "Quy tắc tính"});
        report.getDefinitions().forEach((key, value) -> definitions.add(new Object[]{key, value}));
        return List.of(new SheetData("Tổng quan", summary), new SheetData("Đơn hàng", orders),
            new SheetData("Xe trong đơn", vehicles), new SheetData("Thống kê", stats),
            new SheetData("Định nghĩa", definitions));
    }

    private static class SheetData {
        private String name;
        private List<Object[]> rows;

        public SheetData() {
        }

        public SheetData(String name, List<Object[]> rows) {
            this.name = name;
            this.rows = rows;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Object[]> getRows() {
            return rows;
        }

        public void setRows(List<Object[]> rows) {
            this.rows = rows;
        }
    }
}
