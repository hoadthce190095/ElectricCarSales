package com.electriccarsales.dao;

import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.model.PromotionOffer;
import util.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CatalogVehicleDAO extends DBContext {

    public List<CatalogVehicle> getAll() {
        String sql = """
                SELECT m.ModelID, v.VersionID, m.ModelName, line.CarLineName, v.VersionName,
                       v.CatalogPrice, spec.Dimensions, spec.BatteryCapacity, spec.RangeKM,
                       spec.MaxPower, spec.SeatingCapacity, v.RangeStandard, v.SourceURL,
                       v.PriceSourceURL, image.ImageURL, spec.WheelType, spec.AirbagCount,
                       spec.MaxSpeed, v.RapidChargeMinutes, v.PriceNote, spec.Description AS SpecificationNote
                FROM CarModels m
                INNER JOIN CarLines line ON line.CarLineID = m.CarLineID
                INNER JOIN Brands brand ON brand.BrandID = line.BrandID
                INNER JOIN CarVersions v ON v.ModelID = m.ModelID
                LEFT JOIN Specifications spec ON spec.ModelID = m.ModelID
                OUTER APPLY (
                    SELECT TOP (1) ci.ImageURL
                    FROM CarImages ci
                    WHERE ci.ModelID = m.ModelID
                    ORDER BY ci.IsPrimary DESC, ci.DisplayOrder, ci.CarImageID
                ) image
                WHERE brand.BrandStatusID IN (
                    SELECT BrandStatusID FROM BrandStatuses WHERE StatusName = 'ACTIVE'
                )
                ORDER BY line.CarLineName, m.ModelName, v.VersionName
                """;
        Map<Integer, CatalogVehicleBuilder> vehicles = new LinkedHashMap<>();
        try (PreparedStatement statement = conn.prepareStatement(sql);
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
        loadPromotions(vehicles);
        return vehicles.values().stream().map(CatalogVehicleBuilder::build).toList();
    }

    private void loadColors(Map<Integer, CatalogVehicleBuilder> vehicles) {
        if (vehicles.isEmpty()) {
            return;
        }

        String sql = """
                SELECT mc.ModelID, color.ColorName, mc.ColorImageURL
                FROM ModelColors mc
                INNER JOIN Colors color ON color.ColorID = mc.ColorID
                ORDER BY color.ColorName
                """;
        Map<Integer, List<CatalogVehicleBuilder>> vehiclesByModel = new LinkedHashMap<>();
        for (CatalogVehicleBuilder vehicle : vehicles.values()) {
            vehiclesByModel.computeIfAbsent(vehicle.modelID, ignored -> new ArrayList<>()).add(vehicle);
        }

        try (PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                List<CatalogVehicleBuilder> modelVehicles = vehiclesByModel.get(results.getInt("ModelID"));
                if (modelVehicles == null) {
                    continue;
                }
                String colorName = results.getString("ColorName");
                String colorImage = results.getString("ColorImageURL");
                for (CatalogVehicleBuilder vehicle : modelVehicles) {
                    vehicle.colors.add(colorName);
                    if (colorImage != null && !colorImage.isBlank()) {
                        vehicle.colorImages.put(colorName, colorImage);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in CatalogVehicleDAO.loadColors(): " + e.getMessage(), e);
        }
    }

    private void loadPromotions(Map<Integer, CatalogVehicleBuilder> vehicles) {
        if (vehicles.isEmpty()) {
            return;
        }

        String sql = """
                SELECT pc.ModelID, p.PromotionID, p.PromotionName, p.Description,
                       p.DiscountType, p.DiscountValue, p.EndDate
                FROM PromotionCars pc
                INNER JOIN Promotions p ON p.PromotionID = pc.PromotionID
                WHERE UPPER(LTRIM(RTRIM(p.Status))) = 'ACTIVE'
                  AND p.StartDate <= SYSDATETIME()
                  AND p.EndDate >= SYSDATETIME()
                ORDER BY p.EndDate, p.PromotionName
                """;
        Map<Integer, List<CatalogVehicleBuilder>> vehiclesByModel = new LinkedHashMap<>();
        for (CatalogVehicleBuilder vehicle : vehicles.values()) {
            vehiclesByModel.computeIfAbsent(vehicle.modelID, ignored -> new ArrayList<>()).add(vehicle);
        }

        try (PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                List<CatalogVehicleBuilder> modelVehicles = vehiclesByModel.get(results.getInt("ModelID"));
                if (modelVehicles == null) {
                    continue;
                }

                java.sql.Timestamp endTimestamp = results.getTimestamp("EndDate");
                PromotionOffer promotion = new PromotionOffer(
                        results.getInt("PromotionID"),
                        results.getString("PromotionName"),
                        results.getString("Description"),
                        results.getString("DiscountType"),
                        results.getBigDecimal("DiscountValue"),
                        endTimestamp == null ? null : endTimestamp.toLocalDateTime());
                for (CatalogVehicleBuilder vehicle : modelVehicles) {
                    vehicle.promotions.add(promotion);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in CatalogVehicleDAO.loadPromotions(): " + e.getMessage(), e);
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
        private final List<String> colors = new ArrayList<>();
        private final Map<String, String> colorImages = new LinkedHashMap<>();
        private final List<PromotionOffer> promotions = new ArrayList<>();

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
                    List.copyOf(promotions));
        }
    }
}
