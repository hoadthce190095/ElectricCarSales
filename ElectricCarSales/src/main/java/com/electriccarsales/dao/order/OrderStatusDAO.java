package com.electriccarsales.dao;

import com.electriccarsales.model.OrderStatus;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderStatusDAO {
    public List<OrderStatus> findAll() {
        String sql = "SELECT OrderStatusID, StatusName, Description FROM OrderStatuses ORDER BY OrderStatusID";
        try (Connection conn = DBContext.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<OrderStatus> list = new ArrayList<>();
            while (rs.next()) {
                OrderStatus s = new OrderStatus();
                s.setId(rs.getInt("OrderStatusID"));
                s.setStatusName(rs.getString("StatusName"));
                s.setDescription(rs.getString("Description"));
                list.add(s);
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách trạng thái đơn hàng.", e);
        }
    }
}
