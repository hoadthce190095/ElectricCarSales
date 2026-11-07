package com.electriccarsales.dao;

import com.electriccarsales.model.Contract;
import util.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ContractsDAO extends DBContext {

    @SuppressWarnings("SqlResolve")
    public List<Contract> getAllContracts() {
        List<Contract> list = new ArrayList<>();
        String sql = "SELECT * FROM Contracts";

        // Sử dụng trực tiếp biến 'conn' từ lớp cha DBContext
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Contract c = new Contract(
                        rs.getInt("ContractID"),
                        rs.getInt("OrderID"),
                        rs.getInt("CustomerID"),
                        rs.getInt("EmployeeID"),
                        rs.getString("ContractNumber"),
                        rs.getTimestamp("ContractDate"),
                        rs.getDouble("ContractAmount"),
                        rs.getString("Status"),
                        rs.getString("Note")
                );
                list.add(c);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getAllContracts: " + e.getMessage());
        }
        return list;
    }

    @SuppressWarnings("SqlResolve")
    public boolean addContract(Contract c) {
        String sql = "INSERT INTO Contracts (OrderID, CustomerID, EmployeeID, ContractNumber, ContractAmount, Status, Note) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, c.getOrderID());
            ps.setInt(2, c.getCustomerID());
            ps.setInt(3, c.getEmployeeID());
            ps.setString(4, c.getContractNumber());
            ps.setDouble(5, c.getContractAmount());
            ps.setString(6, c.getStatus());
            ps.setString(7, c.getNote());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addContract: " + e.getMessage());
        }
        return false;
    }
}