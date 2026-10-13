package com.electriccarsales.dao;

import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OTPVerificationDAO extends DBContext {
    public boolean verifyRegistrationOtp(int userId, String code) {
        String find = "SELECT TOP 1 OTPVerificationID FROM dbo.OTPVerifications " +
                "WHERE UserID=? AND OTPCode=? AND Purpose='REGISTER' AND Status='PENDING' " +
                "AND ExpiredDate > SYSDATETIME() ORDER BY CreatedDate DESC";
        try {
            conn.setAutoCommit(false);
            int otpId;
            try (PreparedStatement statement = conn.prepareStatement(find)) {
                statement.setInt(1, userId);
                statement.setString(2, code);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    otpId = rs.getInt(1);
                }
            }
            try (PreparedStatement statement = conn.prepareStatement(
                    "UPDATE dbo.OTPVerifications SET Status='VERIFIED', VerifiedDate=SYSDATETIME() WHERE OTPVerificationID=?")) {
                statement.setInt(1, otpId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = conn.prepareStatement(
                    "UPDATE dbo.Users SET StatusID=(SELECT StatusID FROM dbo.Statuses WHERE StatusName='ACTIVE') WHERE UserID=?")) {
                statement.setInt(1, userId);
                if (statement.executeUpdate() != 1)
                    throw new SQLException("Account not found.");
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new IllegalStateException("Unable to verify OTP.", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }
}
