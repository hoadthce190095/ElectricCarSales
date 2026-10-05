package vn.edu.fpt.electriccarsales.services;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.edu.fpt.electriccarsales.dto.ReportData;
import vn.edu.fpt.electriccarsales.dto.ReportFilter;
import vn.edu.fpt.electriccarsales.dto.StatusTotal;
import vn.edu.fpt.electriccarsales.repositories.ReportRepository;
import vn.edu.fpt.electriccarsales.util.ReportCsv;
import vn.edu.fpt.electriccarsales.util.ReportXlsx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

class ReportServiceTest {
    private ReportService service;
    private String databaseUrl;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-01T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final ReportFilter JANUARY = new ReportFilter(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
        null, null, null);

    @BeforeEach
    void prepareDatabase() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        databaseUrl = url;
        try (Connection connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement()) {
            String schema = """
                CREATE TABLE OrderStatuses(OrderStatusID INT PRIMARY KEY, StatusName VARCHAR(30));
                CREATE TABLE PaymentStatuses(PaymentStatusID INT PRIMARY KEY, StatusID VARCHAR(30));
                CREATE TABLE Customers(CustomerID INT PRIMARY KEY, CustomerName VARCHAR(100));
                CREATE TABLE Employees(EmployeeID INT PRIMARY KEY, EmployeeName VARCHAR(100));
                CREATE TABLE Orders(OrderID INT PRIMARY KEY, CustomerID INT, EmployeeID INT,
                    OrderStatusID INT, PaymentMethodID INT, OrderDate TIMESTAMP(7), TotalAmount DECIMAL(18,2));
                CREATE TABLE Contracts(ContractID INT PRIMARY KEY, OrderID INT UNIQUE, CustomerID INT,
                    ContractNumber VARCHAR(50), ContractAmount DECIMAL(18,2), Status VARCHAR(30));
                CREATE TABLE Invoices(InvoiceID INT PRIMARY KEY, OrderID INT UNIQUE, CustomerID INT,
                    InvoiceNumber VARCHAR(50), TotalAmount DECIMAL(18,2), DiscountAmount DECIMAL(18,2),
                    TaxAmount DECIMAL(18,2), Status VARCHAR(30));
                CREATE TABLE Deliveries(DeliveryID INT PRIMARY KEY, OrderID INT UNIQUE, DeliveryStatus VARCHAR(30));
                CREATE TABLE CarModels(ModelID INT PRIMARY KEY, ModelName VARCHAR(100));
                CREATE TABLE Carversions(VersionID INT PRIMARY KEY, VersionName VARCHAR(100));
                CREATE TABLE CarStatuses(CarStatusID INT PRIMARY KEY, StatusName VARCHAR(30));
                CREATE TABLE Cars(CarID INT PRIMARY KEY, VIN VARCHAR(17), ModelID INT, VersionID INT,
                    CarStatusID INT, Price DECIMAL(18,2));
                CREATE TABLE OrderDetails(OrderDetailID INT PRIMARY KEY, OrderID INT, CarID INT UNIQUE, SalesPrice DECIMAL(18,2));
                CREATE TABLE Payments(PaymentID INT PRIMARY KEY, OrderID INT, PaymentStatusID INT,
                    PaymentDate TIMESTAMP(7), PaymentAmount DECIMAL(18,2));
                CREATE TABLE InstallmentContracts(InstallmentContractID INT PRIMARY KEY, OrderID INT UNIQUE, Status VARCHAR(30));
                CREATE TABLE InstallmentSchedules(ScheduleID INT PRIMARY KEY, InstallmentContractID INT,
                    DueDate DATE, PaymentAmount DECIMAL(18,2), Status VARCHAR(30));
                CREATE TABLE InstallmentPayments(InstallmentPaymentID INT PRIMARY KEY, ScheduleID INT,
                    PaymentStatusID INT, PaymentDate TIMESTAMP(7), PaymentAmount DECIMAL(18,2));
                INSERT INTO OrderStatuses VALUES(1,'COMPLETED'),(2,'CANCELLED'),(3,'PENDING'),(4,'PAID');
                INSERT INTO PaymentStatuses VALUES(1,'COMPLETED'),(2,'PENDING'),(3,'FAILD');
                INSERT INTO Customers VALUES(1,'=HYPERLINK("malicious")');
                INSERT INTO Employees VALUES(1,'Nguyen Van A');
                INSERT INTO CarModels VALUES(1,'VF8'),(2,'VF9');
                INSERT INTO Carversions VALUES(1,'Eco');
                INSERT INTO CarStatuses VALUES(1,'SOLD'),(2,'AVAILABLE');
                INSERT INTO Orders VALUES
                    (1,1,1,1,1,'2026-01-31 23:59:59.9999999',300),
                    (2,1,1,2,1,'2026-01-20',500),
                    (3,1,NULL,3,2,'2026-01-10',200),
                    (4,1,1,1,1,'2025-12-15',400),
                    (5,1,1,4,1,'2026-02-01',600),
                    (6,1,1,1,1,'2026-01-15',800),
                    (7,1,1,4,1,'2026-01-21',100),
                    (8,1,1,1,1,'2026-01-25',350);
                INSERT INTO Cars VALUES
                    (1,'VIN1',1,1,1,900),(2,'VIN2',2,1,1,900),(3,'VIN3',1,1,2,500),
                    (4,'VIN4',1,1,2,200),(5,'VIN5',1,1,1,400),(6,'VIN6',1,1,1,600),
                    (7,'VIN7',1,1,1,800),(8,'VIN8',1,1,1,100),(9,'VIN9',1,1,1,350);
                INSERT INTO OrderDetails VALUES(1,1,1,100),(2,1,2,200),(3,2,3,500),(4,3,4,200),
                    (5,4,5,400),(6,5,6,600),(7,6,7,800),(8,7,8,100),(9,8,9,350);
                INSERT INTO Payments VALUES
                    (1,1,1,'2026-01-10',100),(2,1,1,'2026-01-31 23:59:59.9999999',200),
                    (3,2,1,'2026-01-20',500),(4,3,1,'2025-12-31',50),
                    (5,3,2,'2026-01-20',150),(6,1,3,'2026-01-20',999),
                    (7,4,1,'2026-01-03',40),(8,6,1,'2026-01-15',800),
                    (9,7,1,'2026-01-21',100),(10,8,1,'2026-01-25',350),
                    (11,5,1,'2026-02-01',600);
                INSERT INTO Invoices VALUES(1,6,1,'CANCELLED-INV',800,0,0,'CANCELLED'),
                    (2,8,1,'INV-8',350,20,10,'ISSUED');
                INSERT INTO Contracts VALUES(1,7,1,'MISMATCH',110,'SIGNED'),(2,8,1,'CON-8',350,'SIGNED');
                INSERT INTO Deliveries VALUES(1,8,'DELIVERED');
                INSERT INTO InstallmentContracts VALUES(1,1,'ACTIVE'),(2,3,'ACTIVE'),(3,2,'ACTIVE');
                INSERT INTO InstallmentSchedules VALUES(1,1,'2026-01-01',100,'PENDING'),
                    (2,1,'2026-02-01',200,'PENDING'),(3,3,'2026-01-01',500,'PENDING');
                INSERT INTO InstallmentPayments VALUES(1,1,1,'2026-01-02',30),
                    (2,1,3,'2026-01-03',999),(3,2,1,'2026-01-28',20),(4,3,1,'2026-01-10',500);
                """;
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
        service = new ReportService(new ReportRepository(() -> DriverManager.getConnection(url)), CLOCK);
    }

    @Test
    void sumsEachOrderAndPaymentOnceAndUsesDistinctCalendarPeriods() throws Exception {
        ReportData report = service.generate(JANUARY);
        assertMoney("690", report.getSummary().getRevenue()); // Includes receipt for an older order, not its sales value.
        assertMoney("650", report.getSummary().getSalesAmount());
        assertEquals(3, report.getSummary().getVehiclesSold());
        assertEquals(6, report.getSummary().getTotalOrders());
        assertEquals(2, report.getSummary().getCompletedOrders());
        assertEquals(1, report.getSummary().getCancelledOrders());
        assertEquals(2, report.getSummary().getInconsistentOrders());
        assertEquals(4, report.getSummary().getCompletedPayments());
        assertMoney("150", report.getSummary().getOutstandingAmount()); // December successful payment reduces January
        // balance.
        assertMoney("20", report.getSummary().getDiscountAmount());
        assertMoney("10", report.getSummary().getTaxAmount());
        assertEquals(1, report.getSummary().getContracts());
        assertEquals(2, report.getModels().size());
        assertMoney("450", report.getModels().getFirst().getSalesAmount()); // Historical sales price, not current Cars.Price.
        assertMoney("200", report.getModels().get(1).getSalesAmount());
        assertMoney("690", report.getMonthly().getFirst().getRevenue());
        assertMoney("650", report.getMonthly().getFirst().getSalesAmount());
        assertMoney("300",
            report.getSales().stream().filter(s -> s.getOrderId() == 1).findFirst().orElseThrow().getPaidAmount());
    }

    @Test
    void keepsInstallmentReceiptsSeparateAndExcludesFailedOrCancelledDebts() throws Exception {
        ReportData report = service.generate(JANUARY);
        assertMoney("50", report.getSummary().getInstallmentReceipts());
        assertMoney("70", report.getSummary().getOverdueAmount()); // February 1 is due today, not overdue.
        assertEquals(2, report.getSummary().getInstallmentContracts());
        assertEquals(9, report.getInventory().stream().mapToLong(StatusTotal::getCount).sum());
    }

    @Test
    void modelAndEmployeeFiltersSelectWholeOrdersWithoutMultiplyingAmounts() throws Exception {
        ReportFilter vf9 = new ReportFilter(JANUARY.getFrom(), JANUARY.getTo(), 1, 2, 1);
        ReportData report = service.generate(vf9);
        assertEquals(1, report.getSummary().getTotalOrders());
        assertMoney("300", report.getSummary().getRevenue());
        assertMoney("300", report.getSummary().getSalesAmount());
        assertEquals(2, report.getSummary().getVehiclesSold()); // Whole order containing VF9 also contains VF8.
        assertEquals(1, report.getInventory().stream().mapToLong(StatusTotal::getCount).sum());
        assertMoney("70", report.getSummary().getOverdueAmount());
    }

    @Test
    void emptyIntervalHasZeroTotalsAndCsvProtectsSpreadsheetCells() throws Exception {
        ReportData empty = service
            .generate(new ReportFilter(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null, null, null));
        assertMoney("0", empty.getSummary().getRevenue());
        assertEquals(0, empty.getSummary().getVehiclesSold());
        assertTrue(empty.getSales().isEmpty());
        StringWriter writer = new StringWriter();
        ReportCsv.write(service.generate(JANUARY), writer);
        assertTrue(writer.toString().startsWith("\uFEFF"));
        assertTrue(writer.toString().contains("\"'=HYPERLINK(\"\"malicious\"\")\""));
        assertEquals("\"' +SUM(1)\"", ReportCsv.cell(" +SUM(1)"));
        assertEquals("\"a,\"\"b\"\"\"", ReportCsv.cell("a,\"b\""));
        assertEquals("\"-10\"", ReportCsv.cell(new BigDecimal("-10")));
    }

    @Test
    void validatesDatesAndNumericFilters() {
        assertThrows(IllegalArgumentException.class,
            () -> ReportFilter.parse("2026-02-30", "2026-03-01", null, null, null, LocalDate.now(CLOCK)));
        assertThrows(IllegalArgumentException.class,
            () -> ReportFilter.parse("2026-02-01", "2026-01-01", null, null, null, LocalDate.now(CLOCK)));
        assertThrows(IllegalArgumentException.class,
            () -> ReportFilter.parse(null, null, "1 OR 1=1", null, null, LocalDate.now(CLOCK)));
        assertThrows(IllegalArgumentException.class,
            () -> ReportFilter.parse(null, null, null, "-1", null, LocalDate.now(CLOCK)));
        ReportFilter defaults = ReportFilter.parse(null, null, null, null, null, LocalDate.of(2026, 10, 4));
        assertEquals(LocalDate.of(2026, 10, 1), defaults.getFrom());
        assertEquals(LocalDate.of(2026, 10, 4), defaults.getTo());
        ReportFilter mutable = new ReportFilter();
        mutable.setFrom(LocalDate.of(2026, 1, 1));
        mutable.setTo(LocalDate.of(2026, 1, 31));
        mutable.setEmployeeId(-1);
        assertThrows(IllegalArgumentException.class, () -> service.generate(mutable));
        mutable.setEmployeeId(null);
        mutable.setTo(LocalDate.of(2025, 12, 31));
        assertThrows(IllegalArgumentException.class, () -> service.generate(mutable));
    }

    @Test
    void xlsxIsValidOoxmlWithNumericMoneyAndLiteralCustomerText() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ReportXlsx.write(service.generate(JANUARY), output);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertEquals(5, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("Định nghĩa"));
            Cell revenue = workbook.getSheet("Tổng quan").getRow(4).getCell(1);
            assertEquals(CellType.NUMERIC, revenue.getCellType());
            assertEquals(690.00, revenue.getNumericCellValue(), 0.001);

            boolean foundLiteralCustomer = false;
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        assertNotEquals(CellType.FORMULA, cell.getCellType());
                        if (cell.getCellType() == CellType.STRING
                            && "=HYPERLINK(\"malicious\")".equals(cell.getStringCellValue())) {
                            foundLiteralCustomer = true;
                        }
                    }
                }
            }
            assertTrue(foundLiteralCustomer);
        }
    }

    @Test
    void labelsDemoSalesAtStartOfCsvAndXlsxWithoutChangingTotals() throws Exception {
        ReportData ordinary = service.generate(JANUARY);
        assertFalse(ordinary.getDefinitions().containsKey("demo"));
        try (Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE Cars SET VIN='DEMO0000000000001' WHERE CarID=1");
        }

        ReportData demo = service.generate(JANUARY);
        String message = demo.getDefinitions().get("demo");
        assertNotNull(message);
        assertTrue(message.contains("mô phỏng"));
        assertTrue(message.contains("không phải doanh thu thực tế của VinFast"));
        assertEquals(ordinary.getSummary().getRevenue(), demo.getSummary().getRevenue());
        assertEquals(ordinary.getSummary().getSalesAmount(), demo.getSummary().getSalesAmount());
        assertEquals(ordinary.getSummary().getVehiclesSold(), demo.getSummary().getVehiclesSold());

        StringWriter csv = new StringWriter();
        ReportCsv.write(demo, csv);
        String[] rows = csv.toString().split("\r\n");
        assertTrue(rows[1].startsWith("\"Period\""));
        assertTrue(rows[2].startsWith("\"Source\""));
        assertTrue(rows[2].contains(message));
        assertTrue(csv.toString().contains("demo: " + message));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ReportXlsx.write(demo, output);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            Sheet summary = workbook.getSheet("Tổng quan");
            assertEquals("Nguồn dữ liệu", summary.getRow(1).getCell(0).getStringCellValue());
            assertEquals(message, summary.getRow(1).getCell(1).getStringCellValue());
            assertEquals(690.00, summary.getRow(5).getCell(1).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void demoVehicleExcludedByModelDoesNotLabelOrdinaryOrEmptyReport() throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE Cars SET VIN='DEMO0000000000005' WHERE CarID=5");
        }
        ReportFilter ordinaryModel = new ReportFilter(JANUARY.getFrom(), JANUARY.getTo(), null, 2, null);
        ReportData january = service.generate(ordinaryModel);
        assertFalse(january.getDefinitions().containsKey("demo"));
        StringWriter csv = new StringWriter();
        ReportCsv.write(january, csv);
        assertTrue(csv.toString().split("\r\n")[2].startsWith("\"Summary\""));

        ReportData december = service
            .generate(new ReportFilter(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31), null, null, null));
        assertTrue(december.getDefinitions().containsKey("demo"));
        ReportData empty = service
            .generate(new ReportFilter(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null, 2, null));
        assertFalse(empty.getDefinitions().containsKey("demo"));
    }

    @Test
    void labelsDemoPaymentFromOrderCreatedOutsideSelectedPeriod() throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE Cars SET VIN='DEMO0000000000001' WHERE CarID=1");
        }
        // Đơn chứa hai mẫu xe; bộ lọc mẫu 2 có tồn kho thật nhưng khoản thu có xe DEMO mẫu 1.
        ReportData report = service
            .generate(new ReportFilter(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), null, 2, null));
        assertEquals(0, report.getSummary().getTotalOrders());
        assertMoney("100", report.getSummary().getRevenue());
        assertMoney("0", report.getSummary().getSalesAmount());
        assertEquals(1, report.getInventory().getFirst().getCount());
        assertTrue(report.getDefinitions().containsKey("demo"));
        StringWriter csv = new StringWriter();
        ReportCsv.write(report, csv);
        assertTrue(csv.toString().split("\r\n")[2].startsWith("\"Source\""));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ReportXlsx.write(report, output);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertEquals("Nguồn dữ liệu", workbook.getSheet("Tổng quan").getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void labelsDemoInstallmentReceiptFromOrderCreatedOutsideSelectedPeriod() throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE Cars SET VIN='DEMO0000000000001' WHERE CarID=1");
        }
        ReportData report = service
            .generate(new ReportFilter(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 2), null, 2, null));
        assertEquals(0, report.getSummary().getTotalOrders());
        assertMoney("0", report.getSummary().getRevenue());
        assertMoney("30", report.getSummary().getInstallmentReceipts());
        assertTrue(report.getDefinitions().containsKey("demo"));
    }

    @Test
    void currentDemoInventoryIsLabelledEvenWhenThereAreNoSalesInPeriod() throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE Cars SET VIN='DEMO0000000000005' WHERE CarID=5");
        }
        ReportData report = service
            .generate(new ReportFilter(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null, null, null));
        assertEquals(0, report.getSummary().getTotalOrders());
        assertMoney("0", report.getSummary().getRevenue());
        assertEquals(9, report.getInventory().stream().mapToLong(StatusTotal::getCount).sum());
        assertTrue(report.getDefinitions().containsKey("demo"));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "Expected " + expected + ", got " + actual);
    }
}
