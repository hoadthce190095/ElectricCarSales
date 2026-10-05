package com.electriccarsales.DAO;

import com.electriccarsales.model.ComparisonSession;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ComparisonSessionDAO extends DBContext {
    public List<ComparisonSession> getAll(){
        List<ComparisonSession> list = new ArrayList<>();

        String sql = "SELECT * FROM ComparisonSessions";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ComparisonSession ComS = new ComparisonSession();
                ComS.setComparisonSessionID(rs.getInt("ComparisonSessionID"));
                ComS.setUserID(rs.getInt("UserID"));
                ComS.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());

                list.add(ComS);

            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

}
