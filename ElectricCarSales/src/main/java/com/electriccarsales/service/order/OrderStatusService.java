package com.electriccarsales.service;

import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.dao.OrderStatusDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.OrderStatus;
import com.electriccarsales.model.OrderStatusHistory;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrderStatusService {
    private static final Set<String> ALLOWED_MANUAL_STATUSES = Set.of(
            "PENDING", "RESERVED", "PROCESSING", "CANCELLED");
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderStatusDAO statusDAO = new OrderStatusDAO();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    public List<OrderStatus> getAllStatuses() {
        return statusDAO.findAll();
    }

    public List<OrderStatusHistory> getOrderStatusHistory(int orderId) {
        return historyDAO.findByOrderId(orderId);
    }

    public void updateOrderStatus(int orderId, String newStatus) {
        updateOrderStatus(orderId, newStatus, null, null);
    }

    public boolean updateOrderStatus(int orderId, String newStatus, Integer employeeId, String note) {
        if (orderId <= 0 || newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Thông tin cập nhật trạng thái đơn hàng không hợp lệ.");
        }
        String normalized = newStatus.trim().toUpperCase(Locale.ROOT);
        if ("DEPOSITED".equals(normalized) || "PAID".equals(normalized)) {
            throw new IllegalStateException("Trạng thái " + normalized
                    + " chỉ được cập nhật tự động sau khi Kế toán hoặc Cổng thanh toán xác nhận đủ tiền thật theo RDS.");
        }
        if ("COMPLETED".equals(normalized)) {
            throw new IllegalStateException("Trạng thái COMPLETED chỉ được cập nhật sau khi xuất hóa đơn và xác nhận bàn giao xe theo RDS.");
        }
        if (!ALLOWED_MANUAL_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + normalized);
        }
        Order current = orderDAO.findById(orderId);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
        }
        if ("CANCELLED".equals(normalized)
                && ("PAID".equalsIgnoreCase(current.getStatus()) || "COMPLETED".equalsIgnoreCase(current.getStatus()))) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã thanh toán đủ 100% hoặc đã bàn giao xe.");
        }
        return orderDAO.updateStatus(orderId, normalized, employeeId, note);
    }
}
