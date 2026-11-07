package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "VehicleDetailController", urlPatterns = "/detail")
public class VehicleDetailController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        VehicleCatalogService service = new VehicleCatalogService();
        CarModel car;
        try {
            car = findCar(request, response, service);
        } catch (RuntimeException e) {
            log("Không thể tải thông tin chi tiết xe.", e);
            throw new ServletException("Không thể tải thông tin chi tiết xe.", e);
        }
        if (response.getStatus() == HttpServletResponse.SC_BAD_REQUEST) {
            return;
        }

        if (car == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy mẫu xe.");
            return;
        }

        try {
            request.setAttribute("car", car);
            request.setAttribute("specification", service.getSpecificationByModelId(car.getModelID()));
            request.setAttribute("carImages", service.getImagesByModelId(car.getModelID()));
            request.getRequestDispatcher("/vehicle-detail.jsp").forward(request, response);
        } catch (RuntimeException e) {
            log("Không thể tải thông tin xe với ModelID " + car.getModelID(), e);
            throw new ServletException("Không thể tải thông tin chi tiết xe.", e);
        }
    }

    private CarModel findCar(HttpServletRequest request, HttpServletResponse response,
                             VehicleCatalogService service) throws IOException {
        String id = request.getParameter("id");
        if (id != null && !id.isBlank()) {
            try {
                int modelID = Integer.parseInt(id);
                if (modelID > 0) {
                    return service.getCarModelById(modelID);
                }
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
                return null;
            }
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã xe không hợp lệ.");
            return null;
        }

        String modelName = request.getParameter("name");
        String carLineName = request.getParameter("line");
        if ((modelName == null || modelName.isBlank())
                && (carLineName == null || carLineName.isBlank())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vui lòng cung cấp tên hoặc mã xe.");
            return null;
        }
        return service.getCarModelByNameOrLine(modelName, carLineName);
    }
}
