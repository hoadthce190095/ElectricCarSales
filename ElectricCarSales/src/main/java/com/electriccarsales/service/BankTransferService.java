package com.electriccarsales.service;

import com.electriccarsales.dao.FinanceConnection;
import com.electriccarsales.dao.SystemLogDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quản lý quy trình thanh toán chuyển khoản và cổng thanh toán trực tuyến SePay + MB Bank.
 * Hỗ trợ tạo giao dịch, mã VietQR động, thanh toán cọc 30%, thanh toán 70% còn lại,
 * đối soát Kế toán và giải ngân trả góp sau khi khách hàng ký hợp đồng.
 */
public class BankTransferService {
    private static final String ONLINE_GATEWAY = "SEPAY_MBBANK";
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(?<![A-Z0-9])ECS([0-9]+)[DR]([0-9]+)(?![A-Z0-9])");
    private final SePayService sePayService = new SePayService();

    public record Transfer(int paymentId, int orderId, String type, String status,
                           BigDecimal amount, String reference, String bankName,
                           String accountNumber, String accountName, String methodName,
                           boolean bankReported, String note, String paymentUrl, String orderStatus) {
        public Transfer(int paymentId, int orderId, String type, String status,
                        BigDecimal amount, String reference, String bankName,
                        String accountNumber, String accountName, String methodName,
                        boolean bankReported, String note, String paymentUrl) {
            this(paymentId, orderId, type, status, amount, reference, bankName,
                    accountNumber, accountName, methodName, bankReported, note, paymentUrl, null);
        }
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
        public String getOrderStatus() { return orderStatus; }
        public boolean isOrderCancelled() { return "CANCELLED".equalsIgnoreCase(orderStatus); }
        public String getQrUrl() { return getPaymentUrl(); }
        public String getOrderCode() { return "ECS" + orderId; }
        public String getFormattedType() {
            if ("DEPOSIT".equalsIgnoreCase(type)) return "DEPOSIT_30";
            if ("REMAINING".equalsIgnoreCase(type)) return "REMAINING_70";
            return type != null ? type : "—";
        }
        public String getFormattedAmount() {
            return com.electriccarsales.util.FormatUtils.formatMoney(amount);
        }
    }

    public record Pending(int paymentId, int orderId, String type, BigDecimal amount,
                          String reference, String customer, String note, String methodName,
                          java.sql.Timestamp paymentDate, String status) {
        public Pending(int paymentId, int orderId, String type, BigDecimal amount,
                       String reference, String customer, String note, String methodName) {
            this(paymentId, orderId, type, amount, reference, customer, note, methodName, null, "PENDING");
        }
        public int getPaymentId() { return paymentId; }
        public int getOrderId() { return orderId; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getReference() { return reference; }
        public String getCustomer() { return customer; }
        public String getNote() { return note; }
        public String getMethodName() { return methodName; }
        public java.sql.Timestamp getPaymentDate() { return paymentDate; }
        public String getStatus() { return status != null ? status : "PENDING"; }
        public SePayEvidence getSePayEvidence() {
            return "BANK_TRANSFER".equals(methodName) ? SePayEvidence.fromNote(note) : null;
        }
        public boolean isSePay() { return "BANK_TRANSFER".equals(methodName); }
        public boolean isOcbQr() { return false; }
        public boolean isBankReported() {
            return note != null && (note.contains("SEPAY_ID=") || note.contains("BANKTXN=") || note.contains("VNPAY_TXN="));
        }
        public boolean isReconciled() {
            return note != null && note.contains("RECONCILED=true;");
        }
        public String getReconciledBy() {
            if (note == null) return null;
            Matcher m = Pattern.compile("RECONCILED_BY=([^;]+);").matcher(note);
            return m.find() ? m.group(1) : null;
        }
        public String getReconciledAt() {
            if (note == null) return null;
            Matcher m = Pattern.compile("RECONCILED_AT=([^;]+);").matcher(note);
            return m.find() ? m.group(1) : null;
        }
        public String getFormattedType() {
            if ("DEPOSIT".equalsIgnoreCase(type)) return "DEPOSIT_30";
            if ("REMAINING".equalsIgnoreCase(type)) return "REMAINING_70";
            return type != null ? type : "—";
        }
        public String getOrderCode() {
            return "ECS" + orderId;
        }
        public BigDecimal getReceivedAmount() {
            SePayEvidence ev = getSePayEvidence();
            if (ev != null && ev.getReceived() != null) return ev.getReceived();
            return amount;
        }
        public String getFormattedAmount() {
            return com.electriccarsales.util.FormatUtils.formatMoney(amount);
        }
        public String getFormattedReceivedAmount() {
            return com.electriccarsales.util.FormatUtils.formatMoney(getReceivedAmount());
        }
        public String getFormattedDate() {
            if (paymentDate == null) return "—";
            return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(paymentDate);
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
        public String getOrderCode() { return "ECS" + orderId; }
        public String getFormattedTotalAmount() {
            return com.electriccarsales.util.FormatUtils.formatMoney(totalAmount);
        }
        public String getFormattedDepositAmount() {
            return com.electriccarsales.util.FormatUtils.formatMoney(depositAmount);
        }
        public String getFormattedRemainingAmount() {
            return com.electriccarsales.util.FormatUtils.formatMoney(remainingAmount);
        }
    }

    public Transfer createForCustomer(int orderId, int customerId, String requestedType) throws SQLException {
        return createForCustomer(orderId, customerId, requestedType, "BANK_TRANSFER");
    }

    public Transfer createForCustomer(int orderId, int customerId, String requestedType, String method) throws SQLException {
        return createForCustomer(orderId, customerId, requestedType, method, "127.0.0.1");
    }

    public Transfer createForCustomer(int orderId, int customerId, String requestedType, String method, String clientIp) throws SQLException {
        if (!"BANK_TRANSFER".equals(method)) {
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
                if (transfer != null && !method.equals(transfer.methodName()))
                    throw new IllegalStateException("Giao dịch đang chờ sử dụng phương thức khác.");
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
                if (o.remaining == null || o.remaining.compareTo(BigDecimal.ZERO) == 0) {
                    Transfer done = completedOfType(c, orderId, "REMAINING");
                    if (done != null) return done;
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
            String sql = "SELECT p.PaymentID,p.OrderID,pt.TypeName,ps.StatusID,p.PaymentAmount,p.ReferenceCode,p.Note,pm.MethodName,os.StatusName AS OrderStatusName "
                    + "FROM Payments p JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
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

    public Transfer checkAndAutoCompleteTransfer(int paymentId, int customerId) throws SQLException {
        return findCustomerTransfer(paymentId, customerId);
    }

    /**
     * Đồng bộ toàn bộ giao dịch ngân hàng đã được ghi nhận của khách hàng.
     * Tự động hoàn tất và cập nhật đơn hàng ngay trước khi hiển thị màn hình thanh toán / đơn hàng.
     */
    public void syncCustomerReportedTransfers(int customerId) {
        if (customerId <= 0) return;
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT p.PaymentID "
                    + "FROM Payments p JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                    + "WHERE o.CustomerID=? AND ps.StatusID='PENDING' AND UPPER(os.StatusName)<>'CANCELLED' "
                    + "AND (CHARINDEX('SEPAY_ID=', COALESCE(p.Note,''))>0 OR CHARINDEX('BANKTXN=', COALESCE(p.Note,''))>0 OR CHARINDEX('VNPAY_TXN=', COALESCE(p.Note,''))>0)";
            List<Integer> ids = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ids.add(rs.getInt(1));
                }
            }
            for (int pid : ids) {
                try {
                    checkAndAutoCompleteTransfer(pid, customerId);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public Transfer findPendingCustomerTransferByOrder(int orderId, int customerId, String type) throws SQLException {
        if (orderId <= 0 || customerId <= 0) throw new IllegalArgumentException("Thông tin không hợp lệ.");
        try (Connection c = FinanceConnection.open()) {
            OrderData o = order(c, orderId);
            if (o == null || o.customerId != customerId) return null;
            if ("CANCELLED".equalsIgnoreCase(o.status)) return null;
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

    public List<Pending> lateTransfersAfterCancel() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT p.PaymentID,p.OrderID,pt.TypeName,p.PaymentAmount,p.ReferenceCode, "
                    + "cu.CustomerName,p.Note,pm.MethodName,p.PaymentDate,ps.StatusID FROM Payments p "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                    + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                    + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                    + "JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN Customers cu ON cu.CustomerID=o.CustomerID "
                    + "WHERE ps.StatusID='CANCELLED' AND CHARINDEX('LATE_AFTER_CANCEL=true;', COALESCE(p.Note,''))>0 "
                    + "ORDER BY p.PaymentDate DESC,p.PaymentID DESC";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                List<Pending> list = new ArrayList<>();
                while (rs.next()) list.add(new Pending(rs.getInt("PaymentID"), rs.getInt("OrderID"),
                        rs.getString("TypeName"), rs.getBigDecimal("PaymentAmount"),
                        rs.getString("ReferenceCode"), rs.getString("CustomerName"), rs.getString("Note"),
                        rs.getString("MethodName"), rs.getTimestamp("PaymentDate"), rs.getString("StatusID")));
                return list;
            }
        }
    }

    private List<Pending> paymentsByStatus(String status) throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            String sql = "SELECT p.PaymentID,p.OrderID,pt.TypeName,p.PaymentAmount,p.ReferenceCode, "
                    + "cu.CustomerName,p.Note,pm.MethodName,p.PaymentDate,ps.StatusID FROM Payments p "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                    + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                    + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                    + "JOIN Orders o ON o.OrderID=p.OrderID "
                    + "JOIN Customers cu ON cu.CustomerID=o.CustomerID "
                    + "WHERE ps.StatusID=? AND pm.MethodName IN ('BANK_TRANSFER','INSTALLMENT','CASH') "
                    + "ORDER BY p.PaymentDate DESC,p.PaymentID DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Pending> list = new ArrayList<>();
                    while (rs.next()) list.add(new Pending(rs.getInt("PaymentID"), rs.getInt("OrderID"),
                            rs.getString("TypeName"), rs.getBigDecimal("PaymentAmount"),
                            rs.getString("ReferenceCode"), rs.getString("CustomerName"), rs.getString("Note"),
                            rs.getString("MethodName"), rs.getTimestamp("PaymentDate"), rs.getString("StatusID")));
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
                    + "WHERE o.RemainingAmount>0 AND UPPER(LTRIM(RTRIM(os.StatusName)))<>'CANCELLED' "
                    + "ORDER BY o.OrderDate DESC,o.OrderID DESC";
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
     * Accountant xác nhận đối soát cho giao dịch đã hoàn tất (COMPLETED).
     * Kiểm tra chặt chẽ quyền Accountant, trạng thái COMPLETED, chống đối soát 2 lần,
     * không tạo Payment thứ hai, không làm sai lệch số dư / RemainingAmount.
     */
    public void reconcile(int paymentId, int accountantUserId) throws SQLException {
        if (paymentId <= 0 || accountantUserId <= 0) {
            throw new IllegalArgumentException("Thiếu mã giao dịch hoặc nhân viên đối soát.");
        }
        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int employeeId = accountant(c, accountantUserId);
                if (employeeId <= 0) {
                    throw new SecurityException("Chỉ Accountant hợp lệ mới được thực hiện đối soát.");
                }
                try (Statement lock = c.createStatement()) {
                    lock.executeQuery("SELECT COUNT(*) FROM Payments WITH (TABLOCKX, HOLDLOCK)").close();
                }
                String sql = "SELECT p.PaymentID, p.OrderID, p.PaymentAmount, ps.StatusID, p.Note, pt.TypeName, p.ReferenceCode "
                        + "FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                        + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                        + "WHERE p.PaymentID=?";
                String status;
                String note;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, paymentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Không tìm thấy giao dịch #" + paymentId);
                        }
                        status = rs.getString("StatusID");
                        note = rs.getString("Note");
                    }
                }
                if (!"COMPLETED".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Giao dịch chưa hoàn tất thành công (trạng thái hiện tại: " + status + "), không thể đối soát.");
                }
                if (note != null && note.contains("RECONCILED=true;")) {
                    throw new IllegalStateException("Giao dịch #" + paymentId + " đã được đối soát trước đó; không thể đối soát lại.");
                }
                String timestamp = java.time.OffsetDateTime.now().toString();
                String reconcileTag = "RECONCILED=true; RECONCILED_BY=" + employeeId + "; RECONCILED_AT=" + timestamp + ";";
                String newNote = (note == null || note.isBlank()) ? reconcileTag : (note.trim() + " " + reconcileTag);
                if (newNote.length() > 500) {
                    newNote = (newNote.substring(0, 430) + "... " + reconcileTag);
                    if (newNote.length() > 500) {
                        newNote = reconcileTag;
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE Payments SET Note=? WHERE PaymentID=?")) {
                    ps.setString(1, newNote);
                    ps.setInt(2, paymentId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Không thể lưu trạng thái đối soát cho giao dịch #" + paymentId);
                    }
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
        if (expectedAmount(c, o, type).compareTo(received) != 0) throw new IllegalStateException("Số tiền không khớp công nợ.");
        complete(c, paymentId, o, type, o.employeeId, received, audit, oldNote);
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
        if (expectedAccount.isBlank() || accountNumber == null || !expectedAccount.equals(accountNumber.trim())) {
            throw new IllegalArgumentException("Tài khoản nhận tiền không hợp lệ.");
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

                String sql = "SELECT p.PaymentID, p.OrderID, pt.TypeName, ps.StatusID, p.PaymentAmount, p.Note, "
                        + "os.StatusName AS OrderStatusName, o.OrderDate "
                        + "FROM Payments p JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                        + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                        + "JOIN Orders o ON o.OrderID=p.OrderID "
                        + "JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
                        + "WHERE p.ReferenceCode=? AND p.PaymentMethodID IN (SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName='BANK_TRANSFER')";
                int paymentId;
                int orderId;
                String typeName;
                String statusId;
                BigDecimal expectedAmount;
                String oldNote;
                String orderStatusName;
                LocalDateTime orderDate;

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
                        orderStatusName = rs.getString("OrderStatusName");
                        Timestamp orderTs = rs.getTimestamp("OrderDate");
                        orderDate = orderTs == null ? null : orderTs.toLocalDateTime();
                    }
                }

                String orderReference = "ECS" + orderId + ("DEPOSIT".equals(typeName) ? "D" : "R") + paymentId;
                if (!orderReference.equals(paymentRef) || !List.of("DEPOSIT","REMAINING").contains(typeName))
                    throw new IllegalStateException("Mã giao dịch không khớp đơn hoặc loại thanh toán.");

                String safeGateway = (gateway != null && !gateway.isBlank()) ? gateway.trim() : "MBBank";
                String safeDate = (transactionDate != null) ? transactionDate.trim() : "";
                String safeContent = (content != null) ? content.replaceAll("[\\p{Cntrl};=]", " ").trim() : "";
                if (safeContent.length() > 80) safeContent = safeContent.substring(0, 80);
                String sepayTag = "SEPAY_ID=" + sepayId + ";";

                // Nếu đã hoàn tất, kiểm tra tính lũy đẳng (idempotent)
                if ("COMPLETED".equals(statusId) && !"CANCELLED".equalsIgnoreCase(orderStatusName)) {
                    c.commit();
                    return expectedAmount.compareTo(receivedAmount) == 0 && oldNote != null
                            && oldNote.contains(sepayTag);
                }

                // Kiểm tra mã giao dịch SePay này đã từng dùng cho giao dịch khác chưa
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

                String audit = sepayTag
                        + " SEPAY_GATEWAY=" + safeGateway + ";"
                        + " SEPAY_REF=" + paymentRef + ";"
                        + " SEPAY_DATE=" + safeDate + ";"
                        + " SEPAY_AMOUNT=" + receivedAmount + ";"
                        + " SEPAY_CONTENT=" + safeContent + ";";

                // Đơn hàng hoặc giao dịch đã hủy: KHÔNG kích hoạt lại đơn, KHÔNG xác nhận COMPLETED,
                // nhưng phải ghi nhận giao dịch chuyển khoản muộn để Kế toán xử lý.
                if ("CANCELLED".equalsIgnoreCase(statusId) || "CANCELLED".equalsIgnoreCase(orderStatusName)) {
                    if (oldNote != null && oldNote.contains(sepayTag)) {
                        c.commit();
                        return false;
                    }
                    recordLateTransferForAccountant(c, paymentId, orderId, audit, oldNote, sepayId, receivedAmount, safeDate);
                    c.commit();
                    return false;
                }

                if (!"PENDING".equals(statusId)) {
                    c.rollback();
                    return false;
                }

                // Nếu giao dịch phát sinh sau thời điểm hết hạn (72h cọc hoặc 30 ngày trả 70%), hủy đơn quá hạn
                // và ghi nhận chuyển khoản muộn cho Kế toán thay vì tự động xác nhận.
                LocalDateTime txTime = SePayService.parseTransactionDate(transactionDate);
                if (txTime != null) {
                    if ("DEPOSIT".equals(typeName) && orderDate != null
                            && txTime.isAfter(orderDate.plusHours(OrderExpirationService.DEPOSIT_EXPIRATION_HOURS))) {
                        OrderExpirationService exp = new OrderExpirationService();
                        exp.cancelExpiredOrderInTransaction(c, orderId, true,
                                txTime.minusHours(OrderExpirationService.DEPOSIT_EXPIRATION_HOURS),
                                txTime.minusHours(OrderExpirationService.REMAINING_EXPIRATION_HOURS));
                        String latestNote = readPaymentNote(c, paymentId);
                        recordLateTransferForAccountant(c, paymentId, orderId, audit, latestNote, sepayId, receivedAmount, safeDate);
                        c.commit();
                        return false;
                    }
                    if ("REMAINING".equals(typeName)) {
                        LocalDateTime depositConfirmedAt = OrderExpirationService.resolveDepositConfirmedAt(c, orderId);
                        if (depositConfirmedAt != null
                                && txTime.isAfter(depositConfirmedAt.plusHours(OrderExpirationService.REMAINING_EXPIRATION_HOURS))) {
                            OrderExpirationService exp = new OrderExpirationService();
                            exp.cancelExpiredOrderInTransaction(c, orderId, false,
                                    txTime.minusHours(OrderExpirationService.DEPOSIT_EXPIRATION_HOURS),
                                    txTime.minusHours(OrderExpirationService.REMAINING_EXPIRATION_HOURS));
                            String latestNote = readPaymentNote(c, paymentId);
                            recordLateTransferForAccountant(c, paymentId, orderId, audit, latestNote, sepayId, receivedAmount, safeDate);
                            c.commit();
                            return false;
                        }
                    }
                }

                if (expectedAmount.compareTo(receivedAmount) != 0) {
                    throw new IllegalStateException("Số tiền thực nhận (" + receivedAmount
                            + ") không khớp số tiền yêu cầu (" + expectedAmount + ").");
                }

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
                    "UPDATE Orders SET RemainingAmount=0 WHERE OrderID=?")) {
                ps.setInt(1, o.id);
                ps.executeUpdate();
            }
            updateCarStatus(c, o.id, "SOLD", "DEPOSITED", "SOLD");
            changeOrderStatus(c, o.id, "PAID", employeeId, "70% remaining confirmed, payment " + paymentId);
        }
    }

    public static void completeInstallmentDisbursementAfterContractSign(Connection c, int orderId) throws SQLException {
        throw new IllegalStateException("Chờ xác minh khoản giải ngân thực nhận từ đối tác.");
    }

    private static boolean accountantConfirmedDeposit(Connection c, int orderId, BigDecimal requiredDeposit)
            throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.PaymentAmount),0) FROM Payments p "
                + "JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "WHERE p.OrderID=? AND pt.TypeName='DEPOSIT' AND ps.StatusID='COMPLETED' "
                + "AND (CHARINDEX('RECONCILED=true;',COALESCE(p.Note,''))>0 "
                + "OR (CHARINDEX('ACCOUNTANT=',COALESCE(p.Note,''))>0 "
                + "AND CHARINDEX('CONFIRMED;',COALESCE(p.Note,''))>0))";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && requiredDeposit.compareTo(rs.getBigDecimal(1)) == 0;
            }
        }
    }

    private Transfer completedOfType(Connection c, int orderId, String type) throws SQLException {
        String sql = "SELECT TOP (1) p.PaymentID,p.OrderID,pt.TypeName,ps.StatusID,p.PaymentAmount,p.ReferenceCode,p.Note,pm.MethodName,os.StatusName AS OrderStatusName "
                + "FROM Payments p JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                + "JOIN Orders o ON o.OrderID=p.OrderID JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
                + "WHERE p.OrderID=? AND pt.TypeName=? AND ps.StatusID='COMPLETED' ORDER BY p.PaymentID DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? transfer(rs) : null; }
        }
    }

    private static String readPaymentNote(Connection c, int paymentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT Note FROM Payments WHERE PaymentID=?")) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void recordLateTransferForAccountant(Connection c, int paymentId, int orderId,
                                                        String audit, String oldNote, long sepayId,
                                                        BigDecimal receivedAmount, String safeDate) throws SQLException {
        int cancelledStatusId = paymentStatusId(c, "CANCELLED");
        String tag = "LATE_AFTER_CANCEL=true; " + audit + " AT=" + java.time.OffsetDateTime.now();
        String newNote = appendNotePreservingAudit(oldNote, tag);
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Payments SET PaymentStatusID=?, Note=? WHERE PaymentID=?")) {
            ps.setInt(1, cancelledStatusId);
            ps.setNString(2, newNote);
            ps.setInt(3, paymentId);
            ps.executeUpdate();
        }
        try {
            new SystemLogDAO().record(c, null, "LATE_TRANSFER_AFTER_CANCEL", "ORDER",
                    String.valueOf(orderId),
                    "Khách chuyển khoản sau khi đơn #" + orderId + " đã hủy (Payment #" + paymentId
                            + ", SEPAY_ID=" + sepayId + ", Số tiền=" + receivedAmount
                            + ", Thời gian=" + safeDate + ") - cần Kế toán xử lý.");
        } catch (Exception ignored) {
        }
    }

    public static String appendNotePreservingAudit(String existing, String addition) {
        String add = addition == null ? "" : addition.trim();
        String base = existing == null ? "" : existing.trim();
        if (base.isEmpty()) {
            return add.length() <= 500 ? add : add.substring(0, 500);
        }
        if (add.isEmpty()) {
            return base.length() <= 500 ? base : base.substring(0, 500);
        }
        String combined = base + " " + add;
        if (combined.length() <= 500) {
            return combined;
        }
        // Keep the beginning of the existing note (which holds SEPAY_ID / BANKTXN / RECONCILED)
        // and append the new tag cleanly.
        if (add.length() >= 490) {
            return add.substring(0, 500);
        }
        int keep = Math.max(0, 500 - add.length() - 4);
        if (keep == 0) {
            return add;
        }
        return base.substring(0, Math.min(base.length(), keep)) + "... " + add;
    }

    private static void updatePayment(Connection c, int id, String status, String audit, String oldNote) throws SQLException {
        int statusId = paymentStatusId(c, status);
        String note = appendNotePreservingAudit(oldNote, audit + " AT=" + java.time.OffsetDateTime.now());
        try (PreparedStatement ps = c.prepareStatement("UPDATE Payments SET PaymentStatusID=?, Note=?, "
                + "PaymentDate=CASE WHEN ?='COMPLETED' THEN SYSDATETIME() ELSE PaymentDate END "
                + "WHERE PaymentID=?")) {
            ps.setInt(1, statusId);
            ps.setString(2, note);
            ps.setString(3, status);
            ps.setInt(4, id);
            if (ps.executeUpdate() != 1) throw new IllegalStateException("Không cập nhật được thanh toán.");
        }
    }

    /** Restore a missing canonical reference row atomically; never substitute an unrelated status. */
    private static int paymentStatusId(Connection c, String status) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("Payment status resolution requires a transaction.");
        String description = switch (status) {
            case "PENDING" -> "Payment is pending";
            case "COMPLETED" -> "Payment has been completed";
            case "FAILD" -> "Payment has failed";
            case "CANCELLED" -> "Payment has been cancelled";
            default -> throw new SQLException("Unsupported payment status: " + status);
        };
        // Range lock prevents two transactions inserting the same missing status.
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT PaymentStatusID FROM PaymentStatuses WITH (UPDLOCK,HOLDLOCK) WHERE StatusID=?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO PaymentStatuses(StatusID,Description) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, status);
            ps.setNString(2, description);
            if (ps.executeUpdate() != 1) throw new SQLException("Cannot restore payment status: " + status);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Missing generated payment status ID: " + status);
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
                + "AND CarStatusID IN (SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName) IN (?,?,?))")) {
            ps.setInt(1, carStatusId);
            ps.setInt(2, orderId);
            ps.setString(3, name);
            ps.setString(4, from);
            ps.setString(5, alternate);
            ps.executeUpdate();
        }
    }

    private Transfer pendingOfType(Connection c, int orderId, String type) throws SQLException {
        String sql = "SELECT TOP (1) p.PaymentID,p.OrderID,pt.TypeName,ps.StatusID,p.PaymentAmount,p.ReferenceCode,p.Note,pm.MethodName,os.StatusName AS OrderStatusName "
                + "FROM Payments p JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID "
                + "JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                + "JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID "
                + "JOIN Orders o ON o.OrderID=p.OrderID JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID "
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
        BigDecimal deposit = o.total.multiply(new BigDecimal("0.30")).setScale(0, RoundingMode.HALF_UP);
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
        if ("CANCELLED".equalsIgnoreCase(o.status)) {
            throw new IllegalStateException("Đơn hàng đã bị hủy. Bạn không thể tạo hoặc quét mã thanh toán cho đơn này.");
        }
        if (List.of("PAID", "COMPLETED").contains(o.status.toUpperCase(Locale.ROOT))) {
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
                if (rs.next()) return rs.getInt(1);
            }
        }
        // Thử tìm nới lỏng (không phân biệt hoa thường và bỏ khoảng trắng)
        String flexibleSql = sql.replaceAll("(?i)\\b(\\w+)\\s*=\\s*\\?", "UPPER(LTRIM(RTRIM($1)))=UPPER(LTRIM(RTRIM(?)))");
        try (PreparedStatement ps = c.prepareStatement(flexibleSql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // Tự động khởi tạo nếu thiếu trong database
        if (sql.contains("PaymentStatuses")) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO PaymentStatuses(StatusID, Description) VALUES(?,?)")) {
                ps.setString(1, value.toUpperCase(Locale.ROOT));
                ps.setNString(2, "Trạng thái thanh toán " + value);
                ps.executeUpdate();
                return lookup(c, sql, value);
            } catch (Exception ignored) {}
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP (1) PaymentStatusID FROM PaymentStatuses ORDER BY PaymentStatusID")) {
                if (rs.next()) return rs.getInt(1);
            } catch (Exception ignored) {}
        }
        if (sql.contains("OrderStatuses")) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderStatuses(StatusName, Description) VALUES(?,?)")) {
                ps.setString(1, value.toUpperCase(Locale.ROOT));
                ps.setNString(2, "Trạng thái đơn hàng " + value);
                ps.executeUpdate();
                return lookup(c, sql, value);
            } catch (Exception ignored) {}
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP (1) OrderStatusID FROM OrderStatuses ORDER BY OrderStatusID")) {
                if (rs.next()) return rs.getInt(1);
            } catch (Exception ignored) {}
        }
        if (sql.contains("PaymentMethods")) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO PaymentMethods(MethodName, Description) VALUES(?,?)")) {
                ps.setString(1, value.toUpperCase(Locale.ROOT));
                ps.setNString(2, "Phương thức " + value);
                ps.executeUpdate();
                return lookup(c, sql, value);
            } catch (Exception ignored) {}
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP (1) PaymentMethodID FROM PaymentMethods ORDER BY PaymentMethodID")) {
                if (rs.next()) return rs.getInt(1);
            } catch (Exception ignored) {}
        }
        if (sql.contains("PaymentTypes")) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO PaymentTypes(TypeName, Description) VALUES(?,?)")) {
                ps.setString(1, value.toUpperCase(Locale.ROOT));
                ps.setNString(2, "Loại thanh toán " + value);
                ps.executeUpdate();
                return lookup(c, sql, value);
            } catch (Exception ignored) {}
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP (1) PaymentTypeID FROM PaymentTypes ORDER BY PaymentTypeID")) {
                if (rs.next()) return rs.getInt(1);
            } catch (Exception ignored) {}
        }
        throw new IllegalStateException("Thiếu dữ liệu danh mục: " + value);
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
        String orderStatus = rs.getString("OrderStatusName");
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
        if (!"CANCELLED".equalsIgnoreCase(orderStatus)
                && "PENDING".equals(status) && "BANK_TRANSFER".equals(method)) {
            try {
                qrUrl = sePayService.createVietQrUrl(ref, wholeVnd(amount).longValue(), ref);
            } catch (Exception ignored) {
            }
        }

        return new Transfer(paymentId, orderId, typeName, status, amount, ref,
                SePayService.getBankName(), SePayService.getAccountNumber(), SePayService.getAccountName(),
                method,
                note != null && (note.contains("SEPAY_ID=") || note.contains("BANKTXN=") || note.contains("VNPAY_TXN=")),
                note, qrUrl, orderStatus);
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
