package com.electriccarsales.controller;

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

class AIConversationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
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

    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private JsonNode state(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(application.base + "/api/auth")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return JSON.readTree(response.body());
    }

    private HttpResponse<String> post(HttpClient client, String path, String csrf, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(application.base + path))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (csrf != null) {
            request.header("X-CSRF-Token", csrf);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode ask(HttpClient client, String csrf, String message, String intent) throws Exception {
        String body = JSON.createObjectNode().put("message", message).put("intent", intent).toString();
        HttpResponse<String> response = post(client, "/api/ai", csrf, body);
        assertEquals(200, response.statusCode(), response.body());
        return JSON.readTree(response.body());
    }

    @Test
    void resetForgetsPreviousCriteriaAndKeepsLoginAndOtherBrowsers() throws Exception {
        HttpClient first = browser();
        String csrf = state(first).get("csrfToken").asText();
        HttpResponse<String> login = post(first, "/api/auth", csrf,
            "{\"action\":\"login\",\"username\":\"websales\"," + "\"password\":\"web-test-password-123\"}");
        assertEquals(200, login.statusCode());
        csrf = JSON.readTree(login.body()).get("csrfToken").asText();
        JsonNode old = ask(first, csrf, "Gợi ý xe dưới 600 triệu", "RECOMMEND");
        assertEquals("EV Test", old.get("recommendations").get(0).get("vehicle").get("modelName").asText());
        JsonNode followUp = ask(first, csrf, "Thông số xe đó", "PRODUCT_QA");
        assertEquals(1, followUp.get("comparison").size());
        assertEquals(500000000, followUp.get("comparison").get(0).get("minPrice").asLong());

        HttpClient second = browser();
        String secondCsrf = state(second).get("csrfToken").asText();
        JsonNode other = ask(second, secondCsrf, "Thông số EV Family", "PRODUCT_QA");

        HttpResponse<String> reset = post(first, "/api/ai?action=reset", csrf, "{}");
        assertEquals(200, reset.statusCode(), reset.body());
        JsonNode resetBody = JSON.readTree(reset.body());
        assertTrue(resetBody.get("cleared").asBoolean());
        assertNotEquals(old.get("conversationId").asText(), resetBody.get("conversationId").asText());
        JsonNode fresh = ask(first, csrf, "Thông số xe đó", "PRODUCT_QA");
        assertTrue(fresh.get("comparison").isEmpty());
        assertTrue(fresh.get("constraints").get("budget").isNull());
        assertEquals(resetBody.get("conversationId"), fresh.get("conversationId"));
        assertEquals("websales", state(first).get("user").get("username").asText());

        JsonNode otherFollowUp = ask(second, secondCsrf, "Thông số xe đó", "PRODUCT_QA");
        assertEquals(other.get("conversationId"), otherFollowUp.get("conversationId"));
        assertEquals("EV Family", otherFollowUp.get("comparison").get(0).get("modelName").asText());
    }

    @Test
    void resetRequiresCsrfAndValidJsonAndRejectsUnknownAction() throws Exception {
        HttpClient client = browser();
        String csrf = state(client).get("csrfToken").asText();
        assertEquals(403, post(client, "/api/ai?action=reset", null, "{}").statusCode());
        assertEquals(400, post(client, "/api/ai?action=reset", csrf, "broken").statusCode());
        assertEquals(400, post(client, "/api/ai?action=unknown", csrf, "{}").statusCode());
        assertEquals(200, post(client, "/api/ai?action=reset", csrf, "{}").statusCode());
    }
}
