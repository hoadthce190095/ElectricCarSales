package com.electriccarsales.DAO;

import com.electriccarsales.model.Specification;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SpecificationDAO extends DBContext {
    public List<Specification> getAll() {
        List<Specification> list = new ArrayList<>();

        String sql = "SELECT * FROM Specifications";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                Specification spec = new Specification();

                spec.setSpecificationID(rs.getInt("SpecificationID"));
                spec.setModelID(rs.getInt("ModelID"));
                spec.setDimensions(rs.getString("Dimensions"));
                spec.setBatteryCapacity(rs.getDouble("BatteryCapacity"));
                spec.setWheelType(rs.getString("WheelType"));
                spec.setEnergyConsumption(rs.getDouble("EnergyConsumption"));
                spec.setRangeKM(rs.getDouble("RangeKM"));
                spec.setMaxPower(rs.getDouble("MaxPower"));
                spec.setMaxSpeed(rs.getDouble("MaxSpeed"));
                spec.setSeatingCapacity(rs.getInt("SeatingCapacity"));
                spec.setChargingTime(rs.getDouble("ChargingTime"));
                spec.setAirbagCount(rs.getInt("AirbagCount"));
                spec.setDescription(rs.getString("Description"));

                list.add(spec);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

//    public static void main(String[] args) {
//        SpecificationDAO dao = new SpecificationDAO();
//        List<Specification> list = dao.getAll();
//        for(Specification spec : list){
//            System.out.println(spec);
//        }
//    }
}
