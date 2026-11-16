package com.electriccarsales.dao.order;

import com.electriccarsales.model.Order;
import com.electriccarsales.model.OrderStatusHistory;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
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

    public Order findById(int orderId) {
        String sql = "SELECT o.OrderID,o.CustomerID,o.EmployeeID,o.PaymentMethodID,o.TotalAmount,"
                + "o.DepositAmount,o.RemainingAmount,o.OrderDate,o.Note,s.StatusName,"
                + "m.ModelName,pm.MethodName,ct.Status AS ContractStatus,"
                + "o.CompanyID,fc.CompanyName,"
                + "COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p "
                + "          JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "          WHERE p.OrderID=o.OrderID AND ps.StatusID='COMPLETED'), 0) AS PaidAmount "
                + "FROM Orders o JOIN OrderStatuses s ON s.OrderStatusID=o.OrderStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID "
                + "LEFT JOIN FinancialCompanies fc ON fc.CompanyID=o.CompanyID "
                + "LEFT JOIN Contracts ct ON ct.OrderID=o.OrderID "
                + "LEFT JOIN OrderDetails d ON d.OrderID=o.OrderID "
                + "LEFT JOIN Cars c ON c.CarID=d.CarID "
                + "LEFT JOIN CarModels m ON m.ModelID=c.ModelID "
                + "WHERE o.OrderID=?";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                Order order = new Order();
                order.setId(row.getInt("OrderID"));
                order.setCustomerId(row.getInt("CustomerID"));
                order.setEmployeeId(row.getObject("EmployeeID", Integer.class));
                order.setPaymentMethodId(row.getInt("PaymentMethodID"));
                order.setOriginalAmount(row.getBigDecimal("TotalAmount"));
                order.setDepositAmount(row.getBigDecimal("DepositAmount"));
                order.setRemainingAmount(row.getBigDecimal("RemainingAmount"));
                order.setStatus(row.getString("StatusName"));
                order.setOrderDate(row.getTimestamp("OrderDate"));
                order.setNote(row.getString("Note"));
                order.setVehicleName(row.getString("ModelName"));
                order.setPaymentMethodName(row.getString("MethodName"));
                order.setContractStatus(row.getString("ContractStatus"));
                order.setCompanyId(row.getObject("CompanyID", Integer.class));
                order.setCompanyName(row.getString("CompanyName"));
                order.setPaidAmount(row.getBigDecimal("PaidAmount"));
                return order;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải đơn hàng.", exception);
        }
    }

    public boolean updateStatus(int orderId, String status, Integer employeeId, String note) {
        String sql = "UPDATE Orders SET OrderStatusID=(SELECT TOP 1 OrderStatusID FROM OrderStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))=UPPER(LTRIM(RTRIM(?)))), "
                + "EmployeeID=COALESCE(?,EmployeeID) WHERE OrderID=?";
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if ("CANCELLED".equalsIgnoreCase(status)) {
                    try (PreparedStatement ensureOrderStatus = connection.prepareStatement(
                            "IF NOT EXISTS (SELECT 1 FROM OrderStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))='CANCELLED') "
                                    + "INSERT INTO OrderStatuses(StatusName, Description) VALUES ('CANCELLED', N'Order has been cancelled')")) {
                        ensureOrderStatus.executeUpdate();
                    }
                    try (PreparedStatement ensureCarStatus = connection.prepareStatement(
                            "IF NOT EXISTS (SELECT 1 FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))='AVAILABLE') "
                                    + "INSERT INTO CarStatuses(StatusName, Description) VALUES (N'AVAILABLE', N'Xe sẵn sàng trong kho')")) {
                        ensureCarStatus.executeUpdate();
                    }
                }
                statement.setString(1, status);
                if (employeeId == null || employeeId <= 0) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, employeeId);
                }
                statement.setInt(3, orderId);
                if (statement.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }

                // Khi hủy hợp lệ, đồng bộ Orders, Cars và lịch sử trạng thái theo schema
                if ("CANCELLED".equalsIgnoreCase(status)) {
                    Integer carId = null;
                    try (PreparedStatement findCar = connection.prepareStatement(
                            "SELECT CarID FROM OrderDetails WHERE OrderID = ?")) {
                        findCar.setInt(1, orderId);
                        try (ResultSet rs = findCar.executeQuery()) {
                            if (rs.next()) {
                                carId = rs.getInt("CarID");
                            }
                        }
                    }
                    if (carId != null && carId > 0) {
                        try (PreparedStatement updateCar = connection.prepareStatement(
                            "UPDATE Cars SET CarStatusID = (SELECT TOP 1 CarStatusID FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))='AVAILABLE') WHERE CarID = ?")) {
                            updateCar.setInt(1, carId);
                            updateCar.executeUpdate();
                        }
                        try (PreparedStatement deleteDetail = connection.prepareStatement(
                                "DELETE FROM OrderDetails WHERE OrderID = ?")) {
                            deleteDetail.setInt(1, orderId);
                            deleteDetail.executeUpdate();
                        }
                    }
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setStatus(status);
                history.setEmployeeId(employeeId);
                history.setNote(note);
                new OrderStatusHistoryDAO().insert(connection, history);
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể cập nhật trạng thái đơn hàng.", exception);
        }
    }

    /** Customer-owned orders only; never expose other customers' transactions. */
    public List<Order> findByCustomerId(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        String sql = "SELECT o.OrderID,o.CustomerID,o.EmployeeID,o.PaymentMethodID,o.TotalAmount,"
                + "o.DepositAmount,o.RemainingAmount,o.OrderDate,o.Note,s.StatusName,"
                + "m.ModelName,pm.MethodName,ct.Status AS ContractStatus,"
                + "o.CompanyID,fc.CompanyName,"
                + "COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p "
                + "          JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "          WHERE p.OrderID=o.OrderID AND ps.StatusID='COMPLETED'), 0) AS PaidAmount "
                + "FROM Orders o JOIN OrderStatuses s ON s.OrderStatusID=o.OrderStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID "
                + "LEFT JOIN FinancialCompanies fc ON fc.CompanyID=o.CompanyID "
                + "LEFT JOIN Contracts ct ON ct.OrderID=o.OrderID "
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
                    order.setPaymentMethodName(rows.getString("MethodName"));
                    order.setContractStatus(rows.getString("ContractStatus"));
                    order.setCompanyId(rows.getObject("CompanyID", Integer.class));
                    order.setCompanyName(rows.getString("CompanyName"));
                    order.setPaidAmount(rows.getBigDecimal("PaidAmount"));
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
