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
               cu.CustomerName, cu.Phone AS CustomerPhone, e.EmployeeName,
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
                    paymentStructureNote = paymentStructureNote.substring(0, 500);
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
                history.setNote("Đã lập dự thảo hợp đồng " + contractNo + " (PENDING_SIGNATURE). Chờ khách hàng ký xác thực OTP.");
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
    public Contract signContract(int contractId, int customerId) {
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
                if (!"PENDING_SIGNATURE".equalsIgnoreCase(currentStatus) && !"PENDING".equalsIgnoreCase(currentStatus)) {
                    throw new IllegalStateException("Trạng thái hợp đồng không hợp lệ để ký: " + currentStatus);
                }

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