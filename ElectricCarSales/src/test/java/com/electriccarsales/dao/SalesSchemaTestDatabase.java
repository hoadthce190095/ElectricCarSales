package com.electriccarsales.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Disposable database built from the actual shared SQL schema, without SQL Server or demo data. */
public final class SalesSchemaTestDatabase implements AutoCloseable {
    private final String url = "jdbc:h2:mem:sales_" + UUID.randomUUID()
        + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";

    public SalesSchemaTestDatabase() throws Exception {
        try (Connection connection = open(); var statement = connection.createStatement()) {
            statement.execute("CREATE ALIAS SYSDATETIME FOR 'com.electriccarsales.dao.SalesSchemaTestDatabase.now'");
            String schema = Files.readString(Path.of("../ElectricCarSalesDB.sql"));
            schema = schema.substring(0, schema.indexOf("-- BEGIN DEMO DATA"));
            Matcher tables = Pattern.compile("(?is)\\bCREATE\\s+TABLE\\s+dbo\\.\\w+\\s*\\(.*?;").matcher(schema);
            while (tables.find()) {
                statement.execute(tables.group().replace("dbo.", ""));
            }
            // Non-sequential IDs ensure DAOs resolve lookup names rather than relying on seed order.
            statement.execute("INSERT INTO Roles VALUES(17,'CUSTOMER'),(29,'SALES')");
            statement.execute("INSERT INTO Statuses VALUES(11,'ACTIVE')");
            statement.execute("INSERT INTO Users(UserID,Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                + " VALUES(101,'dao-customer','unused','customer@test.local','0900000071',17,11),"
                + "(102,'dao-sales','unused','sales@test.local','0900000081',29,11)");
            statement.execute("INSERT INTO Customers(CustomerID,UserID,CustomerName,Phone)"
                + " VALUES(71,101,N'Khách hàng kiểm thử','0900000071')");
            statement.execute("INSERT INTO Employees(EmployeeID,UserID,EmployeeName,Phone)"
                + " VALUES(81,102,N'Nhân viên kiểm thử','0900000081')");
            statement.execute("INSERT INTO BrandStatuses VALUES(1,'ACTIVE')");
            statement.execute("INSERT INTO Brands(BrandID,BrandName,BrandStatusID) VALUES(10,'Test Motors',1)");
            statement.execute("INSERT INTO CarLines(CarLineID,BrandID,CarLineName) VALUES(11,10,'SUV')");
            statement.execute("INSERT INTO CarModels(ModelID,CarLineID,ModelName) VALUES(91,11,'EV Test')");
            statement.execute("INSERT INTO OrderStatuses(OrderStatusID,StatusName) VALUES(37,'PENDING'),(38,'PAID')");
            statement.execute("INSERT INTO PaymentMethods(PaymentMethodID,MethodName) VALUES(51,'CASH')");
            statement.execute("INSERT INTO ConsultationStatuses VALUES(63,'PENDING'),(64,'COMPLETED')");
        }
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public void close() throws SQLException {
        try (Connection connection = open(); var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }
}
