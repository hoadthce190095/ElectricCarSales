package com.electriccarsales.dao;

import com.electriccarsales.model.CarModel;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarModelDAO extends DBContext {
    public List<CarModel> getAll(){
        List<CarModel> list = new ArrayList<>();

        String sql = "SELECT * FROM CarModels";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                CarModel carM = new CarModel();
                carM.setModelID(rs.getInt("ModelID"));
                carM.setCarLineID(rs.getInt("CarLineID"));
                carM.setModelName(rs.getString("ModelName"));
                carM.setDescription(rs.getString("Description"));

                list.add(carM);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

public CarModel getlById(int modelID){
        String sql = "SELECT * FROM CarModels WHERE modelID = ?";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, modelID);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                CarModel carM = new CarModel();

                carM.setModelID(rs.getInt("ModelID"));
                carM.setCarLineID(rs.getInt("CarLineID"));
                carM.setModelName(rs.getString("ModelName"));
                carM.setDescription(rs.getString("Description"));
                rs.close();
                ps.close();

                return carM;
            }
        } catch (Exception e) {
            System.out.println("Error in getById: " + e.getMessage());
        }
        return null;
    }
}
