package com.electriccarsales.service;

import com.electriccarsales.dao.FinanceConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Real bank-transfer instructions; only an accountant can confirm received funds. */
public class BankTransferService {
    private static final String BANK = "MB Bank";
    private static final String DEFAULT_MB_ACCOUNT = "0917231548";
    private static final String DEFAULT_MB_ACCOUNT_NAME = "Do Tan Dat";
    private static final Properties LOCAL_CONFIG = loadLocalConfig();

    public record Transfer(int paymentId, int orderId, String type, String status,
                           BigDecimal amount, String reference, String bankName,
                           String accountNumber, String accountName, String methodName,
                           boolean bankReported) {
        public int getPaymentId() { return paymentId; }
        public int getOrderId() { return orderId; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public BigDecimal getAmount() { return amount; }
        public String getReference() { return reference; }
        public String getBankName() { return bankName; }
        public String getAccountNumber() { return accountNumber; }
        public String getAccountName() { return accountName; }
        public String getMethodName() { return methodName; }
    }
    public record Pending(int paymentId, int orderId, String type, BigDecimal amount,
                          String reference, String customer, String note) {
        public int getPaymentId() { return paymentId; }
        public int getOrderId() { return orderId; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getReference() { return reference; }
        public String getCustomer() { return customer; }
        public String getNote() { return note; }
        public boolean isBankReported() { return note != null && note.contains("SEPAY_ID="); }
    }
    public record Debt(int orderId, BigDecimal totalAmount, BigDecimal depositAmount,
                       BigDecimal remainingAmount, String orderStatus, String paymentMethod,
                       String customer, String company, String contractStatus) {
        public int getOrderId() { return orderId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getDepositAmount() { return depositAmount; }
        public BigDecimal getRemainingAmount() { return remainingAmount; }
        public String getOrderStatus() { return orderStatus; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getCustomer() { return customer; }
        public String getCompany() { return company; }
        public String getContractStatus() { return contractStatus; }
    }

    public Transfer createForCustomer(int orderId, int customerId, String requestedType) throws SQLException {
        if (orderId <= 0 || customerId <= 0) throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        BankDetails bankDetails = new BankDetails(BANK, mbAccountNumber(), mbAccountName());
        String type = normalizeType(requestedType);
        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                OrderData o = order(c, orderId);
                if (o == null || o.customerId != customerId) throw new IllegalArgumentException("Không tìm thấy đơn hàng của bạn.");
                validateOpenOrder(c, o);
                if ("REMAINING".equals(type) && "INSTALLMENT".equals(o.method)) {
                    throw new IllegalStateException("70% của đơn trả góp do ngân hàng đối tác giải ngân, không phải khách thanh toán.");
                }
                if ("REMAINING".equals(type) && !hasSignedContract(c, orderId)) {
                    throw new IllegalStateException("Chỉ tạo yêu cầu thanh toán 70% sau khi hợp đồng đã ký.");
                }
                BigDecimal due = expectedAmount(c, o, type);
                Transfer transfer = pendingOfType(c, orderId, type);
                if (transfer != null && !"BANK_TRANSFER".equals(transfer.methodName()))
                    throw new IllegalStateException("Đang có yêu cầu thanh toán bằng kênh khác chờ xử lý.");
                if (transfer != null && transfer.amount().compareTo(due) != 0)
                    throw new IllegalStateException("Yêu cầu đang chờ không còn khớp số tiền đơn hàng.");
                if (transfer == null) transfer = insertPending(c, orderId, "BANK_TRANSFER", type, due);
                c.commit();
                return withBankDetails(transfer, bankDetails);
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Accountant records a partner disbursement only as PENDING; separate confirmation is required. */
    public Transfer createPartnerDisbursement(int orderId) throws SQLException {
        if (orderId <= 0) throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                OrderData o = order(c, orderId);
                if (o == null || !"INSTALLMENT".equals(o.method) || o.companyId == null) {
                    throw new IllegalArgumentException("Đơn hàng không phải trả góp có đối tác hợp lệ.");
                }
                validateOpenOrder(c, o);
                if (!partnerExists(c, o.companyId)) throw new IllegalStateException("Ngân hàng đối tác không tồn tại.");
                if (!hasSignedContract(c, orderId)) {
                    throw new IllegalStateException("Chỉ ghi nhận giải ngân 70% sau khi hợp đồng đã ký.");
                }
                BigDecimal amount = expectedAmount(c, o, "REMAINING");
                Transfer transfer = pendingOfType(c, orderId, "REMAINING");
                if (transfer != null && !"INSTALLMENT".equals(transfer.methodName()))
                    throw new IllegalStateException("Đang có yêu cầu thanh toán 70% khác chờ xử lý.");
                if (transfer != null && transfer.amount().compareTo(amount) != 0)
                    throw new IllegalStateException("Yêu cầu đang chờ không còn khớp số tiền đơn hàng.");
                if (transfer == null) transfer = insertPending(c, orderId, "INSTALLMENT", "REMAINING", amount);
                c.commit();
                return transfer;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public Transfer findCustomerTransfer(int paymentId, int customerId) throws SQLException {
        if (paymentId <= 0 || customerId <= 0) throw new IllegalArgumentException("Giao dịch không hợp lệ.");
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT p.PaymentID,p.OrderID,pt.TypeName,ps.StatusID,p.PaymentAmount,p.ReferenceCode,p.Note,pm.MethodName "
                    + "FROM Payments p JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                    + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                    + "WHERE p.PaymentID=? AND o.CustomerID=? AND pm.MethodName='BANK_TRANSFER'";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, paymentId);
                ps.setInt(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? transfer(rs) : null;
                }
            }
        }
    }

    public List<Pending> pendingPayments() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT p.PaymentID,p.OrderID,pt.TypeName,p.PaymentAmount,p.ReferenceCode, "
                    + "cu.CustomerName,p.Note FROM Payments p "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                    + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                    + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                    + "JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN Customers cu ON cu.CustomerID=o.CustomerID "
                    + "WHERE ps.StatusID='PENDING' AND pm.MethodName IN ('BANK_TRANSFER','INSTALLMENT') "
                    + "ORDER BY p.PaymentDate,p.PaymentID";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                List<Pending> list = new ArrayList<>();
                while (rs.next()) list.add(new Pending(rs.getInt("PaymentID"), rs.getInt("OrderID"),
                        rs.getString("TypeName"), rs.getBigDecimal("PaymentAmount"),
                        rs.getString("ReferenceCode"), rs.getString("CustomerName"), rs.getString("Note")));
                return list;
            }
        }
    }

    public List<Debt> openDebts() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT o.OrderID,o.TotalAmount,o.DepositAmount,o.RemainingAmount,os.StatusName, "
                    + "pm.MethodName,cu.CustomerName,fc.CompanyName,ct.Status AS ContractStatus "
                    + "FROM Orders o JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
                    + "JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID "
                    + "JOIN Customers cu ON cu.CustomerID=o.CustomerID "
                    + "LEFT JOIN FinancialCompanies fc ON fc.CompanyID=o.CompanyID "
                    + "LEFT JOIN Contracts ct ON ct.OrderID=o.OrderID "
                    + "WHERE o.RemainingAmount>0 ORDER BY o.OrderDate DESC,o.OrderID DESC";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                List<Debt> list = new ArrayList<>();
                while (rs.next()) list.add(new Debt(rs.getInt("OrderID"), rs.getBigDecimal("TotalAmount"),
                        rs.getBigDecimal("DepositAmount"), rs.getBigDecimal("RemainingAmount"),
                        rs.getString("StatusName"), rs.getString("MethodName"), rs.getString("CustomerName"),
                        rs.getString("CompanyName"), rs.getString("ContractStatus")));
                return list;
            }
        }
    }

    /** Accountant must first compare the REAL MB Bank statement; no bank API is called. */
    public void decide(int paymentId, int accountantUserId, boolean confirm, String bankTransaction,
                       BigDecimal receivedAmount, String reason) throws SQLException {
        if (paymentId <= 0 || accountantUserId <= 0) throw new IllegalArgumentException("Thiếu mã giao dịch/nhân viên.");
        if (confirm && (bankTransaction == null || !bankTransaction.matches("[A-Za-z0-9./_-]{4,80}"))) {
            throw new IllegalArgumentException("Nhập mã giao dịch thực từ sao kê ngân hàng (4–80 ký tự).");
        }
        if (!confirm && (reason == null || reason.isBlank() || reason.trim().length() > 200)) {
            throw new IllegalArgumentException("Phải ghi lý do từ chối (tối đa 200 ký tự).");
        }
        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int employeeId = accountant(c, accountantUserId);
                if (employeeId <= 0) throw new SecurityException("Chỉ Accountant hợp lệ mới được xác nhận.");
                // Serialize confirmations; prevents reusing the same external transaction reference.
                try (Statement lock = c.createStatement()) {
                    lock.executeQuery("SELECT COUNT(*) FROM Payments WITH (TABLOCKX, HOLDLOCK)").close();
                }
                String sql = "SELECT p.OrderID,p.PaymentAmount,pt.TypeName,ps.StatusID,p.Note,pm.MethodName "
                        + "FROM Payments p JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                        + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                        + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                        + "WHERE p.PaymentID=?";
                int orderId;
                String type;
                String oldNote;
                BigDecimal amount;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, paymentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"PENDING".equals(rs.getString("StatusID"))) {
                            throw new IllegalStateException("Giao dịch không còn ở trạng thái PENDING.");
                        }
                        if (!List.of("BANK_TRANSFER", "INSTALLMENT").contains(rs.getString("MethodName")))
                            throw new IllegalStateException("Khoản thanh toán này không thuộc chuyển khoản hoặc giải ngân.");
                        orderId = rs.getInt("OrderID");
                        amount = rs.getBigDecimal("PaymentAmount");
                        type = rs.getString("TypeName");
                        oldNote = rs.getString("Note");
                    }
                }
                OrderData o = order(c, orderId);
                if (o == null) throw new IllegalStateException("Không tìm thấy đơn hàng.");
                if (confirm) {
                    validateOpenOrder(c, o);
                    BigDecimal due = expectedAmount(c, o, type);
                    if (amount.compareTo(due) != 0) throw new IllegalStateException("Khoản phải thu không khớp đơn hàng.");
                    if (receivedAmount == null || amount.compareTo(receivedAmount) != 0) {
                        throw new IllegalArgumentException("Tiền thực nhận khác nghĩa vụ thanh toán; không thể xác nhận đủ.");
                    }
                    String tag = "BANKTXN=" + bankTransaction.trim() + ";";
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT COUNT(*) FROM Payments WHERE PaymentID<>? AND CHARINDEX(?,COALESCE(Note,''))>0")) {
                        ps.setInt(1, paymentId);
                        ps.setString(2, tag);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            if (rs.getInt(1) > 0) throw new IllegalStateException("Mã sao kê đã được dùng cho giao dịch khác.");
                        }
                    }
                    updatePayment(c, paymentId, "COMPLETED", tag + " ACCOUNTANT=" + employeeId + " CONFIRMED;", oldNote);
                    if ("DEPOSIT".equals(type)) {
                        changeOrderStatus(c, orderId, "DEPOSITED", employeeId, "30% deposit confirmed, payment " + paymentId);
                        updateCarStatus(c, orderId, "Deposited");
                    } else {
                        try (PreparedStatement ps = c.prepareStatement("UPDATE Orders SET RemainingAmount=0 WHERE OrderID=? AND RemainingAmount=?")) {
                            ps.setInt(1, orderId);
                            ps.setBigDecimal(2, due);
                            if (ps.executeUpdate() != 1) throw new IllegalStateException("Công nợ vừa thay đổi; cần đối chiếu lại.");
                        }
                        changeOrderStatus(c, orderId, "PAID", employeeId, "70% remaining confirmed, payment " + paymentId);
                    }
                } else {
                    updatePayment(c, paymentId, "FAILD", "REJECTED by ACCOUNTANT=" + employeeId
                            + " REASON=" + reason.trim() + ";", oldNote);
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static void updatePayment(Connection c, int id, String status, String audit, String oldNote) throws SQLException {
        String note = ((oldNote == null || oldNote.isBlank()) ? "" : oldNote + " ")
                + audit + " AT=" + java.time.OffsetDateTime.now();
        if (note.length() > 500) throw new IllegalArgumentException("Ghi chú giao dịch quá dài.");
        try (PreparedStatement ps = c.prepareStatement("UPDATE Payments SET PaymentStatusID="
                + "(SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID=?), Note=? "
                + "WHERE PaymentID=? AND PaymentStatusID=(SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID='PENDING')")) {
            ps.setString(1, status);
            ps.setString(2, note);
            ps.setInt(3, id);
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Không cập nhật được thanh toán.");
        }
    }

    private static void changeOrderStatus(Connection c, int orderId, String newStatus, int employee, String note)
            throws SQLException {
        int id = lookup(c, "SELECT OrderStatusID FROM OrderStatuses WHERE StatusName=?", newStatus);
        try (PreparedStatement ps = c.prepareStatement("UPDATE Orders SET OrderStatusID=? WHERE OrderID=?")) {
            ps.setInt(1, id);
            ps.setInt(2, orderId);
            if (ps.executeUpdate() != 1) throw new SQLException("Cannot update order status");
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderStatusHistory(OrderID,OrderStatusID,EmployeeID,Note) VALUES(?,?,?,?)")) {
            ps.setInt(1, orderId);
            ps.setInt(2, id);
            ps.setInt(3, employee);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    private static void updateCarStatus(Connection c, int orderId, String name) throws SQLException {
        int carStatusId = lookup(c, "SELECT CarStatusID FROM CarStatuses WHERE StatusName=?", name);
        try (PreparedStatement ps = c.prepareStatement("UPDATE Cars SET CarStatusID=? WHERE CarID IN "
                + "(SELECT CarID FROM OrderDetails WHERE OrderID=?) "
                + "AND CarStatusID IN (SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName) IN ('AVAILABLE','RESERVED'))")) {
            ps.setInt(1, carStatusId);
            ps.setInt(2, orderId);
            if (ps.executeUpdate() < 1) throw new IllegalStateException("Đơn hàng không có xe để chuyển trạng thái.");
        }
    }

    private static Transfer pendingOfType(Connection c, int orderId, String type) throws SQLException {
        String sql = "SELECT TOP (1) p.PaymentID,p.OrderID,pt.TypeName,ps.StatusID,p.PaymentAmount,p.ReferenceCode,p.Note,pm.MethodName "
                + "FROM Payments p JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                + "WHERE p.OrderID=? AND pt.TypeName=? AND ps.StatusID='PENDING' ORDER BY p.PaymentID DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? transfer(rs) : null; }
        }
    }

    private static Transfer insertPending(Connection c, int orderId, String method, String type, BigDecimal amount)
            throws SQLException {
        int methodId = lookup(c, "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName=?", method);
        int typeId = lookup(c, "SELECT PaymentTypeID FROM PaymentTypes WHERE TypeName=?", type);
        int statusId = lookup(c, "SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID=?", "PENDING");
        int paymentId;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Payments "
                + "(OrderID,PaymentMethodID,PaymentTypeID,PaymentStatusID,PaymentAmount) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setInt(2, methodId);
            ps.setInt(3, typeId);
            ps.setInt(4, statusId);
            ps.setBigDecimal(5, amount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Missing payment id");
                paymentId = keys.getInt(1);
            }
        }
        String ref = "ECS" + orderId + ("DEPOSIT".equals(type) ? "D" : "R") + paymentId;
        try (PreparedStatement ps = c.prepareStatement("UPDATE Payments SET ReferenceCode=? WHERE PaymentID=?")) {
            ps.setString(1, ref);
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        }
        return new Transfer(paymentId, orderId, type, "PENDING", amount, ref, BANK,
                mbAccountNumber(), mbAccountName(), method, false);
    }

    private static BigDecimal expectedAmount(Connection c, OrderData o, String type) throws SQLException {
        BigDecimal deposit = o.total.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        if (o.total.signum() <= 0 || o.deposit.compareTo(deposit) != 0) {
            throw new IllegalStateException("Giá trị đặt cọc trong đơn không khớp quy tắc 30%.");
        }
        BigDecimal confirmedDeposit = confirmed(c, o.id, "DEPOSIT");
        BigDecimal confirmedRemaining = confirmed(c, o.id, "REMAINING");
        if ("DEPOSIT".equals(type)) {
            if (!List.of("PENDING", "RESERVED").contains(o.status.toUpperCase(Locale.ROOT)))
                throw new IllegalStateException("Trạng thái đơn hàng không cho phép cọc.");
            if (confirmedDeposit.signum() > 0 || confirmedRemaining.signum() > 0)
                throw new IllegalStateException("Đơn hàng đã thanh toán khoản đặt cọc.");
            return wholeVnd(deposit);
        }
        if (!List.of("DEPOSITED", "PROCESSING").contains(o.status.toUpperCase(Locale.ROOT)))
            throw new IllegalStateException("Trạng thái đơn hàng không cho phép thanh toán 70%.");
        if (confirmedDeposit.compareTo(deposit) != 0 || confirmedRemaining.signum() > 0)
            throw new IllegalStateException("Chỉ thanh toán 70% sau khi cọc 30% đã được xác nhận và chưa trả phần còn lại.");
        BigDecimal remaining = o.total.subtract(deposit).setScale(2, RoundingMode.HALF_UP);
        if (o.remaining.compareTo(remaining) != 0)
            throw new IllegalStateException("Công nợ 70% trong đơn không khớp dữ liệu thanh toán.");
        return wholeVnd(remaining);
    }

    private static BigDecimal confirmed(Connection c, int orderId, String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.PaymentAmount),0) FROM Payments p "
                + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "WHERE p.OrderID=? AND pt.TypeName=? AND ps.StatusID='COMPLETED'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    private static OrderData order(Connection c, int id) throws SQLException {
        String sql = "SELECT o.OrderID,o.CustomerID,o.TotalAmount,o.DepositAmount,o.RemainingAmount, "
                + "o.CompanyID,os.StatusName,pm.MethodName FROM Orders o WITH (UPDLOCK,HOLDLOCK) "
                + "JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID WHERE o.OrderID=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new OrderData(rs.getInt("OrderID"), rs.getInt("CustomerID"),
                        rs.getBigDecimal("TotalAmount"), rs.getBigDecimal("DepositAmount"),
                        rs.getBigDecimal("RemainingAmount"), rs.getString("StatusName"),
                        rs.getString("MethodName"), rs.getObject("CompanyID", Integer.class)) : null;
            }
        }
    }

    private static void validateOpenOrder(Connection c, OrderData o) throws SQLException {
        if (List.of("CANCELLED", "PAID", "COMPLETED").contains(o.status.toUpperCase(Locale.ROOT))) {
            throw new IllegalStateException("Đơn hàng không còn nhận thanh toán.");
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM OrderDetails WHERE OrderID=?")) {
            ps.setInt(1, o.id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) != 1) throw new IllegalStateException("Đơn hàng phải có đúng một xe trước khi thanh toán.");
            }
        }
    }

    private static int accountant(Connection c, int userId) throws SQLException {
        String sql = "SELECT e.EmployeeID FROM Employees e JOIN Users u ON u.UserID=e.UserID "
                + "JOIN Roles r ON r.RoleID=u.RoleID JOIN Statuses s ON s.StatusID=u.StatusID "
                + "WHERE u.UserID=? AND UPPER(r.RoleName)='ACCOUNTANT' AND UPPER(s.StatusName)='ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static boolean partnerExists(Connection c, int companyId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM FinancialCompanies WHERE CompanyID=?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static boolean hasSignedContract(Connection c, int orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM Contracts WHERE OrderID=? AND UPPER(Status)='SIGNED'")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static int lookup(Connection c, String sql, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Thiếu dữ liệu danh mục: " + value);
                return rs.getInt(1);
            }
        }
    }

    private static String normalizeType(String type) {
        if (type == null) throw new IllegalArgumentException("Chọn tiền cọc hoặc thanh toán còn lại.");
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!"DEPOSIT".equals(normalized) && !"REMAINING".equals(normalized)) {
            throw new IllegalArgumentException("Loại khoản thanh toán không hợp lệ.");
        }
        return normalized;
    }

    private static Transfer transfer(ResultSet rs) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("PaymentAmount");
        String ref = rs.getString("ReferenceCode");
        String note = rs.getString("Note");
        return new Transfer(rs.getInt("PaymentID"), rs.getInt("OrderID"), rs.getString("TypeName"),
                rs.getString("StatusID"), amount, ref, BANK,
                mbAccountNumber(), mbAccountName(), rs.getString("MethodName"),
                note != null && note.contains("SEPAY_ID="));
    }

    public static String mbAccountNumber() {
        return configured("ECS_MB_ACCOUNT", DEFAULT_MB_ACCOUNT);
    }

    public static String mbAccountName() {
        return configured("ECS_MB_ACCOUNT_NAME", DEFAULT_MB_ACCOUNT_NAME);
    }

    private static String configured(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) value = System.getenv(key);
        if (value == null || value.isBlank()) value = LOCAL_CONFIG.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static Properties loadLocalConfig() {
        Properties properties = new Properties();
        try (InputStream input = BankTransferService.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) properties.load(input);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return properties;
    }

    private static Transfer withBankDetails(Transfer transfer, BankDetails details) {
        return new Transfer(transfer.paymentId(), transfer.orderId(), transfer.type(), transfer.status(),
                transfer.amount(), transfer.reference(), details.bankName(), details.accountNumber(),
                details.accountName(), transfer.methodName(), transfer.bankReported());
    }

    private static BigDecimal wholeVnd(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalStateException("Số tiền chưa nguyên VND; cần xử lý làm tròn theo quy tắc kế toán trước.");
        }
    }

    private record OrderData(int id, int customerId, BigDecimal total, BigDecimal deposit,
                             BigDecimal remaining, String status, String method, Integer companyId) { }
    private record BankDetails(String bankName, String accountNumber, String accountName) { }
}
