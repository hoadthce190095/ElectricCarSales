package com.electriccarsales.dao;

import com.electriccarsales.model.OrderStatusHistory;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderStatusHistoryDAO extends DBContext {

    public List<OrderStatusHistory> findByOrderId(int orderId) {
        List<OrderStatusHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM OrderStatusHistory WHERE OrderID = ? ORDER BY ChangedDate DESC";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        OrderStatusHistory history = new OrderStatusHistory();
                        history.setId(rs.getInt("OrderStatusHistoryID"));
                        history.setOrderId(rs.getInt("OrderID"));
                        history.setStatus(String.valueOf(rs.getInt("OrderStatusID")));
                        history.setChangedAt(rs.getTimestamp("ChangedDate"));
                        list.add(history);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(OrderStatusHistory history) {
        String sql = "INSERT INTO OrderStatusHistory (OrderID, OrderStatusID, ChangedDate, Note) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, history.getOrderId());
                int statusId = 1;
                try {
                    statusId = Integer.parseInt(history.getStatus());
                } catch (NumberFormatException ignored) {}
                ps.setInt(2, statusId);
                ps.setTimestamp(3, history.getChangedAt() != null ? history.getChangedAt() : new Timestamp(System.currentTimeMillis()));
                ps.setString(4, "Status updated");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}