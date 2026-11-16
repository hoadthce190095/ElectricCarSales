package com.electriccarsales.controller;

import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.CarImage;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

@WebServlet(name = "VehicleController", urlPatterns = {"/cars", "/api/catalogue"})
public class VehicleController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/api/catalogue".equals(request.getServletPath())) {
            doGetCatalogue(request, response);
            return;
        }

        VehicleCatalogService service = new VehicleCatalogService();


        List<CarModel> carList = service.getAllCarModels();


        Map<Integer, String> carImagesMap = new HashMap<>();
        for (CarModel car : carList) {
            List<CarImage> images = service.getImagesByModelId(car.getModelID());
            if (images != null && !images.isEmpty()) {
                carImagesMap.put(car.getModelID(), images.get(0).getImageURL());
            } else {
                carImagesMap.put(car.getModelID(), "assets/images/default-car.png");
            }
        }


        request.setAttribute("carList", carList);
        request.setAttribute("carImagesMap", carImagesMap);


        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    private void doGetCatalogue(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            List<CatalogVehicle> vehicles = new VehicleCatalogService().getCatalogVehicles();
            String[] requestedKeys = request.getParameterValues("key");
            String modelId = request.getParameter("modelId");
            String carId = request.getParameter("carId");

            if (modelId != null && !modelId.isBlank()) {
                int parsedModelId;
                try {
                    parsedModelId = Integer.parseInt(modelId);
                } catch (NumberFormatException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
                    return;
                }
                if (parsedModelId <= 0) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
                    return;
                }
                vehicles = vehicles.stream().filter(vehicle -> vehicle.modelID() == parsedModelId).toList();
            }
            if (carId != null && !carId.isBlank()) {
                int parsedCarId;
                try {
                    parsedCarId = Integer.parseInt(carId);
                } catch (NumberFormatException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe vật lý không hợp lệ.");
                    return;
                }
                if (parsedCarId <= 0) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe vật lý không hợp lệ.");
                    return;
                }
                vehicles = vehicles.stream().filter(vehicle ->
                        vehicle.stockCarID() != null && vehicle.stockCarID() == parsedCarId).toList();
            } else if (requestedKeys != null && (modelId == null || modelId.isBlank())) {
                List<String> keys = Arrays.asList(requestedKeys);
                vehicles = vehicles.stream().filter(vehicle -> keys.contains(comparisonKey(vehicle))).toList();
            }

            Map<Integer, List<com.electriccarsales.model.AdminPromotion>> activePromos = Map.of();
            try (java.sql.Connection conn = util.DBContext.openConnection()) {
                activePromos = com.electriccarsales.dao.PromotionDAO.findAllActiveByModel(conn);
            } catch (Exception ex) {
                log("Không thể tải danh mục khuyến mãi cho danh mục xe.", ex);
            }
            final Map<Integer, List<com.electriccarsales.model.AdminPromotion>> promos = activePromos;
            String payload = vehicles.stream()
                    .map(v -> toJson(v, promos.getOrDefault(v.modelID(), List.of())))
                    .collect(Collectors.joining(",", "{\"note\":" +
                            jsonString("Thông tin và thông số xe được lấy từ danh mục trong cơ sở dữ liệu.") +
                            ",\"models\":[", "]}"));
            response.getWriter().write(payload);
        } catch (Exception e) {
            log("Không thể tải danh mục xe từ cơ sở dữ liệu. Sử dụng phản hồi dự phòng.", e);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"note\":" +
                    jsonString("Thông tin xe đang hiển thị theo danh mục dự phòng.") +
                    ",\"models\":[]}");
        }
    }

    private static String toJson(CatalogVehicle vehicle) {
        return toJson(vehicle, List.of());
    }

    private static String toJson(CatalogVehicle vehicle, List<com.electriccarsales.model.AdminPromotion> promotions) {
        String promoJson = (promotions == null || promotions.isEmpty()) ? "[]" :
                promotions.stream().map(p -> "{"
                        + "\"id\":" + p.getPromotionId()
                        + ",\"name\":" + jsonString(p.getPromotionName())
                        + ",\"discountType\":" + jsonString(p.getDiscountType())
                        + ",\"discountValue\":" + (p.getDiscountValue() == null ? "null" : p.getDiscountValue().stripTrailingZeros().toPlainString())
                        + ",\"description\":" + jsonString(p.getDescription())
                        + ",\"endDate\":" + jsonString(p.getEndDate() == null ? null : p.getEndDate().toLocalDate().toString())
                        + "}").collect(Collectors.joining(",", "[", "]"));

        return "{"
                + "\"modelID\":" + vehicle.modelID()
                + ",\"versionID\":" + vehicle.versionID()
                + ",\"stockCarID\":" + (vehicle.stockCarID() == null ? "null" : vehicle.stockCarID())
                + ",\"stockColorName\":" + jsonString(vehicle.stockColorName())
                + ",\"comparisonKey\":" + jsonString(comparisonKey(vehicle))
                + ",\"modelName\":" + jsonString(vehicle.modelName())
                + ",\"carLine\":" + jsonString(vehicle.carLine())
                + ",\"specificationVersion\":" + jsonString(vehicle.specificationVersion())
                + ",\"priceVnd\":" + jsonNumber(vehicle.priceVnd())
                + ",\"dimensions\":" + jsonString(vehicle.dimensions())
                + ",\"batteryKwh\":" + jsonNumber(vehicle.batteryKwh())
                + ",\"rangeKm\":" + jsonNumber(vehicle.rangeKm())
                + ",\"maxPowerKw\":" + jsonNumber(vehicle.maxPowerKw())
                + ",\"seats\":" + jsonNumber(vehicle.seats())
                + ",\"rangeStandard\":" + jsonString(vehicle.rangeStandard())
                + ",\"sourceUrl\":" + jsonString(vehicle.sourceUrl())
                + ",\"priceSourceUrl\":" + jsonString(vehicle.priceSourceUrl())
                + ",\"imageUrl\":" + jsonString(vehicle.imageUrl())
                + ",\"colors\":" + vehicle.colors().stream().map(VehicleController::jsonString)
                        .collect(Collectors.joining(",", "[", "]"))
                + ",\"colorImages\":" + vehicle.colorImages().entrySet().stream()
                        .map(entry -> jsonString(entry.getKey()) + ":" + jsonString(entry.getValue()))
                        .collect(Collectors.joining(",", "{", "}"))
                + ",\"wheelType\":" + jsonString(vehicle.wheelType())
                + ",\"airbagCount\":" + jsonNumber(vehicle.airbagCount())
                + ",\"maxSpeedKmh\":" + jsonNumber(vehicle.maxSpeedKmh())
                + ",\"rapidChargeMinutes\":" + jsonNumber(vehicle.rapidChargeMinutes())
                + ",\"priceNote\":" + jsonString(vehicle.priceNote())
                + ",\"specificationNote\":" + jsonString(vehicle.specificationNote())
                + ",\"promotions\":" + promoJson
                + "}";
    }

    private static String comparisonKey(CatalogVehicle vehicle) {
        String base = vehicle.modelName().trim() + "-" + vehicle.specificationVersion().trim();
        return vehicle.stockCarID() == null ? base : base + "-STOCK-" + vehicle.stockCarID();
    }

    private static String jsonNumber(Number value) {
        return value == null ? "null" : value.toString();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }
}
