package com.electriccarsales.dao;

import com.electriccarsales.model.Brand;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BrandDAO extends DBContext {
    public List<Brand> getAll(){
        List<Brand> list = new ArrayList<>();

        String sql = "SELECT * FROM Brands";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Brand brand = new Brand();
                brand.setBrandID(rs.getInt("BrandID"));
                brand.setBrandName(rs.getString("BrandName"));
                brand.setDescription(rs.getString("Description"));
                brand.setBrandStatusID(rs.getInt("BrandStatusID"));

                list.add(brand);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            throw  new RuntimeException(e);
        }
        return list;
    }


}
