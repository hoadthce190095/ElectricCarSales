package com.electriccarsales.dao;

import com.electriccarsales.model.Order;
import util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDAO extends DBContext {

    private static final String BASE_SELECT =
            "SELECT o.OrderID, o.CustomerID, o.EmployeeID, o.OrderDate, o.PaymentMethod, o.OrderStatus, " +
            "o.TotalAmount, o.DepositAmount, o.RemainingAmount, o.Note, " +
            "c.FullName AS CustomerName, c.Phone AS CustomerPhone, c.Email AS CustomerEmail, " +
            "e.FullName AS EmployeeName, " +
            "od.CarID, od.UnitPrice, od.Quantity, " +
            "car.VIN, " +
            "cm.ModelName, cv.VersionName, " +
            "col.ColorName " +
            "FROM Orders o " +
            "LEFT JOIN Customers c ON o.CustomerID = c.CustomerID " +
            "LEFT JOIN Employees e ON o.EmployeeID = e.EmployeeID " +
            "LEFT JOIN OrderDetails od ON o.OrderID = od.OrderID " +
            "LEFT JOIN Cars car ON od.CarID = car.CarID " +
            "LEFT JOIN CarVersions cv ON car.VersionID = cv.VersionID " +
            "LEFT JOIN CarModels cm ON cv.ModelID = cm.ModelID " +
            "LEFT JOIN Colors col ON car.ColorID = col.ColorID ";

    private Connection openOrderConnection() throws SQLException {
        Connection connection = new OrderDAOConnection().getConnection();
        if (connection == null) {
            throw new SQLException("Không thể tạo kết nối đến cơ sở dữ liệu.");
        }
        return connection;
    }

    private static class OrderDAOConnection extends DBContext {
        private Connection getConnection() {
            return conn;
        }
    }

    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY o.OrderDate DESC, o.OrderID DESC";
        try (Connection c = openOrderConnection();
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

    public Order findById(int orderId) {
        String sql = BASE_SELECT + "WHERE o.OrderID = ?";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Order> findByCustomerId(int customerId) {
        List<Order> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE o.CustomerID = ? ORDER BY o.OrderDate DESC";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
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

    public int insert(Order order) {
        String sql = "INSERT INTO Orders (CustomerID, EmployeeID, OrderDate, PaymentMethod, OrderStatus, TotalAmount, DepositAmount, RemainingAmount, Note) " +
                     "VALUES (?, ?, SYSDATETIME(), ?, ?, ?, ?, ?, ?)";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getCustomerId() > 0 ? order.getCustomerId() : 1);
            if (order.getEmployeeId() > 0) {
                ps.setInt(2, order.getEmployeeId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            String pm = order.getPaymentMethodName();
            if (pm == null || pm.isBlank()) {
                pm = order.getPaymentMethodId() == 2 ? "INSTALLMENT" : (order.getPaymentMethodId() == 3 ? "BANK_TRANSFER" : "CASH");
            }
            ps.setString(3, pm);

            String statusStr = mapStatusIdToString(order.getOrderStatusId());
            if (order.getStatus() != null && !order.getStatus().isBlank() && !Character.isDigit(order.getStatus().charAt(0))) {
                statusStr = order.getStatus();
            }
            ps.setString(4, statusStr);

            BigDecimal total = order.getOriginalAmount() != null ? order.getOriginalAmount() : BigDecimal.ZERO;
            BigDecimal deposit = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;
            BigDecimal remaining = total.subtract(deposit);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

            ps.setBigDecimal(5, total);
            ps.setBigDecimal(6, deposit);
            ps.setBigDecimal(7, remaining);
            ps.setString(8, order.getNote());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int genId = keys.getInt(1);
                        order.setId(genId);
                        order.setOrderId(genId);
                        return genId;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateStatus(int orderId, int statusId) {
        String statusStr = mapStatusIdToString(statusId);
        String sql = "UPDATE Orders SET OrderStatus = ? WHERE OrderID = ?";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statusStr);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int insertAndReturnId(Order order) {
        return insert(order);
    }

    public int resolveOrAllocateCar(int carId, int modelId, BigDecimal amount) {
        return resolveOrAllocateCar(carId, modelId, null, amount);
    }

    public int resolveOrAllocateCar(int carId, int modelId, String colorName, BigDecimal amount) {
        if (carId > 0) {
            return carId;
        }
        String sqlFind = "SELECT c.CarID FROM Cars c " +
                         "LEFT JOIN CarVersions cv ON c.VersionID = cv.VersionID " +
                         "WHERE (cv.ModelID = ? OR ? = 0) " +
                         "ORDER BY c.CarID";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sqlFind)) {
            ps.setInt(1, modelId);
            ps.setInt(2, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CarID");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public List<Map<String, Object>> getAvailableCarsForSale() {
        List<Map<String, Object>> cars = new ArrayList<>();
        String sql = "SELECT c.CarID, c.Price, c.VIN, c.CarStatus, cm.ModelName, cv.VersionName, col.ColorName " +
                     "FROM Cars c " +
                     "LEFT JOIN CarVersions cv ON c.VersionID = cv.VersionID " +
                     "LEFT JOIN CarModels cm ON cv.ModelID = cm.ModelID " +
                     "LEFT JOIN Colors col ON c.ColorID = col.ColorID " +
                     "ORDER BY c.CarID";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("carId", rs.getInt("CarID"));
                map.put("price", rs.getBigDecimal("Price"));
                map.put("vin", rs.getString("VIN"));
                map.put("carStatus", rs.getString("CarStatus"));
                String mName = rs.getString("ModelName");
                String vName = rs.getString("VersionName");
                map.put("modelName", (mName != null ? mName : "Xe VinFast") + (vName != null ? " " + vName : ""));
                map.put("colorName", rs.getString("ColorName") != null ? rs.getString("ColorName") : "Tiêu chuẩn");
                cars.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cars;
    }

    public List<Map<String, Object>> getCarModelsWithPrices() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT ModelID, ModelName, BasePrice FROM CarModels ORDER BY ModelID";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("modelId", rs.getInt("ModelID"));
                map.put("modelName", rs.getString("ModelName"));
                map.put("price", rs.getBigDecimal("BasePrice"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getEmployees() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT EmployeeID, FullName, Position FROM Employees ORDER BY EmployeeID";
        try (Connection c = openOrderConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("employeeId", rs.getInt("EmployeeID"));
                map.put("employeeName", rs.getString("FullName"));
                map.put("position", rs.getString("Position"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getPaymentMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(Map.of("paymentMethodId", 1, "methodName", "Tiền mặt / Trả thẳng"));
        methods.add(Map.of("paymentMethodId", 2, "methodName", "Mua xe trả góp"));
        methods.add(Map.of("paymentMethodId", 3, "methodName", "Chuyển khoản ngân hàng"));
        return methods;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        int id = rs.getInt("OrderID");
        o.setId(id);
        o.setOrderId(id);
        o.setCustomerId(rs.getInt("CustomerID"));

        int empId = rs.getInt("EmployeeID");
        if (!rs.wasNull()) {
            o.setEmployeeId(empId);
        }

        o.setCreatedAt(rs.getTimestamp("OrderDate"));
        o.setPaymentMethodName(rs.getString("PaymentMethod"));

        String statusStr = rs.getString("OrderStatus");
        int statusId = mapStatusStringToId(statusStr);
        o.setOrderStatusId(statusId);
        o.setStatus(String.valueOf(statusId));
        o.setStatusName(mapStatusToDisplayName(statusStr));

        o.setOriginalAmount(rs.getBigDecimal("TotalAmount"));
        o.setDepositAmount(rs.getBigDecimal("DepositAmount"));
        o.setNote(rs.getString("Note"));

        o.setCustomerName(rs.getString("CustomerName"));
        o.setCustomerPhone(rs.getString("CustomerPhone"));
        o.setCustomerEmail(rs.getString("CustomerEmail"));
        o.setEmployeeName(rs.getString("EmployeeName"));

        int carId = rs.getInt("CarID");
        if (!rs.wasNull()) {
            o.setCarId(carId);
        }
        o.setVinNumber(rs.getString("VIN"));

        String mName = rs.getString("ModelName");
        String vName = rs.getString("VersionName");
        if (mName != null) {
            o.setVehicleName(mName + (vName != null ? " " + vName : ""));
        } else {
            o.setVehicleName("Xe điện VinFast");
        }
        o.setVehicleColor(rs.getString("ColorName") != null ? rs.getString("ColorName") : "Tiêu chuẩn");
        return o;
    }

    private static String mapStatusIdToString(int statusId) {
        switch (statusId) {
            case 2: return "DEPOSITED";
            case 3: return "PROCESSING";
            case 4: return "PAYMENT_PENDING";
            case 5: return "READY_FOR_DELIVERY";
            case 6: return "COMPLETED";
            case 7: return "CANCELLED";
            default: return "PENDING";
        }
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
