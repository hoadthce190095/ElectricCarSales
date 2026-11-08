package com.electriccarsales.dao;

import com.electriccarsales.model.AdminRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdminRoleDAO {

    public List<String> findPermissions(Connection connection, int roleId) throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT PermissionCode FROM RolePermissions WHERE RoleID=?")) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        } catch (SQLException ignored) {
            // Table might not exist yet in basic setup
        }
        return list;
    }

    public List<AdminRole> findAll(Connection connection) throws SQLException {
        List<AdminRole> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT RoleID, RoleName FROM Roles ORDER BY RoleID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AdminRole(rs.getInt("RoleID"), rs.getString("RoleName")));
            }
        }
        return list;
    }

    public String findRoleName(Connection connection, int roleId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT RoleName FROM Roles WHERE RoleID=?")) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public int create(Connection connection, String roleName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Roles(RoleName) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roleName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void addPermissions(Connection connection, int roleId, List<String> permissions) throws SQLException {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO RolePermissions(RoleID, PermissionCode) VALUES(?, ?)")) {
            for (String p : permissions) {
                ps.setInt(1, roleId);
                ps.setString(2, p);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ignored) {
        }
    }

    public void replacePermissions(Connection connection, int roleId, List<String> permissions) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM RolePermissions WHERE RoleID=?")) {
            ps.setInt(1, roleId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
        addPermissions(connection, roleId, permissions);
    }
}
