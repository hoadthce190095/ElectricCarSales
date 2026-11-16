package com.electriccarsales.service;

import com.electriccarsales.dao.SystemLogDAO;
import com.electriccarsales.model.ApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Chỉ tạo tài khoản Admin đầu tiên; không dùng để đổi mật khẩu tài khoản đã có.
 */
public class AdminSetupService {
    public boolean needsAdmin(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users u JOIN Roles r ON r.RoleID=u.RoleID " + "WHERE r.RoleName='ADMIN'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1) == 0;
        }
    }

    public int create(Connection connection, String username, String password, String origin) throws SQLException {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,50}")) {
            throw new ApiException(400, "Tên đăng nhập phải gồm 3–50 chữ, số hoặc ký tự _ . -.");
        }
        PasswordHasher.validate(password);
        String passwordHash = PasswordHasher.hash(password);

        // Kiểm tra và tạo trong cùng giao dịch để chỉ có một Admin đầu tiên.
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setAutoCommit(false);
        try {
            if (!needsAdmin(connection)) {
                throw new ApiException(409, "Đã có Admin. Hãy đăng nhập bằng tài khoản đã tạo.");
            }
            try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM Users WHERE Username=?")) {
                check.setString(1, username);
                try (ResultSet result = check.executeQuery()) {
                    result.next();
                    if (result.getInt(1) > 0) {
                        throw new ApiException(409, "Tên đăng nhập đã được sử dụng.");
                    }
                }
            }
            String sql = "INSERT INTO Users(Username,PasswordHash,RoleID,StatusID) "
                + "SELECT ?,?,r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s "
                + "WHERE r.RoleName='ADMIN' AND s.StatusName='ACTIVE'";
            int userId;
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, username);
                statement.setString(2, passwordHash);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Thiếu danh mục ADMIN/ACTIVE. Chạy script database trước.");
                }
                try (ResultSet result = statement.getGeneratedKeys()) {
                    if (!result.next()) {
                        throw new SQLException("Không đọc được mã Admin.");
                    }
                    userId = result.getInt(1);
                }
            }
            new SystemLogDAO().recordRequired(connection, userId, "ADMIN_BOOTSTRAP", "USER", String.valueOf(userId), origin);
            connection.commit();
            return userId;
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        }
    }
}
