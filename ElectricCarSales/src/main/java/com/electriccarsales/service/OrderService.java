package com.electriccarsales.service;

import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.dao.OrderDetailDAO;
import com.electriccarsales.dao.OrderStatusDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.OrderDetail;
import com.electriccarsales.model.OrderStatus;
import com.electriccarsales.model.OrderStatusHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class OrderService {
    // Chế độ test VNPay: đổi tiền sang mức nhỏ ở bước gửi cổng thanh toán
    private static final boolean VNPAY_TEST_MODE = true;
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAO();
    private final OrderStatusDAO orderStatusDAO = new OrderStatusDAO();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    public BigDecimal calculateGatewayAmount(BigDecimal originalAmount) {
        // Tuyệt đối không dùng hàm này để ghi đè vào Order.originalAmount!
        if (VNPAY_TEST_MODE) {
            return originalAmount.divide(new BigDecimal("10000"), 0, RoundingMode.HALF_UP);
        }
        return originalAmount;
    }

    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    public List<Order> getOrdersByCustomerId(int customerId) {
        return orderDAO.findByCustomerId(customerId);
    }

    public Order getOrderById(int orderId) {
        return orderDAO.findById(orderId);
    }

    public List<OrderDetail> getOrderDetails(int orderId) {
        return orderDetailDAO.findByOrderId(orderId);
    }

    public void updateOrderStatus(int orderId, int statusId) {
        orderDAO.updateStatus(orderId, statusId);
    }

    public void createOrder(Order order) {
        orderDAO.insert(order);
    }

    public int createSalesTransaction(Order order, int carId, BigDecimal salesPrice, Integer employeeId) {
        return createSalesTransaction(order, carId, 0, salesPrice, employeeId);
    }

    /**
     * Tạo giao dịch bán xe mới bao gồm:
     * 1. Bản ghi đơn hàng (Orders)
     * 2. Chi tiết xe bán trong đơn (OrderDetails)
     * 3. Lịch sử trạng thái ban đầu (OrderStatusHistory)
     */
    public int createSalesTransaction(Order order, int carId, int modelId, BigDecimal salesPrice, Integer employeeId) {
        if (order == null) {
            return 0;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        order.setOrderDate(now);
        if (employeeId != null && employeeId > 0) {
            order.setEmployeeId(employeeId);
        }

        BigDecimal total = order.getOriginalAmount() != null ? order.getOriginalAmount() : (salesPrice != null ? salesPrice : BigDecimal.ZERO);
        BigDecimal deposit = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;
        if (deposit.compareTo(total) > 0 && total.compareTo(BigDecimal.ZERO) > 0) {
            deposit = total;
        }
        BigDecimal remaining = total.subtract(deposit).max(BigDecimal.ZERO);

        order.setOriginalAmount(total);
        order.setDepositAmount(deposit);
        order.setRemainingAmount(remaining);

        int initialStatusId = order.getOrderStatusId();
        if (initialStatusId <= 0) {
            if (total.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(BigDecimal.ZERO) == 0) {
                initialStatusId = 5; // PAID
            } else if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                initialStatusId = 3; // DEPOSITED
            } else {
                initialStatusId = 1; // PENDING
            }
            order.setOrderStatusId(initialStatusId);
            order.setStatus(String.valueOf(initialStatusId));
        }

        int newOrderId = orderDAO.insertAndReturnId(order);
        if (newOrderId > 0) {
            int resolvedCarId = orderDAO.resolveOrAllocateCar(carId, modelId, salesPrice != null && salesPrice.compareTo(BigDecimal.ZERO) > 0 ? salesPrice : total);
            if (resolvedCarId > 0) {
                OrderDetail detail = new OrderDetail();
                detail.setOrderId(newOrderId);
                detail.setVehicleId(resolvedCarId);
                detail.setQuantity(1);
                detail.setUnitPrice(salesPrice != null && salesPrice.compareTo(BigDecimal.ZERO) > 0 ? salesPrice : total);
                orderDetailDAO.insert(detail);
            }

            OrderStatus statusObj = orderStatusDAO.findById(initialStatusId);
            String statusDisplay = statusObj != null ? statusObj.getDisplayName() : OrderStatus.resolveDisplayName(initialStatusId, null, null);
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrderId(newOrderId);
            history.setOrderStatusId(initialStatusId);
            history.setStatus(String.valueOf(initialStatusId));
            history.setEmployeeId(employeeId);
            history.setChangedAt(now);
            history.setNote(order.getNote() != null && !order.getNote().isBlank()
                    ? "Khởi tạo giao dịch (" + statusDisplay + "): " + order.getNote()
                    : "Khởi tạo giao dịch bán xe - Trạng thái: " + statusDisplay);
            historyDAO.insert(history);
        }
        return newOrderId;
    }

    public List<Map<String, Object>> getAvailableCarsForSale() {
        return orderDAO.getAvailableCarsForSale();
    }

    public List<Map<String, Object>> getCarModelsWithPrices() {
        return orderDAO.getCarModelsWithPrices();
    }

    public List<Map<String, Object>> getPaymentMethods() {
        return orderDAO.getPaymentMethods();
    }

    public List<Map<String, Object>> getEmployees() {
        return orderDAO.getEmployees();
    }
}
