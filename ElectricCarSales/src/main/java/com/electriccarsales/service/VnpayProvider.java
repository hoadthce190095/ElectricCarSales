package com.electriccarsales.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/** Official VNPAY PAY v2.1.0 URL and IPN signature preparation. No DB status is changed here. */
public final class VnpayProvider {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final String merchantCode;
    private final String hashSecret;
    private final String payUrl;

    public VnpayProvider() {
        this(System.getenv("VNPAY_TMN_CODE"), System.getenv("VNPAY_HASH_SECRET"), System.getenv("VNPAY_PAY_URL"));
    }

    public VnpayProvider(String merchantCode, String hashSecret, String payUrl) {
        this.merchantCode = merchantCode;
        this.hashSecret = hashSecret;
        this.payUrl = payUrl;
    }

    public boolean isConfigured() {
        return merchantCode != null && merchantCode.matches("[A-Za-z0-9]{8}")
                && hashSecret != null && !hashSecret.isBlank()
                && payUrl != null && https(payUrl);
    }

    /** Creates a real hosted checkout URL when merchant credentials exist; does not create a payment record. */
    public String paymentUrl(String uniqueReference, BigDecimal businessAmount, String clientIp, String returnUrl) {
        if (!isConfigured()) throw new IllegalStateException("VNPAY merchant credentials / HTTPS endpoint not configured.");
        if (uniqueReference == null || !uniqueReference.matches("[A-Za-z0-9._-]{1,100}"))
            throw new IllegalArgumentException("Invalid VNPAY payment reference");
        if (clientIp == null || clientIp.isBlank() || clientIp.length() > 45 || !https(returnUrl))
            throw new IllegalArgumentException("Real client IP and HTTPS return URL are required");
        if (businessAmount == null || businessAmount.signum() <= 0 || businessAmount.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("VNPAY amount must be a whole VND amount");
        String scaled = businessAmount.toBigIntegerExact().multiply(java.math.BigInteger.valueOf(100)).toString();
        if (scaled.length() > 12) throw new IllegalArgumentException("Amount exceeds documented VNPAY field limit");
        LocalDateTime now = LocalDateTime.now(VIETNAM);
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", "2.1.0");
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", merchantCode);
        fields.put("vnp_Amount", scaled);
        fields.put("vnp_CreateDate", DATE.format(now));
        fields.put("vnp_ExpireDate", DATE.format(now.plusMinutes(15)));
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_IpAddr", clientIp);
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_OrderInfo", "Thanh toan " + uniqueReference);
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_ReturnUrl", returnUrl);
        fields.put("vnp_TxnRef", uniqueReference);
        String query = canonical(fields);
        return payUrl + (payUrl.contains("?") ? "&" : "?") + query + "&vnp_SecureHash=" + hmac(query);
    }

    /** Rejects unsigned/spoofed responses; success means the provider reports success, not a DB update. */
    public VerifiedIpn verifyIpn(Map<String, String> received) {
        if (!isConfigured() || received == null) return new VerifiedIpn(false, false, null, null, null);
        String signature = received.get("vnp_SecureHash");
        if (signature == null || !signature.matches("(?i)[a-f0-9]{128}"))
            return new VerifiedIpn(false, false, null, null, null);
        Map<String, String> fields = new TreeMap<>();
        for (var entry : received.entrySet()) {
            if (entry.getKey() != null && entry.getKey().startsWith("vnp_")
                    && !"vnp_SecureHash".equals(entry.getKey())
                    && !"vnp_SecureHashType".equals(entry.getKey())) {
                if (entry.getValue() == null) return new VerifiedIpn(false, false, null, null, null);
                fields.put(entry.getKey(), entry.getValue());
            }
        }
        String expected = hmac(canonical(fields));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII))
                || !merchantCode.equals(fields.get("vnp_TmnCode"))) {
            return new VerifiedIpn(false, false, null, null, null);
        }
        String scaled = fields.get("vnp_Amount");
        String reference = fields.get("vnp_TxnRef");
        String transaction = fields.get("vnp_TransactionNo");
        if (scaled == null || !scaled.matches("[0-9]{1,12}") || reference == null || reference.isBlank()) {
            return new VerifiedIpn(false, false, null, null, null);
        }
        BigDecimal amount = new BigDecimal(scaled).movePointLeft(2);
        boolean success = "00".equals(fields.get("vnp_ResponseCode"))
                && "00".equals(fields.get("vnp_TransactionStatus"))
                && transaction != null && !transaction.isBlank();
        return new VerifiedIpn(true, success, reference, amount, transaction);
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot calculate VNPAY signature", e);
        }
    }

    private static String canonical(Map<String, String> fields) {
        StringBuilder value = new StringBuilder();
        for (var entry : new TreeMap<>(fields).entrySet()) {
            if (!value.isEmpty()) value.append('&');
            value.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return value.toString();
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static boolean https(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (RuntimeException e) { return false; }
    }

    public record VerifiedIpn(boolean signatureValid, boolean successful, String paymentReference,
                              BigDecimal amount, String providerTransactionNo) { }
}
