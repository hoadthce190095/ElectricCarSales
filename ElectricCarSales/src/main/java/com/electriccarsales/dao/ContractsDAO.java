package com.electriccarsales.dao;

import com.electriccarsales.model.Contract;
import com.electriccarsales.model.OrderStatusHistory;
import util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ContractsDAO extends DBContext {
    private static final String SELECT_SQL = """
        SELECT ct.ContractID, ct.OrderID, ct.CustomerID, ct.EmployeeID,
               ct.ContractNumber, ct.ContractDate, ct.ContractAmount, ct.Status, ct.Note,
               cu.CustomerName, cu.Phone AS CustomerPhone, cu.Email AS CustomerEmail, cu.Address AS CustomerAddress,
               e.EmployeeName,
               cm.ModelName, cv.VersionName, c.VIN,
               pm.MethodName AS PaymentMethodName, fc.CompanyName, os.StatusName AS OrderStatus
        FROM Contracts ct
        JOIN Orders o ON o.OrderID = ct.OrderID
        JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
        JOIN PaymentMethods pm ON pm.PaymentMethodID = o.PaymentMethodID
        JOIN Customers cu ON cu.CustomerID = ct.CustomerID
        LEFT JOIN Employees e ON e.EmployeeID = ct.EmployeeID
        LEFT JOIN FinancialCompanies fc ON fc.CompanyID = o.CompanyID
        LEFT JOIN OrderDetails od ON od.OrderID = o.OrderID
        LEFT JOIN Cars c ON c.CarID = od.CarID
        LEFT JOIN CarModels cm ON cm.ModelID = c.ModelID
        LEFT JOIN CarVersions cv ON cv.VersionID = c.VersionID
        """;

    public List<Contract> getAllContracts() {
        List<Contract> list = new ArrayList<>();
        String sql = SELECT_SQL + " ORDER BY ct.ContractDate DESC, ct.ContractID DESC";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải danh sách hợp đồng.", e);
        }
        return list;
    }

    public List<Contract> findByCustomerId(int customerId) {
        List<Contract> list = new ArrayList<>();
        String sql = SELECT_SQL + " WHERE ct.CustomerID = ? ORDER BY ct.ContractDate DESC, ct.ContractID DESC";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải hợp đồng của khách hàng.", e);
        }
        return list;
    }

    public Contract findById(int contractId) {
        String sql = SELECT_SQL + " WHERE ct.ContractID = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải chi tiết hợp đồng.", e);
        }
    }

    public Contract findByOrderId(int orderId) {
        String sql = SELECT_SQL + " WHERE ct.OrderID = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải hợp đồng của đơn hàng.", e);
        }
    }

    public boolean addContract(Contract c) {
        String sql = "INSERT INTO Contracts (OrderID, CustomerID, EmployeeID, ContractNumber, ContractAmount, Status, Note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, c.getOrderID());
            ps.setInt(2, c.getCustomerID());
            ps.setInt(3, c.getEmployeeID());
            ps.setString(4, c.getContractNumber());
            ps.setDouble(5, c.getContractAmount());
            ps.setString(6, c.getStatus());
            ps.setNString(7, c.getNote());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addContract: " + e.getMessage());
        }
        return false;
    }

    /**
     * Creates a sales contract for an order that has completed its 30% deposit per RDS 9.1.
     */
    public Contract createContractForOrder(int orderId, Integer staffEmployeeId, String customTerms) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int customerId;
                Integer orderEmpId;
                BigDecimal totalAmount;
                BigDecimal depositAmount;
                BigDecimal remainingAmount;
                String orderStatus;
                String paymentMethod;
                String companyName;

                String orderSql = """
                    SELECT o.CustomerID, o.EmployeeID, o.TotalAmount, o.DepositAmount, o.RemainingAmount,
                           os.StatusName, pm.MethodName, fc.CompanyName
                    FROM Orders o WITH (UPDLOCK, HOLDLOCK)
                    JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
                    JOIN PaymentMethods pm ON pm.PaymentMethodID = o.PaymentMethodID
                    LEFT JOIN FinancialCompanies fc ON fc.CompanyID = o.CompanyID
                    WHERE o.OrderID = ?
                    """;
                try (PreparedStatement ps = connection.prepareStatement(orderSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
                        }
                        customerId = rs.getInt("CustomerID");
                        orderEmpId = rs.getObject("EmployeeID", Integer.class);
                        totalAmount = rs.getBigDecimal("TotalAmount");
                        depositAmount = rs.getBigDecimal("DepositAmount");
                        remainingAmount = rs.getBigDecimal("RemainingAmount");
                        orderStatus = rs.getString("StatusName");
                        paymentMethod = rs.getString("MethodName");
                        companyName = rs.getString("CompanyName");
                    }
                }

                if (!"DEPOSITED".equalsIgnoreCase(orderStatus)
                        && !"PROCESSING".equalsIgnoreCase(orderStatus)
                        && !"PAID".equalsIgnoreCase(orderStatus)) {
                    throw new IllegalStateException("Theo RDS (Mục II.9.1 PRE-1), chỉ được tạo hợp đồng sau khi đơn hàng đã được xác nhận đặt cọc 30% (DEPOSITED). Trạng thái hiện tại: " + orderStatus);
                }

                requireCompletedDeposit(connection, orderId);
                try (PreparedStatement check = connection.prepareStatement("SELECT ContractID, Status FROM Contracts WHERE OrderID = ?")) {
                    check.setInt(1, orderId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException("Đơn hàng #" + orderId + " đã có hợp đồng (#" + rs.getInt("ContractID") + ").");
                        }
                    }
                }

                int responsibleEmp = (staffEmployeeId != null && staffEmployeeId > 0)
                        ? staffEmployeeId
                        : (orderEmpId != null && orderEmpId > 0 ? orderEmpId : resolveFallbackEmployeeId(connection));

                String paymentStructureNote = ("INSTALLMENT".equalsIgnoreCase(paymentMethod) || companyName != null)
                        ? ("Phương thức: Trả góp qua đối tác " + (companyName == null ? "Ngân hàng" : companyName)
                           + " (Khách đã cọc 30%: " + depositAmount.toPlainString()
                           + " VND; Đối tác giải ngân 70%: " + remainingAmount.toPlainString() + " VND).")
                        : ("Phương thức: Trả thẳng (" + paymentMethod + ") (Khách đã cọc 30%: "
                           + depositAmount.toPlainString() + " VND; Khách thanh toán 70% còn lại: "
                           + remainingAmount.toPlainString() + " VND).");
                if (customTerms != null && !customTerms.isBlank()) {
                    paymentStructureNote = paymentStructureNote + " Điều khoản bổ sung: " + customTerms.trim();
                }
                if (paymentStructureNote.length() > 500) {
                    throw new IllegalArgumentException("Điều khoản hợp đồng vượt giới hạn 500 ký tự; vui lòng rút gọn trước khi lưu.");
                }

                String contractNo = "HD-ECS-" + orderId + "-" + (System.currentTimeMillis() % 100000);
                int contractId;
                String insertSql = """
                    INSERT INTO Contracts (OrderID, CustomerID, EmployeeID, ContractNumber, ContractAmount, Status, Note)
                    VALUES (?, ?, ?, ?, ?, 'PENDING_SIGNATURE', ?)
                    """;
                try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    ps.setInt(3, responsibleEmp);
                    ps.setString(4, contractNo);
                    ps.setBigDecimal(5, totalAmount);
                    ps.setNString(6, paymentStructureNote);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không tạo được hợp đồng.");
                        contractId = keys.getInt(1);
                    }
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setStatus(orderStatus);
                history.setEmployeeId(responsibleEmp);
                history.setNote("Đã lập dự thảo hợp đồng " + contractNo + " (PENDING_SIGNATURE). Chờ khách hàng xem nội dung và ký bằng OTP.");
                new OrderStatusHistoryDAO().insert(connection, history);

                connection.commit();
                return findById(contractId);
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException rb) { ex.addSuppressed(rb); }
                throw ex;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tạo hợp đồng mua bán xe.", e);
        }
    }

    /**
     * Marks a contract as SIGNED after customer OTP verification per RDS 9.2.
     */
    private static int consumeSigningOtp(Connection connection, int userId, int customerId, int contractId,
                                         String otp) throws SQLException {
        // Contract row is locked by the signing transaction. Recheck expiry/status in the
        // conditional update so concurrent OTP reissue cannot consume an invalidated code.
        String sql = """
                SELECT TOP 1 v.OTPVerificationID FROM OTPVerifications v WITH (UPDLOCK,HOLDLOCK)
                JOIN Customers cu ON cu.UserID=v.UserID
                WHERE cu.CustomerID=? AND v.UserID=? AND v.Purpose=? AND v.OTPCode=?
                  AND v.Status='PENDING' AND v.VerifiedDate IS NULL AND v.ExpiredDate>SYSDATETIME()
                ORDER BY v.OTPVerificationID DESC
                """;
        Integer otpId = null;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, userId);
            ps.setString(3, signingPurpose(contractId));
            ps.setString(4, otp);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) otpId = rs.getInt(1);
            }
        }
        if (otpId != null) {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE OTPVerifications SET Status='VERIFIED',VerifiedDate=SYSDATETIME()
                    WHERE OTPVerificationID=? AND Status='PENDING' AND VerifiedDate IS NULL
                      AND ExpiredDate>SYSDATETIME()
                    """)) {
                ps.setInt(1, otpId);
                if (ps.executeUpdate() == 1) return otpId;
            }
        }
        throw new IllegalArgumentException("Mã OTP ký hợp đồng không đúng hoặc đã hết hạn.");
    }

    public void requireSigningEligible(int contractId, int customerId, int userId) {
        try (Connection connection = openConnection()) {
            requireSigningEligible(connection, contractId, customerId, userId);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra điều kiện ký hợp đồng.", e);
        }
    }

    private static void requireSigningEligible(Connection connection, int contractId, int customerId,
                                               int userId) throws SQLException {
        String sql = """
                SELECT c.OrderID FROM Contracts c
                JOIN Customers cu ON cu.CustomerID=c.CustomerID
                JOIN Orders o WITH (UPDLOCK,HOLDLOCK) ON o.OrderID=c.OrderID AND o.CustomerID=c.CustomerID
                JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID
                WHERE c.ContractID=? AND c.CustomerID=? AND cu.UserID=?
                  AND c.Status='PENDING_SIGNATURE' AND c.ContractAmount=o.TotalAmount
                  AND os.StatusName IN ('DEPOSITED','PROCESSING')
                """;
        int orderId;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            ps.setInt(2, customerId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Hợp đồng không thuộc tài khoản hoặc chưa đủ điều kiện ký.");
                orderId = rs.getInt("OrderID");
            }
        }
        requireCompletedDeposit(connection, orderId);
    }

    private static void requireCompletedDeposit(Connection connection, int orderId) throws SQLException {
        String sql = """
                SELECT o.DepositAmount, o.TotalAmount,
                       (SELECT COALESCE(SUM(p.PaymentAmount), 0)
                        FROM Payments p JOIN PaymentTypes pt ON p.PaymentTypeID=pt.PaymentTypeID
                        JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID
                        WHERE p.OrderID=o.OrderID AND pt.TypeName='DEPOSIT' AND ps.StatusID='COMPLETED') AS Confirmed
                FROM Orders o WHERE o.OrderID=?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal deposit = rs.getBigDecimal("TotalAmount").multiply(new BigDecimal("0.30"))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    if (deposit.signum() > 0 && deposit.compareTo(rs.getBigDecimal("DepositAmount")) == 0
                            && deposit.compareTo(rs.getBigDecimal("Confirmed")) == 0) return;
                }
            }
        }
        throw new IllegalStateException("Cọc 30% chưa được xác nhận đầy đủ.");
    }

    public static String signingPurpose(int contractId) {
        if (contractId <= 0) throw new IllegalArgumentException("Mã hợp đồng không hợp lệ.");
        return "SIGN_CONTRACT_ID:" + contractId;
    }

    public Contract signContract(int contractId, int customerId, int userId, String otp) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int orderId;
                int ownerCustomerId;
                String currentStatus;
                String contractNo;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT OrderID, CustomerID, Status, ContractNumber FROM Contracts WITH (UPDLOCK, HOLDLOCK) WHERE ContractID = ?")) {
                    ps.setInt(1, contractId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy hợp đồng #" + contractId);
                        orderId = rs.getInt("OrderID");
                        ownerCustomerId = rs.getInt("CustomerID");
                        currentStatus = rs.getString("Status");
                        contractNo = rs.getString("ContractNumber");
                    }
                }
                if (ownerCustomerId != customerId) {
                    throw new SecurityException("Bạn không có quyền ký hợp đồng này.");
                }
                if ("SIGNED".equalsIgnoreCase(currentStatus)) {
                    throw new IllegalStateException("Hợp đồng này đã được ký xác thực trước đó (RDS BR4).");
                }
                if (!"PENDING_SIGNATURE".equalsIgnoreCase(currentStatus)) {
                    throw new IllegalStateException("Trạng thái hợp đồng không hợp lệ để ký: " + currentStatus);
                }

                requireSigningEligible(connection, contractId, customerId, userId);
                consumeSigningOtp(connection, userId, customerId, contractId, otp);

                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Contracts SET Status = 'SIGNED' WHERE ContractID = ?")) {
                    ps.setInt(1, contractId);
                    ps.executeUpdate();
                }

                // Transition order from DEPOSITED to PROCESSING when contract is signed
                String orderStatus;
                try (PreparedStatement ordPs = connection.prepareStatement("""
                        SELECT os.StatusName FROM Orders o
                        JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
                        WHERE o.OrderID = ?
                        """)) {
                    ordPs.setInt(1, orderId);
                    try (ResultSet rs = ordPs.executeQuery()) {
                        orderStatus = rs.next() ? rs.getString(1) : "PROCESSING";
                    }
                }
                String nextOrderStatus = "DEPOSITED".equalsIgnoreCase(orderStatus) ? "PROCESSING" : orderStatus;
                if ("PROCESSING".equals(nextOrderStatus) && !"PROCESSING".equalsIgnoreCase(orderStatus)) {
                    try (PreparedStatement upOrd = connection.prepareStatement("""
                            UPDATE Orders
                            SET OrderStatusID = (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName = 'PROCESSING')
                            WHERE OrderID = ?
                            """)) {
                        upOrd.setInt(1, orderId);
                        upOrd.executeUpdate();
                    }
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setStatus(nextOrderStatus);
                history.setEmployeeId(null);
                history.setNote("Khách hàng đã ký hợp đồng " + contractNo + " thành công bằng mã xác thực OTP (SIGNED).");
                new OrderStatusHistoryDAO().insert(connection, history);

                connection.commit();
                return findById(contractId);
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException rb) { ex.addSuppressed(rb); }
                throw ex;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật trạng thái ký hợp đồng.", e);
        }
    }

    private int resolveFallbackEmployeeId(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT TOP 1 EmployeeID FROM Employees ORDER BY EmployeeID");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new IllegalStateException("Chưa có hồ sơ nhân viên trong bảng Employees.");
    }

    private Contract map(ResultSet rs) throws SQLException {
        Contract c = new Contract(
                rs.getInt("ContractID"),
                rs.getInt("OrderID"),
                rs.getInt("CustomerID"),
                rs.getInt("EmployeeID"),
                rs.getString("ContractNumber"),
                rs.getTimestamp("ContractDate"),
                rs.getDouble("ContractAmount"),
                rs.getString("Status"),
                rs.getString("Note")
        );
        c.setCustomerName(rs.getString("CustomerName"));
        c.setCustomerPhone(rs.getString("CustomerPhone"));
        try { c.setCustomerEmail(rs.getString("CustomerEmail")); } catch (Exception ignored) {}
        try { c.setCustomerAddress(rs.getString("CustomerAddress")); } catch (Exception ignored) {}
        c.setEmployeeName(rs.getString("EmployeeName"));
        c.setModelName(rs.getString("ModelName"));
        c.setVersionName(rs.getString("VersionName"));
        c.setVin(rs.getString("VIN"));
        c.setPaymentMethodName(rs.getString("PaymentMethodName"));
        c.setCompanyName(rs.getString("CompanyName"));
        c.setOrderStatus(rs.getString("OrderStatus"));
        return c;
    }
}