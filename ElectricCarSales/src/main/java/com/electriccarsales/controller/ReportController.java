package com.electriccarsales.controller;

import com.electriccarsales.dao.ConsultationRequestDAO;
import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.model.ConsultationRequest;
import com.electriccarsales.model.Order;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ReportController", urlPatterns = {"/staff/report", "/admin/report", "/staff/reports"})
public class ReportController extends HttpServlet {
    private OrderDAO orderDAO = new OrderDAO();
    private ConsultationRequestDAO consultationDAO = new ConsultationRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Order> orders = orderDAO.findAll();
            List<ConsultationRequest> consultations = consultationDAO.findAll();

            BigDecimal totalRevenue = BigDecimal.ZERO;
            int carsSold = 0;
            int pendingConsultations = 0;

            int countPending = 0;
            int countDeposited = 0;
            int countProcessing = 0;
            int countCompleted = 0;
            int countCancelled = 0;

            Map<String, int[]> modelCounts = new java.util.LinkedHashMap<>();
            Map<String, BigDecimal> modelRevenues = new java.util.LinkedHashMap<>();

            if (orders != null && !orders.isEmpty()) {
                for (Order o : orders) {
                    if (o.getOriginalAmount() != null) {
                        totalRevenue = totalRevenue.add(o.getOriginalAmount());
                    }
                    carsSold++;
                    String status = o.getStatus();
                    if ("1".equals(status)) countPending++;
                    else if ("3".equals(status)) countDeposited++;
                    else if ("4".equals(status)) countProcessing++;
                    else if ("6".equals(status)) countCompleted++;
                    else if ("7".equals(status)) countCancelled++;
                    else countProcessing++;

                    String vName = (o.getVehicleName() != null && !o.getVehicleName().trim().isEmpty()) ? o.getVehicleName() : "Khác";
                    int[] c = modelCounts.getOrDefault(vName, new int[]{0});
                    c[0]++;
                    modelCounts.put(vName, c);

                    BigDecimal curRev = modelRevenues.getOrDefault(vName, BigDecimal.ZERO);
                    if (o.getOriginalAmount() != null) {
                        modelRevenues.put(vName, curRev.add(o.getOriginalAmount()));
                    } else {
                        modelRevenues.put(vName, curRev);
                    }
                }
            }

            java.util.List<java.util.Map<String, Object>> vehicleStats = new java.util.ArrayList<>();
            for (Map.Entry<String, int[]> entry : modelCounts.entrySet()) {
                java.util.Map<String, Object> stat = new java.util.HashMap<>();
                stat.put("modelName", entry.getKey());
                stat.put("count", entry.getValue()[0]);
                stat.put("revenue", modelRevenues.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                vehicleStats.add(stat);
            }

            if (consultations != null) {
                for (ConsultationRequest c : consultations) {
                    if ("1".equals(c.getStatus()) || "PENDING".equalsIgnoreCase(c.getStatus())) {
                        pendingConsultations++;
                    }
                }
            }

            request.setAttribute("totalRevenue", totalRevenue);
            request.setAttribute("carsSold", carsSold);
            request.setAttribute("pendingConsultations", pendingConsultations);
            request.setAttribute("countPending", countPending);
            request.setAttribute("countDeposited", countDeposited);
            request.setAttribute("countProcessing", countProcessing);
            request.setAttribute("countCompleted", countCompleted);
            request.setAttribute("countCancelled", countCancelled);
            request.setAttribute("orders", orders);
            request.setAttribute("vehicleStats", vehicleStats);

            request.getRequestDispatcher("/staff/sales-report.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tải báo cáo");
        }
    }
}
