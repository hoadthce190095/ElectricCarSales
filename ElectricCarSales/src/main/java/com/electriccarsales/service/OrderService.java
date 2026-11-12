package com.electriccarsales.service;

import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.dao.OrderDetailDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.OrderDetail;
import com.electriccarsales.model.OrderStatusHistory;
import util.DBContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderDetailDAO orderDetailDAO = new OrderDetailDAO();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    public BigDecimal calculateGatewayAmount(BigDecimal originalAmount) {
        if (originalAmount == null)
            return BigDecimal.ZERO;
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

    public List<OrderDetail> getOrderDetails(int orderId) {
        return orderDetailDAO.findByOrderId(orderId);
    }

    public List<OrderStatusHistory> getOrderStatusHistory(int orderId) {
        return historyDAO.findByOrderId(orderId);
    }

    /**
     * Tạo đơn hàng hoàn chỉnh gồm Order, OrderDetail và OrderStatusHistory
     * Lấy giá xe thực tế từ bảng Cars theo quy chuẩn RDS.
     */
    public int createOrderWithDetail(int customerId, Integer employeeId, int carId,
                                    int paymentMethodId, Integer companyId, String note) throws SQLException {
        try (Connection conn = DBContext.openConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Lấy giá xe và kiểm tra xe sẵn sàng
                BigDecimal carPrice = null;
                String checkSql = "SELECT Price FROM Cars WHERE CarID = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, carId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            carPrice = rs.getBigDecimal("Price");
                        }
                    }
                }
                if (carPrice == null || carPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Không tìm thấy xe hợp lệ hoặc xe chưa có giá niêm yết.");
                }

                BigDecimal deposit = carPrice.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal remaining = carPrice.subtract(deposit);

                // 2. Tạo đơn hàng Order
                Order order = new Order();
                order.setCustomerId(customerId);
                order.setEmployeeId(employeeId);
                order.setPaymentMethodId(paymentMethodId);
                order.setOriginalAmount(carPrice);
                order.setDepositAmount(deposit);
                order.setRemainingAmount(remaining);
                order.setStatus("PENDING");
                order.setCompanyId(companyId);
                order.setNote(note);
                orderDAO.insert(order);

                // 3. Tạo chi tiết đơn hàng OrderDetail
                OrderDetail detail = new OrderDetail();
                detail.setOrderId(order.getId());
                detail.setVehicleId(carId);
                detail.setQuantity(1);
                detail.setUnitPrice(carPrice);
                orderDetailDAO.insert(conn, detail);

                // 4. Ghi lịch sử trạng thái OrderStatusHistory
                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(order.getId());
                history.setStatus("PENDING");
                history.setEmployeeId(employeeId);
                history.setNote("Tạo đơn hàng mới với xe #" + carId + " (Cọc 30%: " + deposit + ", Còn lại 70%: " + remaining + ").");
                historyDAO.insert(conn, history);

                conn.commit();
                return order.getId();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
