package com.electriccarsales.dao;

import com.electriccarsales.model.Promotion;
import com.electriccarsales.model.AdminPromotion;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromotionDAO extends DBContext {
    public static Map<Integer, List<AdminPromotion>> findAllActiveByModel(Connection connection) throws SQLException {
        if (!SchemaTables.exists(connection, "Promotions") || !SchemaTables.exists(connection, "PromotionCars")) {
            return Map.of();
        }
        String sql = """
            SELECT pc.ModelID, p.PromotionID, p.PromotionName, p.Description, p.DiscountType,
                p.DiscountValue, p.StartDate, p.EndDate, p.Status
            FROM Promotions p JOIN PromotionCars pc ON pc.PromotionID = p.PromotionID
            WHERE p.Status = 'ACTIVE'
                AND p.StartDate <= SYSDATETIME() AND p.EndDate >= SYSDATETIME()
            ORDER BY p.EndDate, p.PromotionID
            """;
        Map<Integer, List<AdminPromotion>> map = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                int modelId = rows.getInt("ModelID");
                AdminPromotion p = new AdminPromotion(rows.getInt("PromotionID"), rows.getString("PromotionName"),
                    rows.getString("Description"), rows.getString("DiscountType"), rows.getBigDecimal("DiscountValue"),
                    rows.getTimestamp("StartDate").toLocalDateTime(), rows.getTimestamp("EndDate").toLocalDateTime(),
                    rows.getString("Status"), List.of(modelId));
                map.computeIfAbsent(modelId, k -> new ArrayList<>()).add(p);
            }
        }
        return map;
    }

    public static List<AdminPromotion> findActiveForModel(Connection connection, int modelId) throws SQLException {
        if (!SchemaTables.exists(connection, "Promotions") || !SchemaTables.exists(connection, "PromotionCars")) {
            return List.of();
        }
        String sql = """
            SELECT p.PromotionID, p.PromotionName, p.Description, p.DiscountType,
                p.DiscountValue, p.StartDate, p.EndDate, p.Status
            FROM Promotions p JOIN PromotionCars pc ON pc.PromotionID = p.PromotionID
            WHERE pc.ModelID = ? AND p.Status = 'ACTIVE'
                AND p.StartDate <= SYSDATETIME() AND p.EndDate >= SYSDATETIME()
            ORDER BY p.EndDate, p.PromotionID
            """;
        List<AdminPromotion> promotions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, modelId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    promotions.add(new AdminPromotion(rows.getInt("PromotionID"), rows.getString("PromotionName"),
                        rows.getString("Description"), rows.getString("DiscountType"), rows.getBigDecimal("DiscountValue"),
                        rows.getTimestamp("StartDate").toLocalDateTime(), rows.getTimestamp("EndDate").toLocalDateTime(),
                        rows.getString("Status"), List.of(modelId)));
                }
            }
        }
        return promotions;
    }

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
