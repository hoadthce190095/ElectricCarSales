package com.electriccarsales.dao;

import com.electriccarsales.model.Invoices;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoiceDAO extends DBContext {
    /** Issues from database-owned order, contract and employee data; Order row is the serialization lock. */
    public void issueForPaidOrder(int orderId, int accountantUserId) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        boolean oldAutoCommit;
        try { oldAutoCommit = conn.getAutoCommit(); conn.setAutoCommit(false); }
        catch (SQLException e) { throw new IllegalStateException("Không thể bắt đầu phát hành hóa đơn.", e); }
        try {
            String eligibility = "SELECT o.CustomerID,o.TotalAmount,o.RemainingAmount,pm.MethodName "
                    + "FROM Orders o WITH (UPDLOCK,HOLDLOCK) JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID "
                    + "JOIN Contracts c ON c.OrderID=o.OrderID AND c.CustomerID=o.CustomerID "
                    + "WHERE o.OrderID=? AND UPPER(c.Status)='SIGNED' "
                    + "AND NOT EXISTS(SELECT 1 FROM Invoices i WITH (UPDLOCK,HOLDLOCK) WHERE i.OrderID=o.OrderID)";
            int customerId; java.math.BigDecimal total; java.math.BigDecimal remaining; String method;
            try (PreparedStatement ps = conn.prepareStatement(eligibility)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Đơn hàng chưa đủ điều kiện hoặc đã có hóa đơn.");
                    customerId = rs.getInt("CustomerID"); total = rs.getBigDecimal("TotalAmount");
                    remaining = rs.getBigDecimal("RemainingAmount"); method = rs.getString("MethodName");
                }
            }
            if ("INSTALLMENT".equalsIgnoreCase(method)) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM InstallmentContracts WITH (UPDLOCK,HOLDLOCK) WHERE OrderID=?")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Đơn trả góp chưa có hồ sơ tài chính.");
                    }
                }
            } else {
                if (remaining == null || remaining.signum() != 0)
                    throw new IllegalArgumentException("Đơn hàng chưa thanh toán đủ.");
                try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(p.PaymentAmount),0) "
                        + "FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID "
                        + "WHERE p.OrderID=? AND ps.StatusID='COMPLETED'")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getBigDecimal(1).compareTo(total) < 0)
                            throw new IllegalArgumentException("Chưa ghi nhận đủ khoản thanh toán hoàn tất.");
                    }
                }
            }
            int employeeId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT e.EmployeeID FROM Employees e "
                    + "JOIN Users u ON u.UserID=e.UserID JOIN Roles r ON r.RoleID=u.RoleID "
                    + "JOIN Statuses s ON s.StatusID=u.StatusID WHERE e.UserID=? "
                    + "AND r.RoleName='ACCOUNTANT' AND s.StatusName='ACTIVE'")) {
                ps.setInt(1, accountantUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Tài khoản không có quyền kế toán phát hành hóa đơn.");
                    employeeId = rs.getInt(1);
                }
            }
            String insert = "INSERT INTO Invoices(OrderID,CustomerID,EmployeeID,InvoiceNumber,SubTotal,DiscountAmount,TaxAmount,TotalAmount,Status) "
                    + "VALUES(?,?,?,?,?,0,0,?,'ISSUED')";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, orderId); ps.setInt(2, customerId); ps.setInt(3, employeeId);
                ps.setString(4, "INV-" + UUID.randomUUID()); ps.setBigDecimal(5, total); ps.setBigDecimal(6, total);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (IllegalArgumentException e) {
            try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601)
                throw new IllegalArgumentException("Đơn hàng đã được phát hành hóa đơn.", e);
            throw new IllegalStateException("Không thể phát hành hóa đơn.", e);
        } finally {
            try { conn.setAutoCommit(oldAutoCommit); }
            catch (SQLException e) { throw new IllegalStateException("Không thể khôi phục trạng thái kết nối.", e); }
        }
    }

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
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn.", e);
        }
    }

    private List<Invoices> find(String sql, int customerId) {
        List<Invoices> invoices = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
