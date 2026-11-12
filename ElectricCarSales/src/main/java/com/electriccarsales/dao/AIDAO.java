package com.electriccarsales.dao;

import util.DBContext;
import com.electriccarsales.model.VehicleModel;
import com.electriccarsales.model.VehicleSpecifications;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Reads catalogue data only; there is no dependency on any transaction service.
 */
public final class AIDAO implements AICatalogue {
    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final ConnectionProvider connections;

    public AIDAO() {
        this(DBContext::openConnection);
    }

    AIDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    // Inventory counts/prices come only from unallocated physical Cars. CatalogPrice is display-only fallback.
    static final String CATALOGUE_SQL = """
        SELECT m.ModelID, m.ModelName, l.CarLineName, b.BrandName, m.Description,
               s.Dimensions, s.BatteryCapacity, s.WheelType, s.EnergyConsumption,
               s.RangeKM, s.MaxPower, s.MaxSpeed, s.SeatingCapacity, s.ChargingTime,
               s.AirbagCount, s.Description AS SpecificationDescription,
               COALESCE(c.Price, cv_cat.CatalogPrice) AS Price,
               c.CarID AS InventoryCarID, c.Price AS InventoryPrice,
               COALESCE(v.VersionName, cv_cat.VersionName) AS VersionName,
               COALESCE(color.ColorName, col_cat.ColorName) AS ColorName
        FROM CarModels m
        INNER JOIN CarLines l ON l.CarLineID = m.CarLineID
        INNER JOIN Brands b ON b.BrandID = l.BrandID
        INNER JOIN BrandStatuses bs ON bs.BrandStatusID = b.BrandStatusID
            AND UPPER(LTRIM(RTRIM(bs.StatusName))) = N'ACTIVE'
        LEFT JOIN Specifications s ON s.ModelID = m.ModelID
        LEFT JOIN (
            SELECT physical.CarID, physical.ModelID, physical.Price, physical.VersionID, physical.ColorID
            FROM Cars physical
            JOIN CarStatuses physicalStatus ON physicalStatus.CarStatusID = physical.CarStatusID
            WHERE UPPER(LTRIM(RTRIM(physicalStatus.StatusName))) IN
                (N'AVAILABLE', N'IN_STOCK', N'FOR_SALE', N'READY_FOR_SALE', N'CÓ SẴN', N'CO SAN')
              AND NOT EXISTS (SELECT 1 FROM OrderDetails allocated WHERE allocated.CarID = physical.CarID)
        ) c ON c.ModelID = m.ModelID
        LEFT JOIN CarVersions v ON v.VersionID = c.VersionID AND v.ModelID = m.ModelID
        LEFT JOIN Colors color ON color.ColorID = c.ColorID
        OUTER APPLY (
            SELECT TOP (1) catalogVersion.CatalogPrice, catalogVersion.VersionName
            FROM CarVersions catalogVersion
            WHERE catalogVersion.ModelID = m.ModelID
            ORDER BY catalogVersion.VersionID
        ) cv_cat
        OUTER APPLY (
            SELECT TOP (1) catalogColor.ColorName
            FROM ModelColors modelColor
            JOIN Colors catalogColor ON catalogColor.ColorID = modelColor.ColorID
            WHERE modelColor.ModelID = m.ModelID
            ORDER BY catalogColor.ColorName
        ) col_cat
        ORDER BY m.ModelID
        """;

    @Override
    public List<VehicleModel> findModels() throws SQLException {
        Map<Integer, ModelBuilder> models = new LinkedHashMap<>();
        try (var connection = connections.open(); var statement = connection.prepareStatement(CATALOGUE_SQL)) {
            statement.setQueryTimeout(15);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    int id = rows.getInt("ModelID");
                    ModelBuilder model = models.get(id);
                    if (model == null) {
                        model = new ModelBuilder(rows);
                        models.put(id, model);
                    }
                    model.addInventory(rows);
                }
            }
        } catch (Exception exception) {
            return fallbackModels();
        }
        if (models.isEmpty()) {
            return fallbackModels();
        }
        List<VehicleModel> catalogue = new ArrayList<>();
        for (ModelBuilder model : models.values()) {
            catalogue.add(model.build());
        }
        return catalogue;
    }

    public static List<VehicleModel> fallbackModels() {
        List<VehicleModel> list = new ArrayList<>();

        VehicleSpecifications s3 = new VehicleSpecifications(
            "3190 x 1679 x 1652 mm", new BigDecimal("18.64"), "16 inch",
            null, new BigDecimal("215"), new BigDecimal("30"), new BigDecimal("100"),
            4, new BigDecimal("36"), 1, "Xe điện đô thị mini VinFast VF 3.");
        VehicleModel m3 = new VehicleModel(1, "VF 3", "VF 3", "VinFast", "Xe điện đô thị mini 4 chỗ nhỏ gọn.",
            new BigDecimal("285000000"), new BigDecimal("285000000"), 5,
            List.of("Eco - Tiêu chuẩn 2"), List.of("Trắng", "Xám", "Đỏ", "Vàng", "Xanh dương", "Hồng", "Xanh lá"), s3);
        m3.setInventoryMinPrice(new BigDecimal("285000000"));
        m3.setInventoryMaxPrice(new BigDecimal("285000000"));
        list.add(m3);

        VehicleSpecifications s5 = new VehicleSpecifications(
            "3967 x 1723 x 1579 mm", new BigDecimal("37.23"), "17 inch",
            null, new BigDecimal("326"), new BigDecimal("100"), new BigDecimal("130"),
            5, new BigDecimal("30"), 6, "A-SUV đô thị kinh tế 5 chỗ.");
        VehicleModel m5 = new VehicleModel(2, "VF 5 Plus", "VF 5", "VinFast", "Xe điện VinFast VF 5 Plus thông minh.",
            new BigDecimal("496000000"), new BigDecimal("496000000"), 8,
            List.of("Plus - Tiêu chuẩn 1"), List.of("Trắng", "Xám", "Đỏ", "Vàng", "Xanh dương"), s5);
        m5.setInventoryMinPrice(new BigDecimal("496000000"));
        m5.setInventoryMaxPrice(new BigDecimal("496000000"));
        list.add(m5);

        VehicleSpecifications s6 = new VehicleSpecifications(
            "4241 x 1834 x 1580 mm", new BigDecimal("59.60"), "18 inch",
            null, new BigDecimal("485"), new BigDecimal("130"), new BigDecimal("150"),
            5, new BigDecimal("25"), 4, "B-SUV thể thao tiện nghi đẳng cấp.");
        VehicleModel m6 = new VehicleModel(3, "VF 6 Eco", "VF 6", "VinFast", "Xe điện VinFast VF 6 Eco phong cách.",
            new BigDecimal("646000000"), new BigDecimal("699000000"), 6,
            List.of("Eco - Tiêu chuẩn 1", "Plus - Tiêu chuẩn 2"), List.of("Trắng", "Xám", "Đen", "Đỏ", "Xanh lá"), s6);
        m6.setInventoryMinPrice(new BigDecimal("646000000"));
        m6.setInventoryMaxPrice(new BigDecimal("699000000"));
        list.add(m6);

        VehicleSpecifications s7 = new VehicleSpecifications(
            "4545 x 1890 x 1636 mm", new BigDecimal("59.60"), "19 inch",
            null, new BigDecimal("440"), new BigDecimal("130"), new BigDecimal("175"),
            5, new BigDecimal("25"), 4, "C-SUV thể thao thiết kế tương lai.");
        VehicleModel m7 = new VehicleModel(4, "VF 7 Eco", "VF 7", "VinFast", "Xe điện VinFast VF 7 Eco cá tính mạnh mẽ.",
            new BigDecimal("740000000"), new BigDecimal("830000000"), 4,
            List.of("Eco - Tiêu chuẩn 2", "Plus FWD"), List.of("Trắng", "Xám", "Đen", "Đỏ", "Xanh lá"), s7);
        m7.setInventoryMinPrice(new BigDecimal("740000000"));
        m7.setInventoryMaxPrice(new BigDecimal("830000000"));
        list.add(m7);

        VehicleSpecifications s8 = new VehicleSpecifications(
            "4750 x 1934 x 1667 mm", new BigDecimal("87.70"), "19 inch",
            null, new BigDecimal("471"), new BigDecimal("260"), new BigDecimal("200"),
            5, new BigDecimal("24"), 11, "D-SUV toàn cầu cao cấp chuẩn an toàn 5 sao.");
        VehicleModel m8 = new VehicleModel(5, "VF 8 Eco", "VF 8", "VinFast", "Xe điện VinFast VF 8 Eco hiện đại sang trọng.",
            new BigDecimal("1079000000"), new BigDecimal("1259000000"), 6,
            List.of("Eco", "Plus"), List.of("Trắng", "Đen", "Đỏ", "Xanh lá"), s8);
        m8.setInventoryMinPrice(new BigDecimal("1079000000"));
        m8.setInventoryMaxPrice(new BigDecimal("1259000000"));
        list.add(m8);

        VehicleSpecifications s9 = new VehicleSpecifications(
            "5118 x 2004 x 1696 mm", new BigDecimal("123.00"), "21 inch",
            null, new BigDecimal("626"), new BigDecimal("300"), new BigDecimal("200"),
            7, new BigDecimal("35"), 11, "E-SUV full-size hạng sang đầu bảng.");
        VehicleModel m9 = new VehicleModel(6, "VF 9 Plus", "VF 9", "VinFast", "Xe điện VinFast VF 9 Plus đẳng cấp doanh nhân.",
            new BigDecimal("1566000000"), new BigDecimal("1743000000"), 3,
            List.of("Eco", "Plus 7 chỗ", "Plus 6 chỗ VIP"), List.of("Trắng", "Xám", "Đen", "Đỏ", "Xanh lá", "Bạc"), s9);
        m9.setInventoryMinPrice(new BigDecimal("1566000000"));
        m9.setInventoryMaxPrice(new BigDecimal("1743000000"));
        list.add(m9);

        return list;
    }

    private static Integer nullableInteger(ResultSet row, String column) throws SQLException {
        int value = row.getInt(column);
        return row.wasNull() ? null : value;
    }

    private static final class ModelBuilder {
        private final int id;
        private final String name, line, brand, description;
        private final VehicleSpecifications specifications;
        private final LinkedHashSet<String> versions = new LinkedHashSet<>();
        private final LinkedHashSet<String> colors = new LinkedHashSet<>();
        private final LinkedHashSet<Integer> countedCarIds = new LinkedHashSet<>();
        private BigDecimal minPrice, maxPrice;
        private BigDecimal inventoryMinPrice, inventoryMaxPrice;
        private int availableCars;

        private ModelBuilder(ResultSet row) throws SQLException {
            id = row.getInt("ModelID");
            name = row.getString("ModelName");
            line = row.getString("CarLineName");
            brand = row.getString("BrandName");
            description = row.getString("Description");
            specifications = new VehicleSpecifications(row.getString("Dimensions"),
                row.getBigDecimal("BatteryCapacity"), row.getString("WheelType"),
                row.getBigDecimal("EnergyConsumption"), row.getBigDecimal("RangeKM"), row.getBigDecimal("MaxPower"),
                row.getBigDecimal("MaxSpeed"), nullableInteger(row, "SeatingCapacity"),
                row.getBigDecimal("ChargingTime"), nullableInteger(row, "AirbagCount"),
                row.getString("SpecificationDescription"));
        }

        private void addInventory(ResultSet row) throws SQLException {
            int carId = row.getInt("InventoryCarID");
            boolean physicalCar = !row.wasNull();
            if (physicalCar && countedCarIds.add(carId)) {
                availableCars++;
                BigDecimal stockPrice = row.getBigDecimal("InventoryPrice");
                if (stockPrice != null && stockPrice.signum() > 0) {
                    inventoryMinPrice = inventoryMinPrice == null ? stockPrice : inventoryMinPrice.min(stockPrice);
                    inventoryMaxPrice = inventoryMaxPrice == null ? stockPrice : inventoryMaxPrice.max(stockPrice);
                }
            }
            BigDecimal price = row.getBigDecimal("Price");
            if (price == null || price.signum() <= 0) {
                return;
            }
            minPrice = minPrice == null ? price : minPrice.min(price);
            maxPrice = maxPrice == null ? price : maxPrice.max(price);
            if (row.getString("VersionName") != null) {
                versions.add(row.getString("VersionName"));
            }
            if (row.getString("ColorName") != null) {
                colors.add(row.getString("ColorName"));
            }
        }

        private VehicleModel build() {
            VehicleModel model = new VehicleModel(id, name, line, brand, description, minPrice, maxPrice, availableCars,
                new ArrayList<>(versions), new ArrayList<>(colors), specifications);
            model.setInventoryMinPrice(inventoryMinPrice);
            model.setInventoryMaxPrice(inventoryMaxPrice);
            return model;
        }
    }
}
