package com.electriccarsales.controller;
import static org.junit.jupiter.api.Assertions.*;
import com.electriccarsales.dao.*;
import java.sql.DriverManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
class ContractWorkflowTest {
 @Test void transferDepositOtpBalanceInvoiceHandover() throws Exception {runWorkflow(false);}
 @Test void partnerDisbursementOtpInvoiceHandover() throws Exception {runWorkflow(true);}
 private void runWorkflow(boolean installment) throws Exception {
  String url="jdbc:h2:mem:contractFlow"+installment+";MODE=MSSQLServer;NON_KEYWORDS=YEAR";
  String[] keys={"DB_URL","DB_USER","DB_PASSWORD","SEPAY_ACCOUNT_NUMBER","SEPAY_ACCOUNT_NAME"};
  String[] old=java.util.Arrays.stream(keys).map(System::getProperty).toArray(String[]::new);
  System.setProperty(keys[0],url);System.setProperty(keys[1],"sa");System.setProperty(keys[2],TestApplication.H2_PASSWORD);
  System.setProperty(keys[3],"123456"); System.setProperty(keys[4],"Fixture");
  try(var db=DriverManager.getConnection(url,"sa",TestApplication.H2_PASSWORD);var statement=db.createStatement()) {
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
                    INSERT INTO CarStatuses(StatusName) VALUES('DEPOSITED'),('AVAILABLE'),('SOLD'),('DELIVERED');
                    INSERT INTO Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)
                    VALUES('UNIT1',1,1,1,1,2026,'TEST0000000000001','TESTENGINE1',100);
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT 1,os.OrderStatusID,pm.PaymentMethodID,'2026-11-01',100,30,70
                    FROM OrderStatuses os CROSS JOIN PaymentMethods pm WHERE os.StatusName='DEPOSITED' AND pm.MethodName='BANK_TRANSFER';
                    INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(1,1,100);
                    INSERT INTO Payments(OrderID,PaymentMethodID,PaymentTypeID,PaymentStatusID,PaymentAmount)
                    SELECT 1,pm.PaymentMethodID,pt.PaymentTypeID,ps.PaymentStatusID,30
                    FROM PaymentMethods pm CROSS JOIN PaymentTypes pt CROSS JOIN PaymentStatuses ps
                    WHERE pm.MethodName='BANK_TRANSFER' AND pt.TypeName='DEPOSIT' AND ps.StatusID='PENDING';
                    """);

   statement.execute("""
    INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)
    SELECT 'accountant','test-only','accountant@test.local','0900000002',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
    WHERE r.RoleName='ACCOUNTANT' AND s.StatusName='ACTIVE';
    INSERT INTO Employees(UserID,EmployeeName,Phone) SELECT UserID,'Fixture accountant','0900000002' FROM Users WHERE Username='accountant';
    """);
   if(installment) statement.execute("""
    INSERT INTO FinancialCompanies(CompanyName) VALUES('Fixture partner');
    UPDATE Orders SET CompanyID=1,PaymentMethodID=(SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName='INSTALLMENT') WHERE OrderID=1;
    """);
   var bank=new com.electriccarsales.service.BankTransferService();
   var contracts=new ContractsDAO();var payments=new PaymentDAO();var invoices=new InvoiceDAO();var deliveries=new DeliveryDAO();
   assertThrows(IllegalStateException.class,()->contracts.createContractForOrder(1,1,"Fixture terms"));
   statement.execute("UPDATE Orders SET OrderStatusID=(SELECT OrderStatusID FROM OrderStatuses WHERE StatusName='PENDING')");
   statement.execute("UPDATE Cars SET CarStatusID=(SELECT CarStatusID FROM CarStatuses WHERE StatusName='AVAILABLE')");
   bank.decide(1,2,true,"CASH-D",new BigDecimal("30"),"Test deposit");
   if (!installment) {
       assertThrows(IllegalArgumentException.class,()->bank.createForCustomer(1,1,"REMAINING","CASH"));
   }
   assertThrows(IllegalArgumentException.class,()->contracts.createContractForOrder(1,1,"X".repeat(501)));
   var contract=contracts.createContractForOrder(1,1,"Fixture terms");
   assertEquals("PENDING_SIGNATURE",contract.getStatus());
   assertThrows(IllegalStateException.class,()->invoices.issueInvoice(1,1,"Test invoice"));
   assertThrows(IllegalStateException.class,()->deliveries.completeHandover(1,1,"Test address","Test"));
   statement.execute("INSERT INTO OTPVerifications(UserID,OTPCode,Purpose,ExpiredDate,Status) VALUES(1,'234567','"
    +ContractsDAO.signingPurpose(contract.getContractID())+"',DATEADD('MINUTE',5,CURRENT_TIMESTAMP),'PENDING')");
   assertEquals("SIGNED",contracts.signContract(contract.getContractID(),1,1,"234567").getStatus());
   assertNotNull(new com.electriccarsales.service.SalesWordService().render(com.electriccarsales.model.SalesPreviewKind.CONTRACT,1,1));
   if (installment) {
       assertThrows(IllegalStateException.class,()->invoices.issueInvoice(1,1,"Test invoice"));
       var disb = bank.createPartnerDisbursement(1);
       assertEquals("PENDING", disb.status());
       assertEquals(0, disb.amount().compareTo(new BigDecimal("70")));
       // Không thể xuất hóa đơn khi đối tác chưa chuyển 70%
       assertThrows(IllegalStateException.class,()->invoices.issueInvoice(1,1,"Test invoice"));
       // Kế toán đối soát thực nhận từ đối tác với mã giao dịch ngân hàng
       assertThrows(IllegalArgumentException.class,()->bank.decide(disb.paymentId(),2,true,"DISB-MB-001",new BigDecimal("69"),"Test"));
       bank.decide(disb.paymentId(),2,true,"DISB-MB-001",new BigDecimal("70"),"Test partner disbursement");
       // Chống xác nhận trùng
       assertThrows(IllegalStateException.class,()->bank.decide(disb.paymentId(),2,true,"DISB-MB-001",new BigDecimal("70"),"Duplicate"));
   }
   if (!installment) {
       assertThrows(IllegalStateException.class,()->invoices.issueInvoice(1,1,"Test invoice"));
       var remaining=bank.createForCustomer(1,1,"REMAINING","BANK_TRANSFER");
       assertEquals(0,remaining.amount().compareTo(new BigDecimal("70")));
       assertThrows(IllegalArgumentException.class,()->bank.decide(remaining.paymentId(),2,true,"BALANCE-R",new BigDecimal("69"),"Test"));
       bank.decide(remaining.paymentId(),2,true,"BALANCE-R",new BigDecimal("70"),"Test balance");
   }
   assertThrows(IllegalStateException.class,()->deliveries.completeHandover(1,1,"Test address","Test"));
   assertEquals("ISSUED",invoices.issueInvoice(1,1,"Test invoice").getStatus());
   assertNotNull(new com.electriccarsales.service.SalesWordService().render(com.electriccarsales.model.SalesPreviewKind.INVOICE,1,1));
   assertThrows(IllegalStateException.class,()->invoices.issueInvoice(1,1,"Duplicate"));
   assertEquals("DELIVERED",deliveries.completeHandover(1,1,"Test address","Test").getDeliveryStatus());
   assertThrows(IllegalStateException.class,()->deliveries.completeHandover(1,1,"Test address","Duplicate"));
   try(var rows=statement.executeQuery("SELECT COUNT(*) FROM InventoryTransactions WHERE TransactionType='EXPORT'")) {
    assertTrue(rows.next());assertEquals(1,rows.getInt(1));
   }
  } finally {for(int i=0;i<keys.length;i++){if(old[i]==null)System.clearProperty(keys[i]);else System.setProperty(keys[i],old[i]);}}
 }
}
