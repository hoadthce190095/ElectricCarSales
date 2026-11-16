package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.service.VehicleCatalogService;
import com.electriccarsales.service.PromotionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "VehicleDetailController", urlPatterns = "/detail")
public class VehicleDetailController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idValue = request.getParameter("id");
        VehicleCatalogService service = new VehicleCatalogService();
        CarModel car;
        int modelID;
        if (idValue != null && !idValue.isBlank()) {
            try {
                modelID = Integer.parseInt(idValue);
                if (modelID <= 0) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
                return;
            }

            try {
                car = service.getCarModelById(modelID);
            } catch (RuntimeException e) {
                log("Không thể tải thông tin xe với ModelID " + modelID, e);
                throw new ServletException("Không thể tải thông tin chi tiết xe.", e);
            }
        } else {
            String modelName = request.getParameter("name");
            String carLineName = request.getParameter("line");
            if ((modelName == null || modelName.isBlank()) && (carLineName == null || carLineName.isBlank())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vui lòng cung cấp tên hoặc mã xe.");
                return;
            }

            try {
                car = service.getCarModelByNameOrLine(modelName, carLineName);
            } catch (RuntimeException e) {
                log("Không thể tải thông tin xe theo tên/dòng xe.", e);
                throw new ServletException("Không thể tải thông tin chi tiết xe.", e);
            }
        }

        if (car == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy mẫu xe.");
            return;
        }

        modelID = car.getModelID();
        try {
            request.setAttribute("car", car);
            request.setAttribute("specification", service.getSpecificationByModelId(modelID));
            request.setAttribute("carImages", service.getImagesByModelId(modelID));
            try {
                request.setAttribute("activePromotions", PromotionService.activeForModel(modelID));
            } catch (SQLException e) {
                log("Không thể tải khuyến mãi của mẫu xe " + modelID, e);
                request.setAttribute("promotionLoadFailed", true);
            }
            request.getRequestDispatcher("/vehicle-detail.jsp").forward(request, response);
        } catch (RuntimeException e) {
            log("Không thể tải thông tin xe với ModelID " + modelID, e);
            throw new ServletException("Không thể tải thông tin chi tiết xe.", e);
        }
    }
}
