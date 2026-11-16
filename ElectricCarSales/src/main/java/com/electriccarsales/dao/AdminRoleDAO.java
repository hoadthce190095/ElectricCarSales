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
    private static final java.util.Map<Integer, List<String>> RUNTIME_PERMISSIONS = new java.util.concurrent.ConcurrentHashMap<>();

    public List<AdminRole> findAll(Connection connection) throws SQLException {
        List<AdminRole> roles = new ArrayList<>();
        try (
            PreparedStatement statement = connection
                .prepareStatement("SELECT RoleID,RoleName FROM Roles ORDER BY RoleID");
            ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                roles.add(new AdminRole(result.getInt("RoleID"), result.getString("RoleName"), List.of()));
            }
        }
        return roles;
    }

    public String findRoleName(Connection connection, int roleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RoleName FROM Roles WHERE RoleID=?")) {
            statement.setInt(1, roleId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("RoleName") : null;
            }
        }
    }

    public List<String> findPermissions(Connection connection, int roleId) throws SQLException {
        if (!SchemaTables.exists(connection, "RolePermissions")) {
            if (RUNTIME_PERMISSIONS.containsKey(roleId)) {
                return RUNTIME_PERMISSIONS.get(roleId);
            }
            String roleName = findRoleName(connection, roleId);
            if (roleName == null) {
                return List.of();
            }
            return switch (roleName.trim().toUpperCase()) {
                case "ADMIN" -> List.of("MANAGE_USERS", "MANAGE_PROMOTIONS", "VIEW_REPORTS", "VIEW_LOGS");
                case "MANAGER", "ACCOUNTANT", "SALES", "OPERATIONS" -> List.of("VIEW_REPORTS");
                default -> List.of();
            };
        }
        List<String> permissions = new ArrayList<>();
        String sql = "SELECT PermissionCode FROM RolePermissions WHERE RoleID=? ORDER BY PermissionCode";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roleId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    permissions.add(result.getString("PermissionCode"));
                }
            }
        }
        return permissions;
    }

    public int create(Connection connection, String roleName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO Roles(RoleName) VALUES(?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, roleName);
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public void replacePermissions(Connection connection, int roleId, List<String> permissions) throws SQLException {
        if (!SchemaTables.exists(connection, "RolePermissions")) {
            RUNTIME_PERMISSIONS.put(roleId, new ArrayList<>(permissions));
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM RolePermissions WHERE RoleID=?")) {
            statement.setInt(1, roleId);
            statement.executeUpdate();
        }
        addPermissions(connection, roleId, permissions);
    }

    public void addPermissions(Connection connection, int roleId, List<String> permissions) throws SQLException {
        if (!SchemaTables.exists(connection, "RolePermissions")) {
            RUNTIME_PERMISSIONS.computeIfAbsent(roleId, k -> new ArrayList<>()).addAll(permissions);
            return;
        }
        try (PreparedStatement statement = connection
            .prepareStatement("INSERT INTO RolePermissions(RoleID,PermissionCode) VALUES(?,?)")) {
            for (String permission : permissions) {
                statement.setInt(1, roleId);
                statement.setString(2, permission);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
