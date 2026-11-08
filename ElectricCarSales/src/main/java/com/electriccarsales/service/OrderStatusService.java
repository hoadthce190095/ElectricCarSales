package com.electriccarsales.service;

import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.dao.OrderStatusDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.OrderStatus;
import com.electriccarsales.model.OrderStatusHistory;

import java.sql.Timestamp;
import java.util.List;

public class OrderStatusService {
    private OrderDAO orderDAO = new OrderDAO();
    private OrderStatusDAO statusDAO = new OrderStatusDAO();
    private OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    public List<OrderStatus> getAllStatuses() {
        return statusDAO.findAll();
    }

    public OrderStatus getStatusById(int statusId) {
        return statusDAO.findById(statusId);
    }

    public List<OrderStatusHistory> getHistoryByOrderId(int orderId) {
        return historyDAO.findByOrderId(orderId);
    }

    public void updateOrderStatus(int orderId, int statusId) {
        updateOrderStatus(orderId, statusId, null, null);
    }

    public void updateOrderStatus(int orderId, int statusId, Integer employeeId, String note) {
        orderDAO.updateStatus(orderId, statusId);
        OrderStatus statusObj = statusDAO.findById(statusId);
        String statusDisplay = statusObj != null
                ? statusObj.getDisplayName()
                : OrderStatus.resolveDisplayName(statusId, OrderStatusDAO.toStatusCode(statusId), null);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOrderStatusId(statusId);
        history.setStatus(String.valueOf(statusId));
        history.setStatusName(statusDisplay);
        history.setEmployeeId(employeeId);
        history.setChangedAt(new Timestamp(System.currentTimeMillis()));
        history.setNote(note != null && !note.isBlank()
                ? note.trim()
                : "Cập nhật trạng thái đơn hàng sang: " + statusDisplay);
        historyDAO.insert(history);
    }
}
