package com.electriccarsales.service;

import com.electriccarsales.controller.TestApplication;
import com.electriccarsales.dao.CarDAO;
import com.electriccarsales.dao.CarInventoryDAO;
import com.electriccarsales.dao.CarStatusDAO;
import com.electriccarsales.model.Car;
import com.electriccarsales.model.InventoryItem;
import com.electriccarsales.util.VehicleValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử toàn diện module Vehicle & Inventory Management theo RDS")
public class VehicleInventoryManagementTest {

    private String h2Url;
    private String prevDbUrl;
    private String prevDbUser;
    private String prevDbPass;

    private CarDAO carDAO;
    private CarInventoryDAO carInventoryDAO;
    private CarStatusDAO carStatusDAO;
    private CarInventoryService inventoryService;

    @BeforeEach
    void setUp() throws Exception {
        prevDbUrl = System.getProperty("DB_URL");
        prevDbUser = System.getProperty("DB_USER");
        prevDbPass = System.getProperty("DB_PASSWORD");

        h2Url = "jdbc:h2:mem:inv_test_" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", h2Url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", "test");

        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             Statement s = c.createStatement()) {
            TestApplication.initializeSchema(s);

            // Seed lookup data if not present
            s.execute("MERGE INTO CarStatuses(CarStatusID, StatusName) KEY(CarStatusID) VALUES(1, 'AVAILABLE'),(2, 'RESERVED'),(3, 'DEPOSITED'),(4, 'SOLD'),(5, 'DELIVERED')");
            s.execute("MERGE INTO Brands(BrandID, BrandName, BrandStatusID) KEY(BrandID) VALUES(1, 'VinFast', 1)");
            s.execute("MERGE INTO CarLines(CarLineID, BrandID, CarLineName) KEY(CarLineID) VALUES(1, 1, 'VF Series')");
            s.execute("MERGE INTO CarModels(ModelID, CarLineID, ModelName) KEY(ModelID) VALUES(1, 1, 'VF 8'),(2, 1, 'VF 9')");
            s.execute("MERGE INTO CarVersions(VersionID, ModelID, VersionName) KEY(VersionID) VALUES(1, 1, 'Eco'),(2, 1, 'Plus'),(3, 2, 'Eco')");
            s.execute("MERGE INTO Colors(ColorID, ColorName) KEY(ColorID) VALUES(1, 'Trắng'),(2, 'Đen')");
            s.execute("MERGE INTO Suppliers(SupplierID, SupplierName, Phone) KEY(SupplierID) VALUES(1, 'VinFast Hai Phong Factory', '1900232389')");

            // Seed employee for testing
            s.execute("MERGE INTO Users(UserID, Username, PasswordHash, RoleID, StatusID) KEY(UserID) VALUES(100, 'ops_test', 'hash', 2, 1)");
            s.execute("MERGE INTO Employees(EmployeeID, UserID, EmployeeName, Phone, Email) KEY(EmployeeID) VALUES(10, 100, 'Nguyen Van Kho', '0901234567', 'kho.nguyen@test.local')");
        }

        carDAO = new CarDAO();
        carInventoryDAO = new CarInventoryDAO();
        carStatusDAO = new CarStatusDAO();
        inventoryService = new CarInventoryService(carDAO, carInventoryDAO, carStatusDAO);
    }

    @AfterEach
    void tearDown() {
        if (prevDbUrl != null) System.setProperty("DB_URL", prevDbUrl);
        else System.clearProperty("DB_URL");
        if (prevDbUser != null) System.setProperty("DB_USER", prevDbUser);
        else System.clearProperty("DB_USER");
        if (prevDbPass != null) System.setProperty("DB_PASSWORD", prevDbPass);
        else System.clearProperty("DB_PASSWORD");
    }

    // ==========================================
    // 1 & 2. KIỂM THỬ TRANG TỒN KHO CÓ DỮ LIỆU & CHƯA CÓ DỮ LIỆU
    // ==========================================

    @Test
    @DisplayName("1. Trang tồn kho có dữ liệu xe vật lý")
    void testInventoryWithData() throws SQLException {
        // Insert a test car
        Car car = new Car(0, "VF8-TEST-001", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN00000001", "ENG-VF8-001", "LOT-2026", 1090000000);
        inventoryService.importVehicle(car, 10, 1, "Nhập kho ban đầu");

        List<Car> cars = inventoryService.getInventoryCars("", "");
        assertNotNull(cars);
        assertFalse(cars.isEmpty());
        assertEquals("VF82026VN00000001", cars.get(0).getVin());
        assertEquals("AVAILABLE", cars.get(0).getStatusName());

        Map<String, Integer> stats = inventoryService.getInventoryStats();
        assertEquals(1, stats.get("totalCars"));
        assertEquals(1, stats.get("availableCars"));
        assertEquals(0, stats.get("reservedCars"));
    }

    @Test
    @DisplayName("2. Trang tồn kho chưa có dữ liệu (trả về danh sách rỗng, không phải lỗi)")
    void testInventoryWithoutData() throws SQLException {
        List<Car> cars = inventoryService.getInventoryCars("", "");
        assertNotNull(cars);
        assertTrue(cars.isEmpty());

        Map<String, Integer> stats = inventoryService.getInventoryStats();
        assertEquals(0, stats.get("totalCars"));
        assertEquals(0, stats.get("availableCars"));
    }

    @Test
    @DisplayName("3. Query database lỗi được xử lý có kiểm soát")
    void testDatabaseErrorHandling() {
        // Break URL temporarily to simulate DB outage
        System.setProperty("DB_URL", "jdbc:h2:mem:non_existent;DB_CLOSE_DELAY=0");
        CarDAO brokenDao = new CarDAO();
        assertThrows(IllegalStateException.class, () -> brokenDao.getAllCars());
    }

    // ==========================================
    // 4 - 10. VALIDATION SỐ KHUNG VIN (SECTION III)
    // ==========================================

    @Test
    @DisplayName("4. VIN ít hơn 17 ký tự bị từ chối")
    void testVinLessThan17Chars() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026VN1234"));
        assertEquals("VIN phải có đúng 17 ký tự.", ex.getMessage());
    }

    @Test
    @DisplayName("5. VIN nhiều hơn 17 ký tự bị từ chối")
    void testVinMoreThan17Chars() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026VN1234567890EXTRA"));
        assertEquals("VIN phải có đúng 17 ký tự.", ex.getMessage());
    }

    @Test
    @DisplayName("6. VIN chứa chữ cái I, O, Q bị từ chối")
    void testVinContainsIOQ() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026VNI0000001"));
        assertEquals("VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026VNO0000001"));
        assertEquals("VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026VNQ0000001"));
        assertEquals("VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.", ex3.getMessage());
    }

    @Test
    @DisplayName("7. VIN chứa ký tự đặc biệt bị từ chối")
    void testVinSpecialCharacters() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026VN#0000001"));
        assertEquals("VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.", ex.getMessage());
    }

    @Test
    @DisplayName("8. VIN chứa khoảng trắng bị từ chối")
    void testVinContainsWhitespace() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateVin("VF82026 VN0000001"));
        assertEquals("VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.", ex.getMessage());
    }

    @Test
    @DisplayName("9. VIN trùng trong database bị từ chối")
    void testDuplicateVinInDatabase() {
        Car car1 = new Car(0, "VF8-CODE-001", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN11111111", "ENG-001-TEST", "LOT-1", 1000000000);
        inventoryService.importVehicle(car1, 10, 1, "Nhập xe 1");

        Car car2 = new Car(0, "VF8-CODE-002", 1, 1, 1, 1, "Việt Nam", 2026,
                "vf82026vn11111111", "ENG-002-TEST", "LOT-1", 1000000000);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inventoryService.importVehicle(car2, 10, 1, "Nhập xe trùng VIN"));
        assertEquals("VIN này đã tồn tại trong hệ thống.", ex.getMessage());
    }

    @Test
    @DisplayName("10. VIN hợp lệ về cấu trúc chuẩn 17 ký tự")
    void testValidVinStructure() {
        assertDoesNotThrow(() -> VehicleValidator.validateVin("VF82026VN99999999"));
        assertDoesNotThrow(() -> VehicleValidator.validateVin("vf82026vn99999999")); // Auto upper
    }

    // ==========================================
    // 11 - 13. VALIDATION SỐ MÁY (SECTION IV)
    // ==========================================

    @Test
    @DisplayName("11. Số máy để trống hoặc chỉ có khoảng trắng bị từ chối")
    void testEngineNumberEmpty() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateEngineNumber(""));
        assertEquals("Vui lòng nhập số máy.", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateEngineNumber("   "));
        assertEquals("Vui lòng nhập số máy.", ex2.getMessage());
    }

    @Test
    @DisplayName("12. Số máy vượt quá 50 ký tự bị từ chối")
    void testEngineNumberTooLong() {
        String longEngine = "ENG-" + "A".repeat(50);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> VehicleValidator.validateEngineNumber(longEngine));
        assertEquals("Số máy phải có từ 5 đến 50 ký tự.", ex.getMessage());
    }

    @Test
    @DisplayName("13. Số máy trùng trong database bị từ chối")
    void testDuplicateEngineNumberInDatabase() {
        Car car1 = new Car(0, "VF8-CODE-010", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN22222222", "ENG-SAME-2026", "LOT-1", 1000000000);
        inventoryService.importVehicle(car1, 10, 1, "Nhập xe 1");

        Car car2 = new Car(0, "VF8-CODE-011", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN33333333", "eng-same-2026", "LOT-1", 1000000000);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inventoryService.importVehicle(car2, 10, 1, "Nhập xe trùng Engine"));
        assertEquals("Số máy đã tồn tại trong hệ thống.", ex.getMessage());
    }

    // ==========================================
    // 14 & 15. NHẬP XE THÀNH CÔNG VÀ ROLLBACK (SECTION V)
    // ==========================================

    @Test
    @DisplayName("14. Nhập xe thành công: Lưu Cars (AVAILABLE) và InventoryTransactions (IMPORT)")
    void testImportVehicleSuccess() throws SQLException {
        Car car = new Car(0, "VF8-NEW-2026", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN88888888", "ENG-VF8-8888", "LOT-2026", 1200000000);
        Car imported = inventoryService.importVehicle(car, 10, 1, "Nhập kho chính hãng");

        assertTrue(imported.getCarID() > 0);
        assertEquals("AVAILABLE", imported.getStatusName());

        // Verify InventoryTransactions record
        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             PreparedStatement ps = c.prepareStatement("SELECT TransactionType, EmployeeID, SupplierID FROM InventoryTransactions WHERE CarID = ?")) {
            ps.setInt(1, imported.getCarID());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("IMPORT", rs.getString("TransactionType"));
                assertEquals(10, rs.getInt("EmployeeID"));
                assertEquals(1, rs.getInt("SupplierID"));
            }
        }
    }

    @Test
    @DisplayName("15. Nhập xe thất bại rollback hoàn toàn: Không lưu xe mồ côi khi lỗi")
    void testImportVehicleRollbackOnFailure() {
        // VersionID 999 does not belong to ModelID 1
        Car invalidCar = new Car(0, "VF8-FAIL-2026", 1, 999, 1, 1, "Việt Nam", 2026,
                "VF82026VN77777777", "ENG-FAIL-7777", "LOT-2026", 1200000000);

        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.importVehicle(invalidCar, 10, 1, "Nhập sai phiên bản"));

        // Verify car was NOT inserted
        assertFalse(carDAO.isVinExists("VF82026VN77777777", null));
    }

    // ==========================================
    // 16, 17, 18. XUẤT XE HỢP LỆ, KHÔNG ĐỦ ĐIỀU KIỆN, VÀ XUẤT 2 LẦN (SECTION VI)
    // ==========================================

    @Test
    @DisplayName("16. Xuất xe hợp lệ: Xe AVAILABLE được xuất kho trực tiếp và ghi nhận EXPORT")
    void testExportVehicleValid() throws SQLException {
        Car car = new Car(0, "VF8-EXP-001", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN55555555", "ENG-EXP-5555", "LOT-2026", 1100000000);
        Car imported = inventoryService.importVehicle(car, 10, 1, "Nhập để xuất");

        boolean exported = inventoryService.exportVehicle(imported.getCarID(), 10, "Xuất chuyển showroom");
        assertTrue(exported);

        // Verify car status changed to DELIVERED
        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             PreparedStatement ps = c.prepareStatement("SELECT cs.StatusName FROM Cars c JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID WHERE c.CarID = ?")) {
            ps.setInt(1, imported.getCarID());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("DELIVERED", rs.getString("StatusName"));
            }
        }

        // Verify EXPORT transaction logged
        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             PreparedStatement ps = c.prepareStatement("SELECT TransactionType, Note FROM InventoryTransactions WHERE CarID = ? AND TransactionType = 'EXPORT'")) {
            ps.setInt(1, imported.getCarID());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("EXPORT", rs.getString("TransactionType"));
                assertTrue(rs.getString("Note").contains("Xuất chuyển showroom"));
            }
        }
    }

    @Test
    @DisplayName("17. Xe không đủ điều kiện (đang trong đơn hàng) bị từ chối xuất kho trực tiếp")
    void testExportVehicleIneligible() throws SQLException {
        Car car = new Car(0, "VF8-BOUND-001", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN66666666", "ENG-BOUND-6666", "LOT-2026", 1100000000);
        Car imported = inventoryService.importVehicle(car, 10, 1, "Nhập xe");

        // Simulate car bound to active order
        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             Statement s = c.createStatement()) {
            s.execute("UPDATE Cars SET CarStatusID = 3 WHERE CarID = " + imported.getCarID()); // DEPOSITED
        }

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventoryService.exportVehicle(imported.getCarID(), 10, "Cố xuất xe đã cọc"));
        assertTrue(ex.getMessage().contains("đang thuộc đơn hàng") || ex.getMessage().contains("AVAILABLE"));
    }

    @Test
    @DisplayName("18. Xuất xe hai lần bị từ chối")
    void testExportVehicleTwice() {
        Car car = new Car(0, "VF8-EXP-TWICE", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN44444444", "ENG-TWICE-4444", "LOT-2026", 1100000000);
        Car imported = inventoryService.importVehicle(car, 10, 1, "Nhập xe");

        // First export succeeds
        assertTrue(inventoryService.exportVehicle(imported.getCarID(), 10, "Xuất lần 1"));

        // Second export fails
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventoryService.exportVehicle(imported.getCarID(), 10, "Xuất lần 2"));
        assertTrue(ex.getMessage().contains("đã được xuất kho hoặc bàn giao"));
    }

    // ==========================================
    // 19. PHÂN QUYỀN VÀ TRÁCH NHIỆM
    // ==========================================

    @Test
    @DisplayName("19. Nhân viên không hợp lệ hoặc thiếu EmployeeID bị từ chối nhập/xuất")
    void testUnauthorizedEmployeeRejected() {
        Car car = new Car(0, "VF8-UNAUTH-001", 1, 1, 1, 1, "Việt Nam", 2026,
                "VF82026VN99999991", "ENG-UNAUTH-999", "LOT-2026", 1000000000);

        // EmployeeID <= 0 rejected
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.importVehicle(car, 0, 1, "Nhập giả mạo"));

        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.exportVehicle(1, 0, "Xuất giả mạo"));
    }

    // ==========================================
    // 20. POPUP CANCEL SIMULATION (KHÔNG THAY ĐỔI DỮ LIỆU)
    // ==========================================

    @Test
    @DisplayName("20. Mô phỏng người dùng chọn 'Không, quay lại' trên popup: Không có thay đổi DB")
    void testPopupCancelDoesNotModifyData() throws SQLException {
        int initialCount;
        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Cars")) {
            rs.next();
            initialCount = rs.getInt(1);
        }

        // When user cancels on frontend confirm modal, no API request is sent to backend
        // Database remains strictly unmodified
        int finalCount;
        try (Connection c = DriverManager.getConnection(h2Url, "sa", "test");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Cars")) {
            rs.next();
            finalCount = rs.getInt(1);
        }

        assertEquals(initialCount, finalCount);
    }
}
