package com.electriccarsales.DAO;

import com.electriccarsales.model.ComparisonItem;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ComparisonItemDAO extends DBContext {
    public List<ComparisonItem> getAll() {
        List<ComparisonItem> list = new ArrayList<>();

        String sql = "SELECT * FROM ComparisonItems";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                ComparisonItem item = new ComparisonItem();
                item.setComparisonItemID(rs.getInt("ComparisonItemID"));
                item.setComparisonSessionID(rs.getInt("ComparisonSessionID"));
                item.setModelID(rs.getInt("ModelID"));

                list.add(item);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

}
