package com.electriccarsales.dao;

import com.electriccarsales.model.PromotionCar;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PromotionCarDAO extends DBContext {
    public List<PromotionCar> getAll(){
        List<PromotionCar> list = new ArrayList<>();

        String sql = "SELECT * FROM PromotionCars";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                PromotionCar pc = new PromotionCar();
                pc.setPromotionCarID(rs.getInt("PromotionCarID"));
                pc.setPromotionID(rs.getInt("PromotionID"));
                pc.setModelID(rs.getInt("ModelID"));

                list.add(pc);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

}
