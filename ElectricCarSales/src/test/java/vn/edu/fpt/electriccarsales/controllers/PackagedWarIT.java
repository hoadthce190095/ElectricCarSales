package vn.edu.fpt.electriccarsales.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;

/**
 * Deploys the built WAR with annotation discovery and web.xml, rather than manually mapped
 * controllers.
 */
class PackagedWarIT {
    private static TestApplication fixture;
    private static Tomcat server;
    private static String base;
    private static final ObjectMapper JSON = new ObjectMapper();
    private HttpClient client;
    private String csrf;

    @BeforeAll
    static void deploy() throws Exception {
        fixture = new TestApplication(0);
        Path serverBase = Files.createTempDirectory("electriccars-war-test-");
        Files.createDirectories(serverBase.resolve("webapps"));
        server = new Tomcat();
        server.setBaseDir(serverBase.toString());
        server.setPort(0);
        server.getConnector().setProperty("address", "127.0.0.1");
        Path war = Path.of("target/ElectricCarSales.war").toAbsolutePath();
        assertTrue(Files.exists(war), "Run Maven verify so package is built before this test.");
        Context context = server.addWebapp("/ElectricCarSales", war.toString());
        context.setParentClassLoader(PackagedWarIT.class.getClassLoader());
        server.start();
        assertTrue(context.getState().isAvailable(), "WAR context must start successfully.");
        base = "http://127.0.0.1:" + server.getConnector().getLocalPort() + "/ElectricCarSales";
    }

    @AfterAll
    static void stop() throws Exception {
        try {
            if (server != null) {
                server.stop();
                server.destroy();
            }
        } finally {
            if (fixture != null) {
                fixture.close();
            }
        }
    }

    @BeforeEach
    void session() throws Exception {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        var response = get("/api/auth");
        assertEquals(200, response.statusCode(), response.body());
        csrf = JSON.readTree(response.body()).get("csrfToken").asText();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String data) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json")
                .header("X-CSRF-Token", csrf).POST(HttpRequest.BodyPublishers.ofString(data)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void welcomeAssetsAndDiscoveredFilterWork() throws Exception {
        var welcome = get("/");
        assertEquals(200, welcome.statusCode(), welcome.body());
        assertTrue(welcome.body().contains("<title>Electric Car Sales</title>"));
        assertEquals(200, get("/assets/css/style.css").statusCode());
        var tanPage = get("/tan.html");
        assertEquals(200, tanPage.statusCode(), tanPage.body());
        assertTrue(tanPage.body().contains("assets/tan.js"));
        String[] scripts = {"tan-common.js", "tan-admin.js", "tan-report.js", "tan-catalogue.js", "tan-ai.js",
            "tan.js"};
        int lastScript = -1;
        for (String script : scripts) {
            int position = tanPage.body().indexOf("assets/" + script);
            assertTrue(position > lastScript, "Scripts must load in order: " + script);
            lastScript = position;
            var asset = get("/assets/" + script);
            assertEquals(200, asset.statusCode(), script);
            assertTrue(asset.body().contains("use strict"), "JavaScript content: " + script);
        }
        assertEquals(200, get("/assets/tan.css").statusCode());
        assertEquals(200, get("/index.jsp").statusCode());
        var setup = get("/api/setup");
        assertEquals(200, setup.statusCode(), setup.body());
        assertFalse(JSON.readTree(setup.body()).get("available").asBoolean());
        assertEquals(403, post("/api/setup", "{}").statusCode());
        assertEquals(401, get("/api/admin/users").statusCode());
        assertEquals(401, get("/api/reports;test=true").statusCode());

        // API chưa có của thành viên khác không chịu CSRF của module Tấn.
        var otherApi = client.send(HttpRequest.newBuilder(URI.create(base + "/api/payments"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
            HttpResponse.BodyHandlers.ofString());
        var unmappedPage = client
            .send(
                HttpRequest.newBuilder(URI.create(base + "/unmapped-team-route"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(unmappedPage.statusCode(), otherApi.statusCode(), "Other APIs keep normal Tomcat behavior.");
    }

    @Test
    void packagedLoginDatabaseQueriesAndReportsWork() throws Exception {
        var login = post("/api/auth",
            "{\"action\":\"login\",\"username\":\"webadmin\",\"password\":\"web-test-password-123\"}");
        assertEquals(200, login.statusCode(), login.body());
        csrf = JSON.readTree(login.body()).get("csrfToken").asText();
        var users = get("/api/admin/users");
        assertEquals(200, users.statusCode(), users.body());
        assertEquals(3, JSON.readTree(users.body()).get("total").asInt());
        var reports = get("/api/reports?from=2026-10-01&to=2026-10-31");
        assertEquals(200, reports.statusCode(), reports.body());
        assertTrue(JSON.readTree(reports.body()).has("summary"));
        assertEquals(200, get("/api/admin/configurations").statusCode());
    }

    @Test
    void packagedCatalogueAssistantWorks() throws Exception {
        var catalogue = get("/api/ai");
        assertEquals(200, catalogue.statusCode(), catalogue.body());
        assertEquals(2, JSON.readTree(catalogue.body()).get("models").size());
        var reply = post("/api/ai", "{\"message\":\"Gợi ý xe dưới 600 triệu, 5 chỗ\"}");
        assertEquals(200, reply.statusCode(), reply.body());
        JsonNode answer = JSON.readTree(reply.body());
        assertEquals("CATALOG_RULES", answer.get("mode").asText());
        assertEquals("EV Test", answer.get("recommendations").get(0).get("vehicle").get("modelName").asText());
    }
}
