package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.junit.jupiter.api.*;

import com.electriccarsales.service.PasswordHasher;
import com.electriccarsales.service.PasswordSession;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthPasswordTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ORIGINAL_PASSWORD = "web-test-password-123";
    private static final String NEW_PASSWORD = "chosen-new-password-456";
    private static TestApplication application;

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
    void resetDisposableAccounts() throws Exception {
        try (Connection connection = application.connection();
            PreparedStatement statement = connection.prepareStatement("UPDATE Users SET PasswordHash=?,StatusID=1")) {
            statement.setString(1, PasswordHasher.hash(ORIGINAL_PASSWORD));
            statement.executeUpdate();
        }
        try (Connection connection = application.connection(); Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM SystemLogs");
        }
    }

    @Test
    @Order(1)
    void changeRotatesCurrentSessionAndInvalidatesOtherBrowsers() throws Exception {
        Browser current = new Browser();
        Browser other = new Browser();
        current.login("webadmin", ORIGINAL_PASSWORD);
        other.login("webadmin", ORIGINAL_PASSWORD);
        String oldSessionId = current.sessionId();
        String oldToken = current.csrf;

        HttpResponse<String> response = current.change(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        assertEquals(200, response.statusCode(), response.body());
        JsonNode result = JSON.readTree(response.body());
        assertEquals("webadmin", result.get("user").get("username").asText());
        assertTrue(result.get("permissions").toString().contains("MANAGE_USERS"));
        assertNotEquals(oldToken, result.get("csrfToken").asText());
        assertNotEquals(oldSessionId, current.sessionId());
        assertFalse(response.body().contains(NEW_PASSWORD));
        assertFalse(response.body().contains("passwordFingerprint"));
        assertFalse(response.body().contains("pbkdf2"));
        current.csrf = result.get("csrfToken").asText();

        assertEquals(200, current.get("/api/admin/users").statusCode());
        assertEquals(401, other.get("/api/admin/users").statusCode());
        HttpResponse<String> oldSession = HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(application.base + "/api/admin/users"))
                    .header("Cookie", "JSESSIONID=" + oldSessionId).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, oldSession.statusCode());
        assertEquals(403, current
            .post("/api/auth", passwordBody(NEW_PASSWORD, "another-valid-password", "another-valid-password"), oldToken)
            .statusCode());
        assertTrue(PasswordHasher.verify(NEW_PASSWORD, passwordHash("webadmin")));
        assertFalse(PasswordHasher.verify(ORIGINAL_PASSWORD, passwordHash("webadmin")));
        assertEquals(1, passwordChanges());
        Browser fresh = new Browser();
        fresh.login("webadmin", NEW_PASSWORD);
    }

    @Test
    @Order(2)
    void incorrectCurrentPasswordKeepsAccountAndSessionUnchanged() throws Exception {
        Browser browser = new Browser();
        browser.login("webadmin", ORIGINAL_PASSWORD);
        String hashBefore = passwordHash("webadmin");
        HttpResponse<String> response = browser.change("wrong-current-password", NEW_PASSWORD, NEW_PASSWORD);
        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains("Mật khẩu hiện tại không đúng"));
        assertEquals(hashBefore, passwordHash("webadmin"));
        assertEquals(0, passwordChanges());
        assertEquals(200, browser.get("/api/admin/users").statusCode());
    }

    @Test
    @Order(3)
    void validatesLengthConfirmationAndDifferentPassword() throws Exception {
        Browser browser = new Browser();
        browser.login("webadmin", ORIGINAL_PASSWORD);
        String hashBefore = passwordHash("webadmin");
        String[][] cases = {{"", NEW_PASSWORD, NEW_PASSWORD}, {"x".repeat(129), NEW_PASSWORD, NEW_PASSWORD},
            {ORIGINAL_PASSWORD, "short", "short"}, {ORIGINAL_PASSWORD, "x".repeat(129), "x".repeat(129)},
            {ORIGINAL_PASSWORD, NEW_PASSWORD, "different-confirmation"},
            {ORIGINAL_PASSWORD, ORIGINAL_PASSWORD, ORIGINAL_PASSWORD}};
        for (String[] values : cases) {
            assertEquals(400, browser.change(values[0], values[1], values[2]).statusCode());
        }
        assertEquals(hashBefore, passwordHash("webadmin"));
        assertEquals(0, passwordChanges());
    }

    @Test
    @Order(4)
    void requiresLoggedInActiveAccountAndCsrf() throws Exception {
        Browser browser = new Browser();
        assertEquals(401, browser.change(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).statusCode());
        browser.login("webadmin", ORIGINAL_PASSWORD);
        assertEquals(403,
            browser.post("/api/auth", passwordBody(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD), null).statusCode());
        try (Connection connection = application.connection(); Statement statement = connection.createStatement()) {
            statement.execute("UPDATE Users SET StatusID=3 WHERE Username='webadmin'");
        }
        assertEquals(401, browser.change(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).statusCode());
        assertEquals(401, browser.get("/api/admin/users").statusCode());
        assertEquals(0, passwordChanges());
        assertTrue(PasswordHasher.verify(ORIGINAL_PASSWORD, passwordHash("webadmin")));
    }

    @Test
    @Order(5)
    void failureToWriteAuditRollsBackPasswordChange() throws Exception {
        Browser browser = new Browser();
        browser.login("webadmin", ORIGINAL_PASSWORD);
        String hashBefore = passwordHash("webadmin");
        try (Connection connection = application.connection(); Statement statement = connection.createStatement()) {
            statement.execute(
                "ALTER TABLE SystemLogs ADD CONSTRAINT RejectPasswordChange" + " CHECK(Action<>'PASSWORD_CHANGE')");
        }
        try {
            assertEquals(503, browser.change(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).statusCode());
            assertEquals(hashBefore, passwordHash("webadmin"));
            assertEquals(0, passwordChanges());
            assertEquals(200, browser.get("/api/admin/users").statusCode());
        } finally {
            try (Connection connection = application.connection(); Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE SystemLogs DROP CONSTRAINT RejectPasswordChange");
            }
        }
    }

    @Test
    @Order(6)
    void adminResetAlsoInvalidatesExistingAccountSessions() throws Exception {
        Browser manager = new Browser();
        Browser admin = new Browser();
        manager.login("webmanager", ORIGINAL_PASSWORD);
        admin.login("webadmin", ORIGINAL_PASSWORD);
        String body = JSON.writeValueAsString(
            Map.of("username", "webmanager", "password", NEW_PASSWORD, "email", "manager@test.local", "phone",
                "0900000002", "employeeName", "Quản lý kiểm thử", "roleId", 5, "statusId", 1));
        HttpResponse<String> response = admin.put("/api/admin/users/2", body);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(401, manager.get("/api/reports").statusCode());
        assertEquals(200, admin.get("/api/admin/users").statusCode());
        manager = new Browser();
        manager.login("webmanager", NEW_PASSWORD);
        assertEquals(200, manager.get("/api/reports").statusCode());
    }

    @Test
    @Order(7)
    void sessionClaimWithoutPasswordFingerprintCannotAccessOrChangePassword() throws Exception {
        Browser browser = new Browser();
        browser.login("webadmin", ORIGINAL_PASSWORD);
        removeFingerprint(browser);
        assertEquals(401, browser.get("/api/admin/users").statusCode());
        browser = new Browser();
        browser.login("webadmin", ORIGINAL_PASSWORD);
        removeFingerprint(browser);
        assertEquals(401, browser.change(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).statusCode());
        assertTrue(JSON.readTree(browser.get("/api/auth").body()).get("user").isNull());
        assertEquals(0, passwordChanges());
    }

    @Test
    @Order(8)
    void repeatedIncorrectCurrentPasswordsAreThrottled() throws Exception {
        Browser browser = new Browser();
        browser.login("websales", ORIGINAL_PASSWORD);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(400, browser.change("wrong-current-password", NEW_PASSWORD, NEW_PASSWORD).statusCode());
        }
        assertEquals(429, browser.change("wrong-current-password", NEW_PASSWORD, NEW_PASSWORD).statusCode());
        assertEquals(429, browser.change(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).statusCode());
        assertTrue(PasswordHasher.verify(ORIGINAL_PASSWORD, passwordHash("websales")));
        assertEquals(0, passwordChanges());
    }

    private static String passwordBody(String current, String next, String confirmation) throws Exception {
        return JSON.writeValueAsString(Map.of("action", "changePassword", "currentPassword", current, "newPassword",
            next, "confirmPassword", confirmation));
    }

    private String passwordHash(String username) throws Exception {
        try (Connection connection = application.connection();
            PreparedStatement statement = connection
                .prepareStatement("SELECT PasswordHash FROM Users WHERE Username=?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    private int passwordChanges() throws Exception {
        try (Connection connection = application.connection();
            Statement statement = connection.createStatement();
            ResultSet result = statement
                .executeQuery("SELECT COUNT(*) FROM SystemLogs WHERE Action='PASSWORD_CHANGE'")) {
            result.next();
            return result.getInt(1);
        }
    }

    private void removeFingerprint(Browser browser) throws Exception {
        Context context = (Context) application.tomcat.getHost().findChild("");
        Session session = context.getManager().findSession(browser.sessionId());
        assertNotNull(session);
        session.getSession().removeAttribute(PasswordSession.ATTRIBUTE);
    }

    private static class Browser {
        private final CookieManager cookies;
        private final HttpClient client;
        private String csrf;

        private Browser() throws Exception {
            cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            client = HttpClient.newBuilder().cookieHandler(cookies).build();
            csrf = JSON.readTree(get("/api/auth").body()).get("csrfToken").asText();
        }

        private void login(String username, String password) throws Exception {
            String body = JSON
                .writeValueAsString(Map.of("action", "login", "username", username, "password", password));
            HttpResponse<String> response = post("/api/auth", body, csrf);
            assertEquals(200, response.statusCode(), response.body());
            csrf = JSON.readTree(response.body()).get("csrfToken").asText();
        }

        private HttpResponse<String> change(String current, String next, String confirmation) throws Exception {
            return post("/api/auth", passwordBody(current, next, confirmation), csrf);
        }

        private HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(application.base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> post(String path, String body, String token) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(application.base + path))
                .header("Content-Type", "application/json");
            if (token != null) {
                request.header("X-CSRF-Token", token);
            }
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> put(String path, String body) throws Exception {
            return client.send(
                HttpRequest.newBuilder(URI.create(application.base + path)).header("Content-Type", "application/json")
                    .header("X-CSRF-Token", csrf).PUT(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        }

        private String sessionId() {
            for (java.net.HttpCookie cookie : cookies.getCookieStore().getCookies()) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            throw new IllegalStateException("Không có session cookie trong kiểm thử.");
        }
    }
}
