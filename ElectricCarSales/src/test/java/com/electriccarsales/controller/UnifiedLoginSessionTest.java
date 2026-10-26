package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.electriccarsales.dao.UserDAO;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.PasswordHasher;
import com.electriccarsales.service.PasswordSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpSession;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

class UnifiedLoginSessionTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "web-test-password-123";
    private static TestApplication application;

    @BeforeAll
    static void start() throws Exception {
        application = new TestApplication(0);
        Context context = (Context) application.tomcat.getHost().findChild("");
        Tomcat.addServlet(context, "main-login", new LoginController());
        context.addServletMappingDecoded("/login", "main-login");
        try (Connection connection = application.connection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO Employees(UserID,EmployeeName,Email,Phone)"
                + " VALUES(2,N'Manager','manager@test.local','0900000002')");
        }
    }

    @AfterAll
    static void stop() throws Exception {
        if (application != null) {
            application.close();
        }
    }

    @Test
    void bothLoginRoutesSharePasswordFormatsAndPasswordChangesRotateBothIdentities() throws Exception {
        String mainHash;
        try (UserDAO users = new UserDAO()) {
            mainHash = users.hashPassword(PASSWORD);
        }
        try (Connection connection = application.connection();
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                    + " SELECT 'bridgesales',?,'bridge@test.local','0900000004',RoleID,1 FROM Roles WHERE RoleName='SALES'")) {
            statement.setString(1, mainHash);
            statement.executeUpdate();
        }
        int bridgeUserId;
        try (Connection connection = application.connection(); Statement statement = connection.createStatement();
            var result = statement.executeQuery("SELECT UserID FROM Users WHERE Username='bridgesales'")) {
            assertTrue(result.next());
            bridgeUserId = result.getInt("UserID");
        }
        Browser browser = new Browser();
        browser.get("/api/auth");
        String previousId = browser.sessionId();
        HttpResponse<String> response = browser.post("/login", "application/x-www-form-urlencoded",
            "username=bridgesales&password=" + PASSWORD, null);
        assertEquals(302, response.statusCode(), response.body());
        assertEquals("/staff/dashboard.jsp", response.headers().firstValue("Location").orElseThrow());
        assertNotEquals(previousId, browser.sessionId());
        HttpSession session = browser.session();
        SessionUser user = assertInstanceOf(SessionUser.class, session.getAttribute("user"));
        assertEquals(bridgeUserId, user.getUserId());
        assertEquals("SALES", user.getRole());
        assertTrue(PasswordSession.matches(session.getAttribute(PasswordSession.ATTRIBUTE), mainHash));
        JsonNode state = JSON.readTree(browser.get("/api/auth").body());
        assertEquals("bridgesales", state.get("user").get("username").asText());
        Browser other = new Browser();
        other.apiLogin("bridgesales");
        String beforePasswordChange = browser.sessionId();
        String replacement = "replacement-password-456";
        String body = JSON.writeValueAsString(Map.of("action", "changePassword", "currentPassword", PASSWORD,
            "newPassword", replacement, "confirmPassword", replacement));
        response = browser.post("/api/auth", "application/json", body, state.get("csrfToken").asText());
        assertEquals(200, response.statusCode(), response.body());
        assertNotEquals(beforePasswordChange, browser.sessionId());
        assertEquals(bridgeUserId, browser.session().getAttribute("userID"));
        assertEquals("SALES", browser.session().getAttribute("roleName"));
        assertTrue(JSON.readTree(other.get("/api/auth").body()).get("user").isNull());

        Browser afterChange = new Browser();
        response = afterChange.post("/login", "application/x-www-form-urlencoded",
            "username=bridgesales&password=" + replacement, null);
        assertEquals(302, response.statusCode(), response.body());
        assertEquals("bridgesales", JSON.readTree(afterChange.get("/api/auth").body()).get("user").get("username").asText());
    }

    @Test
    void apiLoginPopulatesMainIdentityAndOnlyLinksRealEmployees() throws Exception {
        Browser manager = new Browser();
        manager.apiLogin("webmanager");
        HttpSession session = manager.session();
        assertEquals(2, session.getAttribute("userID"));
        assertEquals("webmanager", session.getAttribute("username"));
        assertInstanceOf(Integer.class, session.getAttribute("roleID"));
        assertEquals("MANAGER", session.getAttribute("roleName"));
        assertEquals("manager@test.local", session.getAttribute("email"));
        assertEquals("0900000002", session.getAttribute("phone"));
        assertInstanceOf(Integer.class, session.getAttribute("employeeId"));
        assertEquals("MANAGER", session.getAttribute("role"));

        Browser admin = new Browser();
        admin.apiLogin("webadmin");
        assertNull(admin.session().getAttribute("employeeId"));
        assertNull(admin.session().getAttribute("role"));
    }

    @Test
    void homepageUsesCustomerSessionAfterEitherLoginRouteAndSupportsLegacySessions() throws Exception {
        Browser guest = new Browser();
        guest.get("/api/auth");
        HttpResponse<String> homepage = guest.get("/index.jsp?view=customer");
        assertEquals(200, homepage.statusCode(), homepage.body());
        assertTrue(homepage.body().contains("href=\"/login.jsp\""));
        assertFalse(homepage.body().contains("href=\"/customer/my-account.jsp\""));

        try (Connection connection = application.connection();
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID)"
                    + " SELECT 'webcustomer',?,'customer@test.local','0900000005',RoleID,1 FROM Roles WHERE RoleName='CUSTOMER'")) {
            statement.setString(1, PasswordHasher.hash(PASSWORD));
            statement.executeUpdate();
        }
        Browser customer = new Browser();
        HttpResponse<String> login = customer.post("/login", "application/x-www-form-urlencoded",
            "username=webcustomer&password=" + PASSWORD, null);
        assertEquals(302, login.statusCode(), login.body());
        assertEquals("/index.jsp", login.headers().firstValue("Location").orElseThrow());
        assertCustomerHomepage(customer.get("/index.jsp"));

        Browser apiCustomer = new Browser();
        apiCustomer.apiLogin("webcustomer");
        assertCustomerHomepage(apiCustomer.get("/index.jsp"));

        HttpSession legacy = customer.session();
        legacy.removeAttribute("user");
        assertCustomerHomepage(customer.get("/index.jsp"));
        legacy.removeAttribute("userID");
        assertFalse(customer.get("/index.jsp?view=customer").body().contains("href=\"/customer/my-account.jsp\""));
    }

    private static void assertCustomerHomepage(HttpResponse<String> response) {
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("href=\"/customer/my-account.jsp\""));
        assertFalse(response.body().contains("href=\"/login.jsp\""));
    }

    private static class Browser {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

        private HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(application.base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> post(String path, String type, String body, String csrf) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(application.base + path))
                .header("Content-Type", type);
            if (csrf != null) {
                request.header("X-CSRF-Token", csrf);
            }
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        }

        private void apiLogin(String username) throws Exception {
            JsonNode state = JSON.readTree(get("/api/auth").body());
            String body = JSON.writeValueAsString(Map.of("action", "login", "username", username, "password", PASSWORD));
            HttpResponse<String> response = post("/api/auth", "application/json", body, state.get("csrfToken").asText());
            assertEquals(200, response.statusCode(), response.body());
        }

        private String sessionId() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> "JSESSIONID".equals(cookie.getName()))
                .findFirst().orElseThrow().getValue();
        }

        private HttpSession session() throws Exception {
            Context context = (Context) application.tomcat.getHost().findChild("");
            Session session = context.getManager().findSession(sessionId());
            assertNotNull(session);
            return session.getSession();
        }
    }
}
