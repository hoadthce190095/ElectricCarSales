package com.electriccarsales.dao;

import com.electriccarsales.model.Car;
import com.electriccarsales.model.InventoryCarDetail;
import com.electriccarsales.model.InventoryItem;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarInventoryDAO {

    /**
     * Retrieves high-level inventory summary grouped by Model, Color, and effective status.
     */
    @SuppressWarnings("SqlResolve")
    public List<InventoryItem> getInventorySummary() throws SQLException {
        List<InventoryItem> list = new ArrayList<>();
        String sql = """
            SELECT c.ModelID, m.ModelName, col.ColorName,
                   COALESCE(cs.StatusName, 'AVAILABLE') AS StatusName,
                   COUNT(c.CarID) AS Quantity
            FROM Cars c
            LEFT JOIN CarModels m ON c.ModelID = m.ModelID
            LEFT JOIN Colors col ON c.ColorID = col.ColorID
            LEFT JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
            GROUP BY c.ModelID, m.ModelName, col.ColorName, COALESCE(cs.StatusName, 'AVAILABLE')
            ORDER BY m.ModelName, col.ColorName
            """;

        try (Connection connection = DBContext.openConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setModelId(rs.getInt("ModelID"));
                item.setModelName(rs.getString("ModelName"));
                item.setColor(rs.getString("ColorName"));
                item.setLocation("Kho tổng");
                item.setQuantity(rs.getInt("Quantity"));
                item.setStatusName(rs.getString("StatusName"));
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Retrieves all physical vehicles in inventory with complete attributes:
     * CarCode, ModelName, VersionName, ColorName, VIN, EngineNumber, Price, Year, Origin, LotCode,
     * StatusName, ImportDate, SupplierName.
     * Uses LEFT JOINs so vehicles are never omitted even without import history.
     */
    @SuppressWarnings("SqlResolve")
    public List<Car> getInventoryCars(String keyword, String status) throws SQLException {
        List<Car> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT c.CarID, c.CarCode, c.ModelID, c.VersionID, c.ColorID, c.CarStatusID,
                   c.Origin, c.Year, c.VIN, c.EngineNumber, c.LotCode, c.Price,
                   m.ModelName, v.VersionName, col.ColorName,
                   COALESCE(cs.StatusName, 'AVAILABLE') AS StatusName,
                   it.TransactionDate AS ImportDate,
                   sup.SupplierName
            FROM Cars c
            LEFT JOIN CarModels m ON m.ModelID = c.ModelID
            LEFT JOIN CarVersions v ON v.VersionID = c.VersionID
            LEFT JOIN Colors col ON col.ColorID = c.ColorID
            LEFT JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
            LEFT JOIN (
                SELECT CarID, MAX(TransactionDate) AS MaxImportDate
                FROM InventoryTransactions
                WHERE TransactionType = 'IMPORT'
                GROUP BY CarID
            ) imp ON imp.CarID = c.CarID
            LEFT JOIN InventoryTransactions it ON it.CarID = imp.CarID AND it.TransactionDate = imp.MaxImportDate AND it.TransactionType = 'IMPORT'
            LEFT JOIN Suppliers sup ON sup.SupplierID = it.SupplierID
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            sql.append(" AND (c.VIN LIKE ? OR c.EngineNumber LIKE ? OR c.CarCode LIKE ? OR m.ModelName LIKE ? OR v.VersionName LIKE ? OR col.ColorName LIKE ?)");
            for (int i = 0; i < 6; i++) {
                params.add(kw);
            }
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND UPPER(LTRIM(RTRIM(COALESCE(cs.StatusName, 'AVAILABLE')))) = ?");
            params.add(status.trim().toUpperCase(Locale.ROOT));
        }

        sql.append(" ORDER BY c.CarID DESC");

        try (Connection connection = DBContext.openConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Car c = new Car(
                            rs.getInt("CarID"),
                            rs.getString("CarCode"),
                            rs.getInt("ModelID"),
                            rs.getInt("VersionID"),
                            rs.getInt("ColorID"),
                            rs.getInt("CarStatusID"),
                            rs.getString("Origin"),
                            rs.getInt("Year"),
                            rs.getString("VIN"),
                            rs.getString("EngineNumber"),
                            rs.getString("LotCode"),
                            rs.getDouble("Price")
                    );
                    c.setModelName(rs.getString("ModelName"));
                    c.setVersionName(rs.getString("VersionName"));
                    c.setColorName(rs.getString("ColorName"));
                    c.setStatusName(rs.getString("StatusName"));
                    c.setImportDate(rs.getTimestamp("ImportDate"));
                    c.setSupplierName(rs.getString("SupplierName"));
                    list.add(c);
                }
            }
        }
        return list;
    }

    /**
     * Calculates live statistics from actual physical car records and their statuses.
     */
    @SuppressWarnings("SqlResolve")
    public Map<String, Integer> getInventoryStats() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        int total = 0;
        int available = 0;
        int reserved = 0;
        int deposited = 0;
        int sold = 0;
        int delivered = 0;

        String sql = """
            SELECT UPPER(LTRIM(RTRIM(COALESCE(cs.StatusName, 'AVAILABLE')))) AS StatusName,
                   COUNT(c.CarID) AS Cnt
            FROM Cars c
            LEFT JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
            GROUP BY UPPER(LTRIM(RTRIM(COALESCE(cs.StatusName, 'AVAILABLE'))))
            """;

        try (Connection connection = DBContext.openConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String st = rs.getString("StatusName");
                int cnt = rs.getInt("Cnt");
                total += cnt;
                if ("AVAILABLE".equals(st) || "IN_STOCK".equals(st) || "FOR_SALE".equals(st) || "READY_FOR_SALE".equals(st) || "CÓ SẴN".equals(st)) {
                    available += cnt;
                } else if ("RESERVED".equals(st)) {
                    reserved += cnt;
                } else if ("DEPOSITED".equals(st)) {
                    deposited += cnt;
                } else if ("SOLD".equals(st)) {
                    sold += cnt;
                } else if ("DELIVERED".equals(st) || "EXPORTED".equals(st)) {
                    delivered += cnt;
                }
            }
        }

        stats.put("totalCars", total);
        stats.put("availableCars", available);
        stats.put("reservedCars", reserved);
        stats.put("depositedCars", deposited);
        stats.put("soldCars", sold);
        stats.put("deliveredCars", delivered);
        stats.put("soldDeliveredCars", sold + delivered);
        return stats;
    }

    /**
     * Retrieves physical vehicle details for a specific car model.
     */
    @SuppressWarnings("SqlResolve")
    public List<InventoryCarDetail> getCarDetails(int modelId) throws SQLException {
        List<InventoryCarDetail> list = new ArrayList<>();
        String sql = """
            SELECT c.VIN, c.EngineNumber, col.ColorName,
                   COALESCE(s.StatusName, 'AVAILABLE') AS StatusName, c.Price
            FROM Cars c
            LEFT JOIN Colors col ON c.ColorID = col.ColorID
            LEFT JOIN CarStatuses s ON c.CarStatusID = s.CarStatusID
            WHERE c.ModelID = ?
            ORDER BY c.CarID DESC
            """;

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
