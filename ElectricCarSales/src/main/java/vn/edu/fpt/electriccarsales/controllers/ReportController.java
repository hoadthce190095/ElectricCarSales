package vn.edu.fpt.electriccarsales.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import vn.edu.fpt.electriccarsales.config.Database;
import vn.edu.fpt.electriccarsales.dto.ReportData;
import vn.edu.fpt.electriccarsales.dto.ReportFilter;
import vn.edu.fpt.electriccarsales.repositories.SystemLogRepository;
import vn.edu.fpt.electriccarsales.security.SessionUser;
import vn.edu.fpt.electriccarsales.services.ReportService;
import vn.edu.fpt.electriccarsales.services.SystemConfigurationService;
import vn.edu.fpt.electriccarsales.util.ReportCsv;
import vn.edu.fpt.electriccarsales.util.ReportXlsx;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@WebServlet("/api/reports")
public class ReportController extends JsonServlet {
    private final ReportService reportService = new ReportService();
    private final SystemLogRepository systemLogRepository = new SystemLogRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        ReportFilter filter = readFilter(request);
        String format = request.getParameter("format");
        validateFormat(format);

        try {
            validateReportDays(filter);
            ReportData report = reportService.generate(filter);
            SessionUser user = (SessionUser) request.getSession().getAttribute("user");
            recordReportLog(user, filter, format);
            writeReport(response, report, filter, format);
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private ReportFilter readFilter(HttpServletRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        return ReportFilter.parse(request.getParameter("from"), request.getParameter("to"),
            request.getParameter("employeeId"), request.getParameter("modelId"),
            request.getParameter("paymentMethodId"), today);
    }

    private void validateFormat(String format) {
        if (format != null && !format.isBlank() && !"json".equalsIgnoreCase(format) && !"csv".equalsIgnoreCase(format)
            && !"xlsx".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Định dạng báo cáo phải là json, csv hoặc xlsx.");
        }
    }

    private void validateReportDays(ReportFilter filter) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            int maxDays = SystemConfigurationService.integer(connection, "REPORT_MAX_DAYS");
            // Cộng 1 vì báo cáo tính cả ngày bắt đầu và ngày kết thúc.
            long selectedDays = ChronoUnit.DAYS.between(filter.getFrom(), filter.getTo()) + 1;
            if (selectedDays > maxDays) {
                throw new IllegalArgumentException(
                    "Khoảng báo cáo tối đa " + maxDays + " ngày theo cấu hình hệ thống.");
            }
        }
    }

    private void recordReportLog(SessionUser user, ReportFilter filter, String format) throws SQLException {
        String action = "REPORT_EXPORT";
        if (format == null || "json".equalsIgnoreCase(format)) {
            action = "REPORT_VIEW";
        }
        try (Connection connection = Database.getConnection()) {
            systemLogRepository.record(connection, user.getUserId(), action, "REPORT", null,
                "filter=" + filter + "; format=" + format);
        }
    }

    private void writeReport(HttpServletResponse response, ReportData report, ReportFilter filter, String format)
        throws IOException {
        String filename = "report-" + filter.getFrom() + "-" + filter.getTo();
        response.setHeader("Cache-Control", "no-store");
        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".csv");
            ReportCsv.write(report, response.getWriter());
        } else if ("xlsx".equalsIgnoreCase(format)) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".xlsx");
            ReportXlsx.write(report, response.getOutputStream());
        } else {
            writeJson(response, report);
        }
    }
}
