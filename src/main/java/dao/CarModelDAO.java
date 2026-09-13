/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.CarModels;
import util.DBContext;

/**
 *
 * @author Hoa Hà
 */
public class CarModelDAO extends DBContext {

    public List<CarModels> getAll() {

        List<CarModels> list = new ArrayList<>();

        String sql = "SELECT * FROM CarModels";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CarModels carmodel = new CarModels();

                carmodel.setModelID(rs.getInt("ModelID"));
                carmodel.setBrandID(rs.getInt("BrandID"));
                carmodel.setModelName(rs.getString("ModelName"));
                carmodel.setBatteryCapacity(rs.getDouble("BatteryCapacity"));
                carmodel.setRangeKM(rs.getInt("RangeKM"));
                carmodel.setPrice(rs.getDouble("Price"));
                carmodel.setDescription(rs.getString("Description"));

                list.add(carmodel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public CarModels getByID(int modelID) {
        String sql = "select * from CarModels where ModelID = ?";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, modelID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CarModels car = new CarModels();

                car.setModelID(rs.getInt("ModelID"));
                car.setBrandID(rs.getInt("BrandID"));
                car.setModelName(rs.getString("ModelName"));
                car.setBatteryCapacity(rs.getDouble("BatteryCapacity"));
                car.setRangeKM(rs.getInt("RangeKM"));
                car.setPrice(rs.getDouble("Price"));
                car.setDescription(rs.getString("Description"));

                return car;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
