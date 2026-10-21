package com.electriccarsales.controller;

import com.electriccarsales.model.Delivery;
import com.electriccarsales.service.DeliveryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "DeliveryController", urlPatterns = {"/delivery"})
public class DeliveryController extends HttpServlet {

    private DeliveryService deliveryService;

    @Override
    public void init() throws ServletException {
        deliveryService = new DeliveryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        List<Delivery> deliveries = deliveryService.getAllDeliveries();
        request.setAttribute("deliveryList", deliveries);

        request.getRequestDispatcher("/delivery.jsp").forward(request, response);
    }
}