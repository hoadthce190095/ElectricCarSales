package com.electriccarsales.controller;

import com.electriccarsales.service.OrderStatusService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "OrderStatusController", urlPatterns = {"/order-status", "/staff/order/update-status"})
public class OrderStatusController extends HttpServlet {
    private OrderStatusService service = new OrderStatusService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int statusId = Integer.parseInt(request.getParameter("statusId"));
            service.updateOrderStatus(orderId, statusId);

            String redirectUrl = request.getParameter("redirectUrl");
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                response.sendRedirect(redirectUrl + "?updated=true");
            } else {
                response.sendRedirect(request.getContextPath() + "/staff/transaction-detail?id=" + orderId + "&updated=true");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/staff/transactions?error=true");
        }
    }
}