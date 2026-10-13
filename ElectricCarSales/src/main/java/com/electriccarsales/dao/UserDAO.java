package com.electriccarsales.dao;

import com.electriccarsales.model.User;
import util.DBContext;

import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO extends DBContext {
    public String hashMD5 (String password) {
        String hash = "";
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("md5");
            byte[] bytes = messageDigest.digest(password.getBytes());
            for (byte b : bytes) {
                hash += String.format("%02x", b);
            }
        } catch (Exception e) {
        }
        return hash;
    }

    public User login(String username, String password) {
        String sql = "SELECT Users.UserID, Users.Username, Users.Email, Users.Phone, Users.RoleID, Statuses.StatusName, Users.StatusID\n" +
                "FROM     Users INNER JOIN\n" +
                "                  Statuses ON Users.StatusID = Statuses.StatusID\n" +
                "WHERE Users.Username=? AND Users.PasswordHash=?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, username == null ? "" : username.trim());
            statement.setString(2, hashMD5(password == null ? "" : password));
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next())
                    return null;

                if (!"ACTIVE".equalsIgnoreCase(rs.getString("StatusName")))
                    return null;
                User user = new User();
                user.setUserID(rs.getInt("UserID"));
                user.setUsername(rs.getString("Username"));
                user.setEmail(rs.getString("Email"));
                user.setPhone(rs.getString("Phone"));
                user.setRoleID(rs.getInt("RoleID"));
                user.setStatusID(rs.getInt("StatusID"));
                return user;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Login query error.", e);
        }
    }

    public boolean isUsernameExist(String username) {
        return exists("SELECT 1 FROM dbo.Users WHERE Username=?", username);
    }

    public boolean isEmailExist(String email) {
        return exists("SELECT 1 FROM dbo.Users WHERE Email=?", email);
    }

    public boolean isPhoneExist(String phone) {
        return exists("SELECT 1 FROM dbo.Users WHERE Phone=?", phone);
    }

    private boolean exists(String sql, String value) {
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to verify account data.", e);
        }
    }

    public int registerCustomer(String username, String password, String email, String phone,
                                String customerName, String address, String otp) {
        String insertUser = "INSERT INTO dbo.Users(Username,PasswordHash,Email,Phone,RoleID,StatusID) " +
                "OUTPUT INSERTED.UserID VALUES(?,?,?,?," +
                "(SELECT RoleID FROM dbo.Roles WHERE RoleName='CUSTOMER')," +
                "(SELECT StatusID FROM dbo.Statuses WHERE StatusName='INACTIVE'))";
        String insertCustomer = "INSERT INTO dbo.Customers(UserID,CustomerName,Phone,Email,Address) VALUES(?,?,?,?,?)";
        String insertOtp = "INSERT INTO dbo.OTPVerifications(UserID,OTPCode,Purpose,ExpiredDate,Status) " +
                "VALUES(?,?, 'REGISTER', DATEADD(MINUTE,5,SYSDATETIME()), 'PENDING')";
        try {
            conn.setAutoCommit(false);
            int userId;
            try (PreparedStatement statement = conn.prepareStatement(insertUser)) {
                statement.setString(1, username);
                statement.setString(2, hashMD5(password));
                statement.setString(3, email);
                statement.setString(4, phone);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next())
                        throw new SQLException("Unable to create an account.");
                    userId = rs.getInt(1);
                }
            }
            try (PreparedStatement statement = conn.prepareStatement(insertCustomer)) {
                statement.setInt(1, userId);
                statement.setString(2, customerName);
                statement.setString(3, phone);
                statement.setString(4, email);
                statement.setString(5, address);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = conn.prepareStatement(insertOtp)) {
                statement.setInt(1, userId);
                statement.setString(2, otp);
                statement.executeUpdate();
            }
            conn.commit();
            return userId;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new IllegalStateException("Unable to register. The username, email, or phone number may already exist.", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }
}
