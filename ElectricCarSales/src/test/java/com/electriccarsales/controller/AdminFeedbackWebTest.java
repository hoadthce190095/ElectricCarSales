package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

class AdminFeedbackWebTest {
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
    void login() throws Exception {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        csrf = JSON.readTree(get("/api/auth").body()).get("csrfToken").asText();
        var response = send("POST", "/api/auth", Map.of("action", "login", "username", "webadmin",
            "password", "web-test-password-123"));
        assertEquals(200, response.statusCode(), response.body());
        csrf = JSON.readTree(response.body()).get("csrfToken").asText();
    }

    @Test
    void promotionCreateAndUpdateEnforceCalendarMonthsWithoutWritingInvalidChanges() throws Exception {
        LocalDateTime start = LocalDateTime.of(2027, 1, 31, 9, 0);
        Map<String, Object> input = promotion(start, start.plusMonths(3));
        var created = send("POST", "/api/admin/promotions", input);
        assertEquals(201, created.statusCode(), created.body());
        int id = JSON.readTree(created.body()).get("promotionId").asInt();
        long count = promotionCount();
        input.put("endDate", start.plusMonths(3).plusMinutes(1).toString());
        var rejected = send("POST", "/api/admin/promotions", input);
        assertEquals(400, rejected.statusCode(), rejected.body());
        assertTrue(rejected.body().contains("3 tháng"), rejected.body());
        assertEquals(count, promotionCount());
        rejected = send("PUT", "/api/admin/promotions/" + id, input);
        assertEquals(400, rejected.statusCode(), rejected.body());
        JsonNode original = null;
        for (JsonNode item : JSON.readTree(get("/api/admin/promotions").body())) {
            if (item.get("promotionId").asInt() == id) {
                original = item;
            }
        }
        assertNotNull(original);
        assertEquals(start.plusMonths(3), LocalDateTime.parse(original.get("endDate").asText()));
    }

    @Test
    void invalidDiscountsAreRejectedThroughApi() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 10, 28, 8, 0);
        long count = promotionCount();
        for (Object value : List.of(-1, 0, 100.01, 1.001)) {
            Map<String, Object> input = promotion(start, start.plusMonths(1));
            input.put("discountValue", value);
            var response = send("POST", "/api/admin/promotions", input);
            assertEquals(400, response.statusCode(), response.body());
        }
        Map<String, Object> fixed = promotion(start, start.plusMonths(1));
        fixed.put("discountType", "FIXED");
        fixed.put("discountValue", 500000001);
        var response = send("POST", "/api/admin/promotions", fixed);
        assertEquals(400, response.statusCode(), response.body());
        assertEquals(count, promotionCount());
    }

    @Test
    void accessChangesPreserveProfileAndEmployeeEditsStayInSync() throws Exception {
        int role = lookupId("roles", "SALES"), active = lookupId("statuses", "ACTIVE");
        var created = send("POST", "/api/admin/users", Map.of("username", "feedbackstaff", "password",
            "feedback-password-123", "employeeName", "Nhân viên kiểm thử", "email", "feedback@test.local",
            "phone", "0900000971", "roleId", role, "statusId", active));
        assertEquals(201, created.statusCode(), created.body());
        JsonNode user = JSON.readTree(created.body());
        int id = user.get("userId").asInt(), employee = user.get("employeeId").asInt();
        var access = send("PUT", "/api/admin/users/" + id, Map.of("roleId", role, "statusId", active));
        assertEquals(200, access.statusCode(), access.body());
        assertEquals("feedback@test.local", JSON.readTree(access.body()).get("email").asText());
        assertEquals("Nhân viên kiểm thử", JSON.readTree(access.body()).get("employeeName").asText());
        var profile = send("PUT", "/api/admin/employees/" + employee, Map.of("employeeName", "Tên cập nhật",
            "email", "updated-feedback@test.local", "phone", "0900000972"));
        assertEquals(200, profile.statusCode(), profile.body());
        JsonNode users = JSON.readTree(get("/api/admin/users?q=feedbackstaff").body()).get("items");
        assertEquals(1, users.size());
        assertEquals("updated-feedback@test.local", users.get(0).get("email").asText());
        assertEquals("0900000972", users.get(0).get("phone").asText());
        assertEquals(employee, users.get(0).get("employeeId").asInt());
    }

    private int lookupId(String group, String name) throws Exception {
        for (JsonNode item : JSON.readTree(get("/api/admin/lookups").body()).get(group)) {
            if (name.equals(item.get("name").asText())) {
                return item.get("id").asInt();
            }
        }
        throw new AssertionError("Lookup missing: " + name);
    }

    private Map<String, Object> promotion(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("promotionName", "Calendar test");
        result.put("discountType", "PERCENTAGE");
        result.put("discountValue", 5);
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        result.put("status", "ACTIVE");
        result.put("modelIds", List.of(1));
        return result;
    }

    private long promotionCount() throws Exception {
        try (Connection c = application.connection(); Statement s = c.createStatement();
            ResultSet rows = s.executeQuery("SELECT COUNT(*) FROM Promotions")) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(application.base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path, Object body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(application.base + path))
            .header("Content-Type", "application/json").header("X-CSRF-Token", csrf)
            .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build(),
            HttpResponse.BodyHandlers.ofString());
    }
}
