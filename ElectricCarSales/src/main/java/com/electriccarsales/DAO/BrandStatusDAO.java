package com.electriccarsales.DAO;

import com.electriccarsales.model.BrandStatus;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BrandStatusDAO extends DBContext {
    public List<BrandStatus> getAll(){
        List<BrandStatus> list = new ArrayList<>();

        String sql = "SELECT * FROM BrandStatuses";

        try{
        PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                BrandStatus status = new BrandStatus();
                status.setBrandStatusID(rs.getInt("BrandStatusID"));
                status.setStatusName(rs.getString("StatusName"));

                list.add(status);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public BrandStatus getById(int brandStatusID){
        String sql  = "SELECT * FROM BrandStatuses WHERE brandStatusID = ?";

        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, brandStatusID);
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                BrandStatus status = new BrandStatus();
                status.setBrandStatusID(rs.getInt("BrandStatusID"));
                status.setStatusName(rs.getString("StatusName"));

                return status;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

//    public static void main(String[] args) {
//        BrandStatusDAO dao = new BrandStatusDAO();
//        BrandStatus status = dao.getById(1);
//
//            System.out.println(status);
//
//    }
}
