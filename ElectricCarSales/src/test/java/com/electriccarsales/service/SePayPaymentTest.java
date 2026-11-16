package com.electriccarsales.service;

import com.electriccarsales.controller.TestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SePayPaymentTest {

    private String h2Url;
    private String prevDbUrl;
    private String prevDbUser;
    private String prevDbPass;
    private String prevBankCode;
    private String prevAccNumber;
    private String prevAccName;
    private String prevWebhookKey;

    @BeforeEach
    void setUp() {
        prevDbUrl = System.getProperty("DB_URL");
        prevDbUser = System.getProperty("DB_USER");
        prevDbPass = System.getProperty("DB_PASSWORD");
        prevBankCode = System.getProperty("SEPAY_BANK_CODE");
        prevAccNumber = System.getProperty("SEPAY_ACCOUNT_NUMBER");
        prevAccName = System.getProperty("SEPAY_ACCOUNT_NAME");
        prevWebhookKey = System.getProperty("SEPAY_WEBHOOK_KEY");

        h2Url = "jdbc:h2:mem:sepayTest" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", h2Url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        System.setProperty("SEPAY_BANK_CODE", "MB");
        System.setProperty("SEPAY_ACCOUNT_NUMBER", "0917231548");
        System.setProperty("SEPAY_ACCOUNT_NAME", "Do Tan Dat");
        System.setProperty("SEPAY_WEBHOOK_KEY", "test_sepay_api_secret_key_123");
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
    void testSePayServiceConfigurationAndReadiness() {
        assertTrue(SePayService.isReady(), "SePayService phải READY khi có đủ ngân hàng và số tài khoản MB");
        assertEquals("READY", SePayService.configStatus());
        assertEquals("MB", SePayService.getBankCode());
        assertEquals("0917231548", SePayService.getAccountNumber());
        assertEquals("Do Tan Dat", SePayService.getAccountName());
        assertTrue(SePayService.getBankName().contains("MB Bank"));

        // Kiểm tra khi thiếu cấu hình
        System.setProperty("SEPAY_ACCOUNT_NUMBER", "");
        assertFalse(SePayService.isReady());
        assertTrue(SePayService.configStatus().contains("Thiếu"));

        System.setProperty("SEPAY_ACCOUNT_NUMBER", "0917231548");
    }

    @Test
    void testSePayDynamicVietQrGeneration() {
        SePayService service = new SePayService();
        String ref = "ECS10D5";
        long amount = 300_000_000L;
        String qrUrl = service.createVietQrUrl(ref, amount, ref);

        assertNotNull(qrUrl);
        assertTrue(qrUrl.startsWith("https://qr.sepay.vn/img"), "URL phải bắt đầu bằng endpoint VietQR SePay");
        assertTrue(qrUrl.contains("acc=0917231548"), "Phải chứa số tài khoản MB 0917231548");
        assertTrue(qrUrl.contains("bank=MB"), "Phải chứa mã ngân hàng MB");
        assertTrue(qrUrl.contains("amount=300000000"), "Phải chứa số tiền VND nguyên");
        assertTrue(qrUrl.contains("des=ECS10D5"), "Phải chứa mã nội dung thanh toán");
        assertTrue(qrUrl.contains("template=compact"), "Phải chứa template compact");
    }

    @Test
    void testWebhookAuthorizationVerification() {
        SePayService service = new SePayService();

        // 1. Header đúng: Apikey <key>
        assertTrue(service.verifyWebhookKey("Apikey test_sepay_api_secret_key_123"));
        assertTrue(service.verifyWebhookKey("Bearer test_sepay_api_secret_key_123"));
        assertTrue(service.verifyWebhookKey("test_sepay_api_secret_key_123"));

        // 2. Header sai hoặc rỗng
        assertFalse(service.verifyWebhookKey("Apikey wrong_key_999"));
        assertFalse(service.verifyWebhookKey(""));
        assertFalse(service.verifyWebhookKey(null));
    }

    @Test
    void testSePayEvidenceParsing() {
        // 1. Chuỗi SePay chuẩn mới
        String note = "SEPAY_ID=92704; SEPAY_GATEWAY=MBBank; SEPAY_REF=ECS10D5; SEPAY_DATE=2026-11-16 10:30:00; SEPAY_AMOUNT=300000000.00; SEPAY_CONTENT=ECS10D5 CHUYEN TIEN; AT=2026-11-16T10:30:00";
        SePayEvidence evidence = SePayEvidence.fromNote(note);
        assertNotNull(evidence);
        assertEquals("92704", evidence.getEventId());
        assertEquals("MBBank", evidence.getGateway());
        assertEquals("2026-11-16 10:30:00", evidence.getPayDate());
        assertEquals(new BigDecimal("300000000.00"), evidence.getReceived());
        assertEquals("ECS10D5", evidence.getBankReference());

        // 2. Chuỗi SePay cũ
        String legacyNote = "SEPAY_ID=88123; SEPAY_REF=FT9988; SEPAY_DATE=20261010; SEPAY_RECEIVED=150000000.00; AT=2026-10-10T10:00:00";
        SePayEvidence legacy = SePayEvidence.fromNote(legacyNote);
        assertNotNull(legacy);
        assertEquals("88123", legacy.getEventId());
        assertEquals(new BigDecimal("150000000.00"), legacy.getReceived());

        // 3. Chuỗi rỗng
        assertNull(SePayEvidence.fromNote(null));
        assertNull(SePayEvidence.fromNote(""));
    }

    @Test
    void testExtractPaymentReference() {
        assertEquals("ECS1D1", BankTransferService.extractPaymentReference("Chuyen tien coc xe ECS1D1 xin cam on", null));
        assertEquals("ECS10R5", BankTransferService.extractPaymentReference("MBVCB.12345.ECS10R5", "ECS10R5"));
        assertEquals("ECS99D99", BankTransferService.extractPaymentReference("Thanh toan ecs99d99", null));
        assertNull(BankTransferService.extractPaymentReference("No payment code here", null));
    }

    @Test
    void testDatabaseSePayDepositFlow() throws Exception {
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement()) {
            TestApplication.initializeSchema(s);

            s.execute("""
                    INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                    SELECT 'customer_sepay','pass',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                    INSERT INTO Customers(UserID,CustomerName,Phone)
                    SELECT UserID,'Do Tan Dat Customer','0917231548' FROM Users WHERE Username='customer_sepay';
                    INSERT INTO Brands(BrandName,BrandStatusID)
                    SELECT 'VinFast',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                    INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'VF Series');
                    INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'VF 8');
                    INSERT INTO CarVersions(ModelID,VersionName) VALUES(1,'Plus');
                    INSERT INTO Colors(ColorName) VALUES('Crimson Red');
                    INSERT INTO CarStatuses(StatusName) VALUES('DEPOSITED'),('AVAILABLE'),('SOLD'),('DELIVERED');
                    INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                    VALUES('VF8P01',1,1,1,2,2026,'VIN0000000000001','ENG00001',1000000000);
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT 1,os.OrderStatusID,pm.PaymentMethodID,'2026-11-16',1000000000,300000000,700000000
                    FROM OrderStatuses os CROSS JOIN PaymentMethods pm WHERE os.StatusName='PENDING' AND pm.MethodName='BANK_TRANSFER';
                    INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(1,1,1000000000);
                    """);

            BankTransferService bankService = new BankTransferService();

            // 1. Tạo yêu cầu đặt cọc 30% qua BANK_TRANSFER (SePay MB Bank)
            BankTransferService.Transfer transfer = bankService.createForCustomer(1, 1, "DEPOSIT", "BANK_TRANSFER");
            assertNotNull(transfer);
            assertEquals("PENDING", transfer.status());
            assertEquals("BANK_TRANSFER", transfer.methodName());
            assertEquals(new BigDecimal("300000000"), transfer.amount());
            assertEquals("0917231548", transfer.accountNumber());
            assertEquals("Do Tan Dat", transfer.accountName());
            assertEquals("ECS1D" + transfer.paymentId(), transfer.reference());
            assertNotNull(transfer.paymentUrl());
            assertTrue(transfer.paymentUrl().contains("acc=0917231548"));
            assertTrue(transfer.paymentUrl().contains("des=ECS1D" + transfer.paymentId()));

            // Regression: live incident 100114213 had only PENDING in PaymentStatuses.
            s.executeUpdate("DELETE FROM PaymentStatuses WHERE StatusID='COMPLETED'");

            // 2. Xử lý webhook từ SePay khi khách chuyển khoản thành công
            boolean success = bankService.processSePaySuccess(
                    92704L,
                    "MBBank",
                    "0917231548",
                    "in",
                    new BigDecimal("300000000"),
                    "Chuyen tien coc don " + transfer.reference(),
                    transfer.reference(),
                    "2026-11-16 10:35:00");
            assertTrue(success, "Giao dịch SePay phải được ghi nhận thành công");

            // Kiểm tra trạng thái Payment -> COMPLETED
            try (var rs = s.executeQuery("SELECT ps.StatusID, p.Note FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID WHERE p.PaymentID=" + transfer.paymentId())) {
                assertTrue(rs.next());
                assertEquals("COMPLETED", rs.getString("StatusID"));
                assertTrue(rs.getString("Note").contains("SEPAY_ID=92704;"));
                assertTrue(rs.getString("Note").contains("SEPAY_REF=" + transfer.reference() + ";"));
            }

            // Kiểm tra trạng thái Car -> DEPOSITED (tồn kho khả dụng giảm 1 xe)
            try (var rs = s.executeQuery("SELECT cs.StatusName FROM Cars c JOIN CarStatuses cs ON cs.CarStatusID=c.CarStatusID WHERE c.CarID=1")) {
                assertTrue(rs.next());
                assertEquals("DEPOSITED", rs.getString("StatusName"));
            }

            // Kiểm tra trạng thái Order -> DEPOSITED
            try (var rs = s.executeQuery("SELECT os.StatusName FROM Orders o JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID WHERE o.OrderID=1")) {
                assertTrue(rs.next());
                assertEquals("DEPOSITED", rs.getString("StatusName"));
            }

            // 3. Kiểm tra tính Idempotent: Webhook gửi lại cùng SePay ID và cùng số tiền
            boolean replayed = bankService.processSePaySuccess(
                    92704L,
                    "MBBank",
                    "0917231548",
                    "in",
                    new BigDecimal("300000000"),
                    "Chuyen tien coc don " + transfer.reference(),
                    transfer.reference(),
                    "2026-11-16 10:35:00");
            assertTrue(replayed, "Gửi lại cùng webhook SePay phải thành công an toàn (idempotent)");
            try (var rs = s.executeQuery("SELECT COUNT(*) FROM PaymentStatuses WHERE StatusID='COMPLETED'")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Replay must not create duplicate status rows");
            }
            try (var rs = s.executeQuery("SELECT COUNT(*) FROM OrderStatusHistory WHERE OrderID=1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Replay must not duplicate the deposit transition");
            }


            // 4. Kiểm tra sai số tiền: ném ngoại lệ
            s.execute("""
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT 1,os.OrderStatusID,pm.PaymentMethodID,'2026-11-16',1000000000,300000000,700000000
                    FROM OrderStatuses os CROSS JOIN PaymentMethods pm WHERE os.StatusName='PENDING' AND pm.MethodName='BANK_TRANSFER';
                    INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                    VALUES('VF8P02',1,1,1,2,2026,'VIN0000000000002','ENG00002',1000000000);
                    INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(2,2,1000000000);
                    """);
            BankTransferService.Transfer transfer2 = bankService.createForCustomer(2, 1, "DEPOSIT", "BANK_TRANSFER");
            assertThrows(IllegalStateException.class, () -> {
                bankService.processSePaySuccess(
                        92705L,
                        "MBBank",
                        "0917231548",
                        "in",
                        new BigDecimal("200000000"), // Sai số tiền (thiếu 100tr)
                        "Coc don " + transfer2.reference(),
                        transfer2.reference(),
                        "2026-11-16 10:40:00");
            });

            // 5. Kiểm tra sai tài khoản nhận: ném ngoại lệ
            assertThrows(IllegalArgumentException.class, () -> {
                bankService.processSePaySuccess(
                        92706L,
                        "MBBank",
                        "9999999999", // Sai số tài khoản
                        "in",
                        new BigDecimal("300000000"),
                        "Coc don " + transfer2.reference(),
                        transfer2.reference(),
                        "2026-11-16 10:40:00");
            });

            // 6. Kiểm tra giao dịch tiền ra: trả false, không ghi nhận
            boolean outSuccess = bankService.processSePaySuccess(
                    92707L,
                    "MBBank",
                    "0917231548",
                    "out", // Tiền ra
                    new BigDecimal("300000000"),
                    "Rut tien " + transfer2.reference(),
                    transfer2.reference(),
                    "2026-11-16 10:40:00");
            assertFalse(outSuccess);

            // 7. Kiểm tra tự động hoàn tất giao dịch khi ngân hàng đã ghi nhận (checkAndAutoCompleteTransfer)
            s.execute("UPDATE Payments SET Note = Note + ' SEPAY_ID=99999; GATEWAY=MBBank;' WHERE PaymentID=" + transfer2.paymentId());
            BankTransferService.Transfer autoCompleted = bankService.checkAndAutoCompleteTransfer(transfer2.paymentId(), 1);
            assertNotNull(autoCompleted);
            assertEquals("COMPLETED", autoCompleted.status(), "Giao dịch ngân hàng đã ghi nhận phải tự động chuyển COMPLETED");

            try (var rs = s.executeQuery("SELECT os.StatusName FROM Orders o JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID WHERE o.OrderID=2")) {
                assertTrue(rs.next());
                assertEquals("DEPOSITED", rs.getString("StatusName"), "Đơn hàng phải tự động cập nhật sang DEPOSITED");
            }
        }
    }
}
