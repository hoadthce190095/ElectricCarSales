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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ReportController", urlPatterns = {"/staff/report", "/admin/report", "/staff/reports"})
public class ReportController extends HttpServlet {
    private final OrderDAO orderDAO = new OrderDAO();
    private final ConsultationRequestDAO consultationDAO = new ConsultationRequestDAO();

    public static class VehicleStat {
        private String modelName;
        private int count;
        private BigDecimal revenue;

        public VehicleStat(String modelName, int count, BigDecimal revenue) {
            this.modelName = modelName;
            this.count = count;
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
        }

        public String getModelName() {
            return modelName;
        }

        public int getCount() {
            return count;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }
    }

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

            Map<String, VehicleStatAccumulator> vehicleStatsMap = new LinkedHashMap<>();

            if (orders != null && !orders.isEmpty()) {
                for (Order o : orders) {
                    String status = o.getStatus() != null ? o.getStatus().trim() : "";
                    if ("1".equals(status) || "PENDING".equalsIgnoreCase(status)) {
                        countPending++;
                    } else if ("2".equals(status) || "DEPOSITED".equalsIgnoreCase(status) || "3".equals(status) || "RESERVED".equalsIgnoreCase(status)) {
                        countDeposited++;
                    } else if ("4".equals(status) || "PROCESSING".equalsIgnoreCase(status) || "5".equals(status) || "PAYMENT_PENDING".equalsIgnoreCase(status) || "READY_FOR_DELIVERY".equalsIgnoreCase(status)) {
                        countProcessing++;
                    } else if ("6".equals(status) || "COMPLETED".equalsIgnoreCase(status)) {
                        countCompleted++;
                    } else if ("7".equals(status) || "CANCELLED".equalsIgnoreCase(status)) {
                        countCancelled++;
                    } else {
                        countProcessing++;
                    }

                    if (!"7".equals(status) && !"CANCELLED".equalsIgnoreCase(status)) {
                        BigDecimal amount = o.getOriginalAmount() != null ? o.getOriginalAmount() : BigDecimal.ZERO;
                        totalRevenue = totalRevenue.add(amount);
                        carsSold++;

                        String vName = o.getVehicleName();
                        if (vName == null || vName.isBlank()) {
                            vName = "Dòng xe khác";
                        }
                        VehicleStatAccumulator acc = vehicleStatsMap.computeIfAbsent(vName, VehicleStatAccumulator::new);
                        acc.count++;
                        acc.revenue = acc.revenue.add(amount);
                    }
                }
            }

            if (consultations != null && !consultations.isEmpty()) {
                for (ConsultationRequest c : consultations) {
                    if ("1".equals(c.getStatus()) || "PENDING".equalsIgnoreCase(c.getStatus())) {
                        pendingConsultations++;
                    }
                }
            }

            List<VehicleStat> vehicleStats = new ArrayList<>();
            for (VehicleStatAccumulator acc : vehicleStatsMap.values()) {
                vehicleStats.add(new VehicleStat(acc.modelName, acc.count, acc.revenue));
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
            request.setAttribute("reportControllerLoaded", true);

            request.getRequestDispatcher("/staff/sales-report.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tải báo cáo bán hàng");
        }
    }

    private static class VehicleStatAccumulator {
        String modelName;
        int count;
        BigDecimal revenue = BigDecimal.ZERO;

        VehicleStatAccumulator(String modelName) {
            this.modelName = modelName;
        }
    }
}
