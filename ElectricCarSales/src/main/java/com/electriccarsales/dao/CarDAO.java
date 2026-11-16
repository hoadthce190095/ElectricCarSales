package com.electriccarsales.dao;

import com.electriccarsales.model.Car;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CarDAO extends DBContext {
    private static final String DETAIL_SQL = """
        SELECT c.CarID, c.CarCode, c.ModelID, c.VersionID, c.ColorID, c.CarStatusID,
               c.Origin, c.Year, c.VIN, c.EngineNumber, c.LotCode, c.Price,
               m.ModelName, v.VersionName, col.ColorName, cs.StatusName
        FROM Cars c
        JOIN CarModels m ON m.ModelID = c.ModelID
        JOIN CarVersions v ON v.VersionID = c.VersionID
        JOIN Colors col ON col.ColorID = c.ColorID
        JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
        """;

    @SuppressWarnings("SqlResolve")
    public List<Car> getAllCars() {
        List<Car> list = new ArrayList<>();
        String sql = DETAIL_SQL + " ORDER BY c.CarID DESC";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDetail(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải danh sách xe vật lý từ cơ sở dữ liệu.", e);
        }
        return list;
    }

    public List<Car> getAvailableCarsWithDetails() {
        List<Car> list = new ArrayList<>();
        String sql = DETAIL_SQL + " WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN "
                + "(N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN') "
                + "AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=c.CarID) "
                + "ORDER BY m.ModelName, c.CarID";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDetail(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải xe có sẵn từ cơ sở dữ liệu.", e);
        }
        return list;
    }

    @SuppressWarnings("SqlResolve")
    public boolean addCar(Car c) {
        if (c.getVin() == null || c.getVin().trim().length() != 17) {
            throw new IllegalArgumentException("Số khung (VIN) bắt buộc đúng 17 ký tự và duy nhất (RDS BR-6).");
        }
        String sql = "INSERT INTO Cars (CarCode, ModelID, VersionID, ColorID, CarStatusID, Origin, Year, VIN, EngineNumber, LotCode, Price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getCarCode());
            ps.setInt(2, c.getModelID());
            ps.setInt(3, c.getVersionID());
            ps.setInt(4, c.getColorID());
            ps.setInt(5, c.getCarStatusID());
            ps.setNString(6, c.getOrigin());
            ps.setInt(7, c.getYear());
            ps.setString(8, c.getVin().trim().toUpperCase());
            ps.setString(9, c.getEngineNumber().trim().toUpperCase());
            ps.setString(10, c.getLotCode());
            ps.setDouble(11, c.getPrice());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addCar: " + e.getMessage());
            throw new IllegalStateException("Không thể thêm xe vật lý (kiểm tra trùng VIN/Số máy/Mã xe).", e);
        }
    }

    public boolean updateCarStatus(int carId, String statusName) {
        String sql = """
                UPDATE Cars SET CarStatusID = (SELECT TOP 1 CarStatusID FROM CarStatuses WHERE StatusName = ?)
                WHERE CarID = ? AND NOT EXISTS (SELECT 1 FROM OrderDetails WHERE CarID=Cars.CarID)
                  AND CarStatusID NOT IN (SELECT CarStatusID FROM CarStatuses
                                          WHERE UPPER(StatusName) IN ('RESERVED','DEPOSITED','SOLD','DELIVERED'))
                """;
        if (statusName == null || List.of("RESERVED", "DEPOSITED", "SOLD", "DELIVERED")
                .contains(statusName.strip().toUpperCase(java.util.Locale.ROOT))) return false;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setNString(1, statusName);
            ps.setInt(2, carId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật trạng thái xe vật lý.", e);
        }
    }

    private Car mapDetail(ResultSet rs) throws SQLException {
        Car c = new Car(
                rs.getInt("CarID"),
                rs.getString("CarCode"),
                rs.getInt("ModelID"),
                rs.getInt("VersionID"),
                rs.getInt("ColorID"),
                rs.getInt("CarStatusID"),
                rs.getString("Origin"),
                rs.getInt("Year"),
                rs.getString("VIN"),
                rs.getString("EngineNumber"),
                rs.getString("LotCode"),
                rs.getDouble("Price")
        );
        c.setModelName(rs.getString("ModelName"));
        c.setVersionName(rs.getString("VersionName"));
        c.setColorName(rs.getString("ColorName"));
        c.setStatusName(rs.getString("StatusName"));
        return c;
    }
}
