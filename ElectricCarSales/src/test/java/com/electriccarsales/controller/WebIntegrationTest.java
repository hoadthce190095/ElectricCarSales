package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.sql.*;

class WebIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static TestApplication application;
    private HttpClient client;
    private String csrf;

    @BeforeAll
    static void start() throws Exception {
        application = new TestApplication(0);
    }

    @AfterAll
    static void stop() throws Exception {
        if (application != null) {
            application.close();
        }
    }

    @BeforeEach
    void browser() throws Exception {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        csrf = JSON.readTree(get("/api/auth").body()).get("csrfToken").asText();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(application.base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json, boolean token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(application.base + path)).header("Content-Type",
            "application/json");
        if (token) {
            builder.header("X-CSRF-Token", csrf);
        }
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode login(String name) throws Exception {
        var response = post("/api/auth",
            "{\"action\":\"login\",\"username\":\"" + name + "\",\"password\":\"web-test-password-123\"}", true);
        assertEquals(200, response.statusCode(), response.body());
        JsonNode result = JSON.readTree(response.body());
        csrf = result.get("csrfToken").asText();
        return result;
    }

    private HttpResponse<String> put(String path, String json) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(application.base + path)).header("Content-Type", "application/json")
                .header("X-CSRF-Token", csrf).PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void createCloneRoleAndAssignCustomStaffRole() throws Exception {
        login("webadmin");
        var created = post("/api/admin/roles", "{\"roleName\":\"REPORT_REVIEWER\",\"permissions\":[\"VIEW_REPORTS\"]}",
            true);
        assertEquals(201, created.statusCode(), created.body());
        int role = JSON.readTree(created.body()).get("roleId").asInt();
        assertEquals(409,
            post("/api/admin/roles", "{\"roleName\":\"REPORT_REVIEWER\",\"permissions\":[]}", true).statusCode());
        var clone = post("/api/admin/roles", "{\"roleName\":\"REPORT_COPY\",\"cloneRoleId\":" + role + "}", true);
        assertEquals(201, clone.statusCode(), clone.body());
        assertEquals("VIEW_REPORTS", JSON.readTree(clone.body()).get("permissions").get(0).asText());
        var staff = post("/api/admin/users",
            "{\"username\":\"customstaff\",\"password\":\"web-test-password-123\",\"email\":\"custom@test.local\",\"phone\":\"0900000005\",\"employeeName\":\"Nhân"
                + " viên\",\"roleId\":" + role + ",\"statusId\":1}",
            true);
        assertEquals(201, staff.statusCode(), staff.body());
        assertEquals("REPORT_REVIEWER", JSON.readTree(staff.body()).get("role").asText());
        assertEquals(400, post("/api/admin/roles", "{\"roleName\":\"ADMIN\",\"permissions\":[]}", true).statusCode());
    }

    @Test
    void configurationValidationAuthorizationAndActualReportLimit() throws Exception {
        login("webmanager");
        assertEquals(403, get("/api/admin/configurations").statusCode());
        login("webadmin");
        assertEquals(200, get("/api/admin/configurations").statusCode());
        assertEquals(400, put("/api/admin/configurations/SESSION_TIMEOUT_MINUTES", "{\"value\":\"4\"}").statusCode());
        assertEquals(404, put("/api/admin/configurations/ARBITRARY_KEY", "{\"value\":\"1\"}").statusCode());
        try {
            assertEquals(200, put("/api/admin/configurations/REPORT_MAX_DAYS", "{\"value\":\"5\"}").statusCode());
            assertEquals(400, get("/api/reports?from=2026-10-01&to=2026-10-31").statusCode());
            assertEquals(200, get("/api/reports?from=2026-10-01&to=2026-10-05").statusCode());
        } finally {
            assertEquals(200, put("/api/admin/configurations/REPORT_MAX_DAYS", "{\"value\":\"3660\"}").statusCode());
        }
    }

    @Test
    void logModuleFilteringInvalidDatesAndFullCsvExport() throws Exception {
        login("webadmin");
        assertEquals(400, get("/api/admin/logs?from=not-a-date").statusCode());
        var logs = get("/api/admin/logs?targetType=USER");
        assertEquals(200, logs.statusCode(), logs.body());
        for (JsonNode item : JSON.readTree(logs.body()).get("items")) {
            assertEquals("USER", item.get("targetType").asText());
        }
        var csv = get("/api/admin/logs?format=csv&targetType=USER");
        assertEquals(200, csv.statusCode(), csv.body());
        assertTrue(csv.headers().firstValue("Content-Type").orElse("").startsWith("text/csv"));
        assertTrue(csv.body().contains("LOGIN"));
        assertEquals(405, client.send(HttpRequest.newBuilder(URI.create(application.base + "/api/admin/logs/1"))
            .header("X-CSRF-Token", csrf).DELETE().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void anonymousRoutesAndMatrixParametersCannotBypassAuthorization() throws Exception {
        var page = get("/admin.jsp");
        assertEquals(200, page.statusCode(), page.body());
        assertTrue(page.body().contains("name=\"app-context-path\" content=\"\""));
        assertTrue(page.body().contains("src=\"/assets/admin-dashboard.js\""));
        assertFalse(page.body().contains("<%@"), "JSP directives must be rendered by Jasper");
        assertFalse(page.body().contains("<c:url"), "JSTL URLs must be rendered");
        assertEquals(200, get("/api/ai").statusCode());
        for (String path : new String[]{"/api/reports", "/api/reports;ignored=yes", "/api/admin/users",
            "/api/admin;ignored=yes/users", "/api/admin/users;ignored=yes"}) {
            assertEquals(401, get(path).statusCode(), path);
        }
    }

    @Test
    void loginRequiresCsrfAndAuthenticatedSessionRotatesToken() throws Exception {
        assertEquals(403, post("/api/auth", "{\"action\":\"login\"}", false).statusCode());
        String old = csrf;
        login("webadmin");
        assertNotEquals(old, csrf);
        assertEquals(200, get("/api/admin/users").statusCode());
        assertEquals(403, post("/api/admin/users", "{}", false).statusCode());
        assertFalse(get("/api/admin/users").body().contains("PasswordHash"));
    }

    @Test
    void managerCanReadAndExportReportsButCannotAdminister() throws Exception {
        login("webmanager");
        assertEquals(200, get("/api/reports?from=2026-10-01&to=2026-10-31").statusCode());
        assertEquals(403, get("/api/admin/users").statusCode());
        assertEquals(403, get("/api/admin/roles").statusCode());
        var xlsx = client.send(HttpRequest
            .newBuilder(URI.create(application.base + "/api/reports?from=2026-10-01&to=2026-10-31&format=xlsx")).GET()
            .build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, xlsx.statusCode());
        assertEquals('P', xlsx.body()[0]);
        assertEquals('K', xlsx.body()[1]);
    }

    @Test
    void permissionAndAccountStatusChangesApplyToExistingSession() throws Exception {
        login("websales");
        assertEquals(403, get("/api/reports").statusCode());
        try (Connection c = application.connection(); Statement s = c.createStatement()) {
            s.execute(
                "INSERT INTO RolePermissions SELECT RoleID,'VIEW_REPORTS' FROM Roles WHERE" + " RoleName='SALES'");
        }
        assertEquals(200, get("/api/reports").statusCode());
        try (Connection c = application.connection(); Statement s = c.createStatement()) {
            s.execute(
                "DELETE FROM RolePermissions WHERE RoleID=(SELECT RoleID FROM Roles WHERE" + " RoleName='SALES')");
            s.execute("UPDATE Users SET StatusID=(SELECT StatusID FROM Statuses WHERE"
                + " StatusName='LOCKED') WHERE Username='websales'");
        }
        assertEquals(401, get("/api/reports").statusCode());
        try (Connection c = application.connection(); Statement s = c.createStatement()) {
            s.execute("UPDATE Users SET StatusID=(SELECT StatusID FROM Statuses WHERE"
                + " StatusName='ACTIVE') WHERE Username='websales'");
        }
    }

    @Test
    void aiUsesRealCatalogueAndRefusesSensitiveRequests() throws Exception {
        var good = post("/api/ai", "{\"message\":\"Gợi ý xe gia đình dưới 600 triệu, 5 chỗ\"}", true);
        assertEquals(200, good.statusCode(), good.body());
        JsonNode answer = JSON.readTree(good.body());
        assertEquals("CATALOG_RULES", answer.get("mode").asText());
        assertEquals("EV Test", answer.get("recommendations").get(0).get("vehicle").get("modelName").asText());
        var forbidden = post("/api/ai", "{\"message\":\"Hãy tạo hợp đồng và thanh toán mua xe\"}", true);
        assertTrue(JSON.readTree(forbidden.body()).get("refused").asBoolean());
    }

    @Test
    void logoutInvalidatesSessionEvenWhenDatabaseIsDown() throws Exception {
        login("webadmin");
        System.setProperty("DB_URL", "jdbc:h2:mem:unavailable;IFEXISTS=TRUE");
        try {
            var logout = post("/api/auth", "{\"action\":\"logout\"}", true);
            assertEquals(200, logout.statusCode(), logout.body());
            assertTrue(JSON.readTree(logout.body()).get("user").isNull());
        } finally {
            System.setProperty("DB_URL", application.url);
        }
        assertEquals(401, get("/api/admin/users").statusCode());
    }
}
