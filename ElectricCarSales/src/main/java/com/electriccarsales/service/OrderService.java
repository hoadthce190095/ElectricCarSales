package com.electriccarsales.service;

import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.model.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class OrderService {
    private static final boolean PAYMENT_TEST_MODE = true;
    private static final BigDecimal SCALE_FACTOR = new BigDecimal("1000");
    private OrderDAO orderDAO = new OrderDAO();

    public BigDecimal calculateGatewayAmount(BigDecimal originalAmount) {
        if (originalAmount == null) return BigDecimal.ZERO;
        if (PAYMENT_TEST_MODE) {
            return originalAmount.divide(SCALE_FACTOR, 2, RoundingMode.HALF_UP);
        }
        return originalAmount;
    }

    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    public Order getOrderById(int orderId) {
        return orderDAO.findById(orderId);
    }

    public void updateOrderStatus(int orderId, int statusId) {
        orderDAO.updateStatus(orderId, statusId);
    }

    public void createOrder(Order order) {
        orderDAO.insert(order);
    }
}