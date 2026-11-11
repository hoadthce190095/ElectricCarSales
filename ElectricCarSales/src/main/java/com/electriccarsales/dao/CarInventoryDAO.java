package com.electriccarsales.dao;

import com.electriccarsales.model.InventoryItem;
import com.electriccarsales.model.InventoryCarDetail;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CarInventoryDAO {

    // 1. Hàm lấy Tổng quan tồn kho (Không dùng cột Location trong DB)
    @SuppressWarnings("SqlResolve")
    public List<InventoryItem> getInventorySummary() throws SQLException {
        List<InventoryItem> list = new ArrayList<>();
        // Tồn kho vật lý được nhóm theo mẫu, màu và trạng thái; đơn hàng cũ cũng được hiểu là đã giữ xe.
        String sql = "SELECT c.ModelID, m.ModelName, col.ColorName, effective.StatusName, COUNT(c.CarID) as Quantity " +
            "FROM Cars c " +
            "JOIN CarModels m ON c.ModelID = m.ModelID " +
            "LEFT JOIN Colors col ON c.ColorID = col.ColorID " +
            "JOIN CarStatuses s ON s.CarStatusID=c.CarStatusID " +
            "CROSS APPLY (VALUES (CASE WHEN UPPER(LTRIM(RTRIM(s.StatusName))) IN " +
            "(N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN') " +
            "AND EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=c.CarID) THEN N'RESERVED' " +
            "ELSE s.StatusName END)) effective(StatusName) " +
            "GROUP BY c.ModelID, m.ModelName, col.ColorName, effective.StatusName " +
            "ORDER BY m.ModelName, col.ColorName";

        try (Connection connection = DBContext.openConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setModelId(rs.getInt("ModelID"));
                item.setModelName(rs.getString("ModelName"));
                item.setColor(rs.getString("ColorName"));
                item.setLocation("Kho tổng"); // Gán cứng vị trí vì DB không có cột này
                item.setQuantity(rs.getInt("Quantity"));
                item.setStatusName(rs.getString("StatusName"));
                list.add(item);
            }
        }
        return list;
    }

    // 2. Hàm lấy Chi tiết xe vật lý (Chỉ query theo ModelID)
    @SuppressWarnings("SqlResolve")
    public List<InventoryCarDetail> getCarDetails(int modelId) throws SQLException {
        List<InventoryCarDetail> list = new ArrayList<>();
        String sql = "SELECT c.VIN, c.EngineNumber, col.ColorName, s.StatusName, c.Price " +
            "FROM Cars c " +
            "LEFT JOIN Colors col ON c.ColorID = col.ColorID " +
            "LEFT JOIN CarStatuses s ON c.CarStatusID = s.CarStatusID " +
            "WHERE c.ModelID = ?";

        try (Connection connection = DBContext.openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
        }
        return list;
    }
}
