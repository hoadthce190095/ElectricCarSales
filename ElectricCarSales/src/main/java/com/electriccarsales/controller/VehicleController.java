package com.electriccarsales.controller;

import com.electriccarsales.model.CarImage;
import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WebServlet(name = "VehicleController", urlPatterns = {"/cars", "/api/catalogue"})
public class VehicleController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/api/catalogue".equals(request.getServletPath())) {
            showCatalogue(request, response);
            return;
        }

        showCarList(request, response);
    }

    private void showCarList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        VehicleCatalogService service = new VehicleCatalogService();
        List<CarModel> cars = service.getAllCarModels();
        Map<Integer, String> imagesByCarId = new HashMap<>();

        for (CarModel car : cars) {
            List<CarImage> images = service.getImagesByModelId(car.getModelID());
            String imageUrl = images == null || images.isEmpty()
                    ? "assets/images/default-car.png"
                    : images.get(0).getImageURL();
            imagesByCarId.put(car.getModelID(), imageUrl);
        }

        request.setAttribute("carList", cars);
        request.setAttribute("carImagesMap", imagesByCarId);
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    private void showCatalogue(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            List<CatalogVehicle> vehicles = new VehicleCatalogService().getCatalogVehicles();
            String modelId = request.getParameter("modelId");

            if (modelId != null && !modelId.isBlank()) {
                int parsedModelId = parseModelId(modelId);
                if (parsedModelId < 0) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
                    return;
                }
                List<CatalogVehicle> matchingVehicles = new ArrayList<>();
                for (CatalogVehicle vehicle : vehicles) {
                    if (vehicle.modelID() == parsedModelId) {
                        matchingVehicles.add(vehicle);
                    }
                }
                vehicles = matchingVehicles;
            } else {
                String[] requestedKeys = request.getParameterValues("key");
                if (requestedKeys != null) {
                    Set<String> keys = new HashSet<>(Arrays.asList(requestedKeys));
                    List<CatalogVehicle> matchingVehicles = new ArrayList<>();
                    for (CatalogVehicle vehicle : vehicles) {
                        if (keys.contains(comparisonKey(vehicle))) {
                            matchingVehicles.add(vehicle);
                        }
                    }
                    vehicles = matchingVehicles;
                }
            }

            StringBuilder modelsJson = new StringBuilder();
            for (CatalogVehicle vehicle : vehicles) {
                if (modelsJson.length() > 0) {
                    modelsJson.append(",");
                }
                modelsJson.append(toJson(vehicle));
            }
            String note = jsonString("Thông tin và thông số xe được lấy từ danh mục trong cơ sở dữ liệu.");
            response.getWriter().write("{\"note\":" + note + ",\"models\":[" + modelsJson + "]}");
        } catch (RuntimeException e) {
            log("Không thể tải danh mục xe từ cơ sở dữ liệu.", e);
            throw new ServletException("Không thể tải danh mục xe.", e);
        }
    }

    private int parseModelId(String value) {
        try {
            int modelId = Integer.parseInt(value);
            return modelId > 0 ? modelId : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String toJson(CatalogVehicle vehicle) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"modelID\":").append(vehicle.modelID())
                .append(",\"versionID\":").append(vehicle.versionID())
                .append(",\"comparisonKey\":").append(jsonString(comparisonKey(vehicle)))
                .append(",\"modelName\":").append(jsonString(vehicle.modelName()))
                .append(",\"carLine\":").append(jsonString(vehicle.carLine()))
                .append(",\"specificationVersion\":").append(jsonString(vehicle.specificationVersion()))
                .append(",\"priceVnd\":").append(jsonNumber(vehicle.priceVnd()))
                .append(",\"dimensions\":").append(jsonString(vehicle.dimensions()))
                .append(",\"batteryKwh\":").append(jsonNumber(vehicle.batteryKwh()))
                .append(",\"rangeKm\":").append(jsonNumber(vehicle.rangeKm()))
                .append(",\"maxPowerKw\":").append(jsonNumber(vehicle.maxPowerKw()))
                .append(",\"seats\":").append(jsonNumber(vehicle.seats()))
                .append(",\"rangeStandard\":").append(jsonString(vehicle.rangeStandard()))
                .append(",\"sourceUrl\":").append(jsonString(vehicle.sourceUrl()))
                .append(",\"priceSourceUrl\":").append(jsonString(vehicle.priceSourceUrl()))
                .append(",\"imageUrl\":").append(jsonString(vehicle.imageUrl()));
        json.append(",\"colors\":").append(toJsonArray(vehicle.colors()));
        json.append(",\"colorImages\":").append(toJsonObject(vehicle.colorImages()));
        json.append(",\"wheelType\":").append(jsonString(vehicle.wheelType()))
                .append(",\"airbagCount\":").append(jsonNumber(vehicle.airbagCount()))
                .append(",\"maxSpeedKmh\":").append(jsonNumber(vehicle.maxSpeedKmh()))
                .append(",\"rapidChargeMinutes\":").append(jsonNumber(vehicle.rapidChargeMinutes()))
                .append(",\"priceNote\":").append(jsonString(vehicle.priceNote()))
                .append(",\"specificationNote\":").append(jsonString(vehicle.specificationNote()));
        return json.append("}").toString();
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(jsonString(values.get(i)));
        }
        return json.append("]").toString();
    }

    private static String toJsonObject(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append(jsonString(entry.getKey()))
                    .append(":")
                    .append(jsonString(entry.getValue()));
            first = false;
        }
        return json.append("}").toString();
    }

    private static String comparisonKey(CatalogVehicle vehicle) {
        return vehicle.modelName().trim() + "-" + vehicle.specificationVersion().trim();
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