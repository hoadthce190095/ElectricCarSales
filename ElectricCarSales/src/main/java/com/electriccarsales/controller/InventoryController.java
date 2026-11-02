package com.electriccarsales.controller;

import com.electriccarsales.dao.CarInventoryDAO;
import com.electriccarsales.model.InventoryItem;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

// Khi gõ đường dẫn /staff/inventory, nó sẽ chạy vào đây
@WebServlet(name = "InventoryController", urlPatterns = {"/staff/inventory"})
public class InventoryController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Lấy dữ liệu từ DAO
            CarInventoryDAO dao = new CarInventoryDAO();
            List<InventoryItem> inventoryList = dao.getInventorySummary();

            // Gửi dữ liệu sang dashboard.jsp
            request.setAttribute("inventoryList", inventoryList);
            request.getRequestDispatcher("/staff/dashboard.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
