package com.electriccarsales.dao;

import com.electriccarsales.model.InventoryTransaction;
import util.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class InventoryTransactionDAO extends DBContext {

    @SuppressWarnings("SqlResolve")
    public List<InventoryTransaction> getAllTransactions() {
        List<InventoryTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM InventoryTransactions";

        // Sử dụng trực tiếp biến 'conn' từ lớp cha DBContext
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Integer supplierID = rs.getInt("SupplierID");
                if (rs.wasNull()) {
                    supplierID = null;
                }

                InventoryTransaction it = new InventoryTransaction(
                        rs.getInt("InventoryTransactionID"),
                        rs.getInt("CarID"),
                        rs.getInt("EmployeeID"),
                        supplierID,
                        rs.getString("TransactionType"),
                        rs.getTimestamp("TransactionDate"),
                        rs.getString("Note")
                );
                list.add(it);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getAllTransactions: " + e.getMessage());
        }
        return list;
    }

    @SuppressWarnings("SqlResolve")
    public boolean addTransaction(InventoryTransaction it) {
        String sql = "INSERT INTO InventoryTransactions (CarID, EmployeeID, SupplierID, TransactionType, Note) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, it.getCarID());
            ps.setInt(2, it.getEmployeeID());

            if (it.getSupplierID() != null) {
                ps.setInt(3, it.getSupplierID());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, it.getTransactionType());
            ps.setString(5, it.getNote());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addTransaction: " + e.getMessage());
        }
        return false;
    }
}