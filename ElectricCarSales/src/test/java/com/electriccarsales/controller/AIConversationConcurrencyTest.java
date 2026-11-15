package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.electriccarsales.model.VehicleModel;
import com.electriccarsales.model.VehicleSpecifications;
import com.electriccarsales.service.AIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class AIConversationConcurrencyTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void resetInAnotherTabCannotBeUndoneByAnEarlierPendingAnswer() throws Exception {
        CountDownLatch readingCatalogue = new CountDownLatch(1);
        CountDownLatch finishReading = new CountDownLatch(1);
        AtomicBoolean firstRead = new AtomicBoolean(true);
        VehicleModel vehicle = new VehicleModel(1, "EV Test", "SUV", "Test Motors", null,
            new BigDecimal("500000000"), new BigDecimal("500000000"), 1, List.of("Standard"), List.of(),
            new VehicleSpecifications(null, null, null, null, new BigDecimal("350"), null, null, 5, null, null, null));
        AIService service = new AIService(() -> {
            if (firstRead.getAndSet(false)) {
                readingCatalogue.countDown();
                try {
                    if (!finishReading.await(10, TimeUnit.SECONDS)) {
                        throw new SQLException("Timed out waiting for the reset request");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Catalogue read interrupted", exception);
                }
            }
            return List.of(vehicle);
        });
        Tomcat server = new Tomcat();
        server.setBaseDir(Files.createTempDirectory("electriccars-ai-concurrency-").toString());
        server.setPort(0);
        server.getConnector().setProperty("address", "127.0.0.1");
        Context context = server.addContext("", Files.createTempDirectory("electriccars-ai-context-").toString());
        context.setParentClassLoader(getClass().getClassLoader());
        Tomcat.addServlet(context, "ai", new AIController(service));
        context.addServletMappingDecoded("/api/ai", "ai");
        try {
            server.start();
            String base = "http://127.0.0.1:" + server.getConnector().getLocalPort();
            HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            JsonNode initial = post(browser, base, "?action=reset", "{}");
            var earlierAnswer = browser.sendAsync(
                request(base, "", "{\"message\":\"Gợi ý xe dưới 600 triệu\",\"intent\":\"RECOMMEND\"}"),
                HttpResponse.BodyHandlers.ofString());
            assertTrue(readingCatalogue.await(10, TimeUnit.SECONDS), "The earlier request must be in flight");
            JsonNode reset = post(browser, base, "?action=reset", "{}");
            assertNotEquals(initial.get("conversationId"), reset.get("conversationId"));
            finishReading.countDown();
            HttpResponse<String> earlier = earlierAnswer.get(10, TimeUnit.SECONDS);
            assertEquals(200, earlier.statusCode(), earlier.body());
            assertEquals(initial.get("conversationId"), JSON.readTree(earlier.body()).get("conversationId"));
            JsonNode followUp = post(browser, base, "", "{\"message\":\"Thông số xe đó\",\"intent\":\"PRODUCT_QA\"}");
            assertEquals(reset.get("conversationId"), followUp.get("conversationId"));
            assertTrue(followUp.get("comparison").isEmpty(), "The reset must discard the earlier vehicle selection");
            assertTrue(followUp.get("constraints").get("budget").isNull(), "The reset must discard the earlier budget");
        } finally {
            finishReading.countDown();
            server.stop();
            server.destroy();
        }
    }

    private HttpRequest request(String base, String query, String body) {
        return HttpRequest.newBuilder(URI.create(base + "/api/ai" + query))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private JsonNode post(HttpClient browser, String base, String query, String body) throws Exception {
        HttpResponse<String> response = browser.send(request(base, query, body), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return JSON.readTree(response.body());
    }
}
