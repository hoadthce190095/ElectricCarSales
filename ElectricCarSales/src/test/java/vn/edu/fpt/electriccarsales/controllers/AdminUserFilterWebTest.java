package vn.edu.fpt.electriccarsales.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class AdminUserFilterWebTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static TestApplication application;
    private HttpClient client;

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
    void browser() {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(application.base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private void login(String username) throws Exception {
        String csrf = JSON.readTree(get("/api/auth").body()).get("csrfToken").asText();
        HttpRequest request = HttpRequest.newBuilder(URI.create(application.base + "/api/auth"))
            .header("Content-Type", "application/json").header("X-CSRF-Token", csrf)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"action\":\"login\",\"username\":\"" + username + "\",\"password\":\"web-test-password-123\"}"))
            .build();
        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private int lookupId(JsonNode lookups, String group, String name) {
        for (JsonNode item : lookups.get(group)) {
            if (name.equals(item.get("name").asText())) {
                return item.get("id").asInt();
            }
        }
        throw new AssertionError("Thiếu lookup " + name);
    }

    @Test
    void filtersAreAppliedTogetherAndBlankMeansAllRoles() throws Exception {
        login("webadmin");
        JsonNode lookups = JSON.readTree(get("/api/admin/lookups").body());
        int sales = lookupId(lookups, "roles", "SALES");
        int active = lookupId(lookups, "statuses", "ACTIVE");
        HttpResponse<String> response = get("/api/admin/users?q=web&roleId=" + sales + "&statusId=" + active);
        assertEquals(200, response.statusCode(), response.body());
        JsonNode page = JSON.readTree(response.body());
        assertEquals(1, page.get("total").asInt());
        assertEquals(20, page.get("pageSize").asInt());
        assertEquals("websales", page.get("items").get(0).get("username").asText());
        JsonNode all = JSON.readTree(get("/api/admin/users?q=web&roleId=&statusId=").body());
        assertEquals(3, all.get("total").asInt());
        JsonNode secondPage = JSON.readTree(get("/api/admin/users?page=2&roleId=" + sales).body());
        assertEquals(1, secondPage.get("total").asInt());
        assertTrue(secondPage.get("items").isEmpty());
    }

    @Test
    void badFiltersReturn400AndCustomerIsAValidFilter() throws Exception {
        login("webadmin");
        for (String filter : new String[]{"roleId=0", "roleId=-1", "roleId=999", "roleId=SALES", "statusId=0",
            "statusId=999", "statusId=ACTIVE"}) {
            HttpResponse<String> response = get("/api/admin/users?" + filter);
            assertEquals(400, response.statusCode(), filter + ": " + response.body());
            assertTrue(JSON.readTree(response.body()).has("error"));
        }
        JsonNode lookups = JSON.readTree(get("/api/admin/lookups").body());
        int customer = lookupId(lookups, "roles", "CUSTOMER");
        HttpResponse<String> response = get("/api/admin/users?roleId=" + customer);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(0, JSON.readTree(response.body()).get("total").asInt());
    }

    @Test
    void filteringDoesNotGrantAdministrationToAnotherRole() throws Exception {
        login("webmanager");
        assertEquals(403, get("/api/admin/users?q=web&roleId=1&statusId=1").statusCode());
        assertEquals(403, get("/api/admin/users?roleId=invalid").statusCode());
    }
}
