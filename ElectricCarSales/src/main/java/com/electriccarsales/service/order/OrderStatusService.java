package com.electriccarsales.service;

import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.model.Order;

import java.util.Locale;
import java.util.Set;

public class OrderStatusService {
    private static final Set<String> ALLOWED_MANUAL_STATUSES = Set.of(
            "PENDING", "RESERVED", "PROCESSING", "CANCELLED");
    private final OrderDAO orderDAO = new OrderDAO();

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
                && (current.getPaidAmount().signum() > 0
                || "PAID".equalsIgnoreCase(current.getStatus())
                || "COMPLETED".equalsIgnoreCase(current.getStatus())
                || "DEPOSITED".equalsIgnoreCase(current.getStatus()))) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã có thanh toán.");
        }
        return orderDAO.updateStatus(orderId, normalized, employeeId, note);
    }
}
