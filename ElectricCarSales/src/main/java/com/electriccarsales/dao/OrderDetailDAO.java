package com.electriccarsales.dao;

import com.electriccarsales.model.OrderDetail;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailDAO {
    public List<OrderDetail> findByOrderId(int orderId) {
        String sql = "SELECT OrderDetailID, OrderID, CarID, SalesPrice FROM OrderDetails WHERE OrderID = ?";
        try (Connection conn = DBContext.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderDetail> list = new ArrayList<>();
                while (rs.next()) {
                    OrderDetail d = new OrderDetail();
                    d.setId(rs.getInt("OrderDetailID"));
                    d.setOrderId(rs.getInt("OrderID"));
                    d.setVehicleId(rs.getInt("CarID"));
                    d.setQuantity(1);
                    d.setUnitPrice(rs.getBigDecimal("SalesPrice"));
                    list.add(d);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải chi tiết đơn hàng.", e);
        }
    }

    public void insert(Connection conn, OrderDetail detail) throws SQLException {
        String sql = "INSERT INTO OrderDetails (OrderID, CarID, SalesPrice) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getOrderId());
            ps.setInt(2, detail.getVehicleId());
            ps.setBigDecimal(3, detail.getUnitPrice());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    detail.setId(keys.getInt(1));
                }
            }
        }
    }
}