package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationStatus;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationStatusDAO extends DBContext {

    public List<ConsultationStatus> findAll() {
        List<ConsultationStatus> list = new ArrayList<>();
        String sql = "SELECT * FROM ConsultationStatuses";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    ConsultationStatus status = new ConsultationStatus();
                    status.setId(rs.getInt("ConsultationStatusID"));
                    status.setStatusName(rs.getString("StatusName"));
                    list.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}