package com.electriccarsales.dao;

import com.electriccarsales.model.CarImage;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarImageDAO extends DBContext {
    public List<CarImage> getByModelId(int modelID) {
        List<CarImage> list = new ArrayList<>();
        String sql = "SELECT * FROM CarImages WHERE ModelID = ? ORDER BY IsPrimary DESC, DisplayOrder, CarImageID";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CarImage image = new CarImage();
                    image.setCarImageID(rs.getInt("CarImageID"));
                    image.setModelID(rs.getInt("ModelID"));
                    image.setImageURL(rs.getString("ImageURL"));
                    image.setImageType(rs.getString("ImageType"));
                    image.setIsPrimary(rs.getBoolean("IsPrimary"));
                    image.setDisplayOrder(rs.getInt("DisplayOrder"));
                    java.sql.Timestamp createDate = rs.getTimestamp("CreateDate");
                    image.setCreateDate(createDate == null ? null : createDate.toLocalDateTime());
                    list.add(image);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in CarImageDAO.getByModelId(): " + e.getMessage(), e);
        }
        return list;
    }

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
