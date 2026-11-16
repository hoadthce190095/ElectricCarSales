package com.electriccarsales.dao;

import com.electriccarsales.controller.TestApplication;
import com.electriccarsales.model.ConsultationRequest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationRequestDAOTest {

    @Test
    void testInsertConsultationSuccess() throws Exception {
        String url = "jdbc:h2:mem:consultSuccess" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        try (Connection c = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             Statement s = c.createStatement()) {
            TestApplication.initializeSchema(s);

            s.execute("""
                INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                SELECT 'customer1','test',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                INSERT INTO Customers(UserID,CustomerName,Phone)
                SELECT UserID,'Customer Dat','0917231548' FROM Users WHERE Username='customer1';
                INSERT INTO Brands(BrandName,BrandStatusID)
                SELECT 'VinFast',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'VF 8');
                INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'VF 8 Eco');
            """);

            ConsultationRequestDAO dao = new ConsultationRequestDAO(() -> DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD));

            ConsultationRequest cr = new ConsultationRequest();
            cr.setCustomerId(1);
            cr.setStaffId(0);
            cr.setModelId(1);
            cr.setContent("Tư vấn xe VF 8 Eco");
            cr.setStatus("PENDING");

            dao.insert(cr);
            assertTrue(cr.getId() > 0, "ID phải được tự sinh thành công");
            assertEquals("PENDING", cr.getStatus());

            List<ConsultationRequest> list = dao.findByCustomerId(1);
            assertFalse(list.isEmpty());
            assertEquals("Customer Dat", list.get(0).getCustomerName());
            assertEquals("VF 8 Eco", list.get(0).getModelName());
        }
    }

    @Test
    void testInsertConsultationAutoSeedsWhenStatusesEmpty() throws Exception {
        String url = "jdbc:h2:mem:consultAutoSeed" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        try (Connection c = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             Statement s = c.createStatement()) {
            TestApplication.initializeSchema(s);

            // Xóa sạch ConsultationRequests và ConsultationStatuses để mô phỏng DB thiếu dữ liệu mẫu
            s.execute("DELETE FROM ConsultationRequests");
            s.execute("DELETE FROM ConsultationStatuses");

            s.execute("""
                INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                SELECT 'customer1','test',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                INSERT INTO Customers(UserID,CustomerName,Phone)
                SELECT UserID,'Customer Dat','0917231548' FROM Users WHERE Username='customer1';
                INSERT INTO Brands(BrandName,BrandStatusID)
                SELECT 'VinFast',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE';
                INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,'VF 8');
                INSERT INTO CarModels(CarLineID,ModelName) VALUES(1,'VF 8 Eco');
            """);

            ConsultationRequestDAO dao = new ConsultationRequestDAO(() -> DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD));

            ConsultationRequest cr = new ConsultationRequest();
            cr.setCustomerId(1);
            cr.setStaffId(0);
            cr.setModelId(1);
            cr.setContent("Tư vấn xe khi bảng statuses rỗng");
            cr.setStatus("PENDING");

            // Không ném lỗi, tự động phục hồi và chèn thành công
            assertDoesNotThrow(() -> dao.insert(cr));
            assertTrue(cr.getId() > 0);

            List<ConsultationRequest> found = dao.findByCustomerId(1);
            assertEquals(1, found.size());
            assertEquals("PENDING", found.get(0).getStatus());
        }
    }

    @Test
    void testInsertConsultationHandlesInvalidModelIdSafely() throws Exception {
        String url = "jdbc:h2:mem:consultInvalidModel" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", TestApplication.H2_PASSWORD);

        try (Connection c = DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD);
             Statement s = c.createStatement()) {
            TestApplication.initializeSchema(s);

            s.execute("""
                INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                SELECT 'customer1','test',r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE';
                INSERT INTO Customers(UserID,CustomerName,Phone)
                SELECT UserID,'Customer Dat','0917231548' FROM Users WHERE Username='customer1';
            """);

            ConsultationRequestDAO dao = new ConsultationRequestDAO(() -> DriverManager.getConnection(url, "sa", TestApplication.H2_PASSWORD));

            ConsultationRequest cr = new ConsultationRequest();
            cr.setCustomerId(1);
            cr.setStaffId(0);
            cr.setModelId(9999); // ModelID không tồn tại trong CarModels
            cr.setContent("Tư vấn xe chung");
            cr.setStatus("PENDING");

            // Không bị lỗi Foreign Key FK_ConsultationRequests_CarModels
            assertDoesNotThrow(() -> dao.insert(cr));
            assertTrue(cr.getId() > 0);

            ConsultationRequest saved = dao.findById(cr.getId());
            assertNotNull(saved);
            assertNull(saved.getModelId());
        }
    }
}
