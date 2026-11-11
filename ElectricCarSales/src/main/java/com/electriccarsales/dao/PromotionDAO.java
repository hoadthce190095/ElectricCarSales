package com.electriccarsales.dao;

import com.electriccarsales.model.Promotion;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PromotionDAO extends DBContext {
    public List<Promotion> getAll() {
        List<Promotion> list = new ArrayList<>();
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT CASE WHEN OBJECT_ID(N'dbo.Promotions', N'U') IS NOT NULL THEN 1 ELSE 0 END");
             ResultSet crs = check.executeQuery()) {
            if (!crs.next() || crs.getInt(1) == 0) {
                return list;
            }
        } catch (Exception e) {
            return list;
        }

        String sql = "SELECT * FROM Promotions";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Promotion p = new Promotion();
                p.setPromotionID(rs.getInt("PromotionID"));
                p.setPromotionName(rs.getString("PromotionName"));
                p.setDescription(rs.getString("Description"));
                p.setDiscountType(rs.getString("DiscountType"));
                p.setDiscountValue(rs.getDouble("DiscountValue"));
                p.setStartDate(rs.getTimestamp("StartDate").toLocalDateTime());
                p.setEndDate(rs.getTimestamp("EndDate").toLocalDateTime());
                p.setStatus(rs.getString("Status"));
                list.add(p);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
