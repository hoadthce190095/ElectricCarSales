package com.electriccarsales.dao.consultation;

import com.electriccarsales.model.consultation.ConsultationRequest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationRequestDAO {

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB", "sa", "your_password");
        } catch (Exception e) {
            return null;
        }
    }

    public List<ConsultationRequest> findAll() {
        List<ConsultationRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM ConsultationRequests"; // Tên bảng theo DB
        try (Connection conn = getConnection();
             PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    ConsultationRequest req = new ConsultationRequest();
                    req.setId(rs.getInt("ID"));
                    req.setCustomerId(rs.getInt("CustomerID"));
                    req.setStaffId(rs.getInt("Staffর্দID"));
                    req.setContent(rs.getString("Content"));
                    req.setStatus(rs.getString("Status"));
                    req.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    list.add(req);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(ConsultationRequest request) {
        String sql = "INSERT INTO ConsultationRequests (CustomerID, Content, Status, CreatedAt) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, request.getCustomerId());
                ps.setString(2, request.getContent());
                ps.setString(3, request.getStatus());
                ps.setTimestamp(4, request.getCreatedAt());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}