package com.electriccarsales.DAO;

import com.electriccarsales.model.Car;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static vn.edu.fpt.electriccarsales.config.Database.getConnection;

public class CarDAO {

    @SuppressWarnings("SqlResolve")
    public List<Car> getAllCars() {
        List<Car> list = new ArrayList<>();
        String sql = "SELECT * FROM Cars";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
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
                list.add(c);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getAllCars: " + e.getMessage());
        }
        return list;
    }

    @SuppressWarnings("SqlResolve")
    public boolean addCar(Car c) {
        String sql = "INSERT INTO Cars (CarCode, ModelID, VersionID, ColorID, CarStatusID, Origin, Year, VIN, EngineNumber, LotCode, Price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getCarCode());
            ps.setInt(2, c.getModelID());
            ps.setInt(3, c.getVersionID());
            ps.setInt(4, c.getColorID());
            ps.setInt(5, c.getCarStatusID());
            ps.setString(6, c.getOrigin());
            ps.setInt(7, c.getYear());
            ps.setString(8, c.getVin());
            ps.setString(9, c.getEngineNumber());
            ps.setString(10, c.getLotCode());
            ps.setDouble(11, c.getPrice());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Lỗi tại addCar: " + e.getMessage());
        }
        return false;
    }
}
