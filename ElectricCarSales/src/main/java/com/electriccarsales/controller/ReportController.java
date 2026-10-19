package com.electriccarsales.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ReportController", urlPatterns = {"/staff/report", "/admin/report"})
public class ReportController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Giả lập dữ liệu truyền ra view (Sau này bạn thay bằng hàm gọi từ DAO)
            request.setAttribute("totalRevenue", 1250000000);
            request.setAttribute("carsSold", 15);
            request.setAttribute("pendingConsultations", 8);

            // Forward tới trang giao diện báo cáo của bạn
            request.getRequestDispatcher("/staff/sales-report.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tải báo cáo");
        }
    }
}
