package com.electriccarsales.dao.order;

import com.electriccarsales.model.order.Order;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB", "sa", "your_password");
        } catch (Exception e) {
            return null;
        }
    }

    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM Orders";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt("OrderID"));
                    order.setCustomerId(rs.getInt("CustomerID"));
                    order.setOriginalAmount(rs.getBigDecimal("OriginalAmount")); // Giữ nguyên tiền nghiệp vụ thực tế
                    order.setStatus(rs.getString("Status"));
                    order.setOrderDate(rs.getTimestamp("OrderDate"));
                    list.add(order);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(Order order) {
        String sql = "INSERT INTO Orders (CustomerID, OriginalAmount, Status, OrderDate) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, order.getCustomerId());
                ps.setBigDecimal(2, order.getOriginalAmount()); // Lưu đúng số tiền nghiệp vụ gốc
                ps.setString(3, order.getStatus());
                ps.setTimestamp(4, order.getOrderDate());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}