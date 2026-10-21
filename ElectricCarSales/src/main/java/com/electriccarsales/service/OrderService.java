package com.electriccarsales.service.order;

import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.model.order.Order;
import java.math.BigDecimal;
import java.util.List;

public class OrderService {
    private static final boolean PAYMENT_TEST_MODE = true;
    private static final BigDecimal SCALE_FACTOR = new BigDecimal("1000");
    private OrderDAO orderDAO = new OrderDAO();


    public BigDecimal calculateGatewayAmount(BigDecimal originalAmount) {
        if (originalAmount == null) return BigDecimal.ZERO;
        if (PAYMENT_TEST_MODE) {
            return originalAmount.divide(SCALE_FACTOR, 2, BigDecimal.ROUND_HALF_UP);
        }
        return originalAmount;
    }

    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    public void createOrder(Order order) {
        orderDAO.insert(order);
    }
}