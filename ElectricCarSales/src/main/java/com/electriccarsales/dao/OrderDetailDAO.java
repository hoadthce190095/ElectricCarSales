package com.electriccarsales.dao;

import com.electriccarsales.model.OrderDetail;
import util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderDetailDAO extends DBContext {

    private static final String BASE_SELECT =
            "SELECT od.OrderDetailID, od.OrderID, od.CarID, od.UnitPrice, od.Quantity, " +
            "car.VIN, " +
            "cm.ModelName, cv.VersionName, " +
            "col.ColorName " +
            "FROM OrderDetails od " +
            "LEFT JOIN Cars car ON od.CarID = car.CarID " +
            "LEFT JOIN CarVersions cv ON car.VersionID = cv.VersionID " +
            "LEFT JOIN CarModels cm ON cv.ModelID = cm.ModelID " +
            "LEFT JOIN Colors col ON car.ColorID = col.ColorID ";

    public List<OrderDetail> findAll() {
        List<OrderDetail> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY od.OrderDetailID";
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

    public List<OrderDetail> findByOrderId(int orderId) {
        List<OrderDetail> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE od.OrderID = ? ORDER BY od.OrderDetailID";
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

    public int insert(OrderDetail detail) {
        String sql = "INSERT INTO OrderDetails (OrderID, CarID, UnitPrice, Quantity) VALUES (?, ?, ?, ?)";
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getOrderId());
            ps.setInt(2, detail.getCarId() > 0 ? detail.getCarId() : 1);
            ps.setBigDecimal(3, detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO);
            ps.setInt(4, detail.getQuantity() > 0 ? detail.getQuantity() : 1);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int genId = keys.getInt(1);
                        detail.setId(genId);
                        detail.setOrderDetailId(genId);
                        return genId;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private OrderDetail mapRow(ResultSet rs) throws SQLException {
        OrderDetail d = new OrderDetail();
        int id = rs.getInt("OrderDetailID");
        d.setId(id);
        d.setOrderDetailId(id);
        d.setOrderId(rs.getInt("OrderID"));
        d.setCarId(rs.getInt("CarID"));
        d.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        d.setQuantity(rs.getInt("Quantity"));

        BigDecimal unitPrice = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
        d.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(d.getQuantity())));

        String mName = rs.getString("ModelName");
        String vName = rs.getString("VersionName");
        if (mName != null) {
            d.setVehicleName(mName + (vName != null ? " " + vName : ""));
        } else {
            d.setVehicleName("Xe điện VinFast");
        }
        d.setVehicleColor(rs.getString("ColorName") != null ? rs.getString("ColorName") : "Tiêu chuẩn");
        return d;
    }
}
