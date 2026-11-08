package com.electriccarsales.controller;

import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet(name = "ComparisonController", urlPatterns = "/comparison")
public class ComparisonController extends HttpServlet {

    private static final int MAX_COMPARISON_VEHICLES = 3;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] requestedKeys = request.getParameterValues("key");
        List<String> comparisonKeys = new ArrayList<>();

        if (requestedKeys != null) {
            for (String key : requestedKeys) {
                if (key != null && !key.isBlank() && !comparisonKeys.contains(key.trim())) {
                    comparisonKeys.add(key.trim());
                }
            }
        }

        if (comparisonKeys.isEmpty() || comparisonKeys.size() > MAX_COMPARISON_VEHICLES) {
            request.setAttribute("comparisonError", "Vui lòng chọn từ 1 đến 3 xe để so sánh.");
            request.getRequestDispatcher("/compare.jsp").forward(request, response);
            return;
        }

        try {
            List<CatalogVehicle> catalogue = new VehicleCatalogService().getCatalogVehicles();
            List<CatalogVehicle> vehicles = new ArrayList<>();
            for (String key : comparisonKeys) {
                CatalogVehicle vehicle = findVehicle(catalogue, key);
                if (vehicle != null) {
                    vehicles.add(vehicle);
                }
            }

            if (vehicles.isEmpty()) {
                request.setAttribute("comparisonError", "Không tìm thấy xe đã chọn. Vui lòng quay lại và chọn lại xe.");
            } else {
                if (vehicles.size() != comparisonKeys.size()) {
                    request.setAttribute("comparisonNotice", "Một số lựa chọn không còn trong danh mục; bảng hiển thị các xe tìm thấy.");
                }
                request.setAttribute("comparisonColumns", createColumns(vehicles));
                request.setAttribute("comparisonRows", createRows(vehicles));
                request.setAttribute("comparisonSummary", "Đang đối chiếu " + vehicles.size() + " mẫu xe.");
            }
            request.getRequestDispatcher("/compare.jsp").forward(request, response);
        } catch (RuntimeException e) {
            log("Không thể tải dữ liệu so sánh xe.", e);
            throw new ServletException("Không thể tải dữ liệu so sánh xe.", e);
        }
    }

    private static List<Map<String, Object>> createRows(List<CatalogVehicle> vehicles) {
        String[] labels = {
                "Phiên bản", "Giá tham khảo", "Quãng đường công bố", "Tiêu chuẩn quãng đường",
                "Dung lượng pin", "Công suất tối đa", "Số chỗ ngồi", "Kích thước", "Mâm xe",
                "Số túi khí", "Tốc độ tối đa", "Thời gian sạc nhanh"
        };
        List<Map<String, Object>> result = new ArrayList<>();

        for (int row = 0; row < labels.length; row++) {
            List<String> values = new ArrayList<>();
            for (CatalogVehicle vehicle : vehicles) {
                values.add(valueOrPending(getRowValue(labels[row], vehicle)));
            }
            result.add(Map.of("label", labels[row], "values", values));
        }
        return result;
    }

    private static CatalogVehicle findVehicle(List<CatalogVehicle> catalogue, String key) {
        for (CatalogVehicle vehicle : catalogue) {
            if (comparisonKey(vehicle).equals(key)) {
                return vehicle;
            }
        }
        return null;
    }

    private static String getRowValue(String label, CatalogVehicle vehicle) {
        return switch (label) {
            case "Phiên bản" -> vehicle.specificationVersion();
            case "Giá tham khảo" -> vehicle.priceVnd() == null
                    ? "Liên hệ" : formatNumber(vehicle.priceVnd(), " ₫");
            case "Quãng đường công bố" -> formatNumber(vehicle.rangeKm(), " km");
            case "Tiêu chuẩn quãng đường" -> vehicle.rangeStandard();
            case "Dung lượng pin" -> formatNumber(vehicle.batteryKwh(), " kWh");
            case "Công suất tối đa" -> formatNumber(vehicle.maxPowerKw(), " kW");
            case "Số chỗ ngồi" -> formatNumber(vehicle.seats(), " chỗ");
            case "Kích thước" -> vehicle.dimensions();
            case "Mâm xe" -> vehicle.wheelType();
            case "Số túi khí" -> vehicle.airbagCount() == null || vehicle.airbagCount() <= 0
                    ? null : vehicle.airbagCount().toString();
            case "Tốc độ tối đa" -> formatNumber(vehicle.maxSpeedKmh(), " km/h");
            case "Thời gian sạc nhanh" -> formatNumber(vehicle.rapidChargeMinutes(), " phút");
            default -> "Đang cập nhật";
        };
    }

    private static List<Map<String, Object>> createColumns(List<CatalogVehicle> vehicles) {
        List<Map<String, Object>> columns = new ArrayList<>();
        for (CatalogVehicle vehicle : vehicles) {
            columns.add(Map.of(
                    "modelID", vehicle.modelID(),
                    "modelName", valueOrPending(vehicle.modelName()),
                    "specificationVersion", valueOrPending(vehicle.specificationVersion()),
                    "imageUrl", vehicle.imageUrl() == null ? "" : vehicle.imageUrl()));
        }
        return columns;
    }

    private static String comparisonKey(CatalogVehicle vehicle) {
        return vehicle.modelName().trim() + "-" + vehicle.specificationVersion().trim();
    }

    private static String formatNumber(Number value, String suffix) {
        if (value == null) {
            return null;
        }
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(1);
        format.setMinimumFractionDigits(0);
        return format.format(value) + suffix;
    }

    private static String valueOrPending(String value) {
        return value == null || value.isBlank() ? "Đang cập nhật" : value;
    }
}
