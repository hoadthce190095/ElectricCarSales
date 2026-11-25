package com.electriccarsales.dao;

import com.electriccarsales.model.ModelColor;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ModelColorDAO extends DBContext {
    public List<ModelColor> getAll() {
        List<ModelColor> list = new ArrayList<>();
        String sql = "SELECT * FROM ModelColors";

        try (java.sql.Connection conn = getConnection()) {
            if (conn == null) return list;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    ModelColor mc = new ModelColor();
                    int id = 0;
                    try {
                        id = rs.getInt("ModelColorsID");
                    } catch (Exception ex) {
                        try {
                            id = rs.getInt("ModelColorID");
                        } catch (Exception ex2) {
                            id = rs.getInt(1);
                        }
                    }
                    mc.setModelColorID(id);
                    mc.setModelID(rs.getInt("ModelID"));
                    mc.setColorID(rs.getInt("ColorID"));

                    list.add(mc);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: ModelColorDAO.getAll() failed: " + e.getMessage());
        }
        return list;
    }

}
