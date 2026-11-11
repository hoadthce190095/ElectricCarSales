package com.electriccarsales.service.order;

import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.model.Order;
import java.math.BigDecimal;
import java.util.List;

public class OrderService {
    private OrderDAO orderDAO = new OrderDAO();


    public BigDecimal calculateGatewayAmount(BigDecimal originalAmount) {
        if (originalAmount == null) return BigDecimal.ZERO;
        return originalAmount;
    }

    public List<Order> getCustomerOrders(int customerId) {
        return orderDAO.findByCustomerId(customerId);
    }

    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    public Order getOrderById(int orderId) {
        return orderDAO.findById(orderId);
    }

    public void createOrder(Order order) {
        orderDAO.insert(order);
    }
}
