package com.electriccarsales.controller.consultation;

import com.electriccarsales.service.consultation.ConsultationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ConsultationController", urlPatterns = {"/consultation"})
public class ConsultationController extends HttpServlet {
    private ConsultationService service = new ConsultationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("consultations", service.getAllRequests());
        request.getRequestDispatcher("customer/ai-assistant.jsp").forward(request, response);
    }
}