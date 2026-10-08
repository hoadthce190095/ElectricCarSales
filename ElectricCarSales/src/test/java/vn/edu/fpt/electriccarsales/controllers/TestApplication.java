package vn.edu.fpt.electriccarsales.controllers;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import vn.edu.fpt.electriccarsales.security.PasswordHasher;
import vn.edu.fpt.electriccarsales.security.SecurityFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Disposable H2 + actual Servlet container, independent of any local SQL Server data. */
public final class TestApplication implements AutoCloseable {
    final Tomcat tomcat;
    final String url;
    final String base;
    private final Path temporary;

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public TestApplication(int port) throws Exception {
        this(port, true);
    }

    public TestApplication(int port, boolean initialAdmin) throws Exception {
        url = "jdbc:h2:mem:web" + System.nanoTime() + ";MODE=MSSQLServer;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1";
        System.setProperty("DB_URL", url);
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", "");
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute(
                "CREATE ALIAS SYSDATETIME FOR" + " 'vn.edu.fpt.electriccarsales.controllers.TestApplication.now'");
            String ddl = Files.readString(Path.of("../ElectricCarSalesDB.sql"));
            ddl = ddl.substring(0, ddl.indexOf("-- BEGIN DEMO DATA"));
            // H2 dung bang va du lieu mac dinh tu SQL chung, bo qua USE/GO va trigger cua SQL Server.
            Pattern sqlPattern = Pattern.compile(
                "(?is)\\b(?:CREATE\\s+TABLE|INSERT\\s+INTO)\\s+dbo\\.\\w+\\s*\\(.*?;");
            Matcher sqlMatcher = sqlPattern.matcher(ddl);
            while (sqlMatcher.find()) {
                s.execute(sqlMatcher.group().replace("dbo.", ""));
            }
            if (initialAdmin) {
                insertUser(c, "webadmin", "admin@test.local", "0900000001", "ADMIN");
            }
            insertUser(c, "webmanager", "manager@test.local", "0900000002", "MANAGER");
            insertUser(c, "websales", "sales@test.local", "0900000003", "SALES");
            s.execute("INSERT INTO Brands(BrandName,BrandStatusID) SELECT N'Test"
                + " Motors',BrandStatusID FROM BrandStatuses WHERE StatusName='ACTIVE'");
            s.execute("INSERT INTO CarLines(BrandID,CarLineName) VALUES(1,N'Test electric')");
            s.execute("INSERT INTO CarModels(CarLineID,ModelName,Description) VALUES(1,N'EV"
                + " Test',N'Dữ liệu kiểm thử'),(1,N'EV Family',N'Dữ liệu kiểm thử')");
            s.execute("INSERT INTO Specifications(ModelID,BatteryCapacity,RangeKM,SeatingCapacity)"
                + " VALUES(1,50,350,5),(2,70,450,7)");
            s.execute("INSERT INTO Colors(ColorName) VALUES(N'Trắng')");
            s.execute("INSERT INTO Carversions(ModelID,VersionName)" + " VALUES(1,N'Standard'),(2,N'Plus')");
            s.execute("INSERT INTO CarStatuses(StatusName) VALUES('AVAILABLE')");
            s.execute("INSERT INTO" + " Cars(CarCode,ModelID,VersionID,ColorID,CarStatusID,Year,VIN,EngineNumber,Price)"
                + " VALUES('TEST1',1,1,1,1,2026,'TEST0000000000001','TESTENGINE1',500000000),('TEST2',2,2,1,1,2026,'TEST0000000000002','TESTENGINE2',800000000)");
        }
        temporary = Files.createTempDirectory("electriccars-web-test-");
        tomcat = new Tomcat();
        tomcat.setBaseDir(temporary.toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addContext("", Path.of("src/main/webapp").toAbsolutePath().toString());
        Tomcat.addServlet(context, "auth", new AuthenticationController());
        context.addServletMappingDecoded("/api/auth", "auth");
        Tomcat.addServlet(context, "setup", new AdminSetupController());
        context.addServletMappingDecoded("/api/setup", "setup");
        Tomcat.addServlet(context, "admin", new AdministrationController());
        context.addServletMappingDecoded("/api/admin/*", "admin");
        Tomcat.addServlet(context, "reports", new ReportController());
        context.addServletMappingDecoded("/api/reports", "reports");
        Tomcat.addServlet(context, "ai", new AIController());
        context.addServletMappingDecoded("/api/ai", "ai");
        Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
        context.addServletMappingDecoded("/", "default");
        FilterDef definition = new FilterDef();
        definition.setFilterName("security");
        definition.setFilter(new SecurityFilter());
        context.addFilterDef(definition);
        FilterMap mapping = new FilterMap();
        mapping.setFilterName("security");
        mapping.addURLPattern("/api/*");
        context.addFilterMap(mapping);
        context.setSessionTimeout(30);
        context.setUseHttpOnly(true);
        tomcat.start();
        base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort();
    }

    public Connection connection() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private void insertUser(Connection c, String name, String email, String phone, String role) throws SQLException {
        try (PreparedStatement s = c
            .prepareStatement("INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                + " SELECT ?,?,?,?,r.RoleID,t.StatusID FROM Roles r CROSS JOIN Statuses"
                + " t WHERE r.RoleName=? AND t.StatusName='ACTIVE'")) {
            s.setString(1, name);
            s.setString(2, PasswordHasher.hash("web-test-password-123"));
            s.setString(3, email);
            s.setString(4, phone);
            s.setString(5, role);
            s.executeUpdate();
        }
    }

    @Override
    public void close() throws Exception {
        tomcat.stop();
        tomcat.destroy();
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("SHUTDOWN");
        }
        System.clearProperty("DB_URL");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_PASSWORD");
    }

    public static void main(String[] args) throws Exception {
        TestApplication app = new TestApplication(args.length == 0 ? 18081 : Integer.parseInt(args[0]));
        System.out.println("TEST PREVIEW " + app.base + "/tan.html (disposable test data)");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.close();
            } catch (Exception ignored) {
            }
        }));
        app.tomcat.getServer().await();
    }
}
