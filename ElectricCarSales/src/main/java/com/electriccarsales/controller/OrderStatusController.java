package com.electriccarsales.controller;

import com.electriccarsales.service.OrderStatusService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "OrderStatusController", urlPatterns = {"/order-status"})
public class OrderStatusController extends HttpServlet {
    private OrderStatusService service = new OrderStatusService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int orderId = Integer.parseInt(request.getParameter("orderId"));
        String status = request.getParameter("status");
        service.updateOrderStatus(orderId, status);
        response.sendRedirect("orders");
    }
}