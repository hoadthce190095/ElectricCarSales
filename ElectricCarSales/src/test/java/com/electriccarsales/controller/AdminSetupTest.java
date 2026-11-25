package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import com.electriccarsales.service.PasswordHasher;
import com.electriccarsales.service.AdminSetupService;

import java.lang.reflect.Proxy;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class AdminSetupTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void setupWorksWhenBaseSchemaHasNoSystemLogsTable() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection(
                "jdbc:h2:mem:setupRollback;MODE=MSSQLServer;NON_KEYWORDS=YEAR", "sa", TestApplication.H2_PASSWORD);
            var statement = connection.createStatement()) {
            TestApplication.initializeSchema(statement);
            statement.execute("DROP TABLE SystemLogs");
            assertDoesNotThrow(() -> new AdminSetupService().create(connection, "baseadmin",
                "Local-chosen-password-123", "Kiểm thử schema cơ sở"));
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM Users WHERE Username='baseadmin'")) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        }
    }

    @Test
    void systemLogsEndpointDataReadsFromExistingTablesWhenSystemLogsTableAbsent() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection(
                "jdbc:h2:mem:logsUnavailable;MODE=MSSQLServer;NON_KEYWORDS=YEAR", "sa", TestApplication.H2_PASSWORD);
            var statement = connection.createStatement()) {
            TestApplication.initializeSchema(statement);
            statement.execute("DROP TABLE SystemLogs");
            var result = new com.electriccarsales.dao.AdministrationDAO().logs(connection, "", null,
                java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now(), 1, 20, "");
            assertTrue(result.isAvailable());
            assertNotNull(result.getItems());
        }
    }

    @Test
    void setupRequiresLocalAddressAndExplicitServerOption() {
        String previous = System.getProperty("app.adminSetup.enabled");
        try {
            System.clearProperty("app.adminSetup.enabled");
            assertFalse(AdminSetupController.allowedHere(requestFrom("127.0.0.1")));
            System.setProperty("app.adminSetup.enabled", "true");
            assertTrue(AdminSetupController.allowedHere(requestFrom("127.0.0.1")));
            assertTrue(AdminSetupController.allowedHere(requestFrom("::1")));
            assertFalse(AdminSetupController.allowedHere(requestFrom("192.168.1.20")));
        } finally {
            restore(previous);
        }
    }

    @Test
    void userChoosesFirstAdminPasswordWithCsrfAndCannotReopenSetup() throws Exception {
        String previous = System.getProperty("app.adminSetup.enabled");
        try (TestApplication application = new TestApplication(0, false)) {
            System.setProperty("app.adminSetup.enabled", "true");
            HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
            String csrf = JSON.readTree(get(client, application.base, "/api/auth").body()).get("csrfToken").asText();
            assertTrue(JSON.readTree(get(client, application.base, "/api/setup").body()).get("available").asBoolean());
            String input = "{\"username\":\"chosenadmin\",\"password\":\"Local-chosen-password-123\","
                + "\"confirmPassword\":\"Local-chosen-password-123\"}";
            assertEquals(403, post(client, application.base, "/api/setup", input, null).statusCode());
            assertEquals(400,
                post(client, application.base, "/api/setup",
                    input.replace("\"confirmPassword\":\"Local-chosen-password-123\"",
                        "\"confirmPassword\":\"different-password\""),
                    csrf).statusCode());
            assertEquals(201, post(client, application.base, "/api/setup", input, csrf).statusCode());
            assertFalse(JSON.readTree(get(client, application.base, "/api/setup").body()).get("available").asBoolean());
            assertEquals(409, post(client, application.base, "/api/setup", input, csrf).statusCode());

            try (var connection = application.connection();
                var statement = connection.createStatement();
                var result = statement
                    .executeQuery("SELECT PasswordHash FROM Users WHERE" + " Username='chosenadmin'")) {
                assertTrue(result.next());
                assertTrue(PasswordHasher.verify("Local-chosen-password-123", result.getString(1)));
                assertNotEquals("Local-chosen-password-123", result.getString(1));
            }
            var loggedIn = post(client, application.base, "/api/auth",
                "{\"action\":\"login\",\"username\":\"chosenadmin\",\"password\":\"Local-chosen-password-123\"}", csrf);
            assertEquals(200, loggedIn.statusCode(), loggedIn.body());
            assertEquals("ADMIN", JSON.readTree(loggedIn.body()).get("user").get("role").asText());
            assertEquals(200, get(client, application.base, "/api/reports").statusCode());
            try (var connection = application.connection();
                var statement = connection.createStatement();
                var result = statement
                    .executeQuery("SELECT COUNT(*) FROM SystemLogs WHERE" + " Action='ADMIN_BOOTSTRAP'")) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        } finally {
            restore(previous);
        }
    }

    private static HttpServletRequest requestFrom(String address) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
            new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
                if (method.getName().equals("getRemoteAddr")) {
                    return address;
                }
                if (method.getName().equals("getHeader")) {
                    return "127.0.0.1";
                }
                return null;
            });
    }

    private static void restore(String previous) {
        if (previous == null) {
            System.clearProperty("app.adminSetup.enabled");
        } else {
            System.setProperty("app.adminSetup.enabled", previous);
        }
    }

    private static HttpResponse<String> get(HttpClient client, String base, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String base, String path, String body, String csrf)
        throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        if (csrf != null) {
            builder.header("X-CSRF-Token", csrf);
        }
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
    }
}
