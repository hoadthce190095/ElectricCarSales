package com.electriccarsales.dao;

import com.electriccarsales.model.CarModel;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarModelDAO extends DBContext {

    public List<CarModel> getAll() {
        List<CarModel> list = new ArrayList<>();
        String sql = "SELECT * FROM CarModels";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while(rs.next()){
                CarModel carM = new CarModel();
                carM.setModelID(rs.getInt("ModelID"));
                carM.setCarLineID(rs.getInt("CarLineID"));
                carM.setModelName(rs.getString("ModelName"));
                carM.setDescription(rs.getString("Description"));
                list.add(carM);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public CarModel getById(int modelID) {
        String sql = "SELECT * FROM CarModels WHERE ModelID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelID);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    CarModel carM = new CarModel();
                    carM.setModelID(rs.getInt("ModelID"));
                    carM.setCarLineID(rs.getInt("CarLineID"));
                    carM.setModelName(rs.getString("ModelName"));
                    carM.setDescription(rs.getString("Description"));
                    return carM;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in CarModelDAO.getById(): " + e.getMessage(), e);
        }
        return null;
    }

    public CarModel getByName(String modelName) {
        String sql = "SELECT * FROM CarModels WHERE ModelName = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, modelName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CarModel carM = new CarModel();
                    carM.setModelID(rs.getInt("ModelID"));
                    carM.setCarLineID(rs.getInt("CarLineID"));
                    carM.setModelName(rs.getString("ModelName"));
                    carM.setDescription(rs.getString("Description"));
                    return carM;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in CarModelDAO.getByName(): " + e.getMessage(), e);
        }
        return null;
    }
}