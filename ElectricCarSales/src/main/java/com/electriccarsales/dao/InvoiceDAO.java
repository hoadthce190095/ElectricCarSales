package com.electriccarsales.dao;

import com.electriccarsales.model.Invoices;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) invoices.add(map(rs));
            return invoices;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hóa đơn.", e);
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
