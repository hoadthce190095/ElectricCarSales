package com.electriccarsales.service;

import com.electriccarsales.dao.FinanceConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quản lý quy trình thanh toán chuyển khoản và cổng thanh toán trực tuyến SePay + MB Bank.
 * Hỗ trợ tạo giao dịch, mã VietQR động, thanh toán cọc 30%, thanh toán 70% còn lại,
 * đối soát Kế toán và xác nhận Webhook tức thời từ SePay.
 */
public class BankTransferService {
    private static final String ONLINE_GATEWAY = "SEPAY_MBBANK";
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(?<![A-Z0-9])ECS([0-9]+)[DR]([0-9]+)(?![A-Z0-9])");
    private final SePayService sePayService = new SePayService();

    public record Transfer(int paymentId, int orderId, String type, String status,
                           BigDecimal amount, String reference, String bankName,
                           String accountNumber, String accountName, String methodName,
                           boolean bankReported, String note, String paymentUrl) {
        public Transfer(int paymentId, int orderId, String type, String status,
                        BigDecimal amount, String reference, String bankName,
                        String accountNumber, String accountName, String methodName,
                        boolean bankReported) {
            this(paymentId, orderId, type, status, amount, reference, bankName,
                    accountNumber, accountName, methodName, bankReported, null, null);
        }
        public Transfer(int paymentId, int orderId, String type, String status,
                        BigDecimal amount, String reference, String bankName,
                        String accountNumber, String accountName, String methodName,
                        boolean bankReported, String note) {
            this(paymentId, orderId, type, status, amount, reference, bankName,
                    accountNumber, accountName, methodName, bankReported, note, null);
        }
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
        public String getNote() { return note; }
        public String getPaymentUrl() { return paymentUrl != null ? paymentUrl : ""; }
        public String getQrUrl() { return getPaymentUrl(); }
    }

    public record Pending(int paymentId, int orderId, String type, BigDecimal amount,
                          String reference, String customer, String note, String methodName) {
        public int getPaymentId() { return paymentId; }
        public int getOrderId() { return orderId; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getReference() { return reference; }
        public String getCustomer() { return customer; }
        public String getNote() { return note; }
        public String getMethodName() { return methodName; }
        public SePayEvidence getSePayEvidence() {
            return "BANK_TRANSFER".equals(methodName) ? SePayEvidence.fromNote(note) : null;
        }
        public boolean isSePay() { return "BANK_TRANSFER".equals(methodName); }
        public boolean isOcbQr() { return false; }
        public boolean isBankReported() {
            return note != null && (note.contains("SEPAY_ID=") || note.contains("BANKTXN=") || note.contains("VNPAY_TXN="));
        }
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
        return createForCustomer(orderId, customerId, requestedType, "BANK_TRANSFER");
    }

    public Transfer createForCustomer(int orderId, int customerId, String requestedType, String method) throws SQLException {
        return createForCustomer(orderId, customerId, requestedType, method, "127.0.0.1");
    }

    public Transfer createForCustomer(int orderId, int customerId, String requestedType, String method, String clientIp) throws SQLException {
        if (!"BANK_TRANSFER".equals(method) && !"CASH".equals(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
        if (orderId <= 0 || customerId <= 0) throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
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
                if (transfer != null && !method.equals(transfer.methodName())) {
                    int newMethodId = lookup(c, "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName=?", method);
                    String newNote = "BANK_TRANSFER".equals(method) ? "GATEWAY=SEPAY_MBBANK;" : null;
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE Payments SET PaymentMethodID=?, Note=? WHERE PaymentID=? AND PaymentStatusID="
                            + "(SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID='PENDING')")) {
                        ps.setInt(1, newMethodId);
                        ps.setNString(2, newNote);
                        ps.setInt(3, transfer.paymentId());
                        ps.executeUpdate();
                    }
                    transfer = pendingOfType(c, orderId, type);
                }
                if (transfer != null && transfer.amount().compareTo(due) != 0)
                    throw new IllegalStateException("Yêu cầu đang chờ không còn khớp số tiền đơn hàng.");
                if (transfer == null) {
                    if ("BANK_TRANSFER".equals(method) && !SePayService.isReady()) {
                        throw new IllegalStateException("Cổng thanh toán SePay + MB Bank chưa sẵn sàng: " + SePayService.configStatus());
                    }
                    transfer = insertPending(c, orderId, method, type, due, clientIp);
                }
                c.commit();
                return transfer;
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
                if (transfer != null && !"INSTALLMENT".equals(transfer.methodName())) {
                    int newMethodId = lookup(c, "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName=?", "INSTALLMENT");
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE Payments SET PaymentMethodID=?, Note=? WHERE PaymentID=? AND PaymentStatusID="
                            + "(SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID='PENDING')")) {
                        ps.setInt(1, newMethodId);
                        ps.setNString(2, "DISBURSEMENT=PARTNER;");
                        ps.setInt(3, transfer.paymentId());
                        ps.executeUpdate();
                    }
                    transfer = pendingOfType(c, orderId, "REMAINING");
                }
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
                    + "WHERE p.PaymentID=? AND o.CustomerID=? AND pm.MethodName IN ('BANK_TRANSFER','CASH')";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, paymentId);
                ps.setInt(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return transfer(rs);
                }
            }
        }
    }

    public Transfer findPendingCustomerTransferByOrder(int orderId, int customerId, String type) throws SQLException {
        if (orderId <= 0 || customerId <= 0) throw new IllegalArgumentException("Thông tin không hợp lệ.");
        try (Connection c = FinanceConnection.open()) {
            OrderData o = order(c, orderId);
            if (o == null || o.customerId != customerId) return null;
            String resolvedType = (type == null || type.isBlank()) ? null : normalizeType(type);
            if (resolvedType != null) {
                return pendingOfType(c, orderId, resolvedType);
            }
            Transfer t = pendingOfType(c, orderId, "DEPOSIT");
            if (t == null) t = pendingOfType(c, orderId, "REMAINING");
            return t;
        }
    }

    public List<Pending> pendingPayments() throws SQLException {
        return paymentsByStatus("PENDING");
    }

    public List<Pending> completedPayments() throws SQLException {
        return paymentsByStatus("COMPLETED");
    }

    private List<Pending> paymentsByStatus(String status) throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT p.PaymentID,p.OrderID,pt.TypeName,p.PaymentAmount,p.ReferenceCode, "
                    + "cu.CustomerName,p.Note,pm.MethodName FROM Payments p "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                    + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                    + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                    + "JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN Customers cu ON cu.CustomerID=o.CustomerID "
                    + "WHERE ps.StatusID=? AND pm.MethodName IN ('BANK_TRANSFER','INSTALLMENT','CASH') "
                    + "ORDER BY p.PaymentDate,p.PaymentID";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Pending> list = new ArrayList<>();
                    while (rs.next()) list.add(new Pending(rs.getInt("PaymentID"), rs.getInt("OrderID"),
                            rs.getString("TypeName"), rs.getBigDecimal("PaymentAmount"),
                            rs.getString("ReferenceCode"), rs.getString("CustomerName"), rs.getString("Note"),
                            rs.getString("MethodName")));
                    return list;
                }
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

    /** Accountant confirmation for a payment supported by independent evidence. */
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
                        if (!List.of("BANK_TRANSFER", "INSTALLMENT", "CASH").contains(rs.getString("MethodName")))
                            throw new IllegalStateException("Phương thức thanh toán không hợp lệ.");
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
                    if ("REMAINING".equals(type) && !hasSignedContract(c, orderId)) {
                        throw new IllegalStateException("Hợp đồng phải được ký trước khi xác nhận khoản 70%.");
                    }
                    BigDecimal due = expectedAmount(c, o, type);
                    if (amount.compareTo(due) != 0) throw new IllegalStateException("Khoản phải thu không khớp đơn hàng.");
                    if (receivedAmount == null || amount.compareTo(receivedAmount) != 0) {
                        throw new IllegalArgumentException("Tiền thực nhận khác nghĩa vụ thanh toán; không thể xác nhận đủ.");
                    }
                    String tag = "BANKTXN=" + bankTransaction.trim() + ";";
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT COUNT(*) FROM Payments WHERE PaymentID<>? "
                            + "AND (CHARINDEX(?,COALESCE(Note,''))>0 OR CHARINDEX(?,COALESCE(Note,''))>0 OR CHARINDEX(?,COALESCE(Note,''))>0)")) {
                        ps.setInt(1, paymentId);
                        ps.setString(2, tag);
                        ps.setString(3, "SEPAY_ID=" + bankTransaction.trim() + ";");
                        ps.setString(4, "VNPAY_TXN=" + bankTransaction.trim() + ";");
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            if (rs.getInt(1) > 0) throw new IllegalStateException("Mã sao kê đã được dùng cho giao dịch khác.");
                        }
                    }
                    complete(c, paymentId, o, type, employeeId, due,
                            tag + " ACCOUNTANT=" + employeeId + " CONFIRMED;", oldNote);
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

    /**
     * Xác nhận thanh toán thành công từ SePay Webhook.
     */
    public void completeSePay(Connection c, int paymentId, int orderId, String type, BigDecimal received,
                              String audit, String oldNote) throws SQLException {
        OrderData o = order(c, orderId);
        if (o == null || o.customerId <= 0) throw new IllegalStateException("Không tìm thấy khách hàng của đơn.");
        validateOpenOrder(c, o);
        if ("REMAINING".equals(type) && ("INSTALLMENT".equals(o.method) || !hasSignedContract(c, orderId))) {
            throw new IllegalStateException("Đơn hàng chưa đủ điều kiện thu 70% trực tuyến.");
        }
        BigDecimal due = expectedAmount(c, o, type);
        if (due.compareTo(received) != 0) throw new IllegalStateException("Số tiền không khớp nghĩa vụ thanh toán.");
        complete(c, paymentId, o, type, o.employeeId, due, audit, oldNote);
    }

    /**
     * Xử lý xác nhận giao dịch SePay thành công an toàn với database transaction Serializable.
     * Chống trùng lặp mã giao dịch SePay (idempotent) và kiểm tra nghiêm ngặt số tiền.
     * @return true nếu xác nhận thành công hoặc đã hoàn tất trước đó.
     */
    public boolean processSePaySuccess(long sepayId, String gateway, String accountNumber,
                                       String transferType, BigDecimal receivedAmount,
                                       String content, String referenceCode, String transactionDate) throws SQLException {
        if (sepayId <= 0) throw new IllegalArgumentException("Mã giao dịch SePay không hợp lệ.");
        if (receivedAmount == null || receivedAmount.signum() <= 0) throw new IllegalArgumentException("Số tiền không hợp lệ.");
        if (!"in".equalsIgnoreCase(transferType)) {
            return false;
        }

        String expectedAccount = SePayService.getAccountNumber();
        if (accountNumber != null && !accountNumber.isBlank() && !expectedAccount.equals(accountNumber.trim())) {
            throw new IllegalArgumentException("Số tài khoản nhận (" + accountNumber + ") không khớp tài khoản MB (" + expectedAccount + ").");
        }

        String paymentRef = extractPaymentReference(content, referenceCode);
        if (paymentRef == null || paymentRef.isBlank()) {
            return false;
        }

        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                // Khóa bảo vệ bảng Payments chống xử lý trùng đồng thời
                try (Statement lock = c.createStatement()) {
                    lock.executeQuery("SELECT COUNT(*) FROM Payments WITH (TABLOCKX, HOLDLOCK)").close();
                }

                String sql = "SELECT p.PaymentID, p.OrderID, pt.TypeName, ps.StatusID, p.PaymentAmount, p.Note "
                        + "FROM Payments p JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                        + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                        + "WHERE p.ReferenceCode=?";
                int paymentId;
                int orderId;
                String typeName;
                String statusId;
                BigDecimal expectedAmount;
                String oldNote;

                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, paymentRef);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        paymentId = rs.getInt("PaymentID");
                        orderId = rs.getInt("OrderID");
                        typeName = rs.getString("TypeName");
                        statusId = rs.getString("StatusID");
                        expectedAmount = rs.getBigDecimal("PaymentAmount");
                        oldNote = rs.getString("Note");
                    }
                }

                // Nếu đã hoàn tất, kiểm tra tính lũy đẳng (idempotent)
                if ("COMPLETED".equals(statusId)) {
                    c.commit();
                    return true;
                }

                if (!"PENDING".equals(statusId)) {
                    c.rollback();
                    return false;
                }

                if (expectedAmount.compareTo(receivedAmount) != 0) {
                    throw new IllegalStateException("Số tiền thực nhận (" + receivedAmount
                            + ") không khớp số tiền yêu cầu (" + expectedAmount + ").");
                }

                // Kiểm tra mã giao dịch SePay này đã từng dùng cho giao dịch khác chưa
                String sepayTag = "SEPAY_ID=" + sepayId + ";";
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM Payments WHERE PaymentID<>? AND CHARINDEX(?, COALESCE(Note,''))>0")) {
                    ps.setInt(1, paymentId);
                    ps.setString(2, sepayTag);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            throw new IllegalStateException("Mã giao dịch SePay #" + sepayId + " đã được xử lý cho một đơn khác.");
                        }
                    }
                }

                String safeGateway = (gateway != null && !gateway.isBlank()) ? gateway.trim() : "MBBank";
                String safeDate = (transactionDate != null) ? transactionDate.trim() : "";
                String safeContent = (content != null) ? content.replaceAll("[\\p{Cntrl};=]", " ").trim() : "";
                if (safeContent.length() > 80) safeContent = safeContent.substring(0, 80);

                String audit = sepayTag
                        + " SEPAY_GATEWAY=" + safeGateway + ";"
                        + " SEPAY_REF=" + paymentRef + ";"
                        + " SEPAY_DATE=" + safeDate + ";"
                        + " SEPAY_AMOUNT=" + receivedAmount + ";"
                        + " SEPAY_CONTENT=" + safeContent + ";";

                completeSePay(c, paymentId, orderId, typeName, receivedAmount, audit, oldNote);
                c.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public static String extractPaymentReference(String content, String referenceCode) {
        if (referenceCode != null && !referenceCode.isBlank()) {
            Matcher m = PAYMENT_CODE_PATTERN.matcher(referenceCode.toUpperCase(Locale.ROOT));
            if (m.find()) return m.group();
        }
        if (content != null && !content.isBlank()) {
            Matcher m = PAYMENT_CODE_PATTERN.matcher(content.toUpperCase(Locale.ROOT));
            if (m.find()) return m.group();
        }
        return null;
    }

    private static void complete(Connection c, int paymentId, OrderData o, String type, Integer employeeId,
                                 BigDecimal due, String audit, String oldNote) throws SQLException {
        updatePayment(c, paymentId, "COMPLETED", audit, oldNote);
        if ("DEPOSIT".equals(type)) {
            updateCarStatus(c, o.id, "DEPOSITED", "AVAILABLE", "RESERVED");
            changeOrderStatus(c, o.id, "DEPOSITED", employeeId, "30% deposit confirmed, payment " + paymentId);
        } else {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE Orders SET RemainingAmount=0 WHERE OrderID=? AND RemainingAmount=?")) {
                ps.setInt(1, o.id);
                ps.setBigDecimal(2, due);
                if (ps.executeUpdate() != 1) throw new IllegalStateException("Công nợ vừa thay đổi.");
            }
            updateCarStatus(c, o.id, "SOLD", "DEPOSITED", "SOLD");
            changeOrderStatus(c, o.id, "PAID", employeeId, "70% remaining confirmed, payment " + paymentId);
        }
    }

    private static void updatePayment(Connection c, int id, String status, String audit, String oldNote) throws SQLException {
        String note = ((oldNote == null || oldNote.isBlank()) ? "" : oldNote + " ")
                + audit + " AT=" + java.time.OffsetDateTime.now();
        if (note.length() > 500) throw new IllegalArgumentException("Ghi chú giao dịch quá dài.");
        try (PreparedStatement ps = c.prepareStatement("UPDATE Payments SET PaymentStatusID="
                + "(SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID=?), Note=?, "
                + "PaymentDate=CASE WHEN ?='COMPLETED' THEN SYSDATETIME() ELSE PaymentDate END "
                + "WHERE PaymentID=? AND PaymentStatusID=(SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID='PENDING')")) {
            ps.setString(1, status);
            ps.setString(2, note);
            ps.setString(3, status);
            ps.setInt(4, id);
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Không cập nhật được thanh toán.");
        }
    }

    private static void changeOrderStatus(Connection c, int orderId, String newStatus, Integer employee, String note)
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
            ps.setObject(3, employee, Types.INTEGER);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    private static void updateCarStatus(Connection c, int orderId, String name, String from, String alternate)
            throws SQLException {
        int carStatusId = lookup(c, "SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName)=?", name);
        try (PreparedStatement ps = c.prepareStatement("UPDATE Cars SET CarStatusID=? WHERE CarID IN "
                + "(SELECT CarID FROM OrderDetails WHERE OrderID=?) "
                + "AND CarStatusID IN (SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName) IN (?,?))")) {
            ps.setInt(1, carStatusId);
            ps.setInt(2, orderId);
            ps.setString(3, from);
            ps.setString(4, alternate);
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Xe không còn ở trạng thái cho phép đặt cọc.");
        }
    }

    private Transfer pendingOfType(Connection c, int orderId, String type) throws SQLException {
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

    private Transfer insertPending(Connection c, int orderId, String method, String type, BigDecimal amount)
            throws SQLException {
        return insertPending(c, orderId, method, type, amount, "127.0.0.1");
    }

    private Transfer insertPending(Connection c, int orderId, String method, String type, BigDecimal amount, String clientIp)
            throws SQLException {
        int methodId = lookup(c, "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName=?", method);
        int typeId = lookup(c, "SELECT PaymentTypeID FROM PaymentTypes WHERE TypeName=?", type);
        int statusId = lookup(c, "SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID=?", "PENDING");
        int paymentId;
        String note = "BANK_TRANSFER".equals(method) ? "GATEWAY=SEPAY_MBBANK;" : null;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Payments "
                + "(OrderID,PaymentMethodID,PaymentTypeID,PaymentStatusID,PaymentAmount,Note) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setInt(2, methodId);
            ps.setInt(3, typeId);
            ps.setInt(4, statusId);
            ps.setBigDecimal(5, amount);
            ps.setNString(6, note);
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

        String qrUrl = "";
        if ("BANK_TRANSFER".equals(method)) {
            try {
                qrUrl = sePayService.createVietQrUrl(ref, wholeVnd(amount).longValue(), ref);
            } catch (Exception ignored) {
            }
        }

        return new Transfer(paymentId, orderId, type, "PENDING", amount, ref,
                SePayService.getBankName(), SePayService.getAccountNumber(), SePayService.getAccountName(),
                method, false, note, qrUrl);
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
        String sql = "SELECT o.OrderID,o.CustomerID,o.EmployeeID,o.TotalAmount,o.DepositAmount,o.RemainingAmount, "
                + "o.CompanyID,os.StatusName,pm.MethodName FROM Orders o WITH (UPDLOCK,HOLDLOCK) "
                + "JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID WHERE o.OrderID=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new OrderData(rs.getInt("OrderID"), rs.getInt("CustomerID"),
                        rs.getBigDecimal("TotalAmount"), rs.getBigDecimal("DepositAmount"),
                        rs.getBigDecimal("RemainingAmount"), rs.getString("StatusName"),
                        rs.getString("MethodName"), rs.getObject("CompanyID", Integer.class),
                        rs.getObject("EmployeeID", Integer.class)) : null;
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

    private Transfer transfer(ResultSet rs) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("PaymentAmount");
        String ref = rs.getString("ReferenceCode");
        String note = rs.getString("Note");
        String method = rs.getString("MethodName");
        String status = rs.getString("StatusID");
        int paymentId = rs.getInt("PaymentID");
        int orderId = rs.getInt("OrderID");
        String typeName = rs.getString("TypeName");

        if (ref == null || !ref.matches("ECS[0-9]+[DR][0-9]+")) {
            ref = "ECS" + orderId + ("DEPOSIT".equalsIgnoreCase(typeName) ? "D" : "R") + paymentId;
            try (PreparedStatement ps = rs.getStatement().getConnection().prepareStatement(
                    "UPDATE Payments SET ReferenceCode=? WHERE PaymentID=? AND (ReferenceCode IS NULL OR ReferenceCode='')")) {
                ps.setString(1, ref);
                ps.setInt(2, paymentId);
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }

        String qrUrl = "";
        if ("PENDING".equals(status) && "BANK_TRANSFER".equals(method)) {
            try {
                qrUrl = sePayService.createVietQrUrl(ref, wholeVnd(amount).longValue(), ref);
            } catch (Exception ignored) {
            }
        }

        return new Transfer(paymentId, orderId, typeName, status, amount, ref,
                SePayService.getBankName(), SePayService.getAccountNumber(), SePayService.getAccountName(),
                method,
                note != null && (note.contains("SEPAY_ID=") || note.contains("BANKTXN=") || note.contains("VNPAY_TXN=")),
                note, qrUrl);
    }

    public static boolean isOnlineReady() {
        return SePayService.isReady();
    }

    public static String onlineConfigStatus() {
        return SePayService.configStatus();
    }

    public static boolean sepayReady() {
        return SePayService.isReady();
    }

    public static String sepayConfigStatus() {
        return SePayService.configStatus();
    }

    public static String mbAccountNumber() {
        return SePayService.getAccountNumber();
    }

    public static String mbAccountName() {
        return SePayService.getAccountName();
    }

    public static String mbBankName() {
        return SePayService.getBankName();
    }

    public static BigDecimal wholeVnd(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalStateException("Số tiền chưa nguyên VND; cần xử lý làm tròn theo quy tắc kế toán trước.");
        }
    }

    private record OrderData(int id, int customerId, BigDecimal total, BigDecimal deposit,
                             BigDecimal remaining, String status, String method, Integer companyId, Integer employeeId) { }
}
