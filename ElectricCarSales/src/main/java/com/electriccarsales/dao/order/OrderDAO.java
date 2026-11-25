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
                + "(SELECT TOP 1 h.Note FROM OrderStatusHistory h "
                + " JOIN OrderStatuses hs ON hs.OrderStatusID=h.OrderStatusID "
                + " WHERE h.OrderID=o.OrderID AND UPPER(LTRIM(RTRIM(hs.StatusName)))='CANCELLED' "
                + " ORDER BY h.ChangedDate DESC, h.OrderStatusHistoryID DESC) AS CancelReason,"
                + "COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p "
                + "          JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "          WHERE p.OrderID=o.OrderID AND ps.StatusID='COMPLETED'), 0) AS PaidAmount "
                + "FROM Orders o LEFT JOIN OrderStatuses s ON s.OrderStatusID=o.OrderStatusID "
                + "LEFT JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID "
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
                String modelName = row.getString("ModelName");
                if ((modelName == null || modelName.isBlank()) && order.getNote() != null && order.getNote().contains("Mẫu xe:")) {
                    int idx = order.getNote().indexOf("Mẫu xe:");
                    int end = order.getNote().indexOf(",", idx);
                    if (end < 0) end = order.getNote().indexOf(";", idx);
                    if (end < 0) end = order.getNote().indexOf("\n", idx);
                    if (end < 0) end = order.getNote().length();
                    modelName = order.getNote().substring(idx + 7, end).trim();
                }
                order.setVehicleName(modelName != null && !modelName.isBlank() ? modelName : "—");
                order.setPaymentMethodName(row.getString("MethodName"));
                order.setContractStatus(row.getString("ContractStatus"));
                order.setCompanyId(row.getObject("CompanyID", Integer.class));
                order.setCompanyName(row.getString("CompanyName"));
                order.setPaidAmount(row.getBigDecimal("PaidAmount"));
                order.setCancelReason(row.getString("CancelReason"));
                return order;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải đơn hàng.", exception);
        }
    }

    public boolean updateStatus(int orderId, String status, Integer employeeId, String note) {
        String sql = "UPDATE Orders SET OrderStatusID=(SELECT TOP 1 OrderStatusID FROM OrderStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))=UPPER(LTRIM(RTRIM(?)))), "
                + "EmployeeID=COALESCE(?,EmployeeID) WHERE OrderID=?";
        if ("CANCELLED".equalsIgnoreCase(status)) {
            String allowed = employeeId == null ? "('PENDING')" : "('PENDING','RESERVED')";
            sql += " AND OrderStatusID IN (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName IN " + allowed + ")"
                    + " AND NOT EXISTS (SELECT 1 FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID"
                    + " WHERE p.OrderID=Orders.OrderID AND ps.StatusID='COMPLETED')";
        } else if ("PENDING".equalsIgnoreCase(status)) {
            sql += " AND OrderStatusID NOT IN (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName='PROCESSING')";
        }
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if ("CANCELLED".equalsIgnoreCase(status)) {
                    ensureStatusRow(connection,
                            "SELECT TOP 1 OrderStatusID FROM OrderStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))='CANCELLED'",
                            "INSERT INTO OrderStatuses(StatusName, Description) VALUES ('CANCELLED', N'Order has been cancelled')");
                    ensureStatusRow(connection,
                            "SELECT TOP 1 CarStatusID FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))='AVAILABLE'",
                            "INSERT INTO CarStatuses(StatusName, Description) VALUES (N'AVAILABLE', N'Xe sẵn sàng trong kho')");
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
                    ensureStatusRow(connection,
                            "SELECT TOP 1 PaymentStatusID FROM PaymentStatuses WHERE UPPER(LTRIM(RTRIM(StatusID)))='CANCELLED'",
                            "INSERT INTO PaymentStatuses(StatusID, Description) VALUES ('CANCELLED', N'Payment cancelled because its order was cancelled')");
                    try (PreparedStatement cancelPendingPayments = connection.prepareStatement(
                            "UPDATE Payments SET PaymentStatusID=(SELECT TOP 1 PaymentStatusID FROM PaymentStatuses "
                                    + "WHERE UPPER(LTRIM(RTRIM(StatusID)))='CANCELLED') WHERE OrderID=? AND PaymentStatusID="
                                    + "(SELECT TOP 1 PaymentStatusID FROM PaymentStatuses WHERE UPPER(LTRIM(RTRIM(StatusID)))='PENDING')")) {
                        cancelPendingPayments.setInt(1, orderId);
                        cancelPendingPayments.executeUpdate();
                    }

                    releaseVehicleAndPreserveSnapshot(connection, orderId, note);
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
                + "(SELECT TOP 1 h.Note FROM OrderStatusHistory h "
                + " JOIN OrderStatuses hs ON hs.OrderStatusID=h.OrderStatusID "
                + " WHERE h.OrderID=o.OrderID AND UPPER(LTRIM(RTRIM(hs.StatusName)))='CANCELLED' "
                + " ORDER BY h.ChangedDate DESC, h.OrderStatusHistoryID DESC) AS CancelReason,"
                + "COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p "
                + "          JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "          WHERE p.OrderID=o.OrderID AND ps.StatusID='COMPLETED'), 0) AS PaidAmount "
                + "FROM Orders o LEFT JOIN OrderStatuses s ON s.OrderStatusID=o.OrderStatusID "
                + "LEFT JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID "
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
                    String custModelName = rows.getString("ModelName");
                    if ((custModelName == null || custModelName.isBlank()) && order.getNote() != null && order.getNote().contains("Mẫu xe:")) {
                        int idx = order.getNote().indexOf("Mẫu xe:");
                        int end = order.getNote().indexOf(",", idx);
                        if (end < 0) end = order.getNote().indexOf(";", idx);
                        if (end < 0) end = order.getNote().indexOf("\n", idx);
                        if (end < 0) end = order.getNote().length();
                        custModelName = order.getNote().substring(idx + 7, end).trim();
                    }
                    order.setVehicleName(custModelName != null && !custModelName.isBlank() ? custModelName : "—");
                    order.setPaymentMethodName(rows.getString("MethodName"));
                    order.setContractStatus(rows.getString("ContractStatus"));
                    order.setCompanyId(rows.getObject("CompanyID", Integer.class));
                    order.setCompanyName(rows.getString("CompanyName"));
                    order.setPaidAmount(rows.getBigDecimal("PaidAmount"));
                    order.setCancelReason(rows.getString("CancelReason"));
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
            VALUES(?,?,(SELECT OrderStatusID FROM OrderStatuses WHERE StatusName=?),(SELECT PaymentMethodID FROM PaymentMethods WHERE PaymentMethodID=? AND MethodName IN ('BANK_TRANSFER','INSTALLMENT')),?,?,?,?,?)
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

    /**
     * Releases an unhanded vehicle back to AVAILABLE (removing the UNIQUE OrderDetails lock)
     * while preserving vehicle identity (model, version, VIN) and cancellation reason in Orders.Note.
     * Never releases vehicles that are already SOLD, DELIVERED, or EXPORTED.
     */
    public static void releaseVehicleAndPreserveSnapshot(Connection connection, int orderId, String reason)
            throws SQLException {
        ensureStatusRow(connection,
                "SELECT TOP 1 CarStatusID FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))='AVAILABLE'",
                "INSERT INTO CarStatuses(StatusName, Description) VALUES (N'AVAILABLE', N'Xe sẵn sàng trong kho')");

        Integer carId = null;
        String carStatusName = null;
        String modelName = null;
        String versionName = null;
        String vin = null;
        BigDecimal salesPrice = null;
        String orderNote = null;

        String infoSql = """
                SELECT o.Note AS OrderNote, od.CarID, od.SalesPrice, c.VIN,
                       cm.ModelName, cv.VersionName, cs.StatusName AS CarStatusName
                FROM Orders o
                LEFT JOIN OrderDetails od ON od.OrderID = o.OrderID
                LEFT JOIN Cars c WITH (UPDLOCK, HOLDLOCK) ON c.CarID = od.CarID
                LEFT JOIN CarModels cm ON cm.ModelID = c.ModelID
                LEFT JOIN CarVersions cv ON cv.VersionID = c.VersionID
                LEFT JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
                WHERE o.OrderID = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(infoSql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    orderNote = rs.getString("OrderNote");
                    carId = rs.getObject("CarID", Integer.class);
                    salesPrice = rs.getBigDecimal("SalesPrice");
                    vin = rs.getString("VIN");
                    modelName = rs.getString("ModelName");
                    versionName = rs.getString("VersionName");
                    carStatusName = rs.getString("CarStatusName");
                }
            }
        }

        boolean delivered = false;
        try (PreparedStatement delCheck = connection.prepareStatement(
                "SELECT 1 FROM Deliveries WHERE OrderID = ? AND UPPER(LTRIM(RTRIM(DeliveryStatus))) IN ('DELIVERED', 'COMPLETED')")) {
            delCheck.setInt(1, orderId);
            try (ResultSet rs = delCheck.executeQuery()) {
                delivered = rs.next();
            }
        }

        String updatedOrderNote = buildCancelledOrderNote(orderNote, modelName, versionName, vin, salesPrice, reason);
        if (updatedOrderNote != null) {
            try (PreparedStatement upNote = connection.prepareStatement("UPDATE Orders SET Note = ? WHERE OrderID = ?")) {
                upNote.setNString(1, updatedOrderNote);
                upNote.setInt(2, orderId);
                upNote.executeUpdate();
            }
        }

        if (carId != null && carId > 0 && !delivered) {
            String upperCarStatus = carStatusName == null ? "" : carStatusName.trim().toUpperCase(Locale.ROOT);
            if (!"DELIVERED".equals(upperCarStatus)
                    && !"SOLD".equals(upperCarStatus)
                    && !"EXPORTED".equals(upperCarStatus)) {
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
    }

    private static String buildCancelledOrderNote(String existingNote, String modelName, String versionName,
                                                  String vin, BigDecimal salesPrice, String reason) {
        StringBuilder addition = new StringBuilder();
        boolean hasModelInNote = existingNote != null && existingNote.contains("Mẫu xe:");
        if (!hasModelInNote && modelName != null && !modelName.isBlank()) {
            addition.append("Mẫu xe: ").append(modelName.trim());
            if (versionName != null && !versionName.isBlank()) {
                addition.append(" (").append(versionName.trim()).append(")");
            }
            if (vin != null && !vin.isBlank()) {
                addition.append(", VIN: ").append(vin.trim());
            }
            if (salesPrice != null) {
                addition.append(", Giá: ").append(salesPrice.toPlainString());
            }
        }
        String cleanReason = reason == null ? "" : reason.trim();
        if (!cleanReason.isEmpty() && (existingNote == null || !existingNote.contains("Lý do hủy: " + cleanReason))) {
            if (!addition.isEmpty()) {
                addition.append(" | ");
            }
            addition.append("Lý do hủy: ").append(cleanReason);
        }
        if (addition.isEmpty()) {
            return existingNote;
        }
        String base = existingNote == null ? "" : existingNote.trim();
        String combined = base.isEmpty() ? addition.toString() : (base + " | " + addition);
        if (combined.length() <= 500) {
            return combined;
        }
        String suffix = addition.toString();
        if (suffix.length() >= 495) {
            return suffix.substring(0, 500);
        }
        int keep = Math.max(0, 500 - suffix.length() - 6);
        if (keep == 0) {
            return suffix;
        }
        return base.substring(0, Math.min(base.length(), keep)) + "... | " + suffix;
    }

    private static void ensureStatusRow(Connection connection, String selectSql, String insertSql)
            throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(selectSql);
             ResultSet rs = check.executeQuery()) {
            if (rs.next()) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.executeUpdate();
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
