package com.electriccarsales.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CatalogVehicle(
        int modelID,
        int versionID,
        String modelName,
        String carLine,
        String specificationVersion,
        BigDecimal priceVnd,
        String dimensions,
        BigDecimal batteryKwh,
        BigDecimal rangeKm,
        BigDecimal maxPowerKw,
        Integer seats,
        String rangeStandard,
        String sourceUrl,
        String priceSourceUrl,
        String imageUrl,
        List<String> colors,
        Map<String, String> colorImages,
        String wheelType,
        Integer airbagCount,
        BigDecimal maxSpeedKmh,
        Integer rapidChargeMinutes,
        String priceNote,
        String specificationNote,
        Integer stockCarID,
        String stockColorName) {
}
