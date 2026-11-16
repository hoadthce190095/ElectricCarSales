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
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
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
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    public List<ComparisonSession> getByUserId(int userId) {
        List<ComparisonSession> list = new ArrayList<>();
        String sql = "SELECT * FROM ComparisonSessions WHERE UserID = ? ORDER BY CreateDate DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
        String deleteItemsSql = "DELETE FROM ComparisonItems WHERE ComparisonSessionID = ?";
        String deleteSessionSql = "DELETE FROM ComparisonSessions WHERE ComparisonSessionID = ? AND UserID = ?";
        try {
            Connection connection = getConnection();
            try (PreparedStatement psItems = connection.prepareStatement(deleteItemsSql)) {
                psItems.setInt(1, sessionId);
                psItems.executeUpdate();
            }
            try (PreparedStatement psSession = connection.prepareStatement(deleteSessionSql)) {
                psSession.setInt(1, sessionId);
                psSession.setInt(2, userId);
                return psSession.executeUpdate() > 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting comparison session: " + sessionId, e);
        }
    }
}
