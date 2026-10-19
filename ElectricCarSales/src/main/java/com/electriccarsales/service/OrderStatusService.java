package com.electriccarsales.service;

import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.OrderStatusHistory;

public class OrderStatusService {
    private OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    public void updateOrderStatus(int orderId, String newStatus) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setStatus(newStatus);
        historyDAO.insert(history);
    }
}