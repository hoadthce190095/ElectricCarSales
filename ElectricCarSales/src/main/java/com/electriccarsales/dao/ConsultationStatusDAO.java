package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationStatus;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConsultationStatusDAO {
    public List<ConsultationStatus> findAll() {
        String sql = "SELECT ConsultationStatusID, StatusName FROM ConsultationStatuses ORDER BY ConsultationStatusID";
        try (Connection connection = DBContext.openConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<ConsultationStatus> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new ConsultationStatus(rs.getInt("ConsultationStatusID"), rs.getString("StatusName")));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách trạng thái tư vấn.", e);
        }
    }
}