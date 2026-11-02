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

@WebServlet("/staff/dashboard")
public class DashboardController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            CarInventoryDAO inventoryDAO = new CarInventoryDAO();
            // Lấy danh sách tồn kho từ CSDL
            List<InventoryItem> inventoryList = inventoryDAO.getInventorySummary();

            // Đẩy dữ liệu vào request với tên biến mà dashboard.jsp đang dùng
            req.setAttribute("inventoryList", inventoryList);

            // Forward sang trang giao diện
            req.getRequestDispatcher("/staff/dashboard.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi nếu cần
            req.getRequestDispatcher("/staff/dashboard.jsp").forward(req, resp);
        }
    }
}
