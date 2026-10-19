package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationRequest;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationRequestDAO extends DBContext {

    public List<ConsultationRequest> findAll() {
        List<ConsultationRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM ConsultationRequests";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    ConsultationRequest req = new ConsultationRequest();
                    req.setId(rs.getInt("ConsultationRequestID"));
                    req.setCustomerId(rs.getInt("CustomerID"));
                    req.setStaffId(rs.getInt("EmployeeID"));
                    req.setContent(rs.getString("RequestContent"));
                    req.setStatus(String.valueOf(rs.getInt("ConsultationStatusID")));
                    req.setCreatedAt(rs.getTimestamp("RequestDate"));
                    list.add(req);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(ConsultationRequest request) {
        String sql = "INSERT INTO ConsultationRequests (CustomerID, RequestContent, ConsultationStatusID, RequestDate) VALUES (?, ?, 1, ?)";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, request.getCustomerId());
                ps.setString(2, request.getContent());
                ps.setTimestamp(3, request.getCreatedAt() != null ? request.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}