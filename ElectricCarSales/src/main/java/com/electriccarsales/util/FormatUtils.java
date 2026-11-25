package com.electriccarsales.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tiện ích chuẩn hóa định dạng tiền tệ và số theo chuẩn Việt Nam (vi-VN).
 * - Sử dụng dấu chấm (.) phân tách hàng nghìn: 7.000.000 VND, 75.000.000 VND, 150.000.000 VND.
 * - Không hiển thị phần thập phân .00 khi tiền là VND nguyên.
 * - Hậu tố thống nhất: "VND".
 */
public final class FormatUtils {

    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(new Locale("vi", "VN"));

    static {
        SYMBOLS.setGroupingSeparator('.');
        SYMBOLS.setDecimalSeparator(',');
    }

    private FormatUtils() {}

    /**
     * Định dạng số tiền BigDecimal sang chuỗi hiển thị theo chuẩn vi-VN có hậu tố VND.
     * Ví dụ: 7000000 -> "7.000.000 VND", 75000000 -> "75.000.000 VND".
     */
    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        DecimalFormat df = new DecimalFormat("#,##0", SYMBOLS);
        return df.format(amount.setScale(0, RoundingMode.HALF_UP).longValue()) + " VND";
    }

    /**
     * Định dạng số tiền Number sang chuỗi hiển thị theo chuẩn vi-VN có hậu tố VND.
     */
    public static String formatMoney(Number amount) {
        if (amount == null) {
            return "0 VND";
        }
        DecimalFormat df = new DecimalFormat("#,##0", SYMBOLS);
        return df.format(Math.round(amount.doubleValue())) + " VND";
    }

    /**
     * Định dạng số tiền sang chuỗi số có dấu chấm phân cách hàng nghìn, không kèm hậu tố.
     * Ví dụ: 75000000 -> "75.000.000".
     */
    public static String formatNumber(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        DecimalFormat df = new DecimalFormat("#,##0", SYMBOLS);
        return df.format(amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    /**
     * Định dạng số tiền sang chuỗi số có dấu chấm phân cách hàng nghìn, không kèm hậu tố.
     */
    public static String formatNumber(Number amount) {
        if (amount == null) {
            return "0";
        }
        DecimalFormat df = new DecimalFormat("#,##0", SYMBOLS);
        return df.format(Math.round(amount.doubleValue()));
    }
}
