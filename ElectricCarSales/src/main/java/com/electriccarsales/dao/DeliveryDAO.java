package com.electriccarsales.dao;

import com.electriccarsales.model.Delivery;
import util.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DeliveryDAO extends DBContext {

    @SuppressWarnings("SqlResolve")
    public List<Delivery> getAllDeliveries() {
        List<Delivery> list = new ArrayList<>();
        String sql = "SELECT * FROM Deliveries";

        // Sử dụng trực tiếp biến 'conn' từ lớp cha DBContext
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Delivery d = new Delivery(
                        rs.getInt("DeliveryID"),
                        rs.getInt("OrderID"),
                        rs.getInt("CustomerID"),
                        rs.getInt("EmployeeID"),
                        rs.getInt("CarID"),
                        rs.getTimestamp("DeliveryDate"),
                        rs.getString("DeliveryAddress"),
                        rs.getString("DeliveryStatus"),
                        rs.getString("Note")
                );
                list.add(d);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getAllDeliveries: " + e.getMessage());
        }
        return list;
    }

    @SuppressWarnings("SqlResolve")
    public boolean addDelivery(Delivery d) {
        String sql = "INSERT INTO Deliveries (OrderID, CustomerID, EmployeeID, CarID, DeliveryAddress, DeliveryStatus, Note) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, d.getOrderID());
            ps.setInt(2, d.getCustomerID());
            ps.setInt(3, d.getEmployeeID());
            ps.setInt(4, d.getCarID());
            ps.setString(5, d.getDeliveryAddress());
            ps.setString(6, d.getDeliveryStatus());
            ps.setString(7, d.getNote());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addDelivery: " + e.getMessage());
        }
        return false;
    }
}