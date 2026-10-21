package com.electriccarsales.dao;

import com.electriccarsales.model.Color;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ColorDAO extends DBContext {
    public List<Color> getAll(){
        List<Color> list = new ArrayList<>();

        String sql = "SELECT * FROM Colors";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                Color color = new Color();
                color.setColorID(rs.getInt("ColorID"));
                color.setColorName(rs.getString("ColorName"));
                color.setDescription(rs.getString("Description"));

                list.add(color);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }



}
