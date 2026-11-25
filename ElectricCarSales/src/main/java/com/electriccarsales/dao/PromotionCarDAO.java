package com.electriccarsales.dao;

import com.electriccarsales.model.PromotionCar;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PromotionCarDAO extends DBContext {
    public List<PromotionCar> getAll() {
        List<PromotionCar> list = new ArrayList<>();
        try (java.sql.Connection conn = getConnection();
             PreparedStatement check = conn.prepareStatement(
                "SELECT CASE WHEN OBJECT_ID(N'dbo.PromotionCars', N'U') IS NOT NULL THEN 1 ELSE 0 END");
             ResultSet crs = check.executeQuery()) {
            if (!crs.next() || crs.getInt(1) == 0) {
                return list;
            }
        } catch (Exception e) {
            return list;
        }

        String sql = "SELECT * FROM PromotionCars";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PromotionCar pc = new PromotionCar();
                pc.setPromotionCarID(rs.getInt("PromotionCarID"));
                pc.setPromotionID(rs.getInt("PromotionID"));
                pc.setModelID(rs.getInt("ModelID"));
                list.add(pc);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
