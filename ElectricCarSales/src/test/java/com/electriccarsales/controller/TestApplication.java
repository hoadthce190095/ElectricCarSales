package com.electriccarsales.controller;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.jasper.servlet.JasperInitializer;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.SimpleInstanceManager;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import com.electriccarsales.service.PasswordHasher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Disposable H2 + actual Servlet container, independent of any local SQL Server data. */
public final class TestApplication implements AutoCloseable {
    public static final String H2_PASSWORD = "electriccars-disposable-h2";
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
        System.setProperty("DB_PASSWORD", H2_PASSWORD);
        try (Connection c = connection(); Statement s = c.createStatement()) {
            initializeSchema(s);
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
        context.setParentClassLoader(TestApplication.class.getClassLoader());
        context.setInstanceManager(new SimpleInstanceManager());
        context.addServletContainerInitializer(new JasperInitializer(), null);
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
        Tomcat.addServlet(context, "comparisonHistory", new ComparisonHistoryController());
        context.addServletMappingDecoded("/api/comparison/history", "comparisonHistory");
        Tomcat.addServlet(context, "consultation", new ConsultationController());
        context.addServletMappingDecoded("/consultation", "consultation");
        Tomcat.addServlet(context, "logout", new LogoutController());
        context.addServletMappingDecoded("/logout", "logout");
        Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
        context.addServletMappingDecoded("/", "default");
        Tomcat.addServlet(context, "jsp", new JspServlet()).setLoadOnStartup(1);
        context.addServletMappingDecoded("*.jsp", "jsp");
        context.addWelcomeFile("index.jsp");
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

    public static void initializeSchema(Statement statement) throws Exception {
        statement.execute("CREATE ALIAS SYSDATETIME FOR 'com.electriccarsales.controller.TestApplication.now'");
        String baseline = Files.readString(Path.of("../ElectricCarSalesDB.sql"))
            .replaceAll("(?i)\\bdbo\\.", "");
        Pattern tables = Pattern.compile("(?ims)^\\h*CREATE\\s+TABLE\\s+\\w+\\s*\\(.*?^\\h*GO\\h*$");
        Matcher table = tables.matcher(baseline);
        while (table.find()) {
            String ddl = table.group().replaceFirst("(?im)^\\h*GO\\h*$", "");
            statement.execute(ddl);
        }
        Pattern lookups = Pattern.compile("(?ims)^\\h*INSERT\\s+INTO\\s+"
            + "(?:Roles|Statuses|BrandStatuses|ConsultationStatuses|OrderStatuses|PaymentMethods|PaymentTypes|PaymentStatuses)"
            + "\\s*\\(.*?;");
        Matcher lookup = lookups.matcher(baseline);
        while (lookup.find()) {
            statement.execute(lookup.group());
        }
        try (var input = TestApplication.class.getResourceAsStream("/admin-features-h2.sql")) {
            if (input == null) throw new IllegalStateException("Missing H2 test fixture");
            statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public Connection connection() throws SQLException {
        return DriverManager.getConnection(url, "sa", H2_PASSWORD);
    }

    private void insertUser(Connection c, String name, String email, String phone, String role) throws SQLException {
        try (PreparedStatement s = c
            .prepareStatement("INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                + " SELECT ?,?,?,?,r.RoleID,t.StatusID FROM Roles r CROSS JOIN Statuses"
                + " t WHERE r.RoleName=? AND t.StatusName='ACTIVE'")) {
            s.setString(1, name);
            s.setString(2, PasswordHasher.hash("Web-test-password-123"));
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
        System.out.println("TEST PREVIEW " + app.base + "/admin.jsp (disposable test data)");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.close();
            } catch (Exception ignored) {
            }
        }));
        app.tomcat.getServer().await();
    }
}
