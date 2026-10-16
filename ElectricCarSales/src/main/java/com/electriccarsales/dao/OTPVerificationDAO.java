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
                "AND ExpiredDate>SYSDATETIME() ORDER BY CreatedDate DESC";
        boolean oldAutoCommit;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể bắt đầu xác thực OTP.", e);
        }
        try {
            int otpId;
            try (PreparedStatement ps = conn.prepareStatement(find)) {
                ps.setInt(1, userId); ps.setString(2, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    otpId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE OTPVerifications SET Status='VERIFIED',VerifiedDate=SYSDATETIME() WHERE OTPVerificationID=?")) {
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
}
