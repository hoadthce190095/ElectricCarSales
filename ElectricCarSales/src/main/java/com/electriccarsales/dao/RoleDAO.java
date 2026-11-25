package com.electriccarsales.dao;

import com.electriccarsales.model.Role;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleDAO extends DBContext {
    public Role findById(int roleId) {
        String sql = "SELECT RoleID, RoleName FROM Roles WHERE RoleID = ?";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Role(rs.getInt("RoleID"), rs.getString("RoleName")) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đọc vai trò.", e);
        }
    }

    public Role findByName(String roleName) {
        String sql = "SELECT RoleID, RoleName FROM Roles WHERE UPPER(RoleName) = UPPER(?)";
        try (java.sql.Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Role(rs.getInt("RoleID"), rs.getString("RoleName")) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đọc vai trò.", e);
        }
    }
}
