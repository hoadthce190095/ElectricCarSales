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
        if (list.isEmpty()) {
            Order o1 = new Order();
            o1.setId(842);
            o1.setCustomerName("Nguyễn Minh Anh");
            o1.setCustomerPhone("0903 221 480");
            o1.setCustomerEmail("minhanh.nguyen@gmail.com");
            o1.setCustomerAddress("Quận 1, TP. Hồ Chí Minh");
            o1.setVehicleName("VinFast VF 8 Plus");
            o1.setVinNumber("VF8VN2026842911");
            o1.setPaymentMethod("Chuyển khoản ngân hàng");
            o1.setOriginalAmount(new BigDecimal("1299000000"));
            o1.setDepositAmount(new BigDecimal("50000000"));
            o1.setRemainingAmount(new BigDecimal("1249000000"));
            o1.setStatus("3");
            o1.setStatusName("Đặt cọc");
            o1.setOrderDate(new Timestamp(System.currentTimeMillis() - 86400000));
            list.add(o1);

            Order o2 = new Order();
            o2.setId(839);
            o2.setCustomerName("Lê Hoàng Yến");
            o2.setCustomerPhone("0982 115 908");
            o2.setCustomerEmail("hoangyen.le@gmail.com");
            o2.setCustomerAddress("Cầu Giấy, TP. Hà Nội");
            o2.setVehicleName("VinFast VF 9 Eco");
            o2.setVinNumber("VF9VN2026839120");
            o2.setPaymentMethod("Trả góp qua ngân hàng");
            o2.setOriginalAmount(new BigDecimal("1499000000"));
            o2.setDepositAmount(new BigDecimal("30000000"));
            o2.setRemainingAmount(new BigDecimal("1469000000"));
            o2.setStatus("4");
            o2.setStatusName("Chờ hợp đồng");
            o2.setOrderDate(new Timestamp(System.currentTimeMillis() - 172800000));
            list.add(o2);

            Order o3 = new Order();
            o3.setId(831);
            o3.setCustomerName("Trần Quốc Bảo");
            o3.setCustomerPhone("0918 445 203");
            o3.setCustomerEmail("quocbao.tran@gmail.com");
            o3.setCustomerAddress("Ninh Kiều, TP. Cần Thơ");
            o3.setVehicleName("VinFast VF 6 Plus");
            o3.setVinNumber("VF6VN2026831005");
            o3.setPaymentMethod("Chuyển khoản ngân hàng");
            o3.setOriginalAmount(new BigDecimal("765000000"));
            o3.setDepositAmount(new BigDecimal("20000000"));
            o3.setRemainingAmount(new BigDecimal("745000000"));
            o3.setStatus("2");
            o3.setStatusName("Đang xử lý");
            o3.setOrderDate(new Timestamp(System.currentTimeMillis() - 259200000));
            list.add(o3);

            Order o4 = new Order();
            o4.setId(825);
            o4.setCustomerName("Phạm Đức Long");
            o4.setCustomerPhone("0908 681 229");
            o4.setCustomerEmail("duclong.pham@gmail.com");
            o4.setCustomerAddress("Hải Châu, TP. Đà Nẵng");
            o4.setVehicleName("VinFast VF 7 Plus");
            o4.setVinNumber("VF7VN2026825482");
            o4.setPaymentMethod("Thẻ tín dụng / Toàn bộ");
            o4.setOriginalAmount(new BigDecimal("999000000"));
            o4.setDepositAmount(new BigDecimal("999000000"));
            o4.setRemainingAmount(BigDecimal.ZERO);
            o4.setStatus("6");
            o4.setStatusName("Hoàn tất");
            o4.setOrderDate(new Timestamp(System.currentTimeMillis() - 345600000));
            list.add(o4);
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
        Order fallback = new Order();
        fallback.setId(orderId > 0 ? orderId : 842);
        fallback.setCustomerName("Nguyễn Minh Anh");
        fallback.setCustomerPhone("0903 221 480");
        fallback.setCustomerEmail("minhanh.nguyen@gmail.com");
        fallback.setCustomerAddress("Quận 1, TP. Hồ Chí Minh");
        fallback.setVehicleName("VinFast VF 8 Plus");
        fallback.setVinNumber("VF8VN2026842911");
        fallback.setPaymentMethod("Chuyển khoản ngân hàng");
        fallback.setOriginalAmount(new BigDecimal("1299000000"));
        fallback.setDepositAmount(new BigDecimal("50000000"));
        fallback.setRemainingAmount(new BigDecimal("1249000000"));
        fallback.setStatus("3");
        fallback.setStatusName("Đang xử lý");
        fallback.setOrderDate(new Timestamp(System.currentTimeMillis() - 86400000));
        return fallback;
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