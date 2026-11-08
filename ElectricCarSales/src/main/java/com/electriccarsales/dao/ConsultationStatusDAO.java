package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationStatus;
import util.DBContext;
import java.sql.*;
import java.util.*;

public class ConsultationStatusDAO {
    public List<ConsultationStatus> findAll() {
        List<ConsultationStatus> result = new ArrayList<>();
        try (Connection c=DBContext.openConnection();
             PreparedStatement ps=c.prepareStatement("SELECT ConsultationStatusID,StatusName FROM ConsultationStatuses ORDER BY ConsultationStatusID");
             ResultSet rs=ps.executeQuery()) {
            while(rs.next()) result.add(new ConsultationStatus(rs.getInt(1),rs.getString(2)));
        } catch(SQLException e) { throw new IllegalStateException("Cannot read consultation statuses",e); }
        return result;
    }
}
