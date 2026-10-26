package com.electriccarsales.service;

import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.OrderStatusHistory;
import java.sql.Timestamp;

public class OrderStatusService {
    private OrderDAO orderDAO = new OrderDAO();
    private OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    public void updateOrderStatus(int orderId, int statusId) {
        orderDAO.updateStatus(orderId, statusId);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setStatus(String.valueOf(statusId));
        history.setChangedAt(new Timestamp(System.currentTimeMillis()));
        historyDAO.insert(history);
    }
}