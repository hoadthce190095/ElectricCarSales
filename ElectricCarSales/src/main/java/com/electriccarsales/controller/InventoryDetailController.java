package com.electriccarsales.controller;

import com.electriccarsales.model.InventoryCarDetail;
import com.electriccarsales.service.CarInventoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "InventoryDetailController", urlPatterns = {"/staff/inventory/detail"})
public class InventoryDetailController extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(InventoryDetailController.class.getName());
    private CarInventoryService carInventoryService;

    @Override
    public void init() throws ServletException {
        carInventoryService = new CarInventoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            String modelIdParam = request.getParameter("modelId");
            List<InventoryCarDetail> carDetails = Collections.emptyList();

            if (modelIdParam != null && !modelIdParam.isBlank()) {
                int modelId = Integer.parseInt(modelIdParam.trim());
                carDetails = carInventoryService.getCarDetails(modelId);
                request.setAttribute("selectedLocation", "Kho tổng");
            }

            request.setAttribute("carDetails", carDetails);
            request.getRequestDispatcher("/staff/inventory-detail.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải chi tiết xe tồn kho", e);
            request.getServletContext().log("Cannot load inventory vehicle details", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Không thể tải chi tiết xe từ cơ sở dữ liệu. Kiểm tra cấu hình kết nối SQL Server.");
        }
    }
}
