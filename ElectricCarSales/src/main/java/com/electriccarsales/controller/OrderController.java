package com.electriccarsales.controller;

import com.electriccarsales.service.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "OrderController", urlPatterns = {"/orders"})
public class OrderController extends HttpServlet {
    private OrderService service = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("orders", service.getAllOrders());
        request.getRequestDispatcher("customer/my-orders.jsp").forward(request, response);
    }
}