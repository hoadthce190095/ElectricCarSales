package com.electriccarsales.dao;

import com.electriccarsales.model.User;
import util.DBContext;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class UserDAO extends DBContext {
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();

    public String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("Mật khẩu không được để trống.");
        byte[] salt = new byte[16];
        PASSWORD_RANDOM.nextBytes(salt);
        byte[] hash = derivePassword(password.toCharArray(), salt, PASSWORD_ITERATIONS);
        return "PBKDF2$" + PASSWORD_ITERATIONS + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(hash);
    }

    private boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null) return false;
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !"PBKDF2".equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derivePassword(password.toCharArray(), salt, iterations);
            if (actual.length != expected.length) return false;
            int diff = 0;
            for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
            return diff == 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private byte[] derivePassword(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, PASSWORD_KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể mã hóa mật khẩu.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    public User findByUsername(String username) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        String sql = "SELECT u.UserID,u.Username,u.PasswordHash,u.Email,u.Phone,u.RoleID,u.StatusID,u.CreatedDate,r.RoleName,s.StatusName " +
                "FROM Users u JOIN Roles r ON r.RoleID=u.RoleID JOIN Statuses s ON s.StatusID=u.StatusID " +
                "WHERE LOWER(u.Username)=LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn tài khoản.", e);
        }
        return null;
    }

    public String getRoleName(int roleId) {
        return getName("SELECT RoleName FROM Roles WHERE RoleID=?", roleId);
    }

    public String getStatusName(int statusId) {
        return getName("SELECT StatusName FROM Statuses WHERE StatusID=?", statusId);
    }

    private String getName(String sql, int id) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đọc thông tin tài khoản.", e);
        }
    }

    public boolean authenticate(String rawPassword, User user) {
        return user != null && verifyPassword(rawPassword, user.getPasswordHash());
    }

    public boolean exists(String username, String phone, String email) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        String sql = "SELECT 1 FROM Users WHERE LOWER(Username)=LOWER(?) OR Phone=? OR LOWER(Email)=LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, phone);
            ps.setString(3, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra thông tin đăng ký.", e);
        }
    }

    public int registerCustomer(String username, String passwordHash, String email, String phone,
                                String fullName, String address, String otp) {
        String userSql = "INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID) " +
                "OUTPUT INSERTED.UserID VALUES(?,?,?,?, " +
                "(SELECT RoleID FROM Roles WHERE RoleName='CUSTOMER'), " +
                "(SELECT StatusID FROM Statuses WHERE StatusName='INACTIVE'))";
        String customerSql = "INSERT INTO Customers(UserID,CustomerName,Phone,Email,Address) VALUES(?,?,?,?,?)";
        String otpSql = "INSERT INTO OTPVerifications(UserID,OTPCode,Purpose,ExpiredDate,Status) " +
                "VALUES(?,?,'REGISTER',DATEADD(MINUTE,5,SYSDATETIME()),'PENDING')";
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        try {
            conn.setAutoCommit(false);
            int userId;
            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.setString(3, email);
                ps.setString(4, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không tạo được tài khoản.");
                    userId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(customerSql)) {
                ps.setInt(1, userId); ps.setString(2, fullName); ps.setString(3, phone);
                ps.setString(4, email); ps.setString(5, address);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(otpSql)) {
                ps.setInt(1, userId); ps.setString(2, otp); ps.executeUpdate();
            }
            conn.commit();
            return userId;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601)
                throw new IllegalArgumentException("Tên đăng nhập, số điện thoại hoặc email đã được sử dụng.");
            throw new IllegalStateException("Không thể tạo tài khoản", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserID(rs.getInt("UserID")); u.setUsername(rs.getString("Username"));
        u.setPasswordHash(rs.getString("PasswordHash")); u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone")); u.setRoleID(rs.getInt("RoleID"));
        u.setStatusID(rs.getInt("StatusID"));
        Timestamp created = rs.getTimestamp("CreatedDate");
        if (created != null) u.setCreatedDate(created.toLocalDateTime());
        return u;
    }
}
