package com.electriccarsales.controller;

import com.electriccarsales.model.ConsultationRequest;
import com.electriccarsales.service.ConsultationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

@WebServlet(name = "ConsultationController", urlPatterns = {
    "/consultation",
    "/customer/consultation",
    "/staff/consultations",
    "/staff/consultation/update"
})
public class ConsultationController extends HttpServlet {
    private ConsultationService service = new ConsultationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/staff/consultations".equals(path)) {
            List<ConsultationRequest> list = service.getAllRequests();
            request.setAttribute("consultationList", list);
            request.getRequestDispatcher("/staff/consultation-management.jsp").forward(request, response);
        } else if ("/customer/consultation".equals(path)) {
            request.getRequestDispatcher("/customer/consultation.jsp").forward(request, response);
        } else {
            request.setAttribute("consultations", service.getAllRequests());
            request.getRequestDispatcher("customer/ai-assistant.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();

        if ("/customer/consultation".equals(path)) {
            try {
                String content = request.getParameter("content");
                String modelIdStr = request.getParameter("modelId");
                int modelId = (modelIdStr != null && !modelIdStr.isEmpty()) ? Integer.parseInt(modelIdStr) : 0;

                ConsultationRequest req = new ConsultationRequest();
                req.setCustomerId(1); // Mặc định khách hàng đang đăng nhập hoặc khách vãng lai ID 1
                req.setModelId(modelId);
                req.setContent(content != null ? content : "Yêu cầu tư vấn thông tin xe điện");
                req.setCreatedAt(new Timestamp(System.currentTimeMillis()));

                service.createRequest(req);
                response.sendRedirect(request.getContextPath() + "/customer/consultation?success=true");
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/customer/consultation?error=true");
            }
        } else if ("/staff/consultation/update".equals(path)) {
            try {
                int requestId = Integer.parseInt(request.getParameter("requestId"));
                int statusId = Integer.parseInt(request.getParameter("statusId"));
                service.updateRequestStatus(requestId, statusId);
                response.sendRedirect(request.getContextPath() + "/staff/consultations?updated=true");
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/staff/consultations?error=true");
            }
        }
    }
}