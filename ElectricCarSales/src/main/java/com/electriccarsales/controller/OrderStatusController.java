package com.electriccarsales.controller;

import com.electriccarsales.service.OrderStatusService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "OrderStatusController", urlPatterns = {
        "/order-status",
        "/staff/order/update-status",
        "/staff/order-status/update"
})
public class OrderStatusController extends HttpServlet {
    private final OrderStatusService service = new OrderStatusService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String orderId = request.getParameter("orderId");
        if (orderId == null || orderId.isBlank()) {
            orderId = request.getParameter("id");
        }
        if (orderId != null && !orderId.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/staff/transaction-detail?id=" + orderId.trim());
        } else {
            response.sendRedirect(request.getContextPath() + "/staff/transactions");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        int orderId = 1;
        try {
            orderId = Integer.parseInt(request.getParameter("orderId"));
            int statusId = Integer.parseInt(request.getParameter("statusId"));
            String note = request.getParameter("note");
            Integer employeeId = null;
            String empParam = request.getParameter("employeeId");
            if (empParam != null && !empParam.isBlank()) {
                try {
                    employeeId = Integer.parseInt(empParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            service.updateOrderStatus(orderId, statusId, employeeId, note);

            String redirectTo = request.getParameter("redirectTo");
            if ("list".equalsIgnoreCase(redirectTo)) {
                response.sendRedirect(request.getContextPath() + "/staff/transactions?statusUpdated=true");
            } else {
                response.sendRedirect(request.getContextPath() + "/staff/transaction-detail?id=" + orderId + "&statusUpdated=true");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/staff/transactions?error=true");
        }
    }
}
