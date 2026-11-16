package com.electriccarsales.dao;

import com.electriccarsales.model.OrderStatusHistory;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderStatusHistoryDAO {
    public List<OrderStatusHistory> findByOrderId(int orderId) {
        String sql = """
            SELECT h.OrderStatusHistoryID, h.OrderID, s.StatusName, h.EmployeeID, e.EmployeeName,
                   h.ChangedDate, h.Note
            FROM OrderStatusHistory h
            JOIN OrderStatuses s ON s.OrderStatusID = h.OrderStatusID
            LEFT JOIN Employees e ON e.EmployeeID = h.EmployeeID
            WHERE h.OrderID = ?
            ORDER BY h.ChangedDate DESC, h.OrderStatusHistoryID DESC
            """;
        try (Connection conn = DBContext.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderStatusHistory> list = new ArrayList<>();
                while (rs.next()) {
                    OrderStatusHistory h = new OrderStatusHistory();
                    h.setId(rs.getInt("OrderStatusHistoryID"));
                    h.setOrderId(rs.getInt("OrderID"));
                    h.setStatus(rs.getString("StatusName"));
                    h.setEmployeeId(rs.getObject("EmployeeID", Integer.class));
                    h.setEmployeeName(rs.getString("EmployeeName"));
                    h.setChangedAt(rs.getTimestamp("ChangedDate"));
                    h.setNote(rs.getString("Note"));
                    list.add(h);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải lịch sử trạng thái đơn hàng.", e);
        }
    }

    public void insert(OrderStatusHistory history) {
        try (Connection conn = DBContext.openConnection()) {
            insert(conn, history);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể ghi lịch sử trạng thái đơn hàng.", e);
        }
    }

    public void insert(Connection conn, OrderStatusHistory history) throws SQLException {
        String sql = """
            INSERT INTO OrderStatusHistory (OrderID, OrderStatusID, EmployeeID, ChangedDate, Note)
            VALUES (?, (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName = ?), ?, SYSDATETIME(), ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, history.getOrderId());
            ps.setString(2, history.getStatus().trim().toUpperCase(Locale.ROOT));
            if (history.getEmployeeId() == null || history.getEmployeeId() <= 0) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, history.getEmployeeId());
            }
            ps.setNString(4, history.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    history.setId(keys.getInt(1));
                }
            }
        }
    }
}
