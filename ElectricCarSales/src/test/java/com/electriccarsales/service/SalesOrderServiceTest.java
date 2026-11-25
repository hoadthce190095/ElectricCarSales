package com.electriccarsales.service;

import com.electriccarsales.controller.TestApplication;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SalesOrderServiceTest {

    @Test
    void testOrderCreationAndPaymentMethods() throws Exception {
        String url = "jdbc:h2:mem:salesOrderTest" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        try (Connection c = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             Statement s = c.createStatement()) {
            TestApplication.initializeSchema(s);

            // Fixtures
            s.execute("""
                INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
                SELECT 'sales1','test','sales1@test.com','0901234567',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                WHERE r.RoleName='SALES' AND s.StatusName='ACTIVE';
                INSERT INTO Employees(UserID,EmployeeName,Phone)
                SELECT UserID,'Sales Phong','0901234567' FROM Users WHERE Username='sales1';

                INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
                SELECT 'cust1','test','cust1@test.com','0917231548',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                INSERT INTO Customers(UserID,CustomerName,Phone)
                SELECT UserID,'Customer Dat','0917231548' FROM Users WHERE Username='cust1';

                INSERT INTO Brands(BrandName,BrandStatusID)
                SELECT 'VinFast',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'VF 8');
                INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'VF 8 Eco');
                INSERT INTO CarVersions(ModelID,VersionName) VALUES(1,'Eco 2026');
                INSERT INTO Colors(ColorName) VALUES('Trắng');
                INSERT INTO CarStatuses(StatusName) VALUES('AVAILABLE');
                INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                VALUES('VF8-01',1,1,1,(SELECT CarStatusID FROM CarStatuses WHERE StatusName='AVAILABLE'),2026,'VINVF800000001','ENG001',1234567891),
                      ('VF8-02',1,1,1,(SELECT CarStatusID FROM CarStatuses WHERE StatusName='AVAILABLE'),2026,'VINVF800000002','ENG002',1200000000);

                INSERT INTO FinancialCompanies(CompanyName,Phone,Email,Address,Description)
                VALUES('MB Bank','1900545426','mb247@mbbank.com.vn','Hà Nội','Ngân hàng Quân Đội');
            """);

            SalesOrderService service = new SalesOrderService();

            // 1. Kiểm tra dashboardData(): loại bỏ CASH, có BANK_TRANSFER và INSTALLMENT
            Map<String, Object> data = service.dashboardData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pms = (List<Map<String, Object>>) data.get("paymentMethods");
            assertNotNull(pms);
            assertFalse(pms.stream().anyMatch(m -> "CASH".equalsIgnoreCase(String.valueOf(m.get("methodName")))),
                    "Phương thức tiền mặt (CASH) phải bị loại bỏ ở phần thanh toán 30%");
            assertTrue(pms.stream().anyMatch(m -> "BANK_TRANSFER".equalsIgnoreCase(String.valueOf(m.get("methodName")))),
                    "Phải có phương thức Thanh toán bằng ngân hàng (BANK_TRANSFER)");
            assertTrue(pms.stream().anyMatch(m -> "INSTALLMENT".equalsIgnoreCase(String.valueOf(m.get("methodName")))),
                    "Phải có phương thức Thanh toán qua nhà tài chính (INSTALLMENT)");

            // 2. Kiểm tra dữ liệu nhà tài chính được lấy từ database
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fcs = (List<Map<String, Object>>) data.get("financialCompanies");
            assertNotNull(fcs);
            assertFalse(fcs.isEmpty());
            assertEquals("MB Bank", fcs.get(0).get("companyName"));

            int salesUserId = 1; // ID của user sales1
            int customerId = 1;

            // 3. Thanh toán bằng ngân hàng (BANK_TRANSFER) kể cả khi truyền companyId vẫn không bị lỗi
            int order1 = service.create(salesUserId, customerId, 1, "BANK_TRANSFER", 1);
            assertTrue(order1 > 0, "Đơn thanh toán ngân hàng phải tạo thành công");

            try (var rows = s.executeQuery("SELECT TotalAmount, DepositAmount, RemainingAmount FROM Orders WHERE OrderID=" + order1)) {
                assertTrue(rows.next());
                assertEquals(1234567891L, rows.getLong(1));
                assertEquals(370370367L, rows.getLong(2));
                assertEquals(864197524L, rows.getLong(3));
            }
            assertThrows(IllegalStateException.class, () -> service.create(salesUserId, customerId, 1, "BANK_TRANSFER", null));

            // 4. Thanh toán qua nhà tài chính (INSTALLMENT) thiếu đối tác sẽ báo lỗi rõ ràng
            IllegalArgumentException exIns = assertThrows(IllegalArgumentException.class, () ->
                    service.create(salesUserId, customerId, 2, "INSTALLMENT", null));
            assertTrue(exIns.getMessage().contains("nhà tài chính"), "Thông báo phải nhắc chọn nhà tài chính");

            // 5. Thanh toán qua nhà tài chính (INSTALLMENT) có đối tác tạo thành công
            int order2 = service.create(salesUserId, customerId, 2, "INSTALLMENT", 1);
            assertTrue(order2 > 0, "Đơn trả góp với đối tác MB Bank phải tạo thành công");

            // 6. Thanh toán bằng tiền mặt (CASH) bị chặn đối với đợt cọc 30%
            IllegalArgumentException exCash = assertThrows(IllegalArgumentException.class, () ->
                    service.create(salesUserId, customerId, 1, "CASH", null));
            assertTrue(exCash.getMessage().contains("không áp dụng tiền mặt"));

            // 7. Khuyến mãi phần trăm áp dụng vào dashboardData() và đơn hàng (khách thanh toán theo giá sau ưu đãi)
            s.execute("""
                INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                VALUES('VF8-03',1,1,1,(SELECT CarStatusID FROM CarStatuses WHERE StatusName='AVAILABLE'),2026,'VINVF800000003','ENG003',500000000);
                INSERT INTO Promotions(PromotionName,Description,DiscountType,DiscountValue,StartDate,EndDate,Status)
                VALUES(N'SIÊU SALE',N'Giảm 10%', 'PERCENTAGE', 10, DATEADD(day, -1, SYSDATETIME()), DATEADD(day, 5, SYSDATETIME()), 'ACTIVE');
                INSERT INTO PromotionCars(PromotionID,ModelID) SELECT MAX(PromotionID), 1 FROM Promotions;
            """);
            Map<String, Object> promoDash = service.dashboardData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> promoCars = (List<Map<String, Object>>) promoDash.get("cars");
            Map<String, Object> car3 = promoCars.stream()
                    .filter(m -> "VINVF800000003".equals(m.get("vin")))
                    .findFirst().orElseThrow();
            assertEquals(500_000_000L, ((Number) car3.get("originalPrice")).longValue());
            assertEquals(50_000_000L, ((Number) car3.get("discountAmount")).longValue());
            assertEquals(450_000_000L, ((Number) car3.get("price")).longValue());
            assertEquals("SIÊU SALE", car3.get("promotionName"));
            assertEquals("PERCENTAGE", car3.get("discountType"));
            assertEquals(10, ((Number) car3.get("discountValue")).intValue());

            int car3Id = ((Number) car3.get("carID")).intValue();
            int promoOrder = service.create(salesUserId, customerId, car3Id, "BANK_TRANSFER", null);
            try (var rows = s.executeQuery("SELECT TotalAmount, DepositAmount, RemainingAmount FROM Orders WHERE OrderID=" + promoOrder)) {
                assertTrue(rows.next());
                assertEquals(450_000_000L, rows.getLong(1));
                assertEquals(135_000_000L, rows.getLong(2));
                assertEquals(315_000_000L, rows.getLong(3));
            }
        }
    }
}
