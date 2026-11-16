package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;
import com.electriccarsales.dao.SalesPreviewDAO;
import com.electriccarsales.model.SalesPreviewKind;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;

class SalesPreviewSchemaTest {
    @Test
    void previewsUseCurrentSchemaAndVerifyOwnershipAndSettlement() throws Exception {
        String url = "jdbc:h2:mem:wordPreview;MODE=MSSQLServer;NON_KEYWORDS=YEAR";
        String[] keys = {"DB_URL", "DB_USER", "DB_PASSWORD"};
        String[] old = java.util.Arrays.stream(keys).map(System::getProperty).toArray(String[]::new);
        System.setProperty(keys[0], url);
        System.setProperty(keys[1], "sa");
        System.setProperty(keys[2], TestApplication.H2_PASSWORD);
        try (var db = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             var statement = db.createStatement()) {
            TestApplication.initializeSchema(statement);
            statement.execute("""
                    INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                    SELECT 'owner','test-only',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                    INSERT INTO Employees(UserID,EmployeeName,Phone)
                    SELECT UserID,'Fixture employee','0900000001' FROM Users;
                    INSERT INTO Customers(UserID,CustomerName,Phone)
                    SELECT UserID,'Fixture owner','0900000000' FROM Users;
                    INSERT INTO Brands(BrandName,BrandStatusID)
                    SELECT 'Fixture brand',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                    INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'Fixture line');
                    INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'Fixture model');
                    INSERT INTO CarVersions(ModelID,VersionName) VALUES(1,'Test trim');
                    INSERT INTO Colors(ColorName) VALUES('Test color');
                    INSERT INTO CarStatuses(StatusName) VALUES('DEPOSITED');
                    INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                    VALUES('UNIT1',1,1,1,1,2026,'TEST0000000000001','TESTENGINE1',100);
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT 1,os.OrderStatusID,pm.PaymentMethodID,'2026-11-01',100,30,70
                    FROM OrderStatuses os CROSS JOIN PaymentMethods pm WHERE os.StatusName='DEPOSITED' AND pm.MethodName='CASH';
                    INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(1,1,100);
                    INSERT INTO Contracts(OrderID,CustomerID,EmployeeID,ContractNumber,ContractAmount,Status)
                    VALUES(1,1,1,'C1',100,'PENDING_SIGNATURE');
                    INSERT INTO Payments(OrderID,PaymentMethodID,PaymentTypeID,PaymentStatusID,PaymentAmount)
                    SELECT 1,pm.PaymentMethodID,pt.PaymentTypeID,ps.PaymentStatusID,30
                    FROM PaymentMethods pm CROSS JOIN PaymentTypes pt CROSS JOIN PaymentStatuses ps
                    WHERE pm.MethodName='CASH' AND pt.TypeName='DEPOSIT' AND ps.StatusID='COMPLETED';
                    """);
            var dao = new SalesPreviewDAO();
            assertTrue(dao.find(SalesPreviewKind.DEPOSIT, 1, 2).isEmpty());
            assertEquals("Fixture owner", dao.find(SalesPreviewKind.DEPOSIT, 1, 1).get("customer.name"));
            assertEquals("C1", dao.find(SalesPreviewKind.CONTRACT, 1, 1).get("contract.number"));
            assertTrue(dao.find(SalesPreviewKind.INVOICE, 1, 1).isEmpty());

            var contracts = new com.electriccarsales.dao.ContractsDAO();
            assertDoesNotThrow(() -> contracts.requireSigningEligible(1, 1, 1));
            assertThrows(IllegalStateException.class, () -> contracts.requireSigningEligible(1, 1, 2));
            statement.execute("INSERT INTO OTPVerifications(UserID,OTPCode,Purpose,ExpiredDate,Status) "
                    + "VALUES(1,'123456','SIGN_CONTRACT_ID:2',DATEADD('MINUTE',5,CURRENT_TIMESTAMP),'PENDING')");
            assertThrows(IllegalArgumentException.class, () -> contracts.signContract(1, 1, 1, "123456"));
            statement.execute("UPDATE OTPVerifications SET Purpose='SIGN_CONTRACT_ID:1',ExpiredDate=DATEADD('MINUTE',-1,CURRENT_TIMESTAMP)");
            assertThrows(IllegalArgumentException.class, () -> contracts.signContract(1, 1, 1, "123456"));
            statement.execute("UPDATE OTPVerifications SET ExpiredDate=DATEADD('MINUTE',5,CURRENT_TIMESTAMP)");
            assertThrows(IllegalArgumentException.class, () -> contracts.signContract(1, 1, 1, "000000"));
            assertThrows(SecurityException.class, () -> contracts.signContract(1, 2, 1, "123456"));
            assertEquals("SIGNED", contracts.signContract(1, 1, 1, "123456").getStatus());
            assertThrows(IllegalStateException.class, () -> contracts.signContract(1, 1, 1, "123456"));
            try (var row = statement.executeQuery("SELECT Status,VerifiedDate FROM OTPVerifications")) {
                assertTrue(row.next());
                assertEquals("VERIFIED", row.getString(1));
                assertNotNull(row.getTimestamp(2));
            }
            statement.execute("""
                    UPDATE Orders SET RemainingAmount=0 WHERE OrderID=1;
                    INSERT INTO Invoices(OrderID,CustomerID,EmployeeID,InvoiceNumber,SubTotal,TotalAmount,Status)
                    VALUES(1,1,1,'I1',100,100,'ISSUED');
                    """);
            assertTrue(dao.find(SalesPreviewKind.INVOICE, 1, 1).isEmpty());
            assertTrue(dao.find(SalesPreviewKind.CONTRACT, 1, 1).isEmpty());
            statement.execute("""
                    INSERT INTO Payments(OrderID,PaymentMethodID,PaymentTypeID,PaymentStatusID,PaymentAmount)
                    SELECT 1,pm.PaymentMethodID,pt.PaymentTypeID,ps.PaymentStatusID,70
                    FROM PaymentMethods pm CROSS JOIN PaymentTypes pt CROSS JOIN PaymentStatuses ps
                    WHERE pm.MethodName='CASH' AND pt.TypeName='REMAINING' AND ps.StatusID='COMPLETED';
                    """);
            assertEquals("I1", dao.find(SalesPreviewKind.INVOICE, 1, 1).get("invoice.number"));
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (old[i] == null) System.clearProperty(keys[i]);
                else System.setProperty(keys[i], old[i]);
            }
        }
    }
}
