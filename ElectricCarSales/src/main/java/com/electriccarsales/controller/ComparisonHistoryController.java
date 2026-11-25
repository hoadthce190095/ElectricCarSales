package com.electriccarsales.controller;

import com.electriccarsales.dao.ComparisonItemDAO;
import com.electriccarsales.dao.ComparisonSessionDAO;
import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.model.ComparisonItem;
import com.electriccarsales.model.ComparisonSession;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@WebServlet(name = "ComparisonHistoryController", urlPatterns = "/api/comparison/history")
public class ComparisonHistoryController extends JsonServlet {

    private final ComparisonSessionDAO sessionDAO = new ComparisonSessionDAO();
    private final ComparisonItemDAO itemDAO = new ComparisonItemDAO();
    private final VehicleCatalogService catalogService = new VehicleCatalogService();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer userId = resolveUserId(request);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_OK);
            JSON.writeValue(response.getOutputStream(), Map.of(
                    "authenticated", false,
                    "history", List.of()
            ));
            return;
        }

        try {
            List<ComparisonSession> sessions = sessionDAO.getByUserId(userId);
            List<CatalogVehicle> allVehicles = catalogService.getCatalogVehicles();
            Map<String, CatalogVehicle> vehiclesByKey = allVehicles.stream()
                    .collect(Collectors.toMap(ComparisonHistoryController::comparisonKey, v -> v, (first, ignored) -> first));
            Map<Integer, CatalogVehicle> vehiclesByModelId = allVehicles.stream()
                    .collect(Collectors.toMap(
                            CatalogVehicle::modelID,
                            v -> v,
                            (first, ignored) -> first
                    ));

            List<Map<String, Object>> historyList = new ArrayList<>();
            for (ComparisonSession session : sessions) {
                List<ComparisonItem> items = itemDAO.getBySessionId(session.getComparisonSessionID());
                List<Map<String, Object>> vehicleSummaries = new ArrayList<>();
                List<String> comparisonKeys = new ArrayList<>();

                for (ComparisonItem item : items) {
                    CatalogVehicle cv = item.getComparisonKey() == null || item.getComparisonKey().isBlank()
                            ? vehiclesByModelId.get(item.getModelID())
                            : vehiclesByKey.get(item.getComparisonKey());
                    if (cv != null) {
                        Map<String, Object> summary = new LinkedHashMap<>();
                        summary.put("modelID", cv.modelID());
                        summary.put("modelName", cv.modelName());
                        summary.put("specificationVersion", cv.specificationVersion());
                        summary.put("carLine", cv.carLine());
                        summary.put("priceVnd", cv.priceVnd());
                        summary.put("imageUrl", cv.imageUrl() != null ? cv.imageUrl() : "");
                        summary.put("rangeKm", cv.rangeKm());
                        if (cv.stockCarID() != null) {
                            summary.put("stockCarID", cv.stockCarID());
                            summary.put("stockColorName", cv.stockColorName());
                            summary.put("colors", cv.colors());
                        }
                        String key = comparisonKey(cv);
                        summary.put("comparisonKey", key);
                        comparisonKeys.add(key);
                        vehicleSummaries.add(summary);
                    }
                }

                if (!vehicleSummaries.isEmpty()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("sessionId", session.getComparisonSessionID());
                    entry.put("createDate", session.getCreateDate() != null
                            ? session.getCreateDate().format(DATE_FORMATTER) : "");
                    entry.put("keys", comparisonKeys);
                    entry.put("vehicles", vehicleSummaries);
                    historyList.add(entry);
                }
            }

            response.setStatus(HttpServletResponse.SC_OK);
            JSON.writeValue(response.getOutputStream(), Map.of(
                    "authenticated", true,
                    "history", historyList
            ));
        } catch (Exception e) {
            log("Error fetching comparison history for user: " + userId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSON.writeValue(response.getOutputStream(), Map.of(
                    "error", "Không thể tải lịch sử so sánh."
            ));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer userId = resolveUserId(request);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_OK);
            JSON.writeValue(response.getOutputStream(), Map.of(
                    "authenticated", false,
                    "saved", false,
                    "message", "Khách vãng lai lưu lịch sử qua trình duyệt."
            ));
            return;
        }

        try {
            SaveHistoryRequest body = readJson(request, SaveHistoryRequest.class);
            List<CatalogVehicle> allVehicles = catalogService.getCatalogVehicles();
            Map<String, CatalogVehicle> vehiclesByKey = allVehicles.stream()
                    .collect(Collectors.toMap(ComparisonHistoryController::comparisonKey, v -> v, (first, ignored) -> first));
            List<CatalogVehicle> selectedVehicles;
            if (body.keys() != null && !body.keys().isEmpty()) {
                selectedVehicles = new ArrayList<>(new LinkedHashSet<>(body.keys())).stream()
                        .map(vehiclesByKey::get)
                        .filter(vehicle -> vehicle != null)
                        .toList();
            } else {
                List<Integer> modelIds = body.modelIds();
                selectedVehicles = (modelIds == null ? List.<Integer>of() : modelIds).stream()
                        .map(modelId -> modelId == null ? null : allVehicles.stream()
                                .filter(vehicle -> vehicle.modelID() == modelId && vehicle.stockCarID() == null)
                                .findFirst().orElse(null))
                        .filter(vehicle -> vehicle != null)
                        .toList();
            }

            if (selectedVehicles.size() < 2) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSON.writeValue(response.getOutputStream(), Map.of(
                        "error", "Cần ít nhất 2 mẫu xe để lưu so sánh."
                ));
                return;
            }

            List<Integer> modelIds = new ArrayList<>();
            for (CatalogVehicle vehicle : selectedVehicles) {
                if (modelIds.contains(vehicle.modelID())) {
                    response.setStatus(409);
                    JSON.writeValue(response.getOutputStream(), Map.of("error", "Lịch sử chỉ lưu được mẫu xe. Không thể lưu hai phiên bản của cùng một mẫu xe."));
                    return;
                }
                modelIds.add(vehicle.modelID());
            }
            int sessionId = sessionDAO.saveModels(userId, modelIds);
            if (sessionId > 0) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                JSON.writeValue(response.getOutputStream(), Map.of(
                        "success", true,
                        "sessionId", sessionId
                ));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JSON.writeValue(response.getOutputStream(), Map.of(
                        "error", "Không thể tạo phiên so sánh."
                ));
            }
        } catch (Exception e) {
            log("Error saving comparison history for user: " + userId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSON.writeValue(response.getOutputStream(), Map.of(
                    "error", "Không thể lưu phiên so sánh."
            ));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer userId = resolveUserId(request);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JSON.writeValue(response.getOutputStream(), Map.of("error", "Chưa đăng nhập."));
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSON.writeValue(response.getOutputStream(), Map.of("error", "Thiếu ID phiên so sánh."));
            return;
        }

        try {
            int sessionId = Integer.parseInt(idParam.trim());
            boolean deleted = sessionDAO.deleteSession(sessionId, userId);
            response.setStatus(HttpServletResponse.SC_OK);
            JSON.writeValue(response.getOutputStream(), Map.of("success", deleted));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSON.writeValue(response.getOutputStream(), Map.of("error", "ID không hợp lệ."));
        } catch (Exception e) {
            log("Error deleting comparison session for user: " + userId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSON.writeValue(response.getOutputStream(), Map.of("error", "Không thể xóa phiên so sánh."));
        }
    }

    private Integer resolveUserId(HttpServletRequest request) {
        Object sessionUserObj = request.getSession().getAttribute("user");
        if (sessionUserObj instanceof SessionUser su) {
            return su.getUserId();
        }
        Object legacyId = request.getSession().getAttribute("userID");
        if (legacyId instanceof Number n && n.intValue() > 0) {
            return n.intValue();
        }
        return null;
    }

    private static String comparisonKey(CatalogVehicle vehicle) {
        String base = vehicle.modelName().trim() + "-" + vehicle.specificationVersion().trim();
        return vehicle.stockCarID() == null || vehicle.priceVnd() == null
                ? base : base + "-STOCK-PRICE-" + vehicle.priceVnd().stripTrailingZeros().toPlainString();
    }

    private record SaveHistoryRequest(List<Integer> modelIds, List<String> keys) {}
}
