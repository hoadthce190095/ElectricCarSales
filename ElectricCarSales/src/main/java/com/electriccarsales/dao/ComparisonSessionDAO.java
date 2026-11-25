package com.electriccarsales.dao;

import com.electriccarsales.model.ComparisonSession;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ComparisonSessionDAO extends DBContext {

    public List<ComparisonSession> getAll() {
        List<ComparisonSession> list = new ArrayList<>();
        String sql = "SELECT * FROM ComparisonSessions ORDER BY CreateDate DESC";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ComparisonSession session = new ComparisonSession();
                session.setComparisonSessionID(rs.getInt("ComparisonSessionID"));
                session.setUserID(rs.getInt("UserID"));
                session.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());
                list.add(session);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching comparison sessions", e);
        }
        return list;
    }

    public int createSession(int userId) {
        String sql = "INSERT INTO ComparisonSessions (UserID, CreateDate) VALUES (?, SYSDATETIME())";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating comparison session", e);
        }
        return 0;
    }

    public int saveModels(int userId, List<Integer> modelIds) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement session = connection.prepareStatement(
                    "INSERT INTO ComparisonSessions (UserID,CreateDate) VALUES (?,SYSDATETIME())", Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement item = connection.prepareStatement(
                    "INSERT INTO ComparisonItems (ComparisonSessionID,ModelID) VALUES (?,?)")) {
                session.setInt(1, userId);
                session.executeUpdate();
                try (ResultSet keys = session.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Thiếu mã phiên so sánh.");
                    int id = keys.getInt(1);
                    for (int modelId : modelIds) {
                        item.setInt(1, id);
                        item.setInt(2, modelId);
                        item.addBatch();
                    }
                    item.executeBatch();
                    connection.commit();
                    return id;
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu lịch sử.", ex);
        }
    }

    public List<ComparisonSession> getByUserId(int userId) {
        List<ComparisonSession> list = new ArrayList<>();
        String sql = "SELECT * FROM ComparisonSessions WHERE UserID = ? ORDER BY CreateDate DESC";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComparisonSession session = new ComparisonSession();
                    session.setComparisonSessionID(rs.getInt("ComparisonSessionID"));
                    session.setUserID(rs.getInt("UserID"));
                    session.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());
                    list.add(session);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting comparison sessions for user: " + userId, e);
        }
        return list;
    }

    public boolean deleteSession(int sessionId, int userId) {
        String deleteItemsSql = "DELETE FROM ComparisonItems WHERE ComparisonSessionID = ? AND EXISTS (SELECT 1 FROM ComparisonSessions WHERE ComparisonSessionID=? AND UserID=?)";
        String deleteSessionSql = "DELETE FROM ComparisonSessions WHERE ComparisonSessionID = ? AND UserID = ?";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
            try (PreparedStatement psItems = connection.prepareStatement(deleteItemsSql)) {
                psItems.setInt(1, sessionId);
                psItems.setInt(2, sessionId);
                psItems.setInt(3, userId);
                psItems.executeUpdate();
            }
            try (PreparedStatement psSession = connection.prepareStatement(deleteSessionSql)) {
                psSession.setInt(1, sessionId);
                psSession.setInt(2, userId);
                boolean deleted = psSession.executeUpdate() > 0;
                connection.commit();
                return deleted;
            }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting comparison session: " + sessionId, e);
        }
    }
}
