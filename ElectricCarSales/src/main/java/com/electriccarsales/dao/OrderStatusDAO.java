package com.electriccarsales.dao;

import com.electriccarsales.model.OrderStatus;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderStatusDAO extends DBContext {

    public List<OrderStatus> findAll() {
        List<OrderStatus> list = new ArrayList<>();
        String sql = "SELECT * FROM OrderStatuses";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    OrderStatus status = new OrderStatus();
                    status.setId(rs.getInt("OrderStatusID"));
                    status.setStatusName(rs.getString("StatusName"));
                    list.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}