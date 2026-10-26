package com.electriccarsales.dao;

import com.electriccarsales.model.OrderDetail;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailDAO extends DBContext {

    public List<OrderDetail> findByOrderId(int orderId) {
        List<OrderDetail> list = new ArrayList<>();
        String sql = "SELECT * FROM OrderDetails WHERE OrderID = ?";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        OrderDetail detail = new OrderDetail();
                        detail.setId(rs.getInt("OrderDetailID"));
                        detail.setOrderId(rs.getInt("OrderID"));
                        detail.setVehicleId(rs.getInt("CarID"));
                        detail.setQuantity(1);
                        detail.setUnitPrice(rs.getBigDecimal("SalesPrice"));
                        list.add(detail);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(OrderDetail detail) {
        String sql = "INSERT INTO OrderDetails (OrderID, CarID, SalesPrice) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, detail.getOrderId());
                ps.setInt(2, detail.getVehicleId());
                ps.setBigDecimal(3, detail.getUnitPrice());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}