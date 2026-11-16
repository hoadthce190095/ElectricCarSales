package com.electriccarsales.controller;

import com.electriccarsales.model.Car;
import com.electriccarsales.model.InventoryItem;
import com.electriccarsales.service.CarInventoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "InventoryController", urlPatterns = {"/staff/inventory"})
public class InventoryController extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(InventoryController.class.getName());
    private CarInventoryService carInventoryService;

    @Override
    public void init() throws ServletException {
        carInventoryService = new CarInventoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String keyword = request.getParameter("q");
        if (keyword == null) {
            keyword = request.getParameter("keyword");
        }
        if (keyword != null) {
            keyword = keyword.trim();
        }

        String status = request.getParameter("status");
        if (status != null) {
            status = status.trim();
        }

        request.setAttribute("inventoryPage", Boolean.TRUE);
        request.setAttribute("searchKeyword", keyword != null ? keyword : "");
        request.setAttribute("selectedStatus", status != null ? status : "");

        try {
            List<Car> inventoryCars = carInventoryService.getInventoryCars(keyword, status);
            Map<String, Integer> inventoryStats = carInventoryService.getInventoryStats();
            List<InventoryItem> inventorySummary = carInventoryService.getInventorySummary();

            request.setAttribute("inventoryCars", inventoryCars);
            request.setAttribute("inventoryStats", inventoryStats);
            request.setAttribute("inventoryList", inventorySummary);

            request.getRequestDispatcher("/staff/dashboard.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn dữ liệu tồn kho từ cơ sở dữ liệu", e);
            request.getServletContext().log("Cannot load inventory summary", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            Map<String, Integer> fallbackStats = new LinkedHashMap<>();
            fallbackStats.put("totalCars", 0);
            fallbackStats.put("availableCars", 0);
            fallbackStats.put("reservedCars", 0);
            fallbackStats.put("depositedCars", 0);
            fallbackStats.put("soldCars", 0);
            fallbackStats.put("deliveredCars", 0);
            fallbackStats.put("soldDeliveredCars", 0);

            request.setAttribute("inventoryCars", Collections.emptyList());
            request.setAttribute("inventoryStats", fallbackStats);
            request.setAttribute("inventoryList", Collections.emptyList());
            request.setAttribute("inventoryLoadError",
                    "Không thể tải tồn kho từ cơ sở dữ liệu: " + e.getMessage() + ". Hãy kiểm tra kết nối SQL Server rồi thử lại.");

            request.getRequestDispatcher("/staff/dashboard.jsp").forward(request, response);
        }
    }
}
