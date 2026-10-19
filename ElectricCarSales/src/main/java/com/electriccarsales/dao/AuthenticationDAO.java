package com.electriccarsales.dao;

import com.electriccarsales.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticationDAO {
    public Account byUsername(Connection connection, String username) throws SQLException {
        return find(connection, "u.Username", username);
    }

    public Account byId(Connection connection, int userId) throws SQLException {
        return find(connection, "u.UserID", userId);
    }

    public boolean changePassword(Connection connection, int userId, String currentHash, String newHash)
        throws SQLException {
        String sql = "UPDATE Users SET PasswordHash=? WHERE UserID=? AND PasswordHash=? AND StatusID IN"
            + " (SELECT StatusID FROM Statuses WHERE StatusName='ACTIVE')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newHash);
            statement.setInt(2, userId);
            statement.setString(3, currentHash);
            return statement.executeUpdate() == 1;
        }
    }

    private Account find(Connection connection, String column, Object value) throws SQLException {
        String sql = "SELECT u.UserID,u.Username,u.PasswordHash,u.RoleID,r.RoleName,s.StatusName "
            + "FROM Users u JOIN Roles r ON r.RoleID=u.RoleID " + "JOIN Statuses s ON s.StatusID=u.StatusID WHERE "
            + column + "=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Account(result.getInt("UserID"), result.getString("Username"),
                    result.getString("PasswordHash"), result.getInt("RoleID"), result.getString("RoleName"),
                    result.getString("StatusName"));
            }
        }
    }
}
