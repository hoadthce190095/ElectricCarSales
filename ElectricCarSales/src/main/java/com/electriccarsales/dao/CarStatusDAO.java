package com.electriccarsales.dao;

import com.electriccarsales.model.CarStatus;
import util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CarStatusDAO extends DBContext {

    @SuppressWarnings("SqlResolve")
    public List<CarStatus> getAllCarStatuses() {
        List<CarStatus> list = new ArrayList<>();
        String sql = "SELECT * FROM CarStatuses";
        try (Connection connection = getConnection()) {
            ensureRequiredStatuses(connection);
            try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CarStatus cs = new CarStatus(
                            rs.getInt("CarStatusID"),
                            rs.getString("StatusName"),
                            rs.getString("Description")
                    );
                    list.add(cs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh mục trạng thái xe từ cơ sở dữ liệu.", e);
        }
        return list;
    }

    private static void ensureRequiredStatuses(Connection connection) throws SQLException {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("AVAILABLE", "Xe sẵn sàng trong kho");
        required.put("RESERVED", "Xe đã được giữ chỗ cho đơn hàng");
        required.put("DEPOSITED", "Xe có đơn hàng đã xác nhận đặt cọc");
        required.put("SOLD", "Xe đã bán");
        required.put("DELIVERED", "Xe đã bàn giao cho khách hàng");
        String existsSql = "SELECT 1 FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))=?";
        String insertSql = "INSERT INTO CarStatuses(StatusName,Description) VALUES(?,?)";
        for (Map.Entry<String, String> status : required.entrySet()) {
            boolean exists;
            try (PreparedStatement ps = connection.prepareStatement(existsSql)) {
                ps.setString(1, status.getKey());
                try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
            }
            if (!exists) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setNString(1, status.getKey());
                    ps.setNString(2, status.getValue());
                    ps.executeUpdate();
                }
            }
        }
    }
}
