package com.electriccarsales.controller;

import com.electriccarsales.dao.CarInventoryDAO;
import com.electriccarsales.model.InventoryCarDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "InventoryDetailController", urlPatterns = {"/staff/inventory/detail"})
public class InventoryDetailController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String modelIdParam = request.getParameter("modelId");

            if (modelIdParam != null && !modelIdParam.isEmpty()) {
                int modelId = Integer.parseInt(modelIdParam);

                CarInventoryDAO dao = new CarInventoryDAO();
                // DAO giờ chỉ cần modelId
                List<InventoryCarDetail> carDetails = dao.getCarDetails(modelId);

                request.setAttribute("carDetails", carDetails);
                request.setAttribute("selectedLocation", "Kho tổng");
            }

            request.getRequestDispatcher("/staff/inventory-detail.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/staff/inventory");
        }
    }
}
