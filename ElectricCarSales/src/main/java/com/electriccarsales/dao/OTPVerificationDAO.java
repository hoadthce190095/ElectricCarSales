package com.electriccarsales.dao;

import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OTPVerificationDAO extends DBContext {
    public boolean verifyRegistrationOtp(int userId, String code) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        String find = "SELECT TOP 1 OTPVerificationID FROM OTPVerifications WITH (UPDLOCK, HOLDLOCK) " +
                "WHERE UserID=? AND OTPCode=? AND Purpose='REGISTER' AND Status='PENDING' " +
                "AND VerifiedDate IS NULL AND ExpiredDate>SYSDATETIME() ORDER BY CreatedDate DESC";
        boolean oldAutoCommit;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể bắt đầu xác thực OTP.", e);
        }
        try {
            try (PreparedStatement ps = conn.prepareStatement("SELECT UserID FROM Users WITH (UPDLOCK, HOLDLOCK) " +
                    "WHERE UserID=? AND RoleID=(SELECT RoleID FROM Roles WHERE RoleName='CUSTOMER') " +
                    "AND StatusID=(SELECT StatusID FROM Statuses WHERE StatusName='INACTIVE')")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                }
            }
            int otpId;
            try (PreparedStatement ps = conn.prepareStatement(find)) {
                ps.setInt(1, userId); ps.setString(2, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    otpId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE OTPVerifications SET Status='VERIFIED',VerifiedDate=SYSDATETIME() " +
                            "WHERE OTPVerificationID=? AND Status='PENDING' AND VerifiedDate IS NULL")) {
                ps.setInt(1, otpId);
                if (ps.executeUpdate() != 1) { conn.rollback(); return false; }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Users SET StatusID=(SELECT StatusID FROM Statuses WHERE StatusName='ACTIVE') " +
                            "WHERE UserID=? AND StatusID=(SELECT StatusID FROM Statuses WHERE StatusName='INACTIVE')")) {
                ps.setInt(1, userId);
                if (ps.executeUpdate() != 1) { conn.rollback(); return false; }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE OTPVerifications SET Status='EXPIRED' WHERE UserID=? AND Purpose='REGISTER' " +
                            "AND OTPVerificationID<>? AND Status='PENDING'")) {
                ps.setInt(1, userId); ps.setInt(2, otpId); ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw new IllegalStateException("Không thể xác thực OTP.", e);
        } finally {
            try { conn.setAutoCommit(oldAutoCommit); }
            catch (SQLException e) { throw new IllegalStateException("Không thể khôi phục trạng thái kết nối.", e); }
        }
    }

    public boolean issueRegistrationOtp(int userId, String code) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        boolean oldAutoCommit;
        try { oldAutoCommit = conn.getAutoCommit(); conn.setAutoCommit(false); }
        catch (SQLException e) { throw new IllegalStateException("Không thể bắt đầu phát hành OTP.", e); }
        try {
            try (PreparedStatement ps = conn.prepareStatement("SELECT UserID FROM Users WITH (UPDLOCK, HOLDLOCK) " +
                    "WHERE UserID=? AND RoleID=(SELECT RoleID FROM Roles WHERE RoleName='CUSTOMER') " +
                    "AND StatusID=(SELECT StatusID FROM Statuses WHERE StatusName='INACTIVE')")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE OTPVerifications SET Status='EXPIRED' " +
                    "WHERE UserID=? AND Purpose='REGISTER' AND Status='PENDING'")) {
                ps.setInt(1, userId); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO OTPVerifications" +
                    "(UserID,OTPCode,Purpose,ExpiredDate,Status) VALUES(?,?,'REGISTER',DATEADD(MINUTE,5,SYSDATETIME()),'PENDING')")) {
                ps.setInt(1, userId); ps.setString(2, code); ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw new IllegalStateException("Không thể phát hành OTP.", e);
        } finally {
            try { conn.setAutoCommit(oldAutoCommit); }
            catch (SQLException e) { throw new IllegalStateException("Không thể khôi phục trạng thái kết nối.", e); }
        }
    }

    public void expireFailedRegistrationOtp(int userId, String code) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        String sql = "UPDATE OTPVerifications SET Status='EXPIRED' WHERE UserID=? AND OTPCode=? " +
                "AND Purpose='REGISTER' AND Status='PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, code); ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật trạng thái OTP.", e);
        }
    }

    public String findPendingRegistrationEmail(int userId) {
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        String sql = "SELECT u.Email FROM Users u JOIN Roles r ON r.RoleID=u.RoleID " +
                "JOIN Statuses s ON s.StatusID=u.StatusID JOIN Customers c ON c.UserID=u.UserID " +
                "WHERE u.UserID=? AND r.RoleName='CUSTOMER' AND s.StatusName='INACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đọc thông tin xác thực.", e);
        }
    }
}
