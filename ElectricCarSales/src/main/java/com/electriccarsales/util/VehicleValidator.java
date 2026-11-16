package com.electriccarsales.util;

import java.time.Year;
import java.util.Locale;
import java.util.regex.Pattern;

public final class VehicleValidator {

    /**
     * Standard VIN regex: 17 characters, uppercase Latin letters and numbers, excluding I, O, Q.
     * Pattern: ^[A-HJ-NPR-Z0-9]{17}$
     */
    public static final String VIN_REGEX = "^[A-HJ-NPR-Z0-9]{17}$";
    public static final Pattern VIN_PATTERN = Pattern.compile(VIN_REGEX);

    /**
     * Engine number regex: 5 to 50 characters, Latin letters (A-Z, a-z), digits (0-9), and hyphen (-).
     * No spaces or other special characters.
     * Pattern: ^[A-Za-z0-9-]{5,50}$
     */
    public static final String ENGINE_REGEX = "^[A-Za-z0-9-]{5,50}$";
    public static final Pattern ENGINE_PATTERN = Pattern.compile(ENGINE_REGEX);

    private VehicleValidator() {}

    /**
     * Normalizes VIN by trimming whitespace and converting to uppercase.
     */
    public static String normalizeVin(String vin) {
        if (vin == null) return null;
        return vin.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes Engine Number by trimming whitespace and converting to uppercase.
     */
    public static String normalizeEngineNumber(String engineNumber) {
        if (engineNumber == null) return null;
        return engineNumber.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Validates VIN according to RDS business rules (BR1):
     * - Required
     * - Exactly 17 characters
     * - Only Latin letters and digits, excluding I, O, Q
     * - No special characters, whitespace, or accented characters
     */
    public static void validateVin(String rawVin) {
        if (rawVin == null || rawVin.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số khung VIN.");
        }
        String trimmed = rawVin.trim();
        if (trimmed.length() != 17) {
            throw new IllegalArgumentException("VIN phải có đúng 17 ký tự.");
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!VIN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.");
        }
    }

    /**
     * Validates Engine Number according to SQL constraints and proposed standard:
     * - Required
     * - 5 to 50 characters
     * - Only Latin letters (A-Z, a-z), digits (0-9), and hyphen (-)
     * - No spaces or other special characters
     */
    public static void validateEngineNumber(String rawEngineNumber) {
        if (rawEngineNumber == null || rawEngineNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số máy.");
        }
        String trimmed = rawEngineNumber.trim();
        if (trimmed.length() < 5 || trimmed.length() > 50) {
            throw new IllegalArgumentException("Số máy phải có từ 5 đến 50 ký tự.");
        }
        if (!ENGINE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Số máy chỉ được chứa chữ cái Latin, chữ số và dấu gạch ngang (-).");
        }
    }

    /**
     * Validates vehicle selling price (>= 100,000).
     */
    public static void validatePrice(double price) {
        if (price < 100000 || Double.isNaN(price) || Double.isInfinite(price)) {
            throw new IllegalArgumentException("Giá bán xe phải lớn hơn hoặc bằng 100.000 VNĐ.");
        }
    }

    /**
     * Validates vehicle manufacturing year (between 2000 and current year + 2).
     */
    public static void validateYear(int year) {
        int maxYear = Year.now().getValue() + 2;
        if (year < 2000 || year > maxYear) {
            throw new IllegalArgumentException("Năm sản xuất xe phải từ năm 2000 đến " + maxYear + ".");
        }
    }
}
