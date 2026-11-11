package com.electriccarsales.dao;

import com.electriccarsales.model.VehicleTCO;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VehicleTCODAO extends DBContext {
    public List<VehicleTCO> getAll() {
        List<VehicleTCO> list = new ArrayList<>();
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT CASE WHEN OBJECT_ID(N'dbo.VehicleTCO', N'U') IS NOT NULL THEN 1 ELSE 0 END");
             ResultSet crs = check.executeQuery()) {
            if (!crs.next() || crs.getInt(1) == 0) {
                return list;
            }
        } catch (Exception e) {
            return list;
        }

        String sql = "SELECT * FROM VehicleTCO";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                VehicleTCO vehicle = new VehicleTCO();
                vehicle.setTcoID(rs.getInt("TCOID"));
                vehicle.setModelID(rs.getInt("ModelID"));
                vehicle.setVehiclePrice(rs.getDouble("VehiclePrice"));
                vehicle.setEnergyCost(rs.getDouble("EnergyCost"));
                vehicle.setMaintenanceCost(rs.getDouble("MaintenanceCost"));
                vehicle.setInsuranceCost(rs.getDouble("InsuranceCost"));
                vehicle.setRegistrationCost(rs.getDouble("RegistrationCost"));
                vehicle.setIncentiveAmount(rs.getDouble("IncentiveAmount"));
                vehicle.setTotalCost(rs.getDouble("TotalCost"));
                vehicle.setPeriodMonths(rs.getInt("PeriodMonths"));
                vehicle.setDescription(rs.getString("Description"));
                vehicle.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());
                list.add(vehicle);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
