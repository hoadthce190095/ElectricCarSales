package vn.edu.fpt.electriccarsales.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.edu.fpt.electriccarsales.dto.EmployeeRequest;
import vn.edu.fpt.electriccarsales.dto.PromotionRequest;
import vn.edu.fpt.electriccarsales.dto.UserRequest;
import vn.edu.fpt.electriccarsales.entities.Promotion;
import vn.edu.fpt.electriccarsales.entities.User;
import vn.edu.fpt.electriccarsales.security.PasswordHasher;
import vn.edu.fpt.electriccarsales.util.ApiException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class AdministrationServiceTest {
    private String url;
    private AdministrationService service;

    @BeforeEach
    void database() throws Exception {
        url = "jdbc:h2:mem:admin" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE Roles(RoleID INT PRIMARY KEY,RoleName VARCHAR(20))");
            s.execute("INSERT INTO Roles VALUES(1,'ADMIN'),(2,'MANAGER'),(3,'SALES'),(4,'CUSTOMER')");
            s.execute("CREATE TABLE Statuses(StatusID INT PRIMARY KEY,StatusName VARCHAR(20))");
            s.execute("INSERT INTO Statuses VALUES(1,'ACTIVE'),(2,'LOCKED'),(3,'INACTIVE')");
            s.execute("CREATE TABLE Users(UserID INT IDENTITY PRIMARY KEY,Username VARCHAR(50)"
                + " UNIQUE,PasswordHash VARCHAR(255),Email VARCHAR(100) UNIQUE,Phone"
                + " VARCHAR(20) UNIQUE,RoleID INT,StatusID INT,CreatedDate TIMESTAMP" + " DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE Employees(EmployeeID INT IDENTITY PRIMARY KEY,UserID INT"
                + " UNIQUE,EmployeeName NVARCHAR(100),Phone VARCHAR(20),Email" + " VARCHAR(100))");
            s.execute("CREATE TABLE Customers(UserID INT UNIQUE,Phone VARCHAR(20),Email" + " VARCHAR(100))");
            s.execute("CREATE TABLE SystemLogs(LogID BIGINT IDENTITY PRIMARY KEY,UserID INT,Action"
                + " VARCHAR(50),TargetType VARCHAR(50),TargetID VARCHAR(100),Detail"
                + " NVARCHAR(MAX),CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE RolePermissions(RoleID INT,PermissionCode VARCHAR(50),PRIMARY"
                + " KEY(RoleID,PermissionCode))");
            s.execute("CREATE TABLE CarModels(ModelID INT PRIMARY KEY,ModelName NVARCHAR(100))");
            s.execute("INSERT INTO CarModels VALUES(1,'VF Test'),(2,'EV Test')");
            s.execute("CREATE TABLE Promotions(PromotionID INT IDENTITY PRIMARY KEY,PromotionName"
                + " NVARCHAR(150),Description NVARCHAR(500),DiscountType"
                + " VARCHAR(30),DiscountValue DECIMAL(18,2),StartDate TIMESTAMP,EndDate"
                + " TIMESTAMP,Status VARCHAR(30))");
            s.execute("CREATE TABLE PromotionCars(PromotionCarID INT IDENTITY PRIMARY KEY,PromotionID"
                + " INT,ModelID INT REFERENCES" + " CarModels(ModelID),UNIQUE(PromotionID,ModelID))");
            s.execute("INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                + " VALUES('admin','unused','admin@test.local','0900000001',1,1)");
        }
        service = new AdministrationService(this::connection);
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private UserRequest staff(String username, int role, int status) {
        return new UserRequest(username, "test-password-123", username + "@test.local", "0900000002", role, status,
            "Nhân viên thử");
    }

    private long count(String table) throws Exception {
        try (Connection c = connection();
            Statement s = c.createStatement();
            ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getLong(1);
        }
    }

    private PromotionRequest promotion(List<Integer> ids) {
        return new PromotionRequest("Ưu đãi", "Mô tả", "PERCENTAGE", new BigDecimal("10.5"),
            LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59), "ACTIVE", ids);
    }

    @Test
    void createsHashedStaffAndEmployeeAndAuditTogether() throws Exception {
        User user = service.createUser(1, staff("staff", 3, 1));
        assertEquals("SALES", user.getRole());
        assertNotNull(user.getEmployeeId());
        assertEquals(1, count("Employees"));
        assertEquals(1, count("SystemLogs"));
        try (Connection c = connection();
            Statement s = c.createStatement();
            ResultSet r = s.executeQuery("SELECT PasswordHash FROM Users WHERE Username='staff'")) {
            r.next();
            assertTrue(PasswordHasher.verify("test-password-123", r.getString(1)));
            assertNotEquals("test-password-123", r.getString(1));
        }
    }

    @Test
    void duplicateContactRollsBackUserEmployeeAndLog() throws Exception {
        service.createUser(1, staff("staff", 3, 1));
        ApiException error = assertThrows(ApiException.class, () -> service.createUser(1, staff("other", 3, 1)));
        assertEquals(409, error.getStatus());
        assertEquals(2, count("Users"));
        assertEquals(1, count("Employees"));
        assertEquals(1, count("SystemLogs"));
    }

    @Test
    void duplicateUpdatesPreserveUserEmployeeAndAudit() throws Exception {
        User first = service.createUser(1, staff("first", 3, 1));
        User second = service.createUser(1, new UserRequest("second", "test-password-123", "second@test.local",
            "0900000003", 3, 1, "Nhân viên thứ hai"));
        ApiException accountError = assertThrows(ApiException.class, () -> service.updateUser(1, first.getUserId(),
            new UserRequest(null, null, second.getEmail(), first.getPhone(), 3, 1, "Tên chưa lưu")));
        assertEquals(409, accountError.getStatus());
        ApiException employeeError = assertThrows(ApiException.class, () -> service.updateEmployee(1,
            first.getEmployeeId(), new EmployeeRequest("Tên chưa lưu", second.getPhone(), first.getEmail())));
        assertEquals(409, employeeError.getStatus());
        User unchanged = service.users("first", 1).getItems().getFirst();
        assertEquals(first.getEmail(), unchanged.getEmail());
        assertEquals(first.getPhone(), unchanged.getPhone());
        assertEquals(first.getEmployeeName(), unchanged.getEmployeeName());
        assertEquals(2, count("SystemLogs"));
    }

    @Test
    void failedAuditRollsBackBusinessChange() throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE SystemLogs");
        }
        assertThrows(SQLException.class, () -> service.createUser(1, staff("staff", 3, 1)));
        assertEquals(1, count("Users"));
        assertEquals(0, count("Employees"));
    }

    @Test
    void cannotLockOrDemoteOwnAdmin() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.updateUser(1, 1,
            new UserRequest(null, null, "admin@test.local", "0900000001", 2, 2, "Admin")));
        assertEquals("ADMIN", service.users("admin", 1).getItems().getFirst().getRole());
        assertEquals("ACTIVE", service.users("admin", 1).getItems().getFirst().getStatus());
    }

    @Test
    void cannotDemoteLastActiveAdminEvenAnotherLockedAdminActs() throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                + " VALUES('otheradmin','unused','other@test.local','0900000003',1,2)");
        }
        assertEquals(403, assertThrows(ApiException.class, () -> service.updateUser(2, 1,
            new UserRequest(null, null, "admin@test.local", "0900000001", 2, 1, "Admin"))).getStatus());
    }

    @Test
    void lockingAnotherAdminSucceedsIfOneRemainsAndLogsBeforeAfter() throws Exception {
        User second = service.createUser(1, staff("secondadmin", 1, 1));
        service.updateUser(1, second.getUserId(),
            new UserRequest(null, null, second.getEmail(), second.getPhone(), 1, 2, "Admin 2"));
        assertEquals("LOCKED", service.users("second", 1).getItems().getFirst().getStatus());
        assertEquals(2, count("SystemLogs"));
    }

    @Test
    void rolePermissionAllowsDelegatedStaffButNeverAdminEscalation() throws Exception {
        User manager = service.createUser(1, staff("manager", 2, 1));
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO RolePermissions VALUES(2,'MANAGE_USERS')");
        }
        User created = service.createUser(manager.getUserId(),
            new UserRequest("sales", "test-password-123", "sales@test.local", "0900000004", 3, 1, "Sales"));
        assertEquals("SALES", created.getRole());
        assertEquals(403,
            assertThrows(ApiException.class,
                () -> service.createUser(manager.getUserId(),
                    new UserRequest("escalate", "test-password-123", "x@test.local", "0900000005", 1, 1, "Admin")))
                .getStatus());
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM RolePermissions WHERE RoleID=2");
        }
        assertEquals(403, assertThrows(ApiException.class, () -> service.updateEmployee(manager.getUserId(),
            created.getEmployeeId(), new EmployeeRequest("New", "0900000004", "sales@test.local"))).getStatus());
    }

    @Test
    void promotionLinksAndHistoryRemainOnDeactivation() throws Exception {
        Promotion p = service.savePromotion(1, null, promotion(List.of(1, 2)));
        assertEquals(2, count("PromotionCars"));
        service.deactivatePromotion(1, p.getPromotionId());
        assertEquals(1, count("Promotions"));
        assertEquals(2, count("PromotionCars"));
        assertEquals("INACTIVE", service.promotions().getFirst().getStatus());
        assertEquals(2, count("SystemLogs"));
    }

    @Test
    void invalidPromotionCannotPartiallyReplaceLinks() throws Exception {
        Promotion p = service.savePromotion(1, null, promotion(List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
            () -> service.savePromotion(1, p.getPromotionId(), promotion(List.of(1, 99))));
        assertEquals(List.of(1, 2), service.promotions().getFirst().getModelIds());
        assertEquals(1, count("SystemLogs"));
    }

    @Test
    void validatesPercentagePrecisionDatesAndDuplicateModels() {
        assertThrows(IllegalArgumentException.class,
            () -> AdministrationService.validatePromotion(promotion(List.of(1, 1))));
        PromotionRequest p = promotion(List.of(1));
        assertThrows(IllegalArgumentException.class,
            () -> AdministrationService
                .validatePromotion(new PromotionRequest(p.getPromotionName(), p.getDescription(), p.getDiscountType(),
                    new BigDecimal("100.01"), p.getStartDate(), p.getEndDate(), p.getStatus(), p.getModelIds())));
        assertThrows(IllegalArgumentException.class,
            () -> AdministrationService
                .validatePromotion(new PromotionRequest(p.getPromotionName(), p.getDescription(), p.getDiscountType(),
                    new BigDecimal("0.001"), p.getStartDate(), p.getEndDate(), p.getStatus(), p.getModelIds())));
    }

    @Test
    void searchesLiteralWildcardsAndDoesNotExposeHashes() throws Exception {
        service.createUser(1, staff("staff", 3, 1));
        assertTrue(service.users("%", 1).getItems().isEmpty());
        assertEquals(1, service.users("staff", 1).getTotal());
        assertTrue(service.users("' OR 1=1 --", 1).getItems().isEmpty());
    }

    @Test
    void combinedUserFiltersKeepSearchAndPaginationTotalsConsistent() throws Exception {
        try (Connection connection = connection();
            PreparedStatement insert = connection.prepareStatement("INSERT INTO"
                + " Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)" + " VALUES(?,'unused',?,?,3,1)")) {
            for (int number = 1; number <= 23; number++) {
                String username = String.format("sales%02d", number);
                insert.setString(1, username);
                insert.setString(2, "contact" + number + "@test.local");
                insert.setString(3, String.format("091000%04d", number));
                insert.executeUpdate();
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                    + " VALUES('salesmanager','unused','manager@test.local','0920000001',2,1),"
                    + " ('saleslocked','unused','locked@test.local','0920000002',3,2),"
                    + " ('customer','unused','customer@test.local','0920000003',4,1)");
                statement.execute("INSERT INTO Employees(UserID,EmployeeName)"
                    + " SELECT UserID,N'Bộ phận kinh doanh' FROM Users WHERE RoleID=3");
            }
        }
        var firstPage = service.users("sales", 1, 3, 1);
        var secondPage = service.users("sales", 2, 3, 1);
        assertEquals(23, firstPage.getTotal());
        assertEquals(firstPage.getTotal(), secondPage.getTotal());
        assertEquals(20, firstPage.getItems().size());
        assertEquals(3, secondPage.getItems().size());
        assertEquals("sales03", secondPage.getItems().getFirst().getUsername());
        assertEquals("sales01", secondPage.getItems().getLast().getUsername());
        for (User user : firstPage.getItems()) {
            assertEquals("SALES", user.getRole());
            assertEquals("ACTIVE", user.getStatus());
        }
        assertEquals(23, service.users("Bộ phận kinh doanh", 1, 3, 1).getTotal());
        assertEquals(24, service.users("sales", 1, 3, null).getTotal());
        assertEquals(24, service.users("sales", 1, null, 1).getTotal());
        assertEquals(1, service.users("", 1, 4, 1).getTotal());
        assertEquals(0, service.users("%", 1, 3, 1).getTotal());
        assertEquals(0, service.users("' OR 1=1 --", 1, 3, 1).getTotal());
    }

    @Test
    void userFiltersRejectUnknownOrNonPositiveReferences() {
        for (int invalidId : new int[]{0, -1, 999}) {
            assertThrows(IllegalArgumentException.class, () -> service.users("", 1, invalidId, null));
            assertThrows(IllegalArgumentException.class, () -> service.users("", 1, null, invalidId));
        }
    }
}
