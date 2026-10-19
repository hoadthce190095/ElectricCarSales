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
        String sql = "SELECT o.OrderID, o.CustomerID, o.OrderStatusID, o.PaymentMethodID, o.OrderDate, " +
                     "o.TotalAmount, o.DepositAmount, o.RemainingAmount, o.Note, " +
                     "c.CustomerName, c.Phone AS CustomerPhone, c.Email AS CustomerEmail, c.Address AS CustomerAddress, " +
                     "os.StatusName, " +
                     "pm.MethodName AS PaymentMethod, " +
                     "cm.ModelName, car.VIN " +
                     "FROM Orders o " +
                     "LEFT JOIN Customers c ON o.CustomerID = c.CustomerID " +
                     "LEFT JOIN OrderStatuses os ON o.OrderStatusID = os.OrderStatusID " +
                     "LEFT JOIN PaymentMethods pm ON o.PaymentMethodID = pm.PaymentMethodID " +
                     "LEFT JOIN OrderDetails od ON o.OrderID = od.OrderID " +
                     "LEFT JOIN Cars car ON od.CarID = car.CarID " +
                     "LEFT JOIN CarModels cm ON car.ModelID = cm.ModelID " +
                     "ORDER BY o.OrderDate DESC";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt("OrderID"));
                    order.setCustomerId(rs.getInt("CustomerID"));
                    order.setOriginalAmount(rs.getBigDecimal("TotalAmount"));
                    order.setStatus(String.valueOf(rs.getInt("OrderStatusID")));
                    order.setStatusName(rs.getString("StatusName"));
                    order.setOrderDate(rs.getTimestamp("OrderDate"));
                    order.setDepositAmount(rs.getBigDecimal("DepositAmount"));
                    order.setRemainingAmount(rs.getBigDecimal("RemainingAmount"));
                    order.setNote(rs.getString("Note"));
                    order.setCustomerName(rs.getString("CustomerName"));
                    order.setCustomerPhone(rs.getString("CustomerPhone"));
                    order.setCustomerEmail(rs.getString("CustomerEmail"));
                    order.setCustomerAddress(rs.getString("CustomerAddress"));
                    order.setPaymentMethod(rs.getString("PaymentMethod"));
                    order.setVehicleName(rs.getString("ModelName"));
                    order.setVinNumber(rs.getString("VIN"));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Order findById(int orderId) {
        String sql = "SELECT o.OrderID, o.CustomerID, o.OrderStatusID, o.PaymentMethodID, o.OrderDate, " +
                     "o.TotalAmount, o.DepositAmount, o.RemainingAmount, o.Note, " +
                     "c.CustomerName, c.Phone AS CustomerPhone, c.Email AS CustomerEmail, c.Address AS CustomerAddress, " +
                     "os.StatusName, " +
                     "pm.MethodName AS PaymentMethod, " +
                     "cm.ModelName, car.VIN " +
                     "FROM Orders o " +
                     "LEFT JOIN Customers c ON o.CustomerID = c.CustomerID " +
                     "LEFT JOIN OrderStatuses os ON o.OrderStatusID = os.OrderStatusID " +
                     "LEFT JOIN PaymentMethods pm ON o.PaymentMethodID = pm.PaymentMethodID " +
                     "LEFT JOIN OrderDetails od ON o.OrderID = od.OrderID " +
                     "LEFT JOIN Cars car ON od.CarID = car.CarID " +
                     "LEFT JOIN CarModels cm ON car.ModelID = cm.ModelID " +
                     "WHERE o.OrderID = ?";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Order order = new Order();
                        order.setId(rs.getInt("OrderID"));
                        order.setCustomerId(rs.getInt("CustomerID"));
                        order.setOriginalAmount(rs.getBigDecimal("TotalAmount"));
                        order.setStatus(String.valueOf(rs.getInt("OrderStatusID")));
                        order.setStatusName(rs.getString("StatusName"));
                        order.setOrderDate(rs.getTimestamp("OrderDate"));
                        order.setDepositAmount(rs.getBigDecimal("DepositAmount"));
                        order.setRemainingAmount(rs.getBigDecimal("RemainingAmount"));
                        order.setNote(rs.getString("Note"));
                        order.setCustomerName(rs.getString("CustomerName"));
                        order.setCustomerPhone(rs.getString("CustomerPhone"));
                        order.setCustomerEmail(rs.getString("CustomerEmail"));
                        order.setCustomerAddress(rs.getString("CustomerAddress"));
                        order.setPaymentMethod(rs.getString("PaymentMethod"));
                        order.setVehicleName(rs.getString("ModelName"));
                        order.setVinNumber(rs.getString("VIN"));
                        return order;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateStatus(int orderId, int statusId) {
        String sql = "UPDATE Orders SET OrderStatusID = ? WHERE OrderID = ?";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, statusId);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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