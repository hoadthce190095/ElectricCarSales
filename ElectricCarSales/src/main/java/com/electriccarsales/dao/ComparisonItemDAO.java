package com.electriccarsales.dao;

import com.electriccarsales.model.ComparisonItem;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ComparisonItemDAO extends DBContext {

    public List<ComparisonItem> getAll() {
        List<ComparisonItem> list = new ArrayList<>();
        String sql = "SELECT * FROM ComparisonItems";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ComparisonItem item = new ComparisonItem();
                item.setComparisonItemID(rs.getInt("ComparisonItemID"));
                item.setComparisonSessionID(rs.getInt("ComparisonSessionID"));
                item.setModelID(rs.getInt("ModelID"));
                list.add(item);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching comparison items", e);
        }
        return list;
    }

    public void addItems(int comparisonSessionId, List<Integer> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) return;
        String sql = "INSERT INTO ComparisonItems (ComparisonSessionID, ModelID) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (Integer modelId : modelIds) {
                if (modelId != null && modelId > 0) {
                    ps.setInt(1, comparisonSessionId);
                    ps.setInt(2, modelId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Error adding comparison items for session: " + comparisonSessionId, e);
        }
    }

    public List<ComparisonItem> getBySessionId(int comparisonSessionId) {
        List<ComparisonItem> list = new ArrayList<>();
        String sql = "SELECT * FROM ComparisonItems WHERE ComparisonSessionID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, comparisonSessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComparisonItem item = new ComparisonItem();
                    item.setComparisonItemID(rs.getInt("ComparisonItemID"));
                    item.setComparisonSessionID(rs.getInt("ComparisonSessionID"));
                    item.setModelID(rs.getInt("ModelID"));
                    list.add(item);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting items for session: " + comparisonSessionId, e);
        }
        return list;
    }
}
