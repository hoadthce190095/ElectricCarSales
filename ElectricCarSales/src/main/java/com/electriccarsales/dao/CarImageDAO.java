package com.electriccarsales.dao;

import com.electriccarsales.model.CarImage;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarImageDAO extends DBContext {
    public List<CarImage> getAll(){
        List<CarImage> list = new ArrayList<>();

        String sql = "SELECT * FROM CarImages";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                CarImage c = new CarImage();
                c.setCarImageID(rs.getInt("CarImageID"));
                c.setModelID(rs.getInt("ModelID"));
                c.setImageURL(rs.getString("ImageURL"));
                c.setImageType(rs.getString("ImageType"));
                c.setIsPrimary(rs.getBoolean("IsPrimary"));
                c.setDisplayOrder(rs.getInt("DisplayOrder"));
                c.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());

                list.add(c);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  list;
    }

}
