package com.electriccarsales.dao;

import com.electriccarsales.model.Order;
import util.DBContext;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO extends DBContext {

    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM Orders";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt("OrderID"));
                    order.setCustomerId(rs.getInt("CustomerID"));
                    order.setOriginalAmount(rs.getBigDecimal("TotalAmount"));
                    order.setStatus(String.valueOf(rs.getInt("OrderStatusID")));
                    order.setOrderDate(rs.getTimestamp("OrderDate"));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(Order order) {
        String sql = "INSERT INTO Orders (CustomerID, OrderStatusID, PaymentMethodID, OrderDate, TotalAmount, DepositAmount, RemainingAmount) VALUES (?, 1, 1, ?, ?, 0, ?)";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, order.getCustomerId());
                ps.setTimestamp(2, order.getOrderDate() != null ? order.getOrderDate() : new Timestamp(System.currentTimeMillis()));
                ps.setBigDecimal(3, order.getOriginalAmount() != null ? order.getOriginalAmount() : BigDecimal.ZERO);
                ps.setBigDecimal(4, order.getOriginalAmount() != null ? order.getOriginalAmount() : BigDecimal.ZERO);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}