package com.electriccarsales.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.regex.Pattern;

/**
 * Deploys the built WAR with annotation discovery and web.xml, rather than manually mapped
 * controllers.
 */
class PackagedWarIT {
    private static TestApplication fixture;
    private static Tomcat server;
    private static String base;
    private static String showroomBase;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String UI_VERSION = "v=main-blue-20261017";
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
        Context showroom = server.addWebapp("/showroom", war.toString());
        showroom.setParentClassLoader(PackagedWarIT.class.getClassLoader());
        server.start();
        assertTrue(context.getState().isAvailable(), "WAR context must start successfully.");
        assertTrue(showroom.getState().isAvailable(), "WAR must also start under a different context path.");
        base = "http://127.0.0.1:" + server.getConnector().getLocalPort() + "/ElectricCarSales";
        showroomBase = "http://127.0.0.1:" + server.getConnector().getLocalPort() + "/showroom";
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
    void welcomeHomepageWorkspaceAssetsAndDiscoveredFilterWork() throws Exception {
        for (String page : new String[]{"/", "/index.jsp", "/preview-main.jsp"}) {
            var response = get(page);
            assertEquals(200, response.statusCode(), response.body());
            assertHomepageLinks(response.body(), "/ElectricCarSales");
        }
        var workspace = get("/tan.jsp");
        assertEquals(200, workspace.statusCode(), workspace.body());
        assertWorkspaceLinks(workspace.body(), "/ElectricCarSales");
        String[] scripts = {"tan-common.js", "tan-admin.js", "tan-report.js", "tan-catalogue.js", "tan-ai.js",
            "tan.js"};
        int lastScript = -1;
        for (String script : scripts) {
            int position = workspace.body().indexOf("assets/" + script);
            assertTrue(position > lastScript, "Scripts must load in order: " + script);
            lastScript = position;
            var asset = get("/assets/" + script);
            assertEquals(200, asset.statusCode(), script);
            assertTrue(asset.body().contains("use strict"), "JavaScript content: " + script);
        }
        for (String asset : new String[]{"/assets/css/theme.css", "/assets/css/home.css", "/assets/tan.css",
            "/assets/js/home.js"}) {
            assertEquals(200, get(asset + "?" + UI_VERSION).statusCode(), asset);
        }
        var catalogue = get("/assets/vinfast-catalogue.json");
        assertEquals(200, catalogue.statusCode(), catalogue.body());
        assertEquals(10, JSON.readTree(catalogue.body()).get("models").size());
        var setup = get("/api/setup");
        assertEquals(200, setup.statusCode(), setup.body());
        assertFalse(JSON.readTree(setup.body()).get("available").asBoolean());
        assertEquals(403, post("/api/setup", "{}").statusCode());
        assertEquals(401, get("/api/admin/users").statusCode());
        assertEquals(401, get("/api/reports;test=true").statusCode());
    }

    @Test
    void renamedWarContextRendersJspAndSupportsCatalogueLoginAndExports() throws Exception {
        HttpClient showroomClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        var auth = showroomClient.send(HttpRequest.newBuilder(URI.create(showroomBase + "/api/auth")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, auth.statusCode(), auth.body());
        String showroomCsrf = JSON.readTree(auth.body()).get("csrfToken").asText();
        for (String page : new String[]{"/", "/index.jsp", "/preview-main.jsp", "/tan.jsp"}) {
            var response = showroomClient.send(HttpRequest.newBuilder(URI.create(showroomBase + page)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            if ("/tan.jsp".equals(page)) {
                assertWorkspaceLinks(response.body(), "/showroom");
            } else {
                assertHomepageLinks(response.body(), "/showroom");
            }
            assertFalse(response.body().contains("/ElectricCarSales/"), "Links must follow the deployed context");
        }
        for (String asset : new String[]{"css/theme.css", "css/home.css", "js/home.js", "tan.css", "tan-common.js",
            "tan-admin.js", "tan-report.js", "tan-catalogue.js", "tan-ai.js", "tan.js", "vinfast-catalogue.json"}) {
            var response = showroomClient.send(
                HttpRequest.newBuilder(URI.create(showroomBase + "/assets/" + asset)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), asset);
        }
        var catalogue = showroomClient.send(HttpRequest.newBuilder(URI.create(showroomBase + "/api/ai")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, catalogue.statusCode(), catalogue.body());
        assertEquals(2, JSON.readTree(catalogue.body()).get("models").size());
        var login = showroomClient.send(
            HttpRequest.newBuilder(URI.create(showroomBase + "/api/auth")).header("Content-Type", "application/json")
                .header("X-CSRF-Token", showroomCsrf)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"action\":\"login\",\"username\":\"webadmin\",\"password\":\"web-test-password-123\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode(), login.body());
        for (String format : new String[]{"csv", "xlsx"}) {
            var export = showroomClient.send(HttpRequest
                .newBuilder(URI.create(showroomBase + "/api/reports?from=2026-10-01&to=2026-10-31&format=" + format))
                .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, export.statusCode(), format);
            assertTrue(export.headers().firstValue("Content-Disposition").orElse("").contains("." + format));
            if ("csv".equals(format)) {
                assertTrue(export.headers().firstValue("Content-Type").orElse("").startsWith("text/csv"));
                assertTrue(export.body().length > 0);
            } else {
                assertTrue(export.headers().firstValue("Content-Type").orElse("")
                    .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                assertEquals('P', export.body()[0]);
                assertEquals('K', export.body()[1]);
            }
        }
    }

    private static void assertHomepageLinks(String body, String contextPath) {
        assertRenderedJsp(body, contextPath);
        assertTrue(body.contains("class=\"home-page\""), "The welcome page must render the VinFast homepage");
        assertTrue(body.contains("id=\"vehicle-grid\""));
        assertTrue(body.contains("id=\"compare-dialog\""));
        assertTrue(body.contains("href=\"" + contextPath + "/index.jsp\""));
        assertTrue(body.contains("href=\"" + contextPath + "/tan.jsp?login=1\""),
            "Homepage sign-in must open the workspace login");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/theme.css");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/home.css");
        assertVersionedAsset(body, "src", contextPath + "/assets/js/home.js");
    }

    private static void assertWorkspaceLinks(String body, String contextPath) {
        assertRenderedJsp(body, contextPath);
        assertTrue(body.contains("href=\"" + contextPath + "/index.jsp\""),
            "Workspace brand must return to the shared homepage");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/theme.css");
        assertVersionedAsset(body, "href", contextPath + "/assets/tan.css");
        assertTrue(body.contains("src=\"" + contextPath + "/assets/tan.js"));
    }

    private static void assertVersionedAsset(String body, String attribute, String expectedPath) {
        var resources = Pattern.compile("\\b" + attribute + "=\"([^\"]+)\"").matcher(body);
        while (resources.find()) {
            URI resource = URI.create(resources.group(1));
            if (expectedPath.equals(resource.getPath())) {
                assertEquals(UI_VERSION, resource.getQuery(), "Asset cache version: " + expectedPath);
                return;
            }
        }
        fail("The rendered page must include context-aware asset " + expectedPath);
    }

    private static void assertRenderedJsp(String body, String contextPath) {
        assertTrue(body.contains("name=\"app-context-path\" content=\"" + contextPath + "\""));
        assertFalse(body.contains("<%@"), "JSP directives must be rendered by Jasper");
        assertFalse(body.contains("<c:url"), "JSTL URLs must be rendered");
        assertFalse(body.contains("${pageContext"), "Context paths must be evaluated on the server");
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
