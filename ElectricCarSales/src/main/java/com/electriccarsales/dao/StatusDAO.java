package com.electriccarsales.dao;

import com.electriccarsales.model.Status;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatusDAO extends DBContext {
    public Status findById(int statusId) {
        String sql = "SELECT StatusID, StatusName FROM Statuses WHERE StatusID = ?";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, statusId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Status(rs.getInt("StatusID"), rs.getString("StatusName")) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đọc trạng thái tài khoản.", e);
        }
    }

    public Status findByName(String statusName) {
        String sql = "SELECT StatusID, StatusName FROM Statuses WHERE UPPER(StatusName) = UPPER(?)";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Status(rs.getInt("StatusID"), rs.getString("StatusName")) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đọc trạng thái tài khoản.", e);
        }
    }
}
