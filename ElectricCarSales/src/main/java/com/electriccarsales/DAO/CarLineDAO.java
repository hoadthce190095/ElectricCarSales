package com.electriccarsales.DAO;

import com.electriccarsales.model.CarLine;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarLineDAO extends DBContext {
    public List<CarLine> getAll(){
        List<CarLine> list = new ArrayList<>();

        String sql = "SELECT * FROM CarLines";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CarLine carL = new CarLine();
                carL.setBrandID(rs.getInt("BrandID"));
                carL.setCarLineID(rs.getInt("CarLineID"));
                carL.setCarLineName(rs.getString("CarLineName"));
                carL.setDescription(rs.getString("Description"));

                list.add(carL);
            }
            rs.close();
            ps.close();
        }catch (Exception e){
           throw  new RuntimeException(e);
        }
        return list;
    }

//    public static void main(String[] args) {
//        CarLineDAO carLineDAO = new CarLineDAO();
//        List<CarLine> list = carLineDAO.getAll();
//        for (CarLine carLine : list) {
//
//            System.out.println(carLine);
//        }
//    }
}
