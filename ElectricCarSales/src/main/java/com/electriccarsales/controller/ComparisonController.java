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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet(name = "ComparisonController", urlPatterns = "/comparison")
public class ComparisonController extends HttpServlet {

    private static final int MAX_COMPARISON_VEHICLES = 3;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] requestedKeys = request.getParameterValues("key");
        Set<String> comparisonKeys = requestedKeys == null
                ? Set.of()
                : Arrays.stream(requestedKeys)
                        .filter(key -> key != null && !key.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (comparisonKeys.isEmpty() || comparisonKeys.size() > MAX_COMPARISON_VEHICLES) {
            request.setAttribute("comparisonError", "Vui lòng chọn từ 1 đến 3 xe để so sánh.");
            request.getRequestDispatcher("/compare.jsp").forward(request, response);
            return;
        }

        try {
            List<CatalogVehicle> catalogue = new VehicleCatalogService().getCatalogVehicles();
            Map<String, CatalogVehicle> vehiclesByKey = catalogue.stream()
                    .collect(Collectors.toMap(
                            ComparisonController::comparisonKey,
                            Function.identity(),
                            (first, ignored) -> first));
            List<CatalogVehicle> vehicles = comparisonKeys.stream()
                    .map(vehiclesByKey::get)
                    .filter(vehicle -> vehicle != null)
                    .toList();

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
        List<ComparisonRow> rows = List.of(
                new ComparisonRow("Phiên bản", CatalogVehicle::specificationVersion),
                new ComparisonRow("Giá tham khảo", vehicle -> vehicle.priceVnd() == null
                        ? "Liên hệ" : formatNumber(vehicle.priceVnd(), " ₫")),
                new ComparisonRow("Quãng đường công bố", vehicle -> formatNumber(vehicle.rangeKm(), " km")),
                new ComparisonRow("Tiêu chuẩn quãng đường", CatalogVehicle::rangeStandard),
                new ComparisonRow("Dung lượng pin", vehicle -> formatNumber(vehicle.batteryKwh(), " kWh")),
                new ComparisonRow("Công suất tối đa", vehicle -> formatNumber(vehicle.maxPowerKw(), " kW")),
                new ComparisonRow("Số chỗ ngồi", vehicle -> formatNumber(vehicle.seats(), " chỗ")),
                new ComparisonRow("Kích thước", CatalogVehicle::dimensions),
                new ComparisonRow("Mâm xe", CatalogVehicle::wheelType),
                new ComparisonRow("Số túi khí", vehicle -> vehicle.airbagCount() == null || vehicle.airbagCount() <= 0
                        ? null : vehicle.airbagCount().toString()),
                new ComparisonRow("Tốc độ tối đa", vehicle -> formatNumber(vehicle.maxSpeedKmh(), " km/h")),
                new ComparisonRow("Thời gian sạc nhanh", vehicle -> formatNumber(vehicle.rapidChargeMinutes(), " phút")));

        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (ComparisonRow row : rows) {
            List<String> values = vehicles.stream()
                    .map(row.value())
                    .map(ComparisonController::valueOrPending)
                    .toList();
            result.add(Map.of("label", row.label(), "values", values));
        }
        return result;
    }

    private static List<Map<String, Object>> createColumns(List<CatalogVehicle> vehicles) {
        return vehicles.stream()
                .map(vehicle -> Map.<String, Object>of(
                        "modelID", vehicle.modelID(),
                        "modelName", valueOrPending(vehicle.modelName()),
                        "specificationVersion", valueOrPending(vehicle.specificationVersion()),
                        "imageUrl", vehicle.imageUrl() == null ? "" : vehicle.imageUrl()))
                .toList();
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

    private record ComparisonRow(String label, Function<CatalogVehicle, String> value) {
    }
}
