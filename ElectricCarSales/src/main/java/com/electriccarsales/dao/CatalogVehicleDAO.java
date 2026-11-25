package com.electriccarsales.dao;

import com.electriccarsales.model.CatalogVehicle;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public class CatalogVehicleDAO extends DBContext {

    public List<CatalogVehicle> getAll() {
        String sql = """
                SELECT m.ModelID, v.VersionID, m.ModelName, line.CarLineName, v.VersionName,
                       v.CatalogPrice, spec.Dimensions, spec.BatteryCapacity, spec.RangeKM,
                       spec.MaxPower, spec.SeatingCapacity, v.RangeStandard, v.SourceURL,
                       v.PriceSourceURL,
                       (SELECT TOP (1) ci.ImageURL
                        FROM CarImages ci
                        WHERE ci.ModelID = m.ModelID
                        ORDER BY ci.IsPrimary DESC, ci.DisplayOrder, ci.CarImageID) AS ImageURL,
                       spec.WheelType, spec.AirbagCount,
                       spec.MaxSpeed, v.RapidChargeMinutes, v.PriceNote, spec.Description AS SpecificationNote
                FROM CarModels m
                INNER JOIN CarLines line ON line.CarLineID = m.CarLineID
                INNER JOIN Brands brand ON brand.BrandID = line.BrandID
                INNER JOIN CarVersions v ON v.ModelID = m.ModelID
                LEFT JOIN Specifications spec ON spec.ModelID = m.ModelID
                WHERE brand.BrandStatusID IN (
                    SELECT BrandStatusID FROM BrandStatuses WHERE StatusName = 'ACTIVE'
                )
                ORDER BY line.CarLineName, m.ModelName, v.VersionName
                """;
        Map<Integer, CatalogVehicleBuilder> vehicles = new LinkedHashMap<>();
        try (java.sql.Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                CatalogVehicleBuilder vehicle = new CatalogVehicleBuilder(
                        results.getInt("ModelID"),
                        results.getInt("VersionID"),
                        results.getString("ModelName"),
                        results.getString("CarLineName"),
                        results.getString("VersionName"),
                        results.getBigDecimal("CatalogPrice"),
                        results.getString("Dimensions"),
                        results.getBigDecimal("BatteryCapacity"),
                        results.getBigDecimal("RangeKM"),
                        results.getBigDecimal("MaxPower"),
                        getNullableInteger(results, "SeatingCapacity"),
                        results.getString("RangeStandard"),
                        results.getString("SourceURL"),
                        results.getString("PriceSourceURL"),
                        results.getString("ImageURL"),
                        results.getString("WheelType"),
                        getNullableInteger(results, "AirbagCount"),
                        results.getBigDecimal("MaxSpeed"),
                        getNullableInteger(results, "RapidChargeMinutes"),
                        results.getString("PriceNote"),
                        results.getString("SpecificationNote"));
                vehicles.put(vehicle.versionID, vehicle);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in CatalogVehicleDAO.getAll(): " + e.getMessage(), e);
        }

        loadColors(vehicles);
        List<CatalogVehicle> catalogue = new ArrayList<>(
                vehicles.values().stream().map(CatalogVehicleBuilder::build).toList());
        catalogue.addAll(getAvailablePriceOverrides());
        return List.copyOf(catalogue);
    }

    private List<CatalogVehicle> getAvailablePriceOverrides() {
        String sql = """
                SELECT c.CarID, c.ModelID, c.VersionID, c.Price, color.ColorName,
                       model.ModelName, line.CarLineName, version.VersionName,
                       spec.Dimensions, spec.BatteryCapacity, spec.RangeKM, spec.MaxPower,
                       spec.SeatingCapacity, version.RangeStandard, spec.WheelType,
                       spec.AirbagCount, spec.MaxSpeed,
                       version.RapidChargeMinutes, version.PriceNote,
                       spec.Description AS SpecificationNote,
                       (SELECT TOP (1) mc.ColorImageURL
                        FROM ModelColors mc
                        WHERE mc.ModelID = c.ModelID AND mc.ColorID = c.ColorID) AS ColorImageURL,
                       (SELECT TOP (1) ci.ImageURL
                        FROM CarImages ci
                        WHERE ci.ModelID = c.ModelID
                        ORDER BY ci.IsPrimary DESC, ci.DisplayOrder, ci.CarImageID) AS ImageURL
                FROM Cars c
                INNER JOIN CarStatuses carStatus ON carStatus.CarStatusID = c.CarStatusID
                INNER JOIN CarModels model ON model.ModelID = c.ModelID
                INNER JOIN CarLines line ON line.CarLineID = model.CarLineID
                INNER JOIN Brands brand ON brand.BrandID = line.BrandID
                INNER JOIN CarVersions version ON version.VersionID = c.VersionID
                    AND version.ModelID = c.ModelID
                LEFT JOIN Colors color ON color.ColorID = c.ColorID
                LEFT JOIN Specifications spec ON spec.ModelID = model.ModelID
                WHERE brand.BrandStatusID IN (
                    SELECT BrandStatusID FROM BrandStatuses WHERE StatusName = 'ACTIVE'
                )
                  AND UPPER(LTRIM(RTRIM(carStatus.StatusName))) = 'AVAILABLE'
                  AND c.Price > 0
                  AND (version.CatalogPrice IS NULL OR c.Price <> version.CatalogPrice)
                  AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID = c.CarID)
                ORDER BY line.CarLineName, model.ModelName, version.VersionName, c.CarID
                """;
        Map<OverrideGroupKey, CatalogVehicle> groupedVehicles = new LinkedHashMap<>();
        try (java.sql.Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                String rawColorName = results.getString("ColorName");
                String colorName = rawColorName == null ? null : rawColorName.trim();
                String colorImage = results.getString("ColorImageURL");
                List<String> colors = colorName == null || colorName.isBlank()
                        ? List.of() : List.of(colorName);
                Map<String, String> colorImages = colorImage == null || colorImage.isBlank() || colors.isEmpty()
                        ? Map.of() : Map.of(colorName, colorImage);
                int carId = results.getInt("CarID");
                CatalogVehicle vehicle = new CatalogVehicle(
                        results.getInt("ModelID"), results.getInt("VersionID"),
                        results.getString("ModelName"), results.getString("CarLineName"),
                        results.getString("VersionName"), results.getBigDecimal("Price"),
                        results.getString("Dimensions"), results.getBigDecimal("BatteryCapacity"),
                        results.getBigDecimal("RangeKM"), results.getBigDecimal("MaxPower"),
                        getNullableInteger(results, "SeatingCapacity"), results.getString("RangeStandard"),
                        null, null,
                        results.getString("ImageURL"), colors, colorImages, results.getString("WheelType"),
                        getNullableInteger(results, "AirbagCount"), results.getBigDecimal("MaxSpeed"),
                        getNullableInteger(results, "RapidChargeMinutes"), results.getString("PriceNote"),
                        results.getString("SpecificationNote"), carId, colorName,
                        List.of(carId));
                OverrideGroupKey key = new OverrideGroupKey(vehicle.modelID(), vehicle.versionID(),
                        vehicle.priceVnd() == null ? null : vehicle.priceVnd().stripTrailingZeros());
                CatalogVehicle existing = groupedVehicles.get(key);
                if (existing == null) {
                    groupedVehicles.put(key, vehicle);
                } else {
                    LinkedHashSet<String> colorsInGroup = new LinkedHashSet<>(existing.colors());
                    colorsInGroup.addAll(vehicle.colors());
                    Map<String, String> imagesInGroup = new LinkedHashMap<>(existing.colorImages());
                    imagesInGroup.putAll(vehicle.colorImages());
                    List<Integer> carIdsInGroup = new ArrayList<>(existing.stockCarIDs());
                    carIdsInGroup.add(carId);
                    groupedVehicles.put(key, new CatalogVehicle(
                            existing.modelID(), existing.versionID(), existing.modelName(), existing.carLine(),
                            existing.specificationVersion(), existing.priceVnd(), existing.dimensions(),
                            existing.batteryKwh(), existing.rangeKm(), existing.maxPowerKw(), existing.seats(),
                            existing.rangeStandard(), existing.sourceUrl(), existing.priceSourceUrl(),
                            existing.imageUrl(), List.copyOf(colorsInGroup), Map.copyOf(imagesInGroup),
                            existing.wheelType(), existing.airbagCount(), existing.maxSpeedKmh(),
                            existing.rapidChargeMinutes(), existing.priceNote(), existing.specificationNote(),
                            existing.stockCarID(), existing.stockColorName(), List.copyOf(carIdsInGroup)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in CatalogVehicleDAO.getAvailablePriceOverrides(): " + e.getMessage(), e);
        }
        return List.copyOf(groupedVehicles.values());
    }

    private void loadColors(Map<Integer, CatalogVehicleBuilder> vehicles) {
        if (vehicles.isEmpty()) {
            return;
        }

        String sql = """
                SELECT DISTINCT c.ModelID, c.VersionID, color.ColorName, mc.ColorImageURL
                FROM Cars c
                INNER JOIN CarStatuses carStatus ON carStatus.CarStatusID = c.CarStatusID
                INNER JOIN CarVersions version ON version.VersionID = c.VersionID
                    AND version.ModelID = c.ModelID
                INNER JOIN Colors color ON color.ColorID = c.ColorID
                LEFT JOIN ModelColors mc ON mc.ModelID = c.ModelID AND mc.ColorID = c.ColorID
                WHERE UPPER(LTRIM(RTRIM(carStatus.StatusName))) = 'AVAILABLE'
                  AND c.Price = version.CatalogPrice
                  AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID = c.CarID)
                ORDER BY color.ColorName
                """;
        Map<Integer, List<CatalogVehicleBuilder>> vehiclesByModel = new LinkedHashMap<>();
        for (CatalogVehicleBuilder vehicle : vehicles.values()) {
            vehiclesByModel.computeIfAbsent(vehicle.modelID, ignored -> new ArrayList<>()).add(vehicle);
        }

        try (java.sql.Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                List<CatalogVehicleBuilder> modelVehicles = vehiclesByModel.get(results.getInt("ModelID"));
                if (modelVehicles == null) {
                    continue;
                }
                String rawColorName = results.getString("ColorName");
                String colorName = rawColorName == null ? null : rawColorName.trim();
                String colorImage = results.getString("ColorImageURL");
                int versionId = results.getInt("VersionID");
                for (CatalogVehicleBuilder vehicle : modelVehicles) {
                    if (vehicle.versionID != versionId) continue;
                    if (colorName != null && !colorName.isBlank()) {
                        vehicle.colors.add(colorName);
                        if (colorImage != null && !colorImage.isBlank()) {
                            vehicle.colorImages.put(colorName, colorImage);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in CatalogVehicleDAO.loadColors(): " + e.getMessage(), e);
        }
    }

    private Integer getNullableInteger(ResultSet results, String column) throws SQLException {
        int value = results.getInt(column);
        return results.wasNull() ? null : value;
    }

    private static final class CatalogVehicleBuilder {
        private final int modelID;
        private final int versionID;
        private final String modelName;
        private final String carLine;
        private final String specificationVersion;
        private final java.math.BigDecimal priceVnd;
        private final String dimensions;
        private final java.math.BigDecimal batteryKwh;
        private final java.math.BigDecimal rangeKm;
        private final java.math.BigDecimal maxPowerKw;
        private final Integer seats;
        private final String rangeStandard;
        private final String sourceUrl;
        private final String priceSourceUrl;
        private final String imageUrl;
        private final String wheelType;
        private final Integer airbagCount;
        private final java.math.BigDecimal maxSpeedKmh;
        private final Integer rapidChargeMinutes;
        private final String priceNote;
        private final String specificationNote;
        private final Set<String> colors = new LinkedHashSet<>();
        private final Map<String, String> colorImages = new LinkedHashMap<>();

        private CatalogVehicleBuilder(int modelID, int versionID, String modelName, String carLine,
                                      String specificationVersion, java.math.BigDecimal priceVnd,
                                      String dimensions, java.math.BigDecimal batteryKwh,
                                      java.math.BigDecimal rangeKm, java.math.BigDecimal maxPowerKw,
                                      Integer seats, String rangeStandard, String sourceUrl,
                                      String priceSourceUrl, String imageUrl, String wheelType,
                                      Integer airbagCount, java.math.BigDecimal maxSpeedKmh,
                                      Integer rapidChargeMinutes, String priceNote, String specificationNote) {
            this.modelID = modelID;
            this.versionID = versionID;
            this.modelName = modelName;
            this.carLine = carLine;
            this.specificationVersion = specificationVersion;
            this.priceVnd = priceVnd;
            this.dimensions = dimensions;
            this.batteryKwh = batteryKwh;
            this.rangeKm = rangeKm;
            this.maxPowerKw = maxPowerKw;
            this.seats = seats;
            this.rangeStandard = rangeStandard;
            this.sourceUrl = sourceUrl;
            this.priceSourceUrl = priceSourceUrl;
            this.imageUrl = imageUrl;
            this.wheelType = wheelType;
            this.airbagCount = airbagCount;
            this.maxSpeedKmh = maxSpeedKmh;
            this.rapidChargeMinutes = rapidChargeMinutes;
            this.priceNote = priceNote;
            this.specificationNote = specificationNote;
        }

        private CatalogVehicle build() {
            return new CatalogVehicle(modelID, versionID, modelName, carLine, specificationVersion,
                    priceVnd, dimensions, batteryKwh, rangeKm, maxPowerKw, seats, rangeStandard,
                    sourceUrl, priceSourceUrl, imageUrl, List.copyOf(colors), Map.copyOf(colorImages),
                    wheelType, airbagCount, maxSpeedKmh, rapidChargeMinutes, priceNote, specificationNote,
                    null, null, List.of());
        }
    }

    private record OverrideGroupKey(int modelID, int versionID, java.math.BigDecimal price) {
    }
}
