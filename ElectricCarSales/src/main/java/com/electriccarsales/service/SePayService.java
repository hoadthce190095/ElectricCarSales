package com.electriccarsales.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Quản lý cấu hình và tiện ích tích hợp SePay + MB Bank theo tài liệu chính thức của SePay:
 * - Tạo mã VietQR động qua endpoint SePay: https://qr.sepay.vn/img
 * - Xác thực Webhook nhận từ SePay (Authorization: Apikey <api_key>)
 * - Quản lý thông tin tài khoản MB Bank thụ hưởng (0917231548 - Do Tan Dat).
 */
public class SePayService {

    public static final String DEFAULT_BANK_CODE = "MB";
    public static final String DEFAULT_BANK_NAME = "MB Bank (Ngân hàng TMCP Quân đội)";
    public static final String DEFAULT_ACCOUNT_NUMBER = "";
    public static final String DEFAULT_ACCOUNT_NAME = "";
    public static final String DEFAULT_TEMPLATE = "compact";
    public static final String SEPAY_QR_BASE_URL = "https://qr.sepay.vn/img";

    private static final Properties LOCAL_CONFIG = loadLocalConfig();

    public static String getBankCode() {
        return configured("SEPAY_BANK_CODE", DEFAULT_BANK_CODE);
    }

    public static String getBankName() {
        String code = getBankCode();
        if ("MB".equalsIgnoreCase(code) || "MBBank".equalsIgnoreCase(code)) {
            return DEFAULT_BANK_NAME;
        }
        return code;
    }

    public static String getAccountNumber() {
        return configured("SEPAY_ACCOUNT_NUMBER", DEFAULT_ACCOUNT_NUMBER);
    }

    public static String getAccountName() {
        return configured("SEPAY_ACCOUNT_NAME", DEFAULT_ACCOUNT_NAME);
    }

    public static String getWebhookKey() {
        String key = configured("SEPAY_WEBHOOK_KEY", "");
        if (key.isBlank()) {
            key = configured("SEPAY_WEBHOOK_SECRET", "");
        }
        if (key.isBlank()) {
            key = configured("SEPAY_API_KEY", "");
        }
        return key;
    }

    public static String getEnvironment() {
        return configured("SEPAY_ENVIRONMENT", "production");
    }

    /**
     * Kiểm tra xem SePay đã sẵn sàng phục vụ thanh toán hay chưa.
     */
    public static boolean isReady() {
        String acc = getAccountNumber();
        String bank = getBankCode();
        String name = getAccountName();
        return !acc.isBlank() && !bank.isBlank() && !name.isBlank();
    }

    public static String configStatus() {
        if (getAccountNumber().isBlank()) {
            return "Thiếu số tài khoản nhận tiền SePay (SEPAY_ACCOUNT_NUMBER).";
        }
        if (getBankCode().isBlank()) {
            return "Thiếu mã ngân hàng SePay (SEPAY_BANK_CODE).";
        }
        if (getAccountName().isBlank()) {
            return "Thiếu tên chủ tài khoản SePay (SEPAY_ACCOUNT_NAME).";
        }
        return "READY";
    }

    /**
     * Tạo URL ảnh VietQR động của SePay.
     * Cú pháp chuẩn SePay: https://qr.sepay.vn/img?acc={acc}&bank={bank}&amount={amount}&des={des}&template={template}
     *
     * @param reference   Mã thanh toán duy nhất gắn với PaymentID (VD: ECS1D1)
     * @param amountVnd   Số tiền cần thanh toán bằng VND
     * @param description Nội dung chuyển khoản hiển thị trên QR (mặc định trùng mã thanh toán)
     * @return URL ảnh VietQR hợp lệ
     */
    public String createVietQrUrl(String reference, long amountVnd, String description) {
        return createVietQrUrl(reference, amountVnd, description, DEFAULT_TEMPLATE);
    }

    public String createVietQrUrl(String reference, long amountVnd, String description, String template) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Mã tham chiếu thanh toán không được để trống.");
        }
        if (amountVnd <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        String des = (description != null && !description.isBlank()) ? description.trim() : reference.trim();
        String tpl = (template != null && !template.isBlank()) ? template.trim() : DEFAULT_TEMPLATE;

        return SEPAY_QR_BASE_URL
                + "?acc=" + URLEncoder.encode(getAccountNumber(), StandardCharsets.UTF_8)
                + "&bank=" + URLEncoder.encode(getBankCode(), StandardCharsets.UTF_8)
                + "&amount=" + amountVnd
                + "&des=" + URLEncoder.encode(des, StandardCharsets.UTF_8)
                + "&template=" + URLEncoder.encode(tpl, StandardCharsets.UTF_8);
    }

    /**
     * Xác thực Webhook nhận từ SePay.
     * Theo tài liệu SePay: Header "Authorization: Apikey <api_key>" hoặc Bearer token.
     */
    public boolean verifyWebhookKey(String authorizationHeader) {
        String configuredKey = getWebhookKey();
        if (configuredKey.isBlank()) {
            // Chưa cấu hình Webhook Key bí mật; cho phép tiếp nhận trong môi trường phát triển cục bộ
            return true;
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }

        String actual = authorizationHeader.trim();
        if (actual.regionMatches(true, 0, "Apikey ", 0, 7)) {
            actual = actual.substring(7).trim();
        } else if (actual.regionMatches(true, 0, "Bearer ", 0, 7)) {
            actual = actual.substring(7).trim();
        }

        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                configuredKey.trim().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Tra cứu giao dịch thực tế từ SePay qua User API (nếu có cấu hình API Key).
     * Tự động gọi xử lý xác nhận thanh toán nếu tìm thấy giao dịch ngân hàng khớp mã tham chiếu.
     *
     * @param referenceCode Mã thanh toán duy nhất (VD: ECS1013D1012)
     * @param expectedAmount Số tiền yêu cầu
     * @return true nếu ghi nhận thành công từ SePay
     */
    public boolean checkAndRecordPaymentFromApi(String referenceCode, BigDecimal expectedAmount) {
        return checkAndRecordPaymentFromApiBefore(referenceCode, expectedAmount, null);
    }

    /**
     * Queries SePay User API for an incoming transfer matching {@code referenceCode}.
     * When {@code deadline} is provided, only confirms transactions whose bank timestamp
     * is on or before {@code deadline} (Rule 1 / Rule 2 grace safeguard).
     */
    public boolean checkAndRecordPaymentFromApiBefore(String referenceCode, BigDecimal expectedAmount,
                                                      LocalDateTime deadline) {
        String apiKey = getWebhookKey();
        if (apiKey == null || apiKey.isBlank()) return false;
        String accountNumber = getAccountNumber();
        if (accountNumber == null || accountNumber.isBlank()) return false;
        if (referenceCode == null || referenceCode.isBlank()) return false;

        try {
            String urlStr = "https://my.sepay.vn/userapi/transactions/list?account_number="
                    + URLEncoder.encode(accountNumber, StandardCharsets.UTF_8)
                    + "&limit=20";
            URI uri = URI.create(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Apikey " + apiKey.trim());
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int code = conn.getResponseCode();
            if (code != 200) return false;

            try (java.io.InputStream in = conn.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(in);
                JsonNode transactions = root.path("transactions");
                if (!transactions.isArray()) return false;

                String upperRef = referenceCode.toUpperCase(Locale.ROOT);
                for (JsonNode tx : transactions) {
                    String content = tx.path("transaction_content").asText("");
                    long txId = tx.path("id").asLong(0);
                    BigDecimal amountIn = tx.path("amount_in").decimalValue();
                    String gateway = tx.path("bank_brand_name").asText("MBBank");
                    String txDate = tx.path("transaction_date").asText("");

                    if (content.toUpperCase(Locale.ROOT).contains(upperRef) && txId > 0 && amountIn != null && amountIn.signum() > 0) {
                        if (deadline != null) {
                            LocalDateTime parsed = parseTransactionDate(txDate);
                            if (parsed != null && parsed.isAfter(deadline)) {
                                continue;
                            }
                        }
                        BankTransferService bts = new BankTransferService();
                        return bts.processSePaySuccess(txId, gateway, accountNumber, "in", amountIn, content, referenceCode, txDate);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static LocalDateTime parseTransactionDate(String txDate) {
        if (txDate == null || txDate.isBlank()) {
            return null;
        }
        String trimmed = txDate.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (Exception ignored) {
            }
        }
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String configured(String key, String fallback) {
        String value = System.getProperty(key);
        if (value != null) return value.trim();

        value = System.getenv(key);
        if (value != null) return value.trim();

        value = LOCAL_CONFIG.getProperty(key);
        if (value != null) return value.trim();

        return fallback;
    }

    private static Properties loadLocalConfig() {
        Properties props = new Properties();
        try (InputStream in = SePayService.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
        }

        try {
            Path userHomeProps = Path.of(System.getProperty("user.home"), ".electriccarsales", "db.properties");
            if (Files.isRegularFile(userHomeProps)) {
                try (InputStream in = Files.newInputStream(userHomeProps)) {
                    props.load(in);
                }
            }
        } catch (Exception ignored) {
        }
        return props;
    }
}
