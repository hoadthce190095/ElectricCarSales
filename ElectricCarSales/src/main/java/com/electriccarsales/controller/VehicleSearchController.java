package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "VehicleSearchController", urlPatterns = "/cars-search")
public class VehicleSearchController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String keyword = request.getParameter("q");
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.length() > 100) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Từ khóa tìm kiếm tối đa 100 ký tự.");
                return;
            }
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        Integer brandId;
        Integer carLineId;
        Integer colorId;
        BigDecimal maxPrice;
        BigDecimal minRange;
        String sort = request.getParameter("sort");
        try {
            brandId = parseOptionalPositiveInteger(request, "brandId");
            carLineId = parseOptionalPositiveInteger(request, "carLineId");
            colorId = parseOptionalPositiveInteger(request, "colorId");
            maxPrice = parseOptionalNonNegativeDecimal(request, "maxPrice");
            minRange = parseOptionalNonNegativeDecimal(request, "minRange");
            if (sort == null || sort.isBlank()) {
                sort = "recommended";
            } else if (!List.of("recommended", "price-asc", "price-desc", "range-desc").contains(sort)) {
                throw new IllegalArgumentException("Kiểu sắp xếp không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        try {
            VehicleCatalogService service = new VehicleCatalogService();
            List<CarModel> carList = service.searchCarModels(
                    keyword, brandId, carLineId, maxPrice, minRange, colorId, sort);
            String matchingModelIds = carList.stream()
                    .map(CarModel::getModelID)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            request.setAttribute("serverSearchApplied", true);
            request.setAttribute("serverSearchModelIds", matchingModelIds);
            request.setAttribute("searchKeyword", keyword);
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (RuntimeException e) {
            log("Không thể tìm kiếm xe trong danh mục.", e);
            throw new ServletException("Không thể tìm kiếm xe trong danh mục.", e);
        }
    }

    private Integer parseOptionalPositiveInteger(HttpServletRequest request, String parameterName) {
        String value = request.getParameter(parameterName);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tham số " + parameterName + " không hợp lệ.", e);
        }
        throw new IllegalArgumentException("Tham số " + parameterName + " không hợp lệ.");
    }

    private BigDecimal parseOptionalNonNegativeDecimal(HttpServletRequest request, String parameterName) {
        String value = request.getParameter(parameterName);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() >= 0) {
                return parsed;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tham số " + parameterName + " không hợp lệ.", e);
        }
        throw new IllegalArgumentException("Tham số " + parameterName + " không hợp lệ.");
    }
}
