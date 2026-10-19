package com.electriccarsales.dao;

import com.electriccarsales.model.ModelColor;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ModelColorDAO extends DBContext {
    public List<ModelColor> getAll() {
        List<ModelColor> list = new ArrayList<ModelColor>();

        String sql = "SELECT * FROM ModelColors";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                ModelColor mc = new ModelColor();
                mc.setModelColorID(rs.getInt("ModelColorID"));
                mc.setModelID(rs.getInt("ModelID"));
                mc.setColorID(rs.getInt("ColorID"));

                list.add(mc);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    return list;
    }

}
