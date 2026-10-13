package com.electriccarsales.dao;

import com.electriccarsales.model.CarVersion;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarVersionDAO extends DBContext {
    public List<CarVersion> getAll() {
        List<CarVersion> list = new ArrayList<>();

        String sql = "SELECT * FROM CarVersions";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                CarVersion carV = new CarVersion();
                carV.setVersionID(rs.getInt("VersionID"));
                carV.setModelID(rs.getInt("ModelID"));
                carV.setVersionName(rs.getString("VersionName"));
                carV.setDescription(rs.getString("Description"));

                list.add(carV);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  list;
    }

}
