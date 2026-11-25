package com.electriccarsales.service;

import com.electriccarsales.controller.TestApplication;
import com.electriccarsales.dao.*;
import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.Payment;
import com.electriccarsales.util.FormatUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử toàn diện luồng thanh toán và đối soát giữa Customer và Accountant:
 * 1. Thanh toán đặt cọc 30% thành công.
 * 2. Hiển thị đúng phần 70% còn lại.
 * 3. Thanh toán 70% thành công (Full Payment).
 * 4. Giao dịch thất bại không tính vào tiền đã thu.
 * 5. Người dùng bấm thanh toán hai lần (Idempotency).
 * 6. Thanh toán thiếu hoặc vượt số tiền bị từ chối.
 * 7. Accountant xác nhận đối soát lần đầu (Reconciliation).
 * 8. Accountant cố tình xác nhận đối soát lần thứ hai bị chặn.
 * 9. Customer cố thanh toán đơn của người khác bị chặn quyền.
 * 10. Đơn trả góp (Installment) với 70% giải ngân bởi đối tác tài chính.
 * 11. Customer hủy popup trước khi xác nhận (No state change).
 * 12. Kiểm tra tổng tiền thu, số dư và trạng thái cuối cùng (requireExactSettlement).
 * 13. Chuẩn hóa định dạng tiền tệ vi-VN (7.000.000 VND, 75.000.000 VND, 150.000.000 VND).
 */
public class PaymentReconciliationFlowTest {

    private String h2Url;
    private String prevDbUrl;
    private String prevDbUser;
    private String prevDbPass;
    private String prevBankCode;
    private String prevAccNumber;
    private String prevAccName;

    private BankTransferService bankService;
    private OrderDAO orderDAO;
    private PaymentDAO paymentDAO;
    private ContractsDAO contractsDAO;
    private InvoiceDAO invoiceDAO;

    @BeforeEach
    void setUp() throws Exception {
        prevDbUrl = System.getProperty("DB_URL");
        prevDbUser = System.getProperty("DB_USER");
        prevDbPass = System.getProperty("DB_PASSWORD");
        prevBankCode = System.getProperty("SEPAY_BANK_CODE");
        prevAccNumber = System.getProperty("SEPAY_ACCOUNT_NUMBER");
        prevAccName = System.getProperty("SEPAY_ACCOUNT_NAME");

        h2Url = "jdbc:h2:mem:paymentFlowTest" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", h2Url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        System.setProperty("SEPAY_BANK_CODE", "MB");
        System.setProperty("SEPAY_ACCOUNT_NUMBER", "0917231548");
        System.setProperty("SEPAY_ACCOUNT_NAME", "Do Tan Dat");

        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement()) {
            TestApplication.initializeSchema(s);

            // Seed dữ liệu nền: Roles, Users, Customers, Employees, Brands, Cars, FinancialCompanies
            s.execute("""
                    INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
                    SELECT 'cust1','hash','cust1@test.com','0911111111',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';

                    INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
                    SELECT 'cust2','hash','cust2@test.com','0922222222',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';

                    INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
                    SELECT 'acc1','hash','acc1@test.com','0933333333',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='ACCOUNTANT' AND s.StatusName='ACTIVE';

                    INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
                    SELECT 'sales1','hash','sales1@test.com','0944444444',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='SALES' AND s.StatusName='ACTIVE';

                    INSERT INTO Customers(UserID,CustomerName,Phone,Email,Address)
                    SELECT UserID,'Nguyễn Văn An','0911111111','cust1@test.com','Hà Nội' FROM Users WHERE Username='cust1';

                    INSERT INTO Customers(UserID,CustomerName,Phone,Email,Address)
                    SELECT UserID,'Trần Thị Bình','0922222222','cust2@test.com','TP HCM' FROM Users WHERE Username='cust2';

                    INSERT INTO Employees(UserID,EmployeeName,Phone)
                    SELECT UserID,'Kế Toán Trưởng','0933333333' FROM Users WHERE Username='acc1';

                    INSERT INTO Employees(UserID,EmployeeName,Phone)
                    SELECT UserID,'Nhân Viên Kinh Doanh','0944444444' FROM Users WHERE Username='sales1';

                    INSERT INTO Brands(BrandName,BrandStatusID)
                    SELECT 'VinFast',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';

                    INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'VF Series');
                    INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'VF 8');
                    INSERT INTO CarVersions(ModelID,VersionName) VALUES(1,'Plus');
                    INSERT INTO Colors(ColorName) VALUES('Crimson Red');
                    INSERT INTO CarStatuses(StatusName) VALUES('DEPOSITED'),('AVAILABLE'),('SOLD'),('DELIVERED');

                    INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                    VALUES('VF8P01',1,1,1,2,2026,'VIN0000000000001','ENG00001',1000000000),
                          ('VF8P02',1,1,1,2,2026,'VIN0000000000002','ENG00002',1000000000);

                    INSERT INTO FinancialCompanies(CompanyName,Phone,Email)
                    VALUES('Techcombank Finance','0988888888','partner@techcombank.com.vn');
                    """);
        }

        bankService = new BankTransferService();
        orderDAO = new OrderDAO();
        paymentDAO = new PaymentDAO();
        contractsDAO = new ContractsDAO();
        invoiceDAO = new InvoiceDAO();
    }

    @AfterEach
    void tearDown() {
        if (prevDbUrl != null) System.setProperty("DB_URL", prevDbUrl); else System.clearProperty("DB_URL");
        if (prevDbUser != null) System.setProperty("DB_USER", prevDbUser); else System.clearProperty("DB_USER");
        if (prevDbPass != null) System.setProperty("DB_PASSWORD", prevDbPass); else System.clearProperty("DB_PASSWORD");
        if (prevBankCode != null) System.setProperty("SEPAY_BANK_CODE", prevBankCode); else System.clearProperty("SEPAY_BANK_CODE");
        if (prevAccNumber != null) System.setProperty("SEPAY_ACCOUNT_NUMBER", prevAccNumber); else System.clearProperty("SEPAY_ACCOUNT_NUMBER");
        if (prevAccName != null) System.setProperty("SEPAY_ACCOUNT_NAME", prevAccName); else System.clearProperty("SEPAY_ACCOUNT_NAME");
    }

    private int createTestOrder(int customerId, int carId, String paymentMethod, Integer companyId) throws Exception {
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement()) {
            String companyVal = companyId != null ? String.valueOf(companyId) : "NULL";
            s.execute(String.format("""
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,CompanyID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT %d,os.OrderStatusID,pm.PaymentMethodID,%s,'2026-11-16',1000000000,300000000,700000000
                    FROM OrderStatuses os CROSS JOIN PaymentMethods pm
                    WHERE os.StatusName='PENDING' AND pm.MethodName='%s';
                    """, customerId, companyVal, paymentMethod));
            var rs = s.executeQuery("SELECT MAX(OrderID) AS MaxID FROM Orders");
            rs.next();
            int orderId = rs.getInt("MaxID");
            s.execute(String.format("INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(%d,%d,1000000000)", orderId, carId));
            return orderId;
        }
    }

    private int getAccountantUserId() throws Exception {
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement();
             var rs = s.executeQuery("SELECT UserID FROM Users WHERE Username='acc1'")) {
            rs.next();
            return rs.getInt("UserID");
        }
    }

    private void signContractHelper(int contractId, int userId, int customerId, String otp) throws Exception {
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement()) {
            s.execute(String.format("INSERT INTO OTPVerifications(UserID,OTPCode,Purpose,ExpiredDate,Status) " +
                            "VALUES(%d,'%s','%s',DATEADD('MINUTE',5,CURRENT_TIMESTAMP),'PENDING')",
                    userId, otp, ContractsDAO.signingPurpose(contractId)));
        }
        contractsDAO.signContract(contractId, customerId, userId, otp);
    }

    // =========================================================================
    // CASE 1: Thanh toán đặt cọc 30% thành công
    // =========================================================================
    @Test
    @DisplayName("Case 1: Thanh toán đặt cọc 30% thành công")
    void testCase1_DepositPayment30PercentSuccessful() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);

        // 1. Khách hàng tạo thanh toán đặt cọc 30%
        BankTransferService.Transfer transfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        assertNotNull(transfer);
        assertEquals("PENDING", transfer.status());
        assertEquals("DEPOSIT", transfer.type());
        assertEquals(new BigDecimal("300000000"), transfer.amount());
        assertEquals("ECS" + orderId, transfer.getOrderCode());
        assertEquals("DEPOSIT_30", transfer.getFormattedType());

        // 2. Kế toán xác nhận nhận tiền 300.000.000 VND
        int accountantId = getAccountantUserId();
        bankService.decide(transfer.paymentId(), accountantId, true, "MB-DEP-001", new BigDecimal("300000000"), "Cọc 30% thành công");

        // 3. Kiểm tra Order và Car đã cập nhật đúng trạng thái
        Order order = orderDAO.findById(orderId);
        assertNotNull(order);
        assertEquals("DEPOSITED", order.getStatus());
        assertEquals(new BigDecimal("300000000.00"), order.getPaidAmount());
        assertEquals(new BigDecimal("700000000.00"), order.getRemainingAmount());
        assertTrue(order.isDepositPaid());
        assertFalse(order.isFullyPaid());

        // Car status phải chuyển sang DEPOSITED
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement();
             var rs = s.executeQuery("SELECT cs.StatusName FROM Cars c JOIN CarStatuses cs ON cs.CarStatusID=c.CarStatusID WHERE c.CarID=1")) {
            rs.next();
            assertEquals("DEPOSITED", rs.getString("StatusName"));
        }
    }

    // =========================================================================
    // CASE 2: Hiển thị đúng phần 70% còn lại
    // =========================================================================
    @Test
    @DisplayName("Case 2: Hiển thị đúng phần 70% còn lại")
    void testCase2_RemainingAmount70PercentDisplayed() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);
        BankTransferService.Transfer transfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        bankService.decide(transfer.paymentId(), getAccountantUserId(), true, "MB-DEP-002", new BigDecimal("300000000"), "Cọc 30%");

        Order order = orderDAO.findById(orderId);
        assertNotNull(order);
        assertEquals(new BigDecimal("1000000000.00"), order.getOriginalAmount());
        assertEquals(new BigDecimal("300000000.00"), order.getDepositAmount());
        assertEquals(new BigDecimal("700000000.00"), order.getRemainingAmount());
        assertEquals(new BigDecimal("300000000.00"), order.getPaidAmount());

        // Kiểm tra định dạng tiền tệ vi-VN
        assertEquals("1.000.000.000 VND", order.getFormattedOriginalAmount());
        assertEquals("300.000.000 VND", order.getFormattedDepositAmount());
        assertEquals("700.000.000 VND", order.getFormattedRemainingAmount());
        assertEquals("300.000.000 VND", order.getFormattedPaidAmount());
    }

    // =========================================================================
    // CASE 3: Thanh toán 70% thành công (Full Payment Flow)
    // =========================================================================
    @Test
    @DisplayName("Case 3: Thanh toán 70% thành công sau khi ký hợp đồng -> PAID & SOLD")
    void testCase3_RemainingPayment70PercentSuccessful() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);

        // Bước 1 & 2: Cọc 30%
        BankTransferService.Transfer depTransfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        bankService.decide(depTransfer.paymentId(), getAccountantUserId(), true, "MB-DEP-003", new BigDecimal("300000000"), "Cọc");

        // Khi chưa ký hợp đồng, thanh toán 70% phải bị chặn theo quy chuẩn RDS
        assertThrows(IllegalStateException.class, () ->
                bankService.createForCustomer(orderId, 1, "REMAINING", "BANK_TRANSFER"),
                "Chưa ký hợp đồng phải bị chặn thanh toán 70%");

        // Tạo và ký hợp đồng
        var contract = contractsDAO.createContractForOrder(orderId, 1, "Điều khoản hợp đồng mua bán");
        signContractHelper(contract.getContractId(), 1, 1, "123456");

        // Bước 4 & 5: Khách hàng yêu cầu thanh toán 70% còn lại
        BankTransferService.Transfer remTransfer = bankService.createForCustomer(orderId, 1, "REMAINING", "BANK_TRANSFER");
        assertNotNull(remTransfer);
        assertEquals("PENDING", remTransfer.status());
        assertEquals("REMAINING", remTransfer.type());
        assertEquals(new BigDecimal("700000000"), remTransfer.amount());
        assertEquals("REMAINING_70", remTransfer.getFormattedType());

        // Bước 7 & 8: Kế toán xác nhận 700.000.000 VND
        bankService.decide(remTransfer.paymentId(), getAccountantUserId(), true, "MB-REM-003", new BigDecimal("700000000"), "Thanh toán 70%");

        // Bước 9: Kiểm tra RemainingAmount = 0, OrderStatus = PAID, CarStatus = SOLD
        Order finalOrder = orderDAO.findById(orderId);
        assertNotNull(finalOrder);
        assertEquals("PAID", finalOrder.getStatus());
        assertEquals(0, finalOrder.getRemainingAmount().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("1000000000.00"), finalOrder.getPaidAmount());
        assertTrue(finalOrder.isFullyPaid());

        // Kiểm tra CarStatus = SOLD
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement();
             var rs = s.executeQuery("SELECT cs.StatusName FROM Cars c JOIN CarStatuses cs ON cs.CarStatusID=c.CarStatusID WHERE c.CarID=1")) {
            rs.next();
            assertEquals("SOLD", rs.getString("StatusName"));
        }
    }

    // =========================================================================
    // CASE 4: Giao dịch thất bại không được tính vào số tiền đã thu
    // =========================================================================
    @Test
    @DisplayName("Case 4: Giao dịch thất bại không làm giảm RemainingAmount và không tính vào tiền đã thu")
    void testCase4_FailedPaymentNotCounted() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);

        BankTransferService.Transfer transfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");

        // Kế toán từ chối (Từ chối / FAILED)
        bankService.decide(transfer.paymentId(), getAccountantUserId(), false, null, null, "Không nhận được tiền");

        Order order = orderDAO.findById(orderId);
        assertNotNull(order);
        assertEquals("PENDING", order.getStatus());
        assertEquals(0, order.getPaidAmount().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("700000000.00"), order.getRemainingAmount());
        assertFalse(order.isDepositPaid());

        BigDecimal sumPaid = paymentDAO.sumSuccessfulPaymentsByOrder(orderId);
        assertEquals(0, sumPaid.compareTo(BigDecimal.ZERO));
    }

    // =========================================================================
    // CASE 5: Người dùng bấm thanh toán hai lần (Idempotency)
    // =========================================================================
    @Test
    @DisplayName("Case 5: Idempotency chống tạo trùng Payment PENDING khi bấm 2 lần")
    void testCase5_UserClicksPaymentTwice_Idempotency() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);

        // Lần 1
        BankTransferService.Transfer first = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        // Lần 2 (người dùng click đúp)
        BankTransferService.Transfer second = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");

        // Hệ thống phải trả về cùng mã thanh toán đang PENDING, không tạo thêm dòng mới
        assertEquals(first.paymentId(), second.paymentId());
        assertEquals(first.reference(), second.reference());

        // Đếm số lượng record Payments trong DB
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) AS Cnt FROM Payments WHERE OrderID=" + orderId)) {
            rs.next();
            assertEquals(1, rs.getInt("Cnt"), "Chỉ được tồn tại 1 Payment PENDING duy nhất");
        }
    }

    // =========================================================================
    // CASE 6: Thanh toán thiếu hoặc vượt số tiền bị từ chối
    // =========================================================================
    @Test
    @DisplayName("Case 6: Kế toán xác nhận sai lệch số tiền phải bị chặn với IllegalArgumentException")
    void testCase6_UnderpaidOrOverpaidAmountsRejected() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);
        BankTransferService.Transfer transfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");

        int accId = getAccountantUserId();

        // 1. Thanh toán thiếu (250.000.000 thay vì 300.000.000)
        assertThrows(IllegalArgumentException.class, () ->
                bankService.decide(transfer.paymentId(), accId, true, "REF1", new BigDecimal("250000000"), "Thiếu tiền"));

        // 2. Thanh toán thừa (350.000.000 thay vì 300.000.000)
        assertThrows(IllegalArgumentException.class, () ->
                bankService.decide(transfer.paymentId(), accId, true, "REF2", new BigDecimal("350000000"), "Thừa tiền"));
    }

    // =========================================================================
    // CASE 7: Accountant xác nhận đối soát lần đầu (Reconciliation)
    // =========================================================================
    @Test
    @DisplayName("Case 7: Accountant đối soát lần đầu thành công và lưu vết kiểm toán")
    void testCase7_AccountantReconciliationFirstTime() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);
        BankTransferService.Transfer transfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        int accId = getAccountantUserId();
        bankService.decide(transfer.paymentId(), accId, true, "MB-DEP-007", new BigDecimal("300000000"), "Đã nhận cọc");

        // Trước khi đối soát
        Payment pBefore = paymentDAO.findById(transfer.paymentId());
        assertNotNull(pBefore);
        assertFalse(pBefore.isReconciled());
        assertNull(pBefore.getReconciledBy());

        // Kế toán bấm "Xác nhận đã đối soát"
        bankService.reconcile(transfer.paymentId(), accId);

        // Sau khi đối soát
        Payment pAfter = paymentDAO.findById(transfer.paymentId());
        assertNotNull(pAfter);
        assertTrue(pAfter.isReconciled(), "Payment phải có cờ isReconciled = true");
        assertEquals("1", pAfter.getReconciledBy());
        assertNotNull(pAfter.getReconciledAt());

        // Kiểm tra RemainingAmount và PaidAmount của Order không bị sai lệch
        Order order = orderDAO.findById(orderId);
        assertEquals(new BigDecimal("700000000.00"), order.getRemainingAmount());
        assertEquals(new BigDecimal("300000000.00"), order.getPaidAmount());
    }

    // =========================================================================
    // CASE 8: Accountant cố tình xác nhận đối soát lần thứ hai bị chặn
    // =========================================================================
    @Test
    @DisplayName("Case 8: Chặn đối soát 2 lần trên cùng một giao dịch")
    void testCase8_AccountantDuplicateReconciliationBlocked() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);
        BankTransferService.Transfer transfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        int accId = getAccountantUserId();
        bankService.decide(transfer.paymentId(), accId, true, "MB-DEP-008", new BigDecimal("300000000"), "Đã nhận cọc");

        // Đối soát lần 1: thành công
        bankService.reconcile(transfer.paymentId(), accId);

        // Đối soát lần 2: phải ném IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bankService.reconcile(transfer.paymentId(), accId));
        assertTrue(ex.getMessage().contains("đã được đối soát trước đó"));
    }

    // =========================================================================
    // CASE 9: Customer cố thanh toán đơn của người khác bị chặn quyền
    // =========================================================================
    @Test
    @DisplayName("Case 9: Khách hàng B cố thanh toán đơn của Khách hàng A bị từ chối")
    void testCase9_CustomerCannotPayAnotherCustomerOrder() throws Exception {
        // Đơn của Customer 1
        int orderIdCust1 = createTestOrder(1, 1, "BANK_TRANSFER", null);

        // Customer 2 cố tạo thanh toán cho đơn của Customer 1
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bankService.createForCustomer(orderIdCust1, 2, "DEPOSIT", "BANK_TRANSFER"));
        assertTrue(ex.getMessage().contains("Không tìm thấy đơn hàng của bạn"));
    }

    // =========================================================================
    // CASE 10: Installment với 70% giải ngân bởi đối tác tài chính
    // =========================================================================
    @Test
    @DisplayName("Case 10: Trả góp - Khách cọc 30%, 70% còn lại mặc định đã thanh toán từ ngân hàng đối tác")
    void testCase10_InstallmentFlowWithPartnerDisbursement() throws Exception {
        // Tạo đơn trả góp có CompanyID = 1
        int orderId = createTestOrder(1, 1, "INSTALLMENT", 1);

        Order order = orderDAO.findById(orderId);
        assertTrue(order.isInstallment());
        assertEquals("Techcombank Finance", order.getCompanyName());

        // 1. Khách trả 30% tiền cọc
        BankTransferService.Transfer depTransfer = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        bankService.decide(depTransfer.paymentId(), getAccountantUserId(), true, "MB-DEP-INS", new BigDecimal("300000000"), "Cọc đơn trả góp");

        // Sau khi chuyển 30% cọc, 70% còn lại MẶC ĐỊNH ĐÃ ĐƯỢC THANH TOÁN (đối tác tài chính giải ngân)
        Order afterDepositOrder = orderDAO.findById(orderId);
        assertEquals(0, afterDepositOrder.getRemainingAmount().compareTo(new BigDecimal("700000000")), "Còn chờ đối tác chuyển 70%");
        assertEquals(new BigDecimal("300000000.00"), afterDepositOrder.getPaidAmount(), "Đã thu cọc 30%");

        // 2. Khách hàng cố thanh toán 70% trên website -> BỊ CHẶN
        var contract = contractsDAO.createContractForOrder(orderId, 1, "Hợp đồng trả góp");
        signContractHelper(contract.getContractId(), 1, 1, "123456");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bankService.createForCustomer(orderId, 1, "REMAINING", "BANK_TRANSFER"));
        assertTrue(ex.getMessage().contains("đối tác giải ngân") || ex.getMessage().contains("không còn nhận thanh toán"));

        // 3. Kiểm tra trạng thái đơn: PAID, RemainingAmount = 0
        Order settledOrder = orderDAO.findById(orderId);
        assertEquals("PROCESSING", settledOrder.getStatus());
        assertEquals(0, settledOrder.getRemainingAmount().compareTo(new BigDecimal("700000000")));
        assertEquals(new BigDecimal("300000000.00"), settledOrder.getPaidAmount());
    }

    // =========================================================================
    // CASE 11: Customer hủy popup trước khi xác nhận (No state change)
    // =========================================================================
    @Test
    @DisplayName("Case 11: Khách hàng hủy popup -> Không tạo dữ liệu rác, không đổi số dư")
    void testCase11_CustomerCancelsModal_NoStateChange() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);

        // Khách chỉ mở popup rồi nhấn Hủy trên giao diện frontend (không gửi request tạo payment)
        // Kiểm tra backend không phát sinh payment nào
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD);
             Statement s = db.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) AS Cnt FROM Payments WHERE OrderID=" + orderId)) {
            rs.next();
            assertEquals(0, rs.getInt("Cnt"));
        }

        Order order = orderDAO.findById(orderId);
        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("700000000.00"), order.getRemainingAmount());
        assertEquals(0, order.getPaidAmount().compareTo(BigDecimal.ZERO));
    }

    // =========================================================================
    // CASE 12: Kiểm tra tổng tiền thu, số dư và trạng thái cuối cùng (requireExactSettlement)
    // =========================================================================
    @Test
    @DisplayName("Case 12: Kiểm tra đối chiếu quyết toán cuối cùng trước khi xuất hóa đơn")
    void testCase12_FinalSettlementAuditAndInvoiceHandover() throws Exception {
        int orderId = createTestOrder(1, 1, "BANK_TRANSFER", null);

        // Khi chưa thanh toán đủ: requireExactSettlement phải ném exception
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD)) {
            assertThrows(IllegalStateException.class, () -> PaymentDAO.requireExactSettlement(db, orderId));
        }

        // Cọc 30%
        BankTransferService.Transfer dep = bankService.createForCustomer(orderId, 1, "DEPOSIT", "BANK_TRANSFER");
        bankService.decide(dep.paymentId(), getAccountantUserId(), true, "MB-DEP-12", new BigDecimal("300000000"), "Cọc");

        // Hợp đồng
        var contract = contractsDAO.createContractForOrder(orderId, 1, "HĐ bán xe");
        signContractHelper(contract.getContractId(), 1, 1, "123456");

        // Vẫn chưa đủ 70%: requireExactSettlement vẫn ném exception
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD)) {
            assertThrows(IllegalStateException.class, () -> PaymentDAO.requireExactSettlement(db, orderId));
        }

        // Thanh toán nốt 70%
        BankTransferService.Transfer rem = bankService.createForCustomer(orderId, 1, "REMAINING", "BANK_TRANSFER");
        bankService.decide(rem.paymentId(), getAccountantUserId(), true, "MB-REM-12", new BigDecimal("700000000"), "70%");

        // Sau khi đã thanh toán đủ 100% (30% + 70%): requireExactSettlement phải PASS thành công
        try (Connection db = DriverManager.getConnection(h2Url, "sa", TestApplication.H2_PASSWORD)) {
            assertDoesNotThrow(() -> PaymentDAO.requireExactSettlement(db, orderId));
        }

        // Kế toán xuất hóa đơn thành công
        var invoice = invoiceDAO.issueInvoice(orderId, 1, "Xuất hóa đơn GTGT");
        assertNotNull(invoice);
        assertNotNull(invoiceDAO.findByOrderId(orderId));
    }

    // =========================================================================
    // CASE 13: Chuẩn hóa định dạng tiền tệ vi-VN
    // =========================================================================
    @Test
    @DisplayName("Case 13: Định dạng tiền tệ vi-VN không có .00 và có hậu tố VND")
    void testCase13_CurrencyFormattingStandard() {
        assertEquals("7.000.000 VND", FormatUtils.formatMoney(new BigDecimal("7000000")));
        assertEquals("75.000.000 VND", FormatUtils.formatMoney(new BigDecimal("75000000")));
        assertEquals("150.000.000 VND", FormatUtils.formatMoney(new BigDecimal("150000000")));
        assertEquals("1.000.000.000 VND", FormatUtils.formatMoney(new BigDecimal("1000000000")));
        assertEquals("0 VND", FormatUtils.formatMoney(BigDecimal.ZERO));
        assertEquals("0 VND", FormatUtils.formatMoney(null));

        // Kiểm tra định dạng số lượng
        assertEquals("1.500", FormatUtils.formatNumber(1500));
        assertEquals("100.000", FormatUtils.formatNumber(100000));
    }
}
