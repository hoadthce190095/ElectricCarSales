package com.electriccarsales.dao;

import com.electriccarsales.model.OrderStatusHistory;
import com.electriccarsales.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public List<Payment> findAllDetails() {
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM
                    + "ORDER BY p.PaymentDate DESC, p.PaymentID DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                List<Payment> result = new ArrayList<>();
                while (rs.next()) result.add(mapDetails(rs));
                return result;
            }
        });
    }

    public List<Payment> findByOrderId(int orderId) {
        return withConnection(connection -> findDetailsByOrderId(orderId));
    }

    public List<Payment> findByOrder(int orderId) { return findByOrderId(orderId); }

    public List<Payment> findHistoryByOrderId(int orderId) { return findByOrderId(orderId); }

    public List<Payment> findDetailsByOrderId(int orderId) {
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM
                    + "WHERE p.OrderID = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC";
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
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM
                    + "WHERE ps.StatusID = 'PENDING' ORDER BY p.PaymentDate DESC, p.PaymentID DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                List<Payment> result = new ArrayList<>();
                while (rs.next()) result.add(mapDetails(rs));
                return result;
            }
        });
    }

    public List<Payment> findByStatusCode(String statusCode) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID "
                + "WHERE ps.StatusID = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC", statusCode));
    }

    public List<Payment> search(Integer orderId, Integer paymentStatusId, String referenceCode) {
        return withConnection(connection -> {
            StringBuilder sql = new StringBuilder("SELECT ").append(DETAIL_COLUMNS).append(DETAIL_FROM).append("WHERE 1=1");
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
                    while (rs.next()) result.add(mapDetails(rs));
                    return result;
                }
            }
        });
    }

    public List<Payment> findByReferenceCode(String referenceCode) {
        return withConnection(connection -> {
            String sql = "SELECT " + DETAIL_COLUMNS + DETAIL_FROM
                    + "WHERE p.ReferenceCode = ? ORDER BY p.PaymentDate DESC, p.PaymentID DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referenceCode);
                try (ResultSet rs = statement.executeQuery()) {
                    List<Payment> result = new ArrayList<>();
                    while (rs.next()) result.add(mapDetails(rs));
                    return result;
                }
            }
        });
    }

    public Payment findExistingPending(int orderId, String paymentTypeName) {
        return withConnection(connection -> {
            String sql = "SELECT TOP 1 " + DETAIL_COLUMNS + DETAIL_FROM
                    + "WHERE p.OrderID = ? AND pt.TypeName = ? AND ps.StatusID = 'PENDING' "
                    + "ORDER BY p.PaymentID DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, orderId);
                statement.setString(2, paymentTypeName);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? mapDetails(rs) : null;
                }
            }
        });
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
                statement.setNString(8, payment.getNote());
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                payment.setPaymentID(id);
                return id;
            }
        });
    }

    public Payment createPending(int orderId, int paymentMethodId, String paymentTypeName,
                                 BigDecimal originalAmount, boolean testMode, String referenceCode) {
        return createPendingWithNote(orderId, paymentMethodId, paymentTypeName, originalAmount, referenceCode, null);
    }

    public Payment createPendingWithNote(int orderId, int paymentMethodId, String paymentTypeName,
                                         BigDecimal originalAmount, String referenceCode, String note) {
        return withConnection(connection -> {
            int typeId = lookupId(connection, "SELECT PaymentTypeID FROM PaymentTypes WHERE TypeName = ?",
                    paymentTypeName, "payment type");
            int pendingStatusId = lookupId(connection,
                    "SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID = ?", "PENDING", "payment status");
            String sql = "INSERT INTO Payments (OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID, "
                    + "PaymentAmount, ReferenceCode, Note) VALUES (?, ?, ?, ?, ?, ?, ?)";
            int paymentId;
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setInt(1, orderId);
                statement.setInt(2, paymentMethodId);
                statement.setInt(3, typeId);
                statement.setInt(4, pendingStatusId);
                statement.setBigDecimal(5, originalAmount);
                statement.setString(6, referenceCode);
                statement.setNString(7, note);
                statement.executeUpdate();
                paymentId = FinanceJDBC.generatedId(statement);
            }
            // Standardize deterministic reference code with PaymentID if prefix was temporary
            if (referenceCode == null || referenceCode.startsWith("PAY-") || !referenceCode.contains("-P")) {
                String shortType = "DEPOSIT".equalsIgnoreCase(paymentTypeName) ? "DEP" : "REM";
                String finalRef = "ECS-ORD" + orderId + "-" + shortType + "-P" + paymentId;
                try (PreparedStatement up = connection.prepareStatement(
                        "UPDATE Payments SET ReferenceCode = ? WHERE PaymentID = ?")) {
                    up.setString(1, finalRef);
                    up.setInt(2, paymentId);
                    up.executeUpdate();
                }
            }
            return findDetailsById(paymentId);
        });
    }

    public int getPaymentMethodId(String methodName) {
        return withConnection(connection -> lookupId(connection,
                "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName = ?", methodName, "payment method"));
    }

    public OrderBalance getOrderBalance(int orderId) {
        return withConnection(connection -> {
            String sql = """
                SELECT o.OrderID, o.CustomerID, o.TotalAmount, o.DepositAmount, o.RemainingAmount,
                       o.CompanyID, os.StatusName AS OrderStatusName, pm.MethodName AS PaymentMethodName,
                       fc.CompanyName, cu.CustomerName, cu.Phone AS CustomerPhone,
                       ct.Status AS ContractStatus,
                       COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p
                                 JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID
                                 WHERE p.OrderID = o.OrderID AND ps.StatusID = 'COMPLETED'), 0) AS SuccessfulPayments,
                       COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p
                                 JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID
                                 JOIN PaymentTypes pt ON pt.PaymentTypeID = p.PaymentTypeID
                                 WHERE p.OrderID = o.OrderID AND ps.StatusID = 'COMPLETED' AND pt.TypeName = 'DEPOSIT'), 0) AS DepositPaid,
                       COALESCE((SELECT SUM(p.PaymentAmount) FROM Payments p
                                 JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID
                                 JOIN PaymentTypes pt ON pt.PaymentTypeID = p.PaymentTypeID
                                 WHERE p.OrderID = o.OrderID AND ps.StatusID = 'COMPLETED' AND pt.TypeName = 'REMAINING'), 0) AS RemainingPaid
                FROM Orders o
                JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
                JOIN PaymentMethods pm ON pm.PaymentMethodID = o.PaymentMethodID
                JOIN Customers cu ON cu.CustomerID = o.CustomerID
                LEFT JOIN FinancialCompanies fc ON fc.CompanyID = o.CompanyID
                LEFT JOIN Contracts ct ON ct.OrderID = o.OrderID
                WHERE o.OrderID = ?
                """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, orderId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) return null;
                    return new OrderBalance(
                            rs.getBigDecimal("TotalAmount"),
                            rs.getBigDecimal("DepositAmount"),
                            rs.getBigDecimal("RemainingAmount"),
                            rs.getBigDecimal("SuccessfulPayments"),
                            rs.getBigDecimal("DepositPaid"),
                            rs.getBigDecimal("RemainingPaid"),
                            rs.getString("OrderStatusName"),
                            rs.getString("PaymentMethodName"),
                            rs.getObject("CompanyID", Integer.class),
                            rs.getString("CompanyName"),
                            rs.getString("ContractStatus"),
                            rs.getString("CustomerName"),
                            rs.getString("CustomerPhone"));
                }
            }
        });
    }

    public boolean appendPaymentNote(int paymentId, String additionalNote) {
        return withConnection(connection -> {
            Payment current = findOne(connection, "SELECT " + COLUMNS + " FROM Payments p WHERE p.PaymentID = ?", paymentId);
            if (current == null) return false;
            String base = current.getNote() == null ? "" : current.getNote().trim();
            String combined = base.isEmpty() ? additionalNote : (base + " | " + additionalNote);
            if (combined.length() > 500) combined = combined.substring(combined.length() - 500);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE Payments SET Note = ? WHERE PaymentID = ?")) {
                ps.setNString(1, combined);
                ps.setInt(2, paymentId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /**
     * Atomically verifies and confirms (or rejects/flags) a payment per RDS UC-10, UC-11, II.6.2, II.7.2:
     * - Checks idempotency with UPDLOCK so a payment cannot be confirmed twice.
     * - If actualReceivedAmount < required obligation (e.g., 1/1000 test transfer or underpayment),
     *   records actual amount honestly in Note, flags [AMOUNT_MISMATCH] for review, and NEVER marks
     *   the order DEPOSITED or PAID.
     * - When full 30% DEPOSIT is confirmed: Payment -> COMPLETED, Order -> DEPOSITED, Car -> DEPOSITED,
     *   RemainingAmount = 70% of TotalAmount, and logs OrderStatusHistory.
     * - When full 70% REMAINING is confirmed: Payment -> COMPLETED, Order -> PAID, RemainingAmount = 0,
     *   Car -> SOLD, and logs OrderStatusHistory.
     */
    public ConfirmationResult processVerification(int paymentId, boolean approve, BigDecimal actualReceivedAmount,
                                                  Integer accountantEmployeeId, String verificationRef, String note) {
        return withConnection(connection -> {
            boolean localTx = !hasExternalConnection();
            if (localTx) connection.setAutoCommit(false);
            try {
                int orderId;
                BigDecimal expectedAmount;
                String paymentType;
                String currentPaymentStatus;
                String currentRef;
                String currentNote;

                String lockSql = """
                    SELECT p.OrderID, p.PaymentAmount, p.ReferenceCode, p.Note,
                           pt.TypeName AS PaymentType, ps.StatusID AS PaymentStatus
                    FROM Payments p WITH (UPDLOCK, HOLDLOCK)
                    JOIN PaymentTypes pt ON pt.PaymentTypeID = p.PaymentTypeID
                    JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID
                    WHERE p.PaymentID = ?
                    """;
                try (PreparedStatement ps = connection.prepareStatement(lockSql)) {
                    ps.setInt(1, paymentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Không tìm thấy giao dịch thanh toán #" + paymentId);
                        }
                        orderId = rs.getInt("OrderID");
                        expectedAmount = rs.getBigDecimal("PaymentAmount").setScale(2, RoundingMode.HALF_UP);
                        currentRef = rs.getString("ReferenceCode");
                        currentNote = rs.getString("Note");
                        paymentType = rs.getString("PaymentType");
                        currentPaymentStatus = rs.getString("PaymentStatus");
                    }
                }

                if ("COMPLETED".equalsIgnoreCase(currentPaymentStatus)) {
                    throw new IllegalStateException("Giao dịch #" + paymentId + " đã được xác nhận thành công trước đó (chống xác nhận trùng theo RDS BR5).");
                }
                if (!"PENDING".equalsIgnoreCase(currentPaymentStatus)) {
                    throw new IllegalStateException("Giao dịch #" + paymentId + " đang ở trạng thái " + currentPaymentStatus + ", không thể xác nhận.");
                }

                if (!approve) {
                    // Reject / Cancel payment
                    int failedStatusId = lookupFailedStatusId(connection);
                    String rejectNote = buildAuditNote(currentNote, "REJECTED", accountantEmployeeId, verificationRef,
                            note == null || note.isBlank() ? "Kế toán từ chối xác nhận do chứng từ/sao kê không khớp." : note);
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE Payments SET PaymentStatusID = ?, Note = ? WHERE PaymentID = ?")) {
                        ps.setInt(1, failedStatusId);
                        ps.setNString(2, rejectNote);
                        ps.setInt(3, paymentId);
                        ps.executeUpdate();
                    }
                    if (localTx) connection.commit();
                    return new ConfirmationResult(false, false, "Đã từ chối giao dịch thanh toán #" + paymentId + ".",
                            findDetailsById(paymentId));
                }

                BigDecimal received = actualReceivedAmount == null
                        ? expectedAmount
                        : actualReceivedAmount.setScale(2, RoundingMode.HALF_UP);
                if (received.signum() <= 0) {
                    throw new IllegalArgumentException("Số tiền thực nhận phải lớn hơn 0.");
                }

                // RDS II.7.2 EX-1 & Prompt E.5: If received amount < obligation, record honestly & flag for review, NEVER mark order paid!
                if (received.compareTo(expectedAmount) < 0) {
                    String mismatchNote = buildAuditNote(currentNote, "AMOUNT_MISMATCH", accountantEmployeeId, verificationRef,
                            "CẢNH BÁO CHÊNH LỆCH (RDS EX-1): Thực nhận " + received.toPlainString()
                            + " VND < Nghĩa vụ " + expectedAmount.toPlainString()
                            + " VND. Giữ trạng thái PENDING, KHÔNG đánh dấu đơn hàng đã đặt cọc/thanh toán đủ. "
                            + (note == null ? "" : note));
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE Payments SET Note = ? WHERE PaymentID = ?")) {
                        ps.setNString(1, mismatchNote);
                        ps.setInt(2, paymentId);
                        ps.executeUpdate();
                    }
                    if (localTx) connection.commit();
                    return new ConfirmationResult(false, true,
                            "Đã ghi nhận trung thực số tiền thực nhận (" + received.toPlainString()
                            + " ₫), nhỏ hơn nghĩa vụ theo RDS (" + expectedAmount.toPlainString()
                            + " ₫). Theo RDS (Mục II.7.2 EX-1 & BR1), giao dịch được gắn cờ đối soát và KHÔNG đánh dấu đơn hàng đã hoàn tất nghĩa vụ.",
                            findDetailsById(paymentId));
                }

                int completedStatusId = lookupId(connection,
                        "SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID = ?", "COMPLETED", "payment status");
                String confirmedNote = buildAuditNote(currentNote, "CONFIRMED", accountantEmployeeId, verificationRef,
                        "Đã xác minh nhận đủ " + received.toPlainString() + " VND. " + (note == null ? "" : note));
                String updatedRef = (verificationRef != null && !verificationRef.isBlank() && (currentRef == null || currentRef.isBlank()))
                        ? verificationRef.trim()
                        : currentRef;

                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Payments SET PaymentStatusID = ?, PaymentAmount = ?, ReferenceCode = ?, Note = ? "
                        + "WHERE PaymentID = ? AND PaymentStatusID <> ?")) {
                    ps.setInt(1, completedStatusId);
                    ps.setBigDecimal(2, expectedAmount);
                    ps.setString(3, updatedRef);
                    ps.setNString(4, confirmedNote);
                    ps.setInt(5, paymentId);
                    ps.setInt(6, completedStatusId);
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("Giao dịch đã được xử lý bởi tiến trình khác.");
                    }
                }

                // Update Orders, Cars, and OrderStatusHistory according to paymentType (DEPOSIT vs REMAINING)
                BigDecimal orderTotal;
                BigDecimal orderDeposit;
                Integer carId = null;
                try (PreparedStatement ordPs = connection.prepareStatement("""
                        SELECT o.TotalAmount, o.DepositAmount, od.CarID
                        FROM Orders o WITH (UPDLOCK, HOLDLOCK)
                        LEFT JOIN OrderDetails od ON od.OrderID = o.OrderID
                        WHERE o.OrderID = ?
                        """)) {
                    ordPs.setInt(1, orderId);
                    try (ResultSet rs = ordPs.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Không tìm thấy đơn hàng #" + orderId);
                        orderTotal = rs.getBigDecimal("TotalAmount").setScale(2, RoundingMode.HALF_UP);
                        orderDeposit = rs.getBigDecimal("DepositAmount").setScale(2, RoundingMode.HALF_UP);
                        carId = rs.getObject("CarID", Integer.class);
                    }
                }

                String newOrderStatus;
                BigDecimal newRemainingAmount;
                String newCarStatus;
                String historyNote;

                if ("DEPOSIT".equalsIgnoreCase(paymentType)) {
                    newOrderStatus = "DEPOSITED";
                    newRemainingAmount = orderTotal.subtract(orderDeposit).setScale(2, RoundingMode.HALF_UP);
                    newCarStatus = "DEPOSITED";
                    historyNote = "Đã xác nhận đặt cọc 30% (" + expectedAmount.toPlainString() + " VND). Còn lại 70%: "
                            + newRemainingAmount.toPlainString() + " VND.";
                } else {
                    newOrderStatus = "PAID";
                    newRemainingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    newCarStatus = "SOLD";
                    historyNote = "Đã xác nhận thanh toán 70% còn lại (" + expectedAmount.toPlainString()
                            + " VND). Đơn hàng đã thanh toán đủ 100% (RemainingAmount = 0).";
                }

                try (PreparedStatement upOrd = connection.prepareStatement("""
                        UPDATE Orders
                        SET OrderStatusID = (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName = ?),
                            RemainingAmount = ?
                        WHERE OrderID = ?
                        """)) {
                    upOrd.setString(1, newOrderStatus);
                    upOrd.setBigDecimal(2, newRemainingAmount);
                    upOrd.setInt(3, orderId);
                    upOrd.executeUpdate();
                }

                if (carId != null) {
                    try (PreparedStatement upCar = connection.prepareStatement("""
                            UPDATE Cars
                            SET CarStatusID = COALESCE((SELECT TOP 1 CarStatusID FROM CarStatuses WHERE StatusName = ?), CarStatusID)
                            WHERE CarID = ?
                            """)) {
                        upCar.setNString(1, newCarStatus);
                        upCar.setInt(2, carId);
                        upCar.executeUpdate();
                    }
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setStatus(newOrderStatus);
                history.setEmployeeId(accountantEmployeeId);
                history.setNote(historyNote);
                new OrderStatusHistoryDAO().insert(connection, history);

                if (localTx) connection.commit();
                return new ConfirmationResult(true, false,
                        "Đã xác nhận thanh toán #" + paymentId + " thành công. Trạng thái đơn hàng #" + orderId
                        + " đã chuyển sang " + newOrderStatus + ".",
                        findDetailsById(paymentId));
            } catch (SQLException | RuntimeException ex) {
                if (localTx) {
                    try { connection.rollback(); } catch (SQLException rb) { ex.addSuppressed(rb); }
                }
                throw ex;
            }
        });
    }

    private static int lookupFailedStatusId(Connection connection) throws SQLException {
        // ElectricCarSalesDB.sql seeds 'FAILD' (typo in SQL script); support both 'FAILD' and 'FAILED'
        String sql = "SELECT TOP 1 PaymentStatusID FROM PaymentStatuses WHERE StatusID IN ('FAILD', 'FAILED', 'CANCELLED') ORDER BY PaymentStatusID";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new FinanceDAOException("Payment status FAILD/FAILED not found.");
    }

    private static String buildAuditNote(String existing, String tag, Integer employeeId, String bankRef, String message) {
        StringBuilder sb = new StringBuilder();
        if (existing != null && !existing.isBlank()) {
            sb.append(existing.trim()).append(" | ");
        }
        sb.append("[").append(tag);
        if (employeeId != null && employeeId > 0) sb.append(" by Emp#").append(employeeId);
        if (bankRef != null && !bankRef.isBlank()) sb.append(" Ref:").append(bankRef.trim());
        sb.append("] ").append(message == null ? "" : message.trim());
        String result = sb.toString();
        return result.length() <= 500 ? result : result.substring(result.length() - 500);
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

    public static void requireExactSettlement(Connection connection, int orderId) throws SQLException {
        String sql = """
                SELECT o.TotalAmount, o.DepositAmount, o.RemainingAmount,
                       COALESCE(SUM(p.PaymentAmount), 0) AS Paid,
                       COALESCE(SUM(CASE WHEN pt.TypeName='DEPOSIT' THEN p.PaymentAmount ELSE 0 END), 0) AS DepositPaid,
                       COALESCE(SUM(CASE WHEN pt.TypeName='REMAINING' THEN p.PaymentAmount ELSE 0 END), 0) AS RemainingPaid
                FROM Orders o
                LEFT JOIN Payments p ON p.OrderID=o.OrderID AND p.PaymentStatusID IN
                    (SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID='COMPLETED')
                LEFT JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID
                WHERE o.OrderID=?
                GROUP BY o.TotalAmount, o.DepositAmount, o.RemainingAmount
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("TotalAmount");
                    BigDecimal requiredDeposit = total.multiply(new BigDecimal("0.30"))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    if (total.signum() > 0 && rs.getBigDecimal("RemainingAmount").signum() == 0
                            && requiredDeposit.compareTo(rs.getBigDecimal("DepositAmount")) == 0
                            && requiredDeposit.compareTo(rs.getBigDecimal("DepositPaid")) == 0
                            && total.subtract(requiredDeposit).compareTo(rs.getBigDecimal("RemainingPaid")) == 0
                            && total.compareTo(rs.getBigDecimal("Paid")) == 0) return;
                }
            }
        }
        throw new IllegalStateException("Chưa đối chiếu đủ khoản cọc 30% và khoản thanh toán hoặc giải ngân 70%.");
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
        return new Payment(rs.getInt("PaymentID"), rs.getInt("OrderID"),
                rs.getInt("PaymentMethodID"), rs.getInt("PaymentTypeID"), rs.getInt("PaymentStatusID"),
                rs.getBigDecimal("PaymentAmount"), rs.getTimestamp("PaymentDate"),
                rs.getString("ReferenceCode"), rs.getString("Note"));
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
            statement.setString(1, value.trim().toUpperCase(Locale.ROOT));
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new FinanceDAOException("Unknown " + description + ": " + value);
                return rs.getInt(1);
            }
        }
    }

    public record ConfirmationResult(boolean confirmed, boolean flaggedMismatch, String message, Payment payment) {
    }

    public static final class OrderBalance {
        private final BigDecimal totalAmount;
        private final BigDecimal depositAmount;
        private final BigDecimal remainingAmount;
        private final BigDecimal successfulPayments;
        private final BigDecimal depositPaid;
        private final BigDecimal remainingPaid;
        private final String orderStatus;
        private final String paymentMethodName;
        private final Integer companyId;
        private final String companyName;
        private final String contractStatus;
        private final String customerName;
        private final String customerPhone;

        public OrderBalance(BigDecimal totalAmount, BigDecimal depositAmount, BigDecimal remainingAmount,
                            BigDecimal successfulPayments) {
            this(totalAmount, depositAmount, remainingAmount, successfulPayments,
                 BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, null, null);
        }

        public OrderBalance(BigDecimal totalAmount, BigDecimal depositAmount, BigDecimal remainingAmount,
                            BigDecimal successfulPayments, BigDecimal depositPaid, BigDecimal remainingPaid,
                            String orderStatus, String paymentMethodName, Integer companyId, String companyName,
                            String contractStatus, String customerName, String customerPhone) {
            this.totalAmount = totalAmount;
            this.depositAmount = depositAmount;
            this.remainingAmount = remainingAmount;
            this.successfulPayments = successfulPayments;
            this.depositPaid = depositPaid;
            this.remainingPaid = remainingPaid;
            this.orderStatus = orderStatus;
            this.paymentMethodName = paymentMethodName;
            this.companyId = companyId;
            this.companyName = companyName;
            this.contractStatus = contractStatus;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
        }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getDepositAmount() { return depositAmount; }
        public BigDecimal getRemainingAmount() { return remainingAmount; }
        public BigDecimal getSuccessfulPayments() { return successfulPayments; }
        public BigDecimal getDepositPaid() { return depositPaid; }
        public BigDecimal getRemainingPaid() { return remainingPaid; }
        public String getOrderStatus() { return orderStatus; }
        public String getPaymentMethodName() { return paymentMethodName; }
        public Integer getCompanyId() { return companyId; }
        public String getCompanyName() { return companyName; }
        public String getContractStatus() { return contractStatus; }
        public String getCustomerName() { return customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public boolean isDepositCompleted() {
            return depositPaid != null && depositAmount != null && depositPaid.compareTo(depositAmount) >= 0;
        }
        public boolean isFullyPaid() {
            return successfulPayments != null && totalAmount != null && successfulPayments.compareTo(totalAmount) >= 0;
        }
        public boolean isInstallmentOrder() {
            return (companyId != null && companyId > 0) || "INSTALLMENT".equalsIgnoreCase(paymentMethodName);
        }
    }
}
