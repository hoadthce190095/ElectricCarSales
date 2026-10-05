package vn.edu.fpt.electriccarsales.dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Inclusive calendar dates in the database business time zone.
 */
public class ReportFilter {
    private LocalDate from;
    private LocalDate to;
    private Integer employeeId;
    private Integer modelId;
    private Integer paymentMethodId;

    public ReportFilter() {
    }

    public ReportFilter(LocalDate from, LocalDate to, Integer employeeId, Integer modelId, Integer paymentMethodId) {
        this.from = from;
        this.to = to;
        this.employeeId = employeeId;
        this.modelId = modelId;
        this.paymentMethodId = paymentMethodId;
        validate();
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getModelId() {
        return modelId;
    }

    public void setModelId(Integer modelId) {
        this.modelId = modelId;
    }

    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public void validate() {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
        }
        if (from.getYear() < 1 || to.getYear() > 9998) {
            throw new IllegalArgumentException("Ngày báo cáo nằm ngoài phạm vi hỗ trợ.");
        }
        if ((employeeId != null && employeeId <= 0) || (modelId != null && modelId <= 0)
            || (paymentMethodId != null && paymentMethodId <= 0)) {
            throw new IllegalArgumentException("Mã bộ lọc phải là số nguyên dương.");
        }
    }

    public static ReportFilter parse(String from, String to, String employeeId, String modelId, String paymentMethodId,
        LocalDate today) {
        try {
            LocalDate end = blank(to) ? today : LocalDate.parse(to);
            LocalDate start = blank(from) ? end.withDayOfMonth(1) : LocalDate.parse(from);
            return new ReportFilter(start, end, id(employeeId), id(modelId), id(paymentMethodId));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày phải có định dạng YYYY-MM-DD.");
        }
    }

    private static Integer id(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Mã bộ lọc phải là số nguyên dương.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public String toString() {
        return "ReportFilter{from=" + from + ", to=" + to + ", employeeId=" + employeeId + ", modelId=" + modelId
            + ", paymentMethodId=" + paymentMethodId + '}';
    }
}
