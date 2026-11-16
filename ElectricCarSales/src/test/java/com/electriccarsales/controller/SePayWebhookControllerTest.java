package com.electriccarsales.controller;

import com.electriccarsales.service.BankTransferService;
import com.electriccarsales.service.SePayService;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SePayWebhookControllerTest {

    private String h2Url;
    private String prevDbUrl;
    private String prevDbUser;
    private String prevDbPass;
    private String prevBankCode;
    private String prevAccNumber;
    private String prevAccName;
    private String prevWebhookKey;

    @BeforeEach
    void setUp() throws Exception {
        prevDbUrl = System.getProperty("DB_URL");
        prevDbUser = System.getProperty("DB_USER");
        prevDbPass = System.getProperty("DB_PASSWORD");
        prevBankCode = System.getProperty("SEPAY_BANK_CODE");
        prevAccNumber = System.getProperty("SEPAY_ACCOUNT_NUMBER");
        prevAccName = System.getProperty("SEPAY_ACCOUNT_NAME");
        prevWebhookKey = System.getProperty("SEPAY_WEBHOOK_KEY");

        h2Url = "jdbc:h2:mem:sepayWebhookTest" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", h2Url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        System.setProperty("SEPAY_BANK_CODE", "MB");
        System.setProperty("SEPAY_ACCOUNT_NUMBER", "0917231548");
        System.setProperty("SEPAY_ACCOUNT_NAME", "Do Tan Dat");
        System.setProperty("SEPAY_WEBHOOK_KEY", "secret_key_mb_123");

        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement()) {
            TestApplication.initializeSchema(s);
        }
    }

    @AfterEach
    void tearDown() {
        if (prevDbUrl != null) System.setProperty("DB_URL", prevDbUrl); else System.clearProperty("DB_URL");
        if (prevDbUser != null) System.setProperty("DB_USER", prevDbUser); else System.clearProperty("DB_USER");
        if (prevDbPass != null) System.setProperty("DB_PASSWORD", prevDbPass); else System.clearProperty("DB_PASSWORD");
        if (prevBankCode != null) System.setProperty("SEPAY_BANK_CODE", prevBankCode); else System.clearProperty("SEPAY_BANK_CODE");
        if (prevAccNumber != null) System.setProperty("SEPAY_ACCOUNT_NUMBER", prevAccNumber); else System.clearProperty("SEPAY_ACCOUNT_NUMBER");
        if (prevAccName != null) System.setProperty("SEPAY_ACCOUNT_NAME", prevAccName); else System.clearProperty("SEPAY_ACCOUNT_NAME");
        if (prevWebhookKey != null) System.setProperty("SEPAY_WEBHOOK_KEY", prevWebhookKey); else System.clearProperty("SEPAY_WEBHOOK_KEY");
    }

    @Test
    void testUnauthorizedWebhook() throws Exception {
        SePayWebhookController controller = new SePayWebhookController();
        String jsonPayload = "{\"id\":1001,\"transferAmount\":5000000}";

        // 1. Không có header Authorization
        StringWriter sw1 = new StringWriter();
        int[] status1 = new int[]{200};
        HttpServletRequest req1 = mockRequest(jsonPayload, Map.of());
        HttpServletResponse resp1 = mockResponse(sw1, status1);
        controller.doPost(req1, resp1);
        assertEquals(401, status1[0], "Thiếu auth header phải trả về 401 Unauthorized");

        // 2. Header sai API Key
        StringWriter sw2 = new StringWriter();
        int[] status2 = new int[]{200};
        HttpServletRequest req2 = mockRequest(jsonPayload, Map.of("Authorization", "Apikey wrong_secret"));
        HttpServletResponse resp2 = mockResponse(sw2, status2);
        controller.doPost(req2, resp2);
        assertEquals(401, status2[0], "Sai API key phải trả về 401");
    }

    @Test
    void testSePayTestModeAcknowledgement() throws Exception {
        SePayWebhookController controller = new SePayWebhookController();
        String testPayload = "{\"id\":0,\"code\":\"SEPAYTEST\",\"transferAmount\":0}";

        StringWriter sw = new StringWriter();
        int[] status = new int[]{200};
        HttpServletRequest req = mockRequest(testPayload, Map.of("Authorization", "Apikey secret_key_mb_123"));
        HttpServletResponse resp = mockResponse(sw, status);

        controller.doPost(req, resp);
        assertEquals(200, status[0]);
        assertTrue(sw.toString().contains("\"success\":true"));
        assertTrue(sw.toString().contains("acknowledged"));
    }

    @Test
    void testWrongBankAccountRejected() throws Exception {
        SePayWebhookController controller = new SePayWebhookController();
        String payload = """
                {
                    "id": 99991,
                    "gateway": "MBBank",
                    "accountNumber": "0123456789",
                    "transferType": "in",
                    "transferAmount": 300000000,
                    "content": "ECS1D1"
                }
                """;

        StringWriter sw = new StringWriter();
        int[] status = new int[]{200};
        HttpServletRequest req = mockRequest(payload, Map.of("Authorization", "Apikey secret_key_mb_123"));
        HttpServletResponse resp = mockResponse(sw, status);

        controller.doPost(req, resp);
        assertEquals(422, status[0], "Sai tài khoản nhận tiền phải trả về HTTP 422");
        assertTrue(sw.toString().contains("Wrong bank account"));
    }

    @Test
    void testTransferOutIgnoredSafely() throws Exception {
        SePayWebhookController controller = new SePayWebhookController();
        String payload = """
                {
                    "id": 99992,
                    "gateway": "MBBank",
                    "accountNumber": "0917231548",
                    "transferType": "out",
                    "transferAmount": 50000000,
                    "content": "Rut tien mat"
                }
                """;

        StringWriter sw = new StringWriter();
        int[] status = new int[]{200};
        HttpServletRequest req = mockRequest(payload, Map.of("Authorization", "Apikey secret_key_mb_123"));
        HttpServletResponse resp = mockResponse(sw, status);

        controller.doPost(req, resp);
        assertEquals(200, status[0]);
        assertTrue(sw.toString().contains("Ignored transfer out"));
    }

    @Test
    void testValidWebhookDepositFlowAndIdempotency() throws Exception {
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement()) {
            s.execute("""
                    INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                    SELECT 'cust_wh','pass',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                    INSERT INTO Customers(UserID,CustomerName,Phone)
                    SELECT UserID,'Customer Webhook','0988776655' FROM Users WHERE Username='cust_wh';
                    INSERT INTO Brands(BrandName,BrandStatusID)
                    SELECT 'VinFast',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                    INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'VF Series');
                    INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'VF 9');
                    INSERT INTO CarVersions(ModelID,VersionName) VALUES(1,'Plus');
                    INSERT INTO Colors(ColorName) VALUES('Deep Ocean');
                    INSERT INTO CarStatuses(StatusName) VALUES('DEPOSITED'),('AVAILABLE'),('SOLD'),('DELIVERED');
                    INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                    VALUES('VF9P01',1,1,1,2,2026,'VIN0000000000099','ENG00099',1500000000);
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT 1,os.OrderStatusID,pm.PaymentMethodID,'2026-11-16',1500000000,450000000,1050000000
                    FROM OrderStatuses os CROSS JOIN PaymentMethods pm WHERE os.StatusName='PENDING' AND pm.MethodName='BANK_TRANSFER';
                    INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(1,1,1500000000);
                    """);

            BankTransferService bankService = new BankTransferService();
            BankTransferService.Transfer transfer = bankService.createForCustomer(1, 1, "DEPOSIT", "BANK_TRANSFER");
            assertNotNull(transfer);
            String ref = transfer.reference();

            SePayWebhookController controller = new SePayWebhookController();
            String payload = String.format("""
                    {
                        "id": 888123,
                        "gateway": "MBBank",
                        "accountNumber": "0917231548",
                        "transactionDate": "2026-11-16 11:00:00",
                        "transferType": "in",
                        "transferAmount": 450000000,
                        "content": "Chuyen khoan coc xe %s xin cam on",
                        "referenceCode": "%s"
                    }
                    """, ref, ref);

            // Lần 1: Xử lý thành công
            StringWriter sw1 = new StringWriter();
            int[] status1 = new int[]{200};
            HttpServletRequest req1 = mockRequest(payload, Map.of("Authorization", "Apikey secret_key_mb_123"));
            HttpServletResponse resp1 = mockResponse(sw1, status1);

            controller.doPost(req1, resp1);
            assertEquals(200, status1[0]);
            assertTrue(sw1.toString().contains("\"success\":true"));
            assertTrue(sw1.toString().contains("Payment confirmed"));

            // Kiểm tra trạng thái DB
            try (var rs = s.executeQuery("SELECT ps.StatusID FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID WHERE p.PaymentID=" + transfer.paymentId())) {
                assertTrue(rs.next());
                assertEquals("COMPLETED", rs.getString("StatusID"));
            }

            // Lần 2: Gửi lại cùng webhook (Idempotency)
            StringWriter sw2 = new StringWriter();
            int[] status2 = new int[]{200};
            HttpServletRequest req2 = mockRequest(payload, Map.of("Authorization", "Apikey secret_key_mb_123"));
            HttpServletResponse resp2 = mockResponse(sw2, status2);

            controller.doPost(req2, resp2);
            assertEquals(200, status2[0]);
            assertTrue(sw2.toString().contains("\"success\":true"));
        }
    }

    private static HttpServletRequest mockRequest(String bodyJson, Map<String, String> headers) {
        byte[] bytes = bodyJson.getBytes(StandardCharsets.UTF_8);
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getHeader".equals(name) && args != null && args.length > 0) {
                        return headers.get((String) args[0]);
                    }
                    if ("getInputStream".equals(name)) {
                        return new SimpleServletInputStream(new ByteArrayInputStream(bytes));
                    }
                    if ("getContentType".equals(name)) {
                        return "application/json";
                    }
                    return null;
                }
        );
    }

    private static HttpServletResponse mockResponse(StringWriter stringWriter, int[] statusHolder) {
        PrintWriter pw = new PrintWriter(stringWriter);
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getWriter".equals(name)) {
                        return pw;
                    }
                    if ("setStatus".equals(name) && args != null && args.length > 0) {
                        statusHolder[0] = (Integer) args[0];
                        return null;
                    }
                    if ("setContentType".equals(name) || "setHeader".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );
    }

    private static class SimpleServletInputStream extends ServletInputStream {
        private final InputStream source;
        SimpleServletInputStream(InputStream source) { this.source = source; }
        @Override public boolean isFinished() { return false; }
        @Override public boolean isReady() { return true; }
        @Override public void setReadListener(ReadListener readListener) {}
        @Override public int read() throws IOException { return source.read(); }
        @Override public int read(byte[] b, int off, int len) throws IOException { return source.read(b, off, len); }
    }
}
