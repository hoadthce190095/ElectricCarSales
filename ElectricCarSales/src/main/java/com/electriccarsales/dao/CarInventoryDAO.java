package com.electriccarsales.dao;

import com.electriccarsales.model.InventoryItem;
import com.electriccarsales.model.InventoryCarDetail;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CarInventoryDAO extends DBContext {

    // 1. Hàm lấy Tổng quan tồn kho (Không dùng cột Location trong DB)
    @SuppressWarnings("SqlResolve")
    public List<InventoryItem> getInventorySummary() {
        List<InventoryItem> list = new ArrayList<>();
        // Chỉ gom nhóm theo Model và Color
        String sql = "SELECT c.ModelID, m.ModelName, col.ColorName, COUNT(c.CarID) as Quantity " +
            "FROM Cars c " +
            "JOIN CarModels m ON c.ModelID = m.ModelID " +
            "LEFT JOIN Colors col ON c.ColorID = col.ColorID " +
            "GROUP BY c.ModelID, m.ModelName, col.ColorName " +
            "ORDER BY m.ModelName, col.ColorName";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setModelId(rs.getInt("ModelID"));
                item.setModelName(rs.getString("ModelName"));
                item.setColor(rs.getString("ColorName"));
                item.setLocation("Kho tổng"); // Gán cứng vị trí vì DB không có cột này
                item.setQuantity(rs.getInt("Quantity"));
                list.add(item);
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getInventorySummary: " + e.getMessage());
        }
        return list;
    }

    // 2. Hàm lấy Chi tiết xe vật lý (Chỉ query theo ModelID)
    @SuppressWarnings("SqlResolve")
    public List<InventoryCarDetail> getCarDetails(int modelId) {
        List<InventoryCarDetail> list = new ArrayList<>();
        String sql = "SELECT c.VIN, c.EngineNumber, col.ColorName, s.StatusName, c.Price " +
            "FROM Cars c " +
            "LEFT JOIN Colors col ON c.ColorID = col.ColorID " +
            "LEFT JOIN CarStatuses s ON c.CarStatusID = s.CarStatusID " +
            "WHERE c.ModelID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryCarDetail car = new InventoryCarDetail();
                    car.setVin(rs.getString("VIN"));
                    car.setEngineNumber(rs.getString("EngineNumber"));
                    car.setColor(rs.getString("ColorName"));
                    car.setStatus(rs.getString("StatusName"));
                    car.setPrice(rs.getDouble("Price"));
                    list.add(car);
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getCarDetails: " + e.getMessage());
        }
        return list;
    }
}
