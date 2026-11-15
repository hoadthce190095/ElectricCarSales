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
               COALESCE(color.ColorName, col_cat.ColorName) AS ColorName,
               color.ColorName AS InventoryColorName
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
        }
        List<VehicleModel> catalogue = new ArrayList<>();
        for (ModelBuilder model : models.values()) {
            catalogue.add(model.build());
        }
        return catalogue;
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
        private final Map<String, Integer> inventoryByColor = new LinkedHashMap<>();
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
                String inventoryColor = row.getString("InventoryColorName");
                if (inventoryColor != null && !inventoryColor.isBlank()) {
                    inventoryByColor.merge(inventoryColor, 1, Integer::sum);
                }
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
            model.setInventoryByColor(inventoryByColor);
            return model;
        }
    }
}
