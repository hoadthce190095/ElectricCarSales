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
import java.util.Collections;
import java.util.List;

public class OrderStatusHistoryDAO extends DBContext {

    private static final String BASE_SELECT =
            "SELECT h.HistoryID, h.OrderID, h.OldStatus, h.NewStatus, h.ChangedBy, h.ChangedDate, h.Note, " +
            "e.FullName AS EmployeeName " +
            "FROM OrderStatusHistory h " +
            "LEFT JOIN Employees e ON h.ChangedBy = e.EmployeeID ";

    public List<OrderStatusHistory> findAll() {
        List<OrderStatusHistory> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY h.ChangedDate DESC, h.HistoryID DESC";
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<OrderStatusHistory> findByOrderId(int orderId) {
        List<OrderStatusHistory> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE h.OrderID = ? ORDER BY h.ChangedDate DESC, h.HistoryID DESC";
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int insert(OrderStatusHistory history) {
        String sql = "INSERT INTO OrderStatusHistory (OrderID, OldStatus, NewStatus, ChangedBy, ChangedDate, Note) " +
                     "VALUES (?, ?, ?, ?, SYSDATETIME(), ?)";
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, history.getOrderId());
            ps.setString(2, history.getOldStatus());
            ps.setString(3, history.getNewStatus() != null ? history.getNewStatus() : (history.getStatus() != null ? history.getStatus() : "PENDING"));
            if (history.getChangedById() != null && history.getChangedById() > 0) {
                ps.setInt(4, history.getChangedById());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, history.getNote());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int genId = keys.getInt(1);
                        history.setId(genId);
                        history.setHistoryId(genId);
                        return genId;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private OrderStatusHistory mapRow(ResultSet rs) throws SQLException {
        OrderStatusHistory h = new OrderStatusHistory();
        int id = rs.getInt("HistoryID");
        h.setId(id);
        h.setHistoryId(id);
        h.setOrderId(rs.getInt("OrderID"));
        h.setOldStatus(rs.getString("OldStatus"));
        h.setNewStatus(rs.getString("NewStatus"));
        h.setStatus(rs.getString("NewStatus"));

        int changedBy = rs.getInt("ChangedBy");
        if (!rs.wasNull()) {
            h.setChangedBy(changedBy);
        }
        h.setChangedAt(rs.getTimestamp("ChangedDate"));
        h.setNote(rs.getString("Note"));

        String empName = rs.getString("EmployeeName");
        h.setChangedByName(empName != null && !empName.isBlank() ? empName : "Nhân viên kinh doanh");
        h.setStatusName(mapStatusToDisplayName(h.getNewStatus()));
        h.setStatusId(mapStatusStringToId(h.getNewStatus()));
        return h;
    }

    private static int mapStatusStringToId(String status) {
        if (status == null || status.isBlank()) return 1;
        switch (status.toUpperCase()) {
            case "DEPOSITED": case "2": return 2;
            case "PROCESSING": case "3": return 3;
            case "PAYMENT_PENDING": case "4": return 4;
            case "READY_FOR_DELIVERY": case "5": return 5;
            case "COMPLETED": case "6": return 6;
            case "CANCELLED": case "7": return 7;
            default: return 1;
        }
    }

    private static String mapStatusToDisplayName(String status) {
        if (status == null || status.isBlank()) return "Chờ đặt cọc";
        switch (status.toUpperCase()) {
            case "DEPOSITED": case "2": return "Đã đặt cọc";
            case "PROCESSING": case "3": return "Chờ hợp đồng";
            case "PAYMENT_PENDING": case "4": return "Chờ thanh toán";
            case "READY_FOR_DELIVERY": case "5": return "Chờ bàn giao";
            case "COMPLETED": case "6": return "Đã hoàn tất";
            case "CANCELLED": case "7": return "Đã hủy";
            default: return "Chờ đặt cọc";
        }
    }
}
