package com.electriccarsales.dao;

import com.electriccarsales.model.Invoices;
import com.electriccarsales.model.OrderStatusHistory;
import util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceDAO extends DBContext {
    public List<Invoices> findByCustomerId(int customerId) {
        return find("SELECT InvoiceID, OrderID, CustomerID, EmployeeID, InvoiceNumber, InvoiceDate, "
                + "SubTotal, DiscountAmount, TaxAmount, TotalAmount, Status, Note FROM Invoices "
                + "WHERE CustomerID = ? ORDER BY InvoiceDate DESC, InvoiceID DESC", customerId);
    }

    public List<Invoices> findAll() {
        String sql = "SELECT InvoiceID, OrderID, CustomerID, EmployeeID, InvoiceNumber, InvoiceDate, "
                + "SubTotal, DiscountAmount, TaxAmount, TotalAmount, Status, Note FROM Invoices "
                + "ORDER BY InvoiceDate DESC, InvoiceID DESC";
        List<Invoices> invoices = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) invoices.add(map(rs));
            return invoices;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn.", e);
        }
    }

    public List<Invoices> findByEmployeeUserId(int userId) {
        String sql = "SELECT i.InvoiceID, i.OrderID, i.CustomerID, i.EmployeeID, i.InvoiceNumber, i.InvoiceDate, "
                + "i.SubTotal, i.DiscountAmount, i.TaxAmount, i.TotalAmount, i.Status, i.Note "
                + "FROM Invoices i JOIN Orders o ON o.OrderID = i.OrderID "
                + "JOIN Employees e ON e.EmployeeID = o.EmployeeID WHERE e.UserID = ? "
                + "ORDER BY i.InvoiceDate DESC, i.InvoiceID DESC";
        List<Invoices> invoices = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) invoices.add(map(rs));
            }
            return invoices;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn được phân công.", e);
        }
    }

    public Invoices findByIdForCustomer(int invoiceId, int customerId) {
        String sql = "SELECT InvoiceID, OrderID, CustomerID, EmployeeID, InvoiceNumber, InvoiceDate, "
                + "SubTotal, DiscountAmount, TaxAmount, TotalAmount, Status, Note FROM Invoices "
                + "WHERE InvoiceID = ? AND CustomerID = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn.", e);
        }
    }

    public Invoices findByOrderId(int orderId) {
        String sql = "SELECT InvoiceID, OrderID, CustomerID, EmployeeID, InvoiceNumber, InvoiceDate, "
                + "SubTotal, DiscountAmount, TaxAmount, TotalAmount, Status, Note FROM Invoices WHERE OrderID = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn theo đơn hàng.", e);
        }
    }

    /**
     * Issues an official invoice per RDS 9.3:
     * - Verifies Contract exists and has Status = 'SIGNED' (PRE-1).
     * - Verifies 100% payment is complete (Orders.RemainingAmount = 0 and Order status is PAID) (PRE-2 & BR3).
     */
    public Invoices issueInvoice(int orderId, Integer accountantEmployeeId, String note) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int customerId;
                Integer orderEmpId;
                BigDecimal totalAmount;
                BigDecimal remainingAmount;
                String orderStatus;
                String contractStatus;

                String sql = """
                    SELECT o.CustomerID, o.EmployeeID, o.TotalAmount, o.RemainingAmount,
                           os.StatusName AS OrderStatus, ct.Status AS ContractStatus
                    FROM Orders o WITH (UPDLOCK, HOLDLOCK)
                    JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
                    LEFT JOIN Contracts ct ON ct.OrderID = o.OrderID
                    WHERE o.OrderID = ?
                    """;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
                        customerId = rs.getInt("CustomerID");
                        orderEmpId = rs.getObject("EmployeeID", Integer.class);
                        totalAmount = rs.getBigDecimal("TotalAmount");
                        remainingAmount = rs.getBigDecimal("RemainingAmount");
                        orderStatus = rs.getString("OrderStatus");
                        contractStatus = rs.getString("ContractStatus");
                    }
                }

                if (contractStatus == null || !"SIGNED".equalsIgnoreCase(contractStatus)) {
                    throw new IllegalStateException("Theo RDS (Mục II.9.3 PRE-1), hợp đồng của đơn hàng phải ở trạng thái đã ký (SIGNED) trước khi xuất hóa đơn.");
                }
                if (remainingAmount == null || remainingAmount.signum() != 0 || !"PAID".equalsIgnoreCase(orderStatus)) {
                    throw new IllegalStateException("Theo RDS (Mục II.9.3 BR3), chỉ được xuất hóa đơn sau khi đơn hàng đã xác nhận thanh toán đủ 100% (RemainingAmount = 0, trạng thái PAID). Công nợ hiện tại: "
                            + (remainingAmount == null ? "?" : remainingAmount.toPlainString()) + " VND.");
                }

                PaymentDAO.requireExactSettlement(connection, orderId);

                try (PreparedStatement check = connection.prepareStatement("SELECT InvoiceID FROM Invoices WHERE OrderID = ?")) {
                    check.setInt(1, orderId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException("Đơn hàng #" + orderId + " đã được xuất hóa đơn trước đó.");
                        }
                    }
                }

                int empId = (accountantEmployeeId != null && accountantEmployeeId > 0)
                        ? accountantEmployeeId
                        : (orderEmpId != null && orderEmpId > 0 ? orderEmpId : resolveFallbackEmployeeId(connection));
                String invoiceNo = "INV-ECS-" + orderId + "-" + (System.currentTimeMillis() % 100000);
                String invNote = (note == null || note.isBlank())
                        ? "Hóa đơn GTGT xe điện VinFast (Đã thu đủ 100% giá trị đơn hàng)."
                        : note.trim();

                int invoiceId;
                String insertSql = """
                    INSERT INTO Invoices (OrderID, CustomerID, EmployeeID, InvoiceNumber, SubTotal, DiscountAmount, TaxAmount, TotalAmount, Status, Note)
                    VALUES (?, ?, ?, ?, ?, 0, 0, ?, 'ISSUED', ?)
                    """;
                try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    ps.setInt(3, empId);
                    ps.setString(4, invoiceNo);
                    ps.setBigDecimal(5, totalAmount);
                    ps.setBigDecimal(6, totalAmount);
                    ps.setNString(7, invNote);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không tạo được hóa đơn.");
                        invoiceId = keys.getInt(1);
                    }
                }

                // Some paid orders predate the SOLD transition at payment completion.
                try (PreparedStatement ps = connection.prepareStatement("""
                        UPDATE Cars SET CarStatusID=(SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName)='SOLD')
                        WHERE CarID IN (SELECT CarID FROM OrderDetails WHERE OrderID=?)
                          AND CarStatusID IN (SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName) IN ('DEPOSITED','SOLD'))
                        """)) {
                    ps.setInt(1, orderId);
                    if (ps.executeUpdate() != 1) throw new IllegalStateException("Không xác định được xe đã bán của đơn hàng.");
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setStatus(orderStatus);
                history.setEmployeeId(empId);
                history.setNote("Kế toán đã phát hành hóa đơn " + invoiceNo + " (ISSUED). Đủ điều kiện xuất kho và bàn giao xe.");
                new OrderStatusHistoryDAO().insert(connection, history);

                connection.commit();
                return findByOrderId(orderId);
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException rb) { ex.addSuppressed(rb); }
                throw ex;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể phát hành hóa đơn.", e);
        }
    }

    /**
     * Truy vấn tất cả Orders có trạng thái PAID/DEPOSITED/PROCESSING kèm thông tin
     * Customer, Car, Contract, Invoice để hiển thị trên trang staff/invoices.jsp.
     */
    public List<Map<String, Object>> findOrdersForInvoicePage() {
        String sql = "SELECT o.OrderID, o.TotalAmount, o.RemainingAmount,"
                + " os.StatusName AS OrderStatus,"
                + " ct.Status AS ContractStatus,"
                + " cu.CustomerName, cu.Phone,"
                + " c.VIN, cm.ModelName, cv.VersionName,"
                + " inv.InvoiceID"
                + " FROM Orders o"
                + " JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID"
                + " JOIN Customers cu ON cu.CustomerID = o.CustomerID"
                + " LEFT JOIN OrderDetails od ON od.OrderID = o.OrderID"
                + " LEFT JOIN Cars c ON c.CarID = od.CarID"
                + " LEFT JOIN CarModels cm ON cm.ModelID = c.ModelID"
                + " LEFT JOIN CarVersions cv ON cv.VersionID = c.VersionID"
                + " LEFT JOIN Contracts ct ON ct.OrderID = o.OrderID"
                + " LEFT JOIN Invoices inv ON inv.OrderID = o.OrderID"
                + " WHERE os.StatusName IN ('PAID','DEPOSITED','PROCESSING')"
                + " ORDER BY o.OrderID DESC";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("orderId", rs.getInt("OrderID"));
                row.put("totalAmount", rs.getBigDecimal("TotalAmount"));
                row.put("remainingAmount", rs.getBigDecimal("RemainingAmount"));
                row.put("orderStatus", rs.getString("OrderStatus"));
                row.put("contractStatus", rs.getString("ContractStatus"));
                row.put("customerName", rs.getString("CustomerName"));
                row.put("phone", rs.getString("Phone"));
                row.put("vin", rs.getString("VIN"));
                row.put("modelName", rs.getString("ModelName"));
                row.put("versionName", rs.getString("VersionName"));
                int invoiceId = rs.getInt("InvoiceID");
                row.put("invoiceId", rs.wasNull() ? null : invoiceId);
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn danh sách đơn hàng cho trang hóa đơn.", e);
        }
    }

    private int resolveFallbackEmployeeId(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT TOP 1 EmployeeID FROM Employees ORDER BY EmployeeID");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new IllegalStateException("Chưa có hồ sơ nhân viên trong bảng Employees.");
    }

    private List<Invoices> find(String sql, int customerId) {
        List<Invoices> invoices = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) invoices.add(map(rs));
            }
            return invoices;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn.", e);
        }
    }

    private Invoices map(ResultSet rs) throws SQLException {
        Invoices invoice = new Invoices();
        invoice.setInvoiceID(rs.getInt("InvoiceID"));
        invoice.setOrderID(rs.getInt("OrderID"));
        invoice.setCustomerID(rs.getInt("CustomerID"));
        invoice.setEmployeeID(rs.getInt("EmployeeID"));
        invoice.setInvoiceNumber(rs.getString("InvoiceNumber"));
        Timestamp date = rs.getTimestamp("InvoiceDate");
        if (date != null) invoice.setInvoiceDate(date.toLocalDateTime());
        invoice.setSubTotal(rs.getBigDecimal("SubTotal"));
        invoice.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
        invoice.setTaxAmount(rs.getBigDecimal("TaxAmount"));
        invoice.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        invoice.setStatus(rs.getString("Status"));
        invoice.setNote(rs.getString("Note"));
        return invoice;
    }
}
