/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Brands;
import util.DBContext;

/**
 *
 * @author Hoa Hà
 */
public class BrandDAO extends DBContext{
    public List<Brands> getAll(){
        
        List<Brands> list = new ArrayList<>();
        
        String sql ="  select * from Brands";
        
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()){
                Brands brand = new Brands();
                brand.setBrandID(rs.getInt("BrandID"));
                brand.setBrandName(rs.getString("BrandName"));
                brand.setCountry(rs.getString("Country"));
                
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
