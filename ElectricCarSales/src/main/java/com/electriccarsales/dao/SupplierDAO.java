package com.electriccarsales.dao;

import com.electriccarsales.model.Supplier;
import util.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO extends DBContext {

    @SuppressWarnings("SqlResolve")
    public List<Supplier> getAllSuppliers() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM Suppliers";


        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Supplier s = new Supplier(
                        rs.getInt("SupplierID"),
                        rs.getString("SupplierName"),
                        rs.getString("Phone"),
                        rs.getString("Email"),
                        rs.getString("Address"),
                        rs.getString("Description")
                );
                list.add(s);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getAllSuppliers: " + e.getMessage());
        }
        return list;
    }

    @SuppressWarnings("SqlResolve")
    public boolean addSupplier(Supplier s) {
        String sql = "INSERT INTO Suppliers (SupplierName, Phone, Email, Address, Description) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getSupplierName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getAddress());
            ps.setString(5, s.getDescription());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addSupplier: " + e.getMessage());
        }
        return false;
    }
}