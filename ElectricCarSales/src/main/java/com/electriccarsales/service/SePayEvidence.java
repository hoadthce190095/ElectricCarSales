package com.electriccarsales.service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cung cấp thông tin đối soát giao dịch thanh toán trực tuyến SePay + MB Bank từ trường Note trong bảng Payments.
 * Hỗ trợ định dạng SePay hiện tại và tương thích ngược với dữ liệu lịch sử.
 */
public record SePayEvidence(String eventId, String gateway, String payDate, BigDecimal received,
                            String content, String bankReference) {

    private static final Pattern SEPAY_PATTERN = Pattern.compile(
            "SEPAY_ID=([0-9]+);.*?"
            + "(?:SEPAY_GATEWAY=([A-Za-z0-9._-]+);.*?)?"
            + "(?:SEPAY_REF=([A-Za-z0-9./_-]+);.*?)?"
            + "(?:SEPAY_DATE=([^;]+);.*?)?"
            + "SEPAY_AMOUNT=([0-9]+(?:\\.[0-9]+)?);.*?"
            + "(?:SEPAY_CONTENT=([^;]+);)?"
    );

    private static final Pattern LEGACY_SEPAY_PATTERN = Pattern.compile(
            "SEPAY_ID=([1-9][0-9]{0,18});.*?"
            + "(?:SEPAY_REF=([A-Za-z0-9./_-]{1,80});.*?)?"
            + "(?:SEPAY_DATE=([^;]{1,32});.*?)?"
            + "SEPAY_RECEIVED=([0-9]{1,18}(?:\\.[0-9]{1,2})?);"
    );

    private static final Pattern VNPAY_LEGACY_PATTERN = Pattern.compile(
            "VNPAY_TXN=([A-Za-z0-9._-]+);.*?"
            + "(?:VNPAY_BANK=([A-Za-z0-9._-]+);.*?)?"
            + "(?:VNPAY_DATE=([^;]+);.*?)?"
            + "VNPAY_AMOUNT=([0-9]+(?:\\.[0-9]+)?);"
    );

    public String getEventId() { return eventId != null ? eventId : ""; }
    public String getTransactionNo() { return eventId != null ? eventId : ""; }
    public String getGateway() { return gateway != null ? gateway : "MBBank"; }
    public String getBankCode() { return getGateway(); }
    public String getPayDate() { return payDate != null ? payDate : ""; }
    public String getTransactionDate() { return getPayDate(); }
    public BigDecimal getReceived() { return received; }
    public String getContent() { return content != null ? content : ""; }
    public String getBankReference() { return bankReference != null ? bankReference : (eventId != null ? eventId : ""); }
    public String bankReference() { return getBankReference(); }

    public static SePayEvidence fromNote(String note) {
        if (note == null || note.isBlank()) return null;

        // 1. Định dạng SePay tiêu chuẩn mới
        Matcher sepayMatcher = SEPAY_PATTERN.matcher(note);
        if (sepayMatcher.find()) {
            String id = sepayMatcher.group(1);
            String gw = sepayMatcher.group(2);
            String ref = sepayMatcher.group(3);
            String date = sepayMatcher.group(4);
            String amtStr = sepayMatcher.group(5);
            String cnt = sepayMatcher.group(6);
            try {
                BigDecimal amt = new BigDecimal(amtStr);
                if (amt.signum() > 0) {
                    return new SePayEvidence(id, gw != null ? gw : "MBBank", date, amt, cnt, ref);
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Định dạng SePay cũ
        Matcher legacyMatcher = LEGACY_SEPAY_PATTERN.matcher(note);
        if (legacyMatcher.find()) {
            String id = legacyMatcher.group(1);
            String ref = legacyMatcher.group(2);
            String date = legacyMatcher.group(3);
            String amtStr = legacyMatcher.group(4);
            try {
                BigDecimal amt = new BigDecimal(amtStr);
                if (amt.signum() > 0) {
                    return new SePayEvidence(id, "MBBank", date, amt, "", ref);
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Định dạng VNPay demo cũ nếu có trong DB test
        Matcher vnpMatcher = VNPAY_LEGACY_PATTERN.matcher(note);
        if (vnpMatcher.find()) {
            String txn = vnpMatcher.group(1);
            String bank = vnpMatcher.group(2);
            String date = vnpMatcher.group(3);
            String amtStr = vnpMatcher.group(4);
            try {
                BigDecimal amt = new BigDecimal(amtStr);
                if (amt.signum() > 0) {
                    return new SePayEvidence(txn, bank != null ? bank : "VNPAY", date, amt, "", txn);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
