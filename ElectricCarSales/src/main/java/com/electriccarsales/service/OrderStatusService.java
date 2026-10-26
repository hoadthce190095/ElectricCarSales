package com.electriccarsales.service.order;

import com.electriccarsales.dao.order.OrderStatusHistoryDAO;
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
