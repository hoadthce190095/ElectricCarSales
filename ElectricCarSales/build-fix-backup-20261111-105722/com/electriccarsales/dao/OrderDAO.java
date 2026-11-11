package com.electriccarsales.dao.order;

import com.electriccarsales.model.Order;
import util.DBContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDAO {
    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final ConnectionProvider connections;

    public OrderDAO() {
        this(DBContext::openConnection);
    }

    OrderDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    public List<Order> findAll() {
        String sql = """
            SELECT o.OrderID,o.CustomerID,o.EmployeeID,o.PaymentMethodID,o.TotalAmount,
                   o.DepositAmount,o.RemainingAmount,o.OrderDate,o.Note,s.StatusName
            FROM Orders o JOIN OrderStatuses s ON s.OrderStatusID=o.OrderStatusID
            ORDER BY o.OrderDate DESC,o.OrderID DESC
            """;
        try (Connection connection = connections.open();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rows = statement.executeQuery()) {
            List<Order> orders = new ArrayList<>();
            while (rows.next()) {
                Order order = new Order();
                order.setId(rows.getInt("OrderID"));
                order.setCustomerId(rows.getInt("CustomerID"));
                order.setEmployeeId(rows.getObject("EmployeeID", Integer.class));
                order.setPaymentMethodId(rows.getInt("PaymentMethodID"));
                order.setOriginalAmount(rows.getBigDecimal("TotalAmount"));
                order.setDepositAmount(rows.getBigDecimal("DepositAmount"));
                order.setRemainingAmount(rows.getBigDecimal("RemainingAmount"));
                order.setStatus(rows.getString("StatusName"));
                order.setOrderDate(rows.getTimestamp("OrderDate"));
                order.setNote(rows.getString("Note"));
                orders.add(order);
            }
            return orders;
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải danh sách đơn hàng.", exception);
        }
    }

    /** Customer-owned orders only; never expose other customers' transactions. */
    public List<Order> findByCustomerId(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        String sql = "SELECT o.OrderID,o.CustomerID,o.EmployeeID,o.PaymentMethodID,o.TotalAmount,"
                + "o.DepositAmount,o.RemainingAmount,o.OrderDate,o.Note,s.StatusName,"
                + "m.ModelName FROM Orders o JOIN OrderStatuses s ON s.OrderStatusID=o.OrderStatusID "
                + "LEFT JOIN OrderDetails d ON d.OrderID=o.OrderID "
                + "LEFT JOIN Cars c ON c.CarID=d.CarID "
                + "LEFT JOIN CarModels m ON m.ModelID=c.ModelID "
                + "WHERE o.CustomerID=? ORDER BY o.OrderDate DESC,o.OrderID DESC";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rows.next()) {
                    Order order = new Order();
                    order.setId(rows.getInt("OrderID"));
                    order.setCustomerId(rows.getInt("CustomerID"));
                    order.setEmployeeId(rows.getObject("EmployeeID", Integer.class));
                    order.setPaymentMethodId(rows.getInt("PaymentMethodID"));
                    order.setOriginalAmount(rows.getBigDecimal("TotalAmount"));
                    order.setDepositAmount(rows.getBigDecimal("DepositAmount"));
                    order.setRemainingAmount(rows.getBigDecimal("RemainingAmount"));
                    order.setStatus(rows.getString("StatusName"));
                    order.setOrderDate(rows.getTimestamp("OrderDate"));
                    order.setNote(rows.getString("Note"));
                    order.setVehicleName(rows.getString("ModelName"));
                    orders.add(order);
                }
                return orders;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải đơn hàng của khách.", e);
        }
    }

    public void insert(Order order) {
        if (order == null || order.getCustomerId() <= 0 || order.getPaymentMethodId() <= 0
            || (order.getEmployeeId() != null && order.getEmployeeId() <= 0)) {
            throw new IllegalArgumentException("Đơn hàng cần khách hàng và phương thức thanh toán hợp lệ.");
        }
        BigDecimal total = order.getOriginalAmount() == null ? null : money(order.getOriginalAmount());
        BigDecimal deposit = money(order.getDepositAmount() == null ? BigDecimal.ZERO : order.getDepositAmount());
        if (total == null || total.signum() < 0 || deposit.signum() < 0 || deposit.compareTo(total) > 0) {
            throw new IllegalArgumentException("Tổng tiền và tiền đặt cọc không hợp lệ.");
        }
        BigDecimal remaining = order.getRemainingAmount() == null ? total.subtract(deposit)
            : money(order.getRemainingAmount());
        if (remaining.signum() < 0 || deposit.add(remaining).compareTo(total) != 0) {
            throw new IllegalArgumentException("Tiền đặt cọc và tiền còn lại phải bằng tổng tiền đơn hàng.");
        }
        String status = order.getStatus() == null ? "PENDING" : order.getStatus().trim().toUpperCase(Locale.ROOT);
        Timestamp date = order.getOrderDate() == null ? new Timestamp(System.currentTimeMillis()) : order.getOrderDate();
        String sql = """
            INSERT INTO Orders(CustomerID,EmployeeID,OrderStatusID,PaymentMethodID,OrderDate,
                               TotalAmount,DepositAmount,RemainingAmount,Note)
            VALUES(?,?,(SELECT OrderStatusID FROM OrderStatuses WHERE StatusName=?),?,?,?,?,?,?)
            """;
        try (Connection connection = connections.open();
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, order.getCustomerId());
            if (order.getEmployeeId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, order.getEmployeeId());
            }
            statement.setString(3, status);
            statement.setInt(4, order.getPaymentMethodId());
            statement.setTimestamp(5, date);
            statement.setBigDecimal(6, total);
            statement.setBigDecimal(7, deposit);
            statement.setBigDecimal(8, remaining);
            statement.setString(9, order.getNote());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không tạo được đơn hàng.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setId(keys.getInt(1));
                }
            }
            order.setStatus(status);
            order.setOrderDate(date);
            order.setDepositAmount(deposit);
            order.setRemainingAmount(remaining);
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tạo đơn hàng.", exception);
        }
    }

    private BigDecimal money(BigDecimal amount) {
        BigDecimal exact;
        try {
            exact = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Số tiền chỉ được có tối đa hai chữ số thập phân.", exception);
        }
        if (exact.precision() > 18) {
            throw new IllegalArgumentException("Số tiền vượt quá giới hạn lưu trữ DECIMAL(18,2).");
        }
        return exact;
    }
}
