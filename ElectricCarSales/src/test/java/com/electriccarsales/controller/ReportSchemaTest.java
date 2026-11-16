package com.electriccarsales.controller;

import com.electriccarsales.dao.ReportDAO;
import com.electriccarsales.dao.PromotionDAO;
import com.electriccarsales.model.ReportFilter;
import com.electriccarsales.service.ReportService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportSchemaTest {
    @Test
    void promotionsAreLimitedToTheSelectedModelAndCurrentWindow() throws Exception {
        String url = "jdbc:h2:mem:promotionModel;MODE=MSSQLServer;NON_KEYWORDS=YEAR";
        try (var database = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             var statement = database.createStatement()) {
            TestApplication.initializeSchema(statement);
            statement.execute("""
                INSERT INTO Brands(BrandName,BrandStatusID)
                SELECT 'Fixture',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'Fixture line');
                INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'First model'),(1,'Second model');
                INSERT INTO Promotions(PromotionName,DiscountType,DiscountValue,StartDate,EndDate,Status)
                VALUES ('Current','PERCENTAGE',15,DATEADD(DAY,-1,SYSDATETIME()),DATEADD(DAY,1,SYSDATETIME()),'ACTIVE'),
                       ('Expired','FIXED',150,DATEADD(DAY,-2,SYSDATETIME()),DATEADD(DAY,-1,SYSDATETIME()),'ACTIVE'),
                       ('Inactive','FIXED',150,DATEADD(DAY,-1,SYSDATETIME()),DATEADD(DAY,1,SYSDATETIME()),'INACTIVE');
                INSERT INTO PromotionCars(PromotionID,ModelID) VALUES(1,1),(2,1),(3,1);
                """);
            var promotions = PromotionDAO.findActiveForModel(database, 1);
            assertEquals(1, promotions.size());
            assertEquals("Current", promotions.getFirst().getPromotionName());
            assertEquals(new BigDecimal("15.00"), promotions.getFirst().getDiscountValue());
            assertEquals(0, PromotionDAO.findActiveForModel(database, 2).size());
        }
    }

    @Test
    void baseSchemaCountsCompletedPaymentsWithoutOptionalInstallmentTables() throws Exception {
        String url = "jdbc:h2:mem:reportBase;MODE=MSSQLServer;NON_KEYWORDS=YEAR";
        LocalDate date = LocalDate.of(2026, 11, 1);
        try (var database = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             var statement = database.createStatement()) {
            TestApplication.initializeSchema(statement);
            statement.execute("""
                INSERT INTO Users(Username, PasswordHash, RoleID, StatusID)
                SELECT 'reportcustomer', 'fixture-only', r.RoleID, s.StatusID
                FROM Roles r CROSS JOIN Statuses s
                WHERE r.RoleName = 'CUSTOMER' AND s.StatusName = 'ACTIVE';
                INSERT INTO Customers(UserID, CustomerName, Phone)
                SELECT UserID, 'Report Customer', '0900000000' FROM Users;
                INSERT INTO Orders(CustomerID, OrderStatusID, PaymentMethodID, OrderDate,
                    TotalAmount, DepositAmount, RemainingAmount)
                SELECT c.CustomerID, os.OrderStatusID, pm.PaymentMethodID, '2026-11-01', 100, 30, 70
                FROM Customers c CROSS JOIN OrderStatuses os CROSS JOIN PaymentMethods pm
                WHERE os.StatusName IN ('COMPLETED', 'CANCELLED', 'DEPOSITED') AND pm.MethodName = 'INSTALLMENT';
                INSERT INTO Payments(OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID, PaymentAmount, PaymentDate)
                SELECT o.OrderID, o.PaymentMethodID, pt.PaymentTypeID, ps.PaymentStatusID,
                    CASE pt.TypeName WHEN 'DEPOSIT' THEN 30 ELSE 70 END, '2026-11-01'
                FROM Orders o JOIN OrderStatuses os ON o.OrderStatusID = os.OrderStatusID
                CROSS JOIN PaymentTypes pt CROSS JOIN PaymentStatuses ps
                WHERE ps.StatusID = CASE WHEN os.StatusName = 'DEPOSITED' AND pt.TypeName = 'REMAINING'
                    THEN 'PENDING' ELSE 'COMPLETED' END;
                """);
            var dao = new ReportDAO(() -> DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD));
            var result = new ReportService(dao, Clock.systemUTC()).generate(new ReportFilter(date, date, null, null, null));
            var summary = result.getSummary();
            assertEquals(new BigDecimal("130.00"), summary.getRevenue());
            assertEquals(new BigDecimal("100.00"), summary.getSalesAmount());
            assertEquals(new BigDecimal("70.00"), summary.getOutstandingAmount());
            assertEquals(1, summary.getCompletedOrders());
            assertEquals(2, summary.getInstallmentContracts());
        }
    }
}
