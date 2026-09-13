/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Cars;
import util.DBContext;

/**
 *
 * @author Hoa Hà
 */
public class CarsDAO extends DBContext {

    public List<Cars> getByModelID(int modelID) {
        List<Cars> list = new ArrayList<>();
        String sql = "select * from Cars where modelID = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, modelID);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                Cars car = new Cars();
                
                car.setCarID(rs.getInt("CarID"));
                car.setModelID(rs.getInt("ModelID"));
                car.setVin(rs.getString("VIN"));
                car.setColor(rs.getString("Color"));
                car.setManufacturingYear(rs.getInt("ManufacturingYear"));
                car.setCarStatus(rs.getString("CarStatus"));
                
                list.add(car);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;

    }

    public Cars getByID(int carID) {
        Cars car = null;
        String sql = "  select * from Cars where CarID = ?";
        
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, carID);
            
            ResultSet rs = ps.executeQuery();
            
            if(rs.next()){
               car = new Cars();
                
                car.setCarID(rs.getInt("CarID"));
                car.setModelID(rs.getInt("ModelID"));
                car.setVin(rs.getString("VIN"));
                car.setColor(rs.getString("Color"));
                car.setManufacturingYear(rs.getInt("ManufacturingYear"));
                car.setCarStatus(rs.getString("CarStatus"));
                             
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return car;
         }
}
