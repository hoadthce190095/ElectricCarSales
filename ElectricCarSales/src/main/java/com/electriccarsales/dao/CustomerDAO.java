package com.electriccarsales.dao;

import com.electriccarsales.model.Customer;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO extends DBContext {
    public Customer findByUserId(int userId) {
        String sql = "SELECT CustomerID, UserID, CustomerName, Phone, Email, Address FROM Customers WHERE UserID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hồ sơ khách hàng.", e);
        }
    }

    public Avatar findAvatarByUserId(int userId) {
        String sql = "SELECT AvatarData, AvatarContentType FROM Customers WHERE UserID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                byte[] data = rs.getBytes("AvatarData");
                String contentType = rs.getString("AvatarContentType");
                return data == null || data.length == 0 ? null : new Avatar(data, contentType);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải ảnh đại diện.", e);
        }
    }

    public boolean updateAvatar(int userId, byte[] image, String contentType) {
        String sql = "UPDATE Customers SET AvatarData = ?, AvatarContentType = ? WHERE UserID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBytes(1, image);
            ps.setString(2, contentType);
            ps.setInt(3, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể lưu ảnh đại diện.", e);
        }
    }

    public record Avatar(byte[] data, String contentType) { }

    public Customer findById(int customerId) {
        String sql = "SELECT CustomerID, UserID, CustomerName, Phone, Email, Address FROM Customers WHERE CustomerID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn hồ sơ khách hàng.", e);
        }
    }

    public List<Customer> findAll() {
        String sql = "SELECT CustomerID, UserID, CustomerName, Phone, Email, Address FROM Customers ORDER BY CustomerID";
        List<Customer> customers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) customers.add(map(rs));
            return customers;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn danh sách khách hàng.", e);
        }
    }

    public boolean updateOwnedProfile(int userId, String customerName, String phone, String email, String address) {
        String userSql = "UPDATE u SET Phone = ?, Email = ? FROM Users u WHERE u.UserID = ? "
                + "AND NOT EXISTS (SELECT 1 FROM Users x WHERE x.UserID <> u.UserID "
                + "AND (x.Phone = ? OR (? IS NOT NULL AND x.Email = ?)))";
        String sql = "UPDATE c SET CustomerName = ?, Phone = ?, Email = ?, Address = ? "
                + "FROM Customers c WHERE c.UserID = ? "
                + "AND NOT EXISTS (SELECT 1 FROM Customers x WHERE x.CustomerID <> c.CustomerID "
                + "AND (x.Phone = ? OR (? IS NOT NULL AND x.Email = ?))) "
                + "AND NOT EXISTS (SELECT 1 FROM Users u WHERE u.UserID <> c.UserID "
                + "AND (u.Phone = ? OR (? IS NOT NULL AND u.Email = ?)))";
        if (conn == null) throw new IllegalStateException("Không kết nối được cơ sở dữ liệu.");
        boolean oldAutoCommit;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể bắt đầu cập nhật hồ sơ.", e);
        }
        try (PreparedStatement userPs = conn.prepareStatement(userSql)) {
            userPs.setString(1, phone);
            userPs.setString(2, email);
            userPs.setInt(3, userId);
            userPs.setString(4, phone);
            userPs.setString(5, email);
            userPs.setString(6, email);
            if (userPs.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerName);
                ps.setString(2, phone);
                ps.setString(3, email);
                ps.setString(4, address);
                ps.setInt(5, userId);
                ps.setString(6, phone);
                ps.setString(7, email);
                ps.setString(8, email);
                ps.setString(9, phone);
                ps.setString(10, email);
                ps.setString(11, email);
                boolean updated = ps.executeUpdate() == 1;
                if (updated) conn.commit(); else conn.rollback();
                return updated;
            }
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601)
                throw new IllegalArgumentException("Số điện thoại hoặc email đã được sử dụng.", e);
            throw new IllegalStateException("Không thể cập nhật hồ sơ khách hàng.", e);
        } finally {
            try { conn.setAutoCommit(oldAutoCommit); }
            catch (SQLException e) { throw new IllegalStateException("Không thể khôi phục trạng thái kết nối.", e); }
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("CustomerID"), rs.getInt("UserID"), rs.getString("CustomerName"),
                rs.getString("Phone"), rs.getString("Email"), rs.getString("Address"));
    }
}
