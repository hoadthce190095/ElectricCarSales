package com.electriccarsales.dao;

import com.electriccarsales.model.CarStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


import static com.electriccarsales.config.Database.getConnection;

public class CarStatusDAO {

    @SuppressWarnings("SqlResolve")
    public List<CarStatus> getAllCarStatuses() {
        List<CarStatus> list = new ArrayList<>();
        String sql = "SELECT * FROM CarStatuses";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CarStatus cs = new CarStatus(
                        rs.getInt("CarStatusID"),
                        rs.getString("StatusName"),
                        rs.getString("Description")
                );
                list.add(cs);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getAllCarStatuses: " + e.getMessage());
        }
        return list;
    }
}