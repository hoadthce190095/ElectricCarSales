package com.electriccarsales.dao;

import com.electriccarsales.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO extends FinanceDAOBase {
    private static final String COLUMNS = "p.PaymentID, p.OrderID, p.PaymentMethodID, p.PaymentTypeID, "
            + "p.PaymentStatusID, p.PaymentAmount, p.PaymentDate, p.ReferenceCode, p.Note";
    private static final String DETAIL_COLUMNS = COLUMNS + ", pm.MethodName AS PaymentMethodName, "
            + "pt.TypeName AS PaymentTypeName, ps.StatusID AS PaymentStatusCode, "
            + "cu.CustomerName, o.TotalAmount AS OrderTotalAmount, o.RemainingAmount AS OrderRemainingAmount";
    private static final String DETAIL_FROM = " FROM Payments p "
            + "JOIN PaymentMethods pm ON pm.PaymentMethodID = p.PaymentMethodID "
            + "JOIN PaymentTypes pt ON pt.PaymentTypeID = p.PaymentTypeID "
            + "JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID "
            + "JOIN Orders o ON o.OrderID = p.OrderID "
            + "JOIN Customers cu ON cu.CustomerID = o.CustomerID ";

    public PaymentDAO() { super(); }
    public PaymentDAO(Connection transactionConnection) { super(transactionConnection); }

    public Payment findById(int paymentId) {
        return withConnection(connection -> findOne(connection, "SELECT " + COLUMNS
                + " FROM Payments p WHERE p.PaymentID = ?", paymentId));
    }

    public Payment findDetailsById(int paymentId) {
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM + "WHERE p.PaymentID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, paymentId);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? mapDetails(rs) : null;
                }
            }
        });
    }

    public List<Payment> findByOrderId(int orderId) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM Payments p WHERE p.OrderID = ? ORDER BY p.PaymentDate, p.PaymentID", orderId));
    }

    /** Backward-compatible name used by the existing PaymentService. */
    public List<Payment> findByOrder(int orderId) { return findByOrderId(orderId); }

    public List<Payment> findHistoryByOrderId(int orderId) { return findByOrderId(orderId); }

    public List<Payment> findDetailsByOrderId(int orderId) {
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM
                    + "WHERE p.OrderID = ? ORDER BY p.PaymentDate, p.PaymentID";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, orderId);
                try (ResultSet rs = statement.executeQuery()) {
                    List<Payment> result = new ArrayList<>();
                    while (rs.next()) result.add(mapDetails(rs));
                    return result;
                }
            }
        });
    }

    public List<Payment> findDetailsByCustomerId(int customerId) {
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM
                    + "WHERE o.CustomerID = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, customerId);
                try (ResultSet rs = statement.executeQuery()) {
                    List<Payment> result = new ArrayList<>();
                    while (rs.next()) result.add(mapDetails(rs));
                    return result;
                }
            }
        });
    }

    public boolean orderBelongsToCustomer(int orderId, int customerId) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM Orders WHERE OrderID = ? AND CustomerID = ?")) {
                statement.setInt(1, orderId);
                statement.setInt(2, customerId);
                try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
            }
        });
    }

    public List<Payment> findByStatusId(int paymentStatusId) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM Payments p WHERE p.PaymentStatusID = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC",
                paymentStatusId));
    }

    public List<Payment> findPendingPayments() {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID "
                + "WHERE ps.StatusID = 'PENDING' ORDER BY p.PaymentDate, p.PaymentID"));
    }

    /** Filters by the lookup code in PaymentStatuses.StatusID, never the numeric key. */
    public List<Payment> findByStatusCode(String statusCode) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID "
                + "WHERE ps.StatusID = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC", statusCode));
    }

    public List<Payment> search(Integer orderId, Integer paymentStatusId, String referenceCode) {
        return withConnection(connection -> {
            StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM Payments p WHERE 1=1");
            List<Object> values = new ArrayList<>();
            if (orderId != null) { sql.append(" AND p.OrderID = ?"); values.add(orderId); }
            if (paymentStatusId != null) { sql.append(" AND p.PaymentStatusID = ?"); values.add(paymentStatusId); }
            if (referenceCode != null && !referenceCode.isBlank()) {
                sql.append(" AND p.ReferenceCode LIKE ?"); values.add("%" + referenceCode.trim() + "%");
            }
            sql.append(" ORDER BY p.PaymentDate DESC, p.PaymentID DESC");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.size(); i++) statement.setObject(i + 1, values.get(i));
                try (ResultSet rs = statement.executeQuery()) {
                    List<Payment> result = new ArrayList<>();
                    while (rs.next()) result.add(map(rs));
                    return result;
                }
            }
        });
    }

    public List<Payment> findByReferenceCode(String referenceCode) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM Payments p WHERE p.ReferenceCode = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC",
                referenceCode));
    }

    public int insert(Payment payment) {
        return withConnection(connection -> {
            String sql = "INSERT INTO Payments (OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID, "
                    + "PaymentAmount, PaymentDate, ReferenceCode, Note) VALUES (?, ?, ?, ?, ?, "
                    + "COALESCE(?, SYSDATETIME()), ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setInt(1, payment.getOrderID());
                statement.setInt(2, payment.getPaymentMethodID());
                statement.setInt(3, payment.getPaymentTypeID());
                statement.setInt(4, payment.getPaymentStatusID());
                statement.setBigDecimal(5, payment.getPaymentAmount());
                statement.setTimestamp(6, payment.getPaymentDate());
                statement.setString(7, payment.getReferenceCode());
                statement.setString(8, payment.getNote());
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                payment.setPaymentID(id);
                return id;
            }
        });
    }

    /**
     * Creates a pending request using the original business amount. The test-mode flag is deliberately
     * not applied here: any gateway conversion belongs to the payment service/integration layer.
     */
    public Payment createPending(int orderId, int paymentMethodId, String paymentTypeName,
                                 BigDecimal originalAmount, boolean testMode, String referenceCode) {
        return withConnection(connection -> {
            int typeId = lookupId(connection, "SELECT PaymentTypeID FROM PaymentTypes WHERE TypeName = ?",
                    paymentTypeName, "payment type");
            int pendingStatusId = lookupId(connection,
                    "SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID = ?", "PENDING", "payment status");
            Payment payment = new Payment(0, orderId, paymentMethodId, typeId, pendingStatusId,
                    originalAmount, null, referenceCode, null);
            String sql = "INSERT INTO Payments (OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID, "
                    + "PaymentAmount, ReferenceCode) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setInt(1, payment.getOrderID());
                statement.setInt(2, payment.getPaymentMethodID());
                statement.setInt(3, payment.getPaymentTypeID());
                statement.setInt(4, payment.getPaymentStatusID());
                statement.setBigDecimal(5, payment.getPaymentAmount());
                statement.setString(6, payment.getReferenceCode());
                statement.executeUpdate();
                payment.setPaymentID(FinanceJDBC.generatedId(statement));
                return payment;
            }
        });
    }

    /** Resolves an existing method lookup value without relying on seeded identity values. */
    public int getPaymentMethodId(String methodName) {
        return withConnection(connection -> lookupId(connection,
                "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName = ?", methodName, "payment method"));
    }

    /** Order amounts and completed payment total are all expressed in the original business currency. */
    public OrderBalance getOrderBalance(int orderId) {
        return withConnection(connection -> {
            String sql = "SELECT o.TotalAmount, o.DepositAmount, o.RemainingAmount, "
                    + "COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID "
                    + "WHERE p.OrderID = o.OrderID AND ps.StatusID = 'COMPLETED'), 0) AS SuccessfulPayments "
                    + "FROM Orders o WHERE o.OrderID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, orderId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) return null;
                    return new OrderBalance(rs.getBigDecimal("TotalAmount"), rs.getBigDecimal("DepositAmount"),
                            rs.getBigDecimal("RemainingAmount"), rs.getBigDecimal("SuccessfulPayments"));
                }
            }
        });
    }

    public boolean updateStatus(int paymentId, int paymentStatusId, String referenceCode) {
        return withConnection(connection -> {
            String sql = "UPDATE Payments SET PaymentStatusID = ?, ReferenceCode = COALESCE(?, ReferenceCode) "
                    + "WHERE PaymentID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, paymentStatusId);
                statement.setString(2, referenceCode);
                statement.setInt(3, paymentId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    /** Guarded transition for service workflows that must avoid processing the same pending payment twice. */
    public boolean updateStatusIfCurrent(int paymentId, int expectedStatusId, int newStatusId,
                                         String referenceCode) {
        return withConnection(connection -> {
            String sql = "UPDATE Payments SET PaymentStatusID = ?, "
                    + "ReferenceCode = COALESCE(?, ReferenceCode) "
                    + "WHERE PaymentID = ? AND PaymentStatusID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, newStatusId);
                statement.setString(2, referenceCode);
                statement.setInt(3, paymentId);
                statement.setInt(4, expectedStatusId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public BigDecimal sumSuccessfulPaymentsByOrder(int orderId) {
        return withConnection(connection -> sum(connection,
                "SELECT COALESCE(SUM(p.PaymentAmount), 0) FROM Payments p "
                        + "JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID "
                        + "WHERE p.OrderID = ? AND ps.StatusID = 'COMPLETED'", orderId));
    }

    private static Payment findOne(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<Payment> list(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<Payment> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static Payment map(ResultSet rs) throws SQLException {
        Payment payment = new Payment(rs.getInt("PaymentID"), rs.getInt("OrderID"),
                rs.getInt("PaymentMethodID"), rs.getInt("PaymentTypeID"), rs.getInt("PaymentStatusID"),
                rs.getBigDecimal("PaymentAmount"), rs.getTimestamp("PaymentDate"),
                rs.getString("ReferenceCode"), rs.getString("Note"));
        return payment;
    }

    private static Payment mapDetails(ResultSet rs) throws SQLException {
        Payment payment = map(rs);
        payment.setPaymentMethodName(rs.getString("PaymentMethodName"));
        payment.setPaymentTypeName(rs.getString("PaymentTypeName"));
        payment.setPaymentStatusCode(rs.getString("PaymentStatusCode"));
        payment.setCustomerName(rs.getString("CustomerName"));
        payment.setOrderTotalAmount(rs.getBigDecimal("OrderTotalAmount"));
        payment.setOrderRemainingAmount(rs.getBigDecimal("OrderRemainingAmount"));
        return payment;
    }

    private static BigDecimal sum(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    private static int lookupId(Connection connection, String sql, String value, String description)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new FinanceDAOException("Unknown " + description + ": " + value);
                return rs.getInt(1);
            }
        }
    }

    public static final class OrderBalance {
        private final BigDecimal totalAmount;
        private final BigDecimal depositAmount;
        private final BigDecimal remainingAmount;
        private final BigDecimal successfulPayments;

        public OrderBalance(BigDecimal totalAmount, BigDecimal depositAmount, BigDecimal remainingAmount,
                            BigDecimal successfulPayments) {
            this.totalAmount = totalAmount;
            this.depositAmount = depositAmount;
            this.remainingAmount = remainingAmount;
            this.successfulPayments = successfulPayments;
        }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getDepositAmount() { return depositAmount; }
        public BigDecimal getRemainingAmount() { return remainingAmount; }
        public BigDecimal getSuccessfulPayments() { return successfulPayments; }
    }
}
