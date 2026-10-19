package com.electriccarsales.controller;

import com.electriccarsales.service.ConsultationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ConsultationController", urlPatterns = {"/consultation", "/customer/consultation", "/staff/consultations"})
public class ConsultationController extends HttpServlet {
    private ConsultationService service = new ConsultationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/staff/consultations".equals(path)) {
            request.getRequestDispatcher("/staff/consultation-management.jsp").forward(request, response);
        } else if ("/customer/consultation".equals(path)) {
            request.getRequestDispatcher("/customer/consultation.jsp").forward(request, response);
        } else {
            request.setAttribute("consultations", service.getAllRequests());
            request.getRequestDispatcher("customer/ai-assistant.jsp").forward(request, response);
        }
    }
}