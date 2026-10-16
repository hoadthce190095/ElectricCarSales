package com.electriccarsales.dao;

import com.electriccarsales.model.CarModel;
import util.DBContext;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CarModelDAO extends DBContext {

    public List<CarModel> search(String keyword, Integer brandId, Integer carLineId,
                                 BigDecimal maxPrice, BigDecimal minRange, Integer colorId,
                                 String sort) {
        List<CarModel> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT m.ModelID, m.CarLineID, m.ModelName, m.Description
                FROM CarModels m
                INNER JOIN CarLines cl ON cl.CarLineID = m.CarLineID
                INNER JOIN Brands b ON b.BrandID = cl.BrandID
                LEFT JOIN Specifications s ON s.ModelID = m.ModelID
                OUTER APPLY (
                    SELECT MIN(v.CatalogPrice) AS MinPrice
                    FROM CarVersions v
                    WHERE v.ModelID = m.ModelID
                ) vehiclePrice
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         m.ModelName LIKE ? ESCAPE '!'
                         OR m.Description LIKE ? ESCAPE '!'
                         OR cl.CarLineName LIKE ? ESCAPE '!'
                         OR b.BrandName LIKE ? ESCAPE '!'
                     )
                    """);
            String searchPattern = "%" + escapeLikePattern(keyword.trim()) + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }
        if (brandId != null) {
            sql.append(" AND b.BrandID = ?");
            parameters.add(brandId);
        }
        if (carLineId != null) {
            sql.append(" AND cl.CarLineID = ?");
            parameters.add(carLineId);
        }
        if (maxPrice != null) {
            sql.append(" AND vehiclePrice.MinPrice <= ?");
            parameters.add(maxPrice);
        }
        if (minRange != null) {
            sql.append(" AND s.RangeKM >= ?");
            parameters.add(minRange);
        }
        if (colorId != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM ModelColors mc
                         WHERE mc.ModelID = m.ModelID AND mc.ColorID = ?
                     )
                    """);
            parameters.add(colorId);
        }

        sql.append(" ORDER BY ").append(getSortExpression(sort));

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CarModel carModel = new CarModel();
                    carModel.setModelID(rs.getInt("ModelID"));
                    carModel.setCarLineID(rs.getInt("CarLineID"));
                    carModel.setModelName(rs.getString("ModelName"));
                    carModel.setDescription(rs.getString("Description"));
                    list.add(carModel);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in CarModelDAO.search(): " + e.getMessage(), e);
        }
        return list;
    }

    private String escapeLikePattern(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
    }

    private String getSortExpression(String sort) {
        if ("price-asc".equals(sort)) {
            return "CASE WHEN vehiclePrice.MinPrice IS NULL THEN 1 ELSE 0 END, vehiclePrice.MinPrice ASC, m.ModelName ASC";
        }
        if ("price-desc".equals(sort)) {
            return "CASE WHEN vehiclePrice.MinPrice IS NULL THEN 1 ELSE 0 END, vehiclePrice.MinPrice DESC, m.ModelName ASC";
        }
        if ("range-desc".equals(sort)) {
            return "CASE WHEN s.RangeKM IS NULL THEN 1 ELSE 0 END, s.RangeKM DESC, m.ModelName ASC";
        }
        return "m.ModelName ASC";
    }

    public List<CarModel> getAll() {
        List<CarModel> list = new ArrayList<>();
        String sql = "SELECT * FROM CarModels";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while(rs.next()){
                CarModel carM = new CarModel();
                carM.setModelID(rs.getInt("ModelID"));
                carM.setCarLineID(rs.getInt("CarLineID"));
                carM.setModelName(rs.getString("ModelName"));
                carM.setDescription(rs.getString("Description"));
                list.add(carM);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public CarModel getById(int modelID) {
        String sql = "SELECT * FROM CarModels WHERE ModelID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelID);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    CarModel carM = new CarModel();
                    carM.setModelID(rs.getInt("ModelID"));
                    carM.setCarLineID(rs.getInt("CarLineID"));
                    carM.setModelName(rs.getString("ModelName"));
                    carM.setDescription(rs.getString("Description"));
                    return carM;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in CarModelDAO.getById(): " + e.getMessage(), e);
        }
        return null;
    }

    public CarModel getByName(String modelName) {
        String sql = "SELECT * FROM CarModels WHERE ModelName = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, modelName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CarModel carM = new CarModel();
                    carM.setModelID(rs.getInt("ModelID"));
                    carM.setCarLineID(rs.getInt("CarLineID"));
                    carM.setModelName(rs.getString("ModelName"));
                    carM.setDescription(rs.getString("Description"));
                    return carM;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in CarModelDAO.getByName(): " + e.getMessage(), e);
        }
        return null;
    }
}