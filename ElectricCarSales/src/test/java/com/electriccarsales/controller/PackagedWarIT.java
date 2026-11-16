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
    private static Path smtpFile;
    private static String previousSmtpFile;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String UI_VERSION = "v=main-blue-20261017";
    private HttpClient client;
    private String csrf;

    @BeforeAll
    static void deploy() throws Exception {
        // This fixture tests the unconfigured-email state, independently of local user settings.
        previousSmtpFile = System.getProperty("ECARS_SMTP_FILE");
        smtpFile = Files.createTempFile("electriccars-war-smtp-", ".properties");
        System.setProperty("ECARS_SMTP_FILE", smtpFile.toString());
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
            try {
                if (fixture != null) {
                    fixture.close();
                }
            } finally {
                if (previousSmtpFile == null) {
                    System.clearProperty("ECARS_SMTP_FILE");
                } else {
                    System.setProperty("ECARS_SMTP_FILE", previousSmtpFile);
                }
                if (smtpFile != null) {
                    Files.deleteIfExists(smtpFile);
                }
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
    void customerCanDownloadOnlyTheirOwnWordPreview(@org.junit.jupiter.api.io.TempDir Path templates)
            throws Exception {
        assertEquals(302, get("/customer/word-preview?kind=deposit&orderId=1").statusCode());
        int orderId;
        try (var db = fixture.connection(); var stmt = db.createStatement()) {
            String hash = com.electriccarsales.service.PasswordHasher.hash("preview-test-password-123");
            try (var user = db.prepareStatement("""
                    INSERT INTO Users(Username,PasswordHash,RoleID,StatusID)
                    SELECT 'previewowner',?,r.RoleID,s.StatusID FROM Roles r CROSS JOIN Statuses s
                    WHERE r.RoleName='CUSTOMER' AND s.StatusName='ACTIVE'
                    """)) {
                user.setString(1, hash);
                assertEquals(1, user.executeUpdate());
            }
            stmt.execute("""
                    INSERT INTO Customers(UserID,CustomerName,Phone)
                    SELECT UserID,'Preview Owner','0987658765' FROM Users WHERE Username='previewowner';
                    INSERT INTO Orders(CustomerID,OrderStatusID,PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount)
                    SELECT c.CustomerID,os.OrderStatusID,pm.PaymentMethodID,'2026-11-01',100,30,70
                    FROM Customers c JOIN Users u ON u.UserID=c.UserID
                    CROSS JOIN OrderStatuses os CROSS JOIN PaymentMethods pm
                    WHERE u.Username='previewowner' AND os.StatusName='DEPOSITED' AND pm.MethodName='CASH';
                    """);
            try (var rows = stmt.executeQuery("""
                    SELECT o.OrderID FROM Orders o JOIN Customers c ON c.CustomerID=o.CustomerID
                    JOIN Users u ON u.UserID=c.UserID WHERE u.Username='previewowner'
                    """)) {
                assertTrue(rows.next());
                orderId = rows.getInt(1);
            }
            try (var insert = db.prepareStatement("INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(?,1,100)")) {
                insert.setInt(1, orderId);
                insert.executeUpdate();
            }
            try (var insert = db.prepareStatement("""
                    INSERT INTO Payments(OrderID,PaymentMethodID,PaymentTypeID,PaymentStatusID,PaymentAmount)
                    SELECT o.OrderID,o.PaymentMethodID,pt.PaymentTypeID,ps.PaymentStatusID,30
                    FROM Orders o CROSS JOIN PaymentTypes pt CROSS JOIN PaymentStatuses ps
                    WHERE o.OrderID=? AND pt.TypeName='DEPOSIT' AND ps.StatusID='COMPLETED'
                    """)) {
                insert.setInt(1, orderId);
                insert.executeUpdate();
            }
        }

        try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             var out = Files.newOutputStream(templates.resolve("deposit-preview.docx"))) {
            doc.createParagraph().createRun().setText("{{preview.label}}");
            doc.createParagraph().createRun().setText("{{customer.name}} {{order.id}} {{payment.id}} {{payment.amount}}");
            doc.write(out);
        }
        String previous = System.getProperty("ECS_WORD_PREVIEW_DIR");
        System.setProperty("ECS_WORD_PREVIEW_DIR", templates.toString());
        try {
            assertEquals(200, post("/api/auth",
                    "{\"action\":\"login\",\"username\":\"previewowner\",\"password\":\"preview-test-password-123\"}").statusCode());
            assertEquals(403,get("/staff/sales-document?kind=deposit&orderId="+orderId).statusCode());
            assertEquals(404,get("/customer/sales-document?kind=invoice&orderId="+orderId).statusCode());
            assertEquals(404,get("/customer/sales-document?kind=deposit&orderId="+(orderId+1000)).statusCode());
            var actualDoc=client.send(HttpRequest.newBuilder(URI.create(base+"/customer/sales-document?kind=deposit&orderId="+orderId)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,actualDoc.statusCode());
            try(var doc=new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(actualDoc.body()));
                var extract=new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
                assertTrue(extract.getText().contains("Preview Owner"));
                assertTrue(extract.getText().contains("096205004458"));
            }
            int threadId;
            try (var db=fixture.connection(); var stmt=db.createStatement()) {
                stmt.execute("""
                    INSERT INTO ConsultationRequests(CustomerID,ModelID,ConsultationStatusID,RequestContent)
                    SELECT c.CustomerID,1,s.ConsultationStatusID,'<script>unsafe</script>'
                    FROM Customers c JOIN Users u ON u.UserID=c.UserID CROSS JOIN ConsultationStatuses s
                    WHERE u.Username='previewowner' AND s.StatusName='PENDING'
                    """);
                try(var row=stmt.executeQuery("SELECT MAX(ConsultationRequestID) FROM ConsultationRequests")) {
                    assertTrue(row.next());threadId=row.getInt(1);
                }
            }
            var chat=get("/sales-chat");
            assertEquals(200,chat.statusCode(),chat.body());
            assertTrue(chat.body().contains("&lt;script&gt;unsafe&lt;/script&gt;"));
            var token=Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(chat.body());
            assertTrue(token.find());
            String chatToken=token.group(1);
            var invalid=client.send(HttpRequest.newBuilder(URI.create(base+"/sales-chat"))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("requestId="+threadId+"&message=reply"))
                    .build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(403,invalid.statusCode());
            var reply=client.send(HttpRequest.newBuilder(URI.create(base+"/sales-chat"))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("requestId="+threadId+"&message=reply&csrfToken="+chatToken))
                    .build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(302,reply.statusCode(),reply.body());
            assertTrue(get("/sales-chat").body().contains("reply"));
            var consent=client.send(HttpRequest.newBuilder(URI.create(base+"/contract?format=json"))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("action=requestOtp&contractId=1&csrfToken="+chatToken))
                    .build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(400,consent.statusCode(),consent.body());
            assertTrue(consent.body().contains("đồng ý ký hợp đồng"));
            assertEquals(404, get("/customer/word-preview?kind=deposit&orderId=" + (orderId + 1000)).statusCode());
            assertEquals(404, get("/customer/word-preview?kind=invoice&orderId=" + orderId).statusCode());
            var download = client.send(HttpRequest.newBuilder(
                    URI.create(base + "/customer/word-preview?kind=deposit&orderId=" + orderId)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, download.statusCode());
            assertEquals("private, no-store", download.headers().firstValue("Cache-Control").orElseThrow());
            assertTrue(download.headers().firstValue("Content-Disposition").orElseThrow().contains("deposit-preview.docx"));
            try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                    new java.io.ByteArrayInputStream(download.body()))) {
                assertTrue(doc.getParagraphs().get(1).getText().startsWith("Preview Owner " + orderId + " "));
            }
        } finally {
            if (previous == null) System.clearProperty("ECS_WORD_PREVIEW_DIR");
            else System.setProperty("ECS_WORD_PREVIEW_DIR", previous);
        }
    }

    @Test
    void welcomeHomepageWorkspaceAssetsAndDiscoveredFilterWork() throws Exception {
        for (String page : new String[]{"/", "/index.jsp"}) {
            var response = get(page);
            assertEquals(200, response.statusCode(), response.body());
            assertHomepageLinks(response.body(), "/ElectricCarSales");
        }
        assertEquals(302, get("/admin.jsp").statusCode(), "Guest must authenticate before opening Admin");
        assertEquals(302, get("/admin/admin.html").statusCode());
        assertEquals(200, post("/api/auth",
            "{\"action\":\"login\",\"username\":\"websales\",\"password\":\"web-test-password-123\"}").statusCode());
        assertEquals(403, get("/admin.jsp").statusCode(), "Sales must use the staff portal");
        assertEquals(403, get("/api/admin/users").statusCode());
        session();
        assertEquals(200, post("/api/auth",
            "{\"action\":\"login\",\"username\":\"webadmin\",\"password\":\"web-test-password-123\"}").statusCode());
        var workspace = get("/admin.jsp");
        assertEquals(200, workspace.statusCode(), workspace.body());
        assertWorkspaceLinks(workspace.body(), "/ElectricCarSales");
        String[] scripts = {"admin-common.js", "admin-management.js", "sales-reports.js", "vehicle-catalogue.js",
            "admin-dashboard.js"};
        int lastScript = -1;
        for (String script : scripts) {
            int position = workspace.body().indexOf("assets/" + script);
            assertTrue(position > lastScript, "Scripts must load in order: " + script);
            lastScript = position;
            var asset = get("/assets/" + script);
            assertEquals(200, asset.statusCode(), script);
            assertTrue(asset.body().contains("use strict"), "JavaScript content: " + script);
        }
        for (String asset : new String[]{"/assets/css/theme.css", "/assets/css/home.css", "/assets/admin-dashboard.css",
            "/assets/js/home.js"}) {
            assertEquals(200, get(asset + "?" + UI_VERSION).statusCode(), asset);
        }
        var catalogue = get("/assets/vinfast-catalogue.json");
        assertEquals(200, catalogue.statusCode(), catalogue.body());
        assertEquals(10, JSON.readTree(catalogue.body()).get("models").size());
        session();
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
        for (String page : new String[]{"/", "/index.jsp"}) {
            var response = showroomClient.send(HttpRequest.newBuilder(URI.create(showroomBase + page)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            assertHomepageLinks(response.body(), "/showroom");
            assertFalse(response.body().contains("/ElectricCarSales/"), "Links must follow the deployed context");
        }
        for (String asset : new String[]{"css/theme.css", "css/home.css", "js/home.js", "admin-dashboard.css",
            "admin-common.js", "admin-management.js", "sales-reports.js", "vehicle-catalogue.js", "js/customer-ai.js",
            "admin-dashboard.js", "vinfast-catalogue.json"}) {
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
        var workspace = showroomClient.send(HttpRequest.newBuilder(URI.create(showroomBase + "/admin.jsp"))
            .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, workspace.statusCode(), workspace.body());
        assertWorkspaceLinks(workspace.body(), "/showroom");
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

    @Test
    void renderedLoginFormPostsToDiscoveredServletAndDisplaysServerValidation() throws Exception {
        for (String deployment : new String[]{base, showroomBase}) {
            HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            var page = browser.send(HttpRequest.newBuilder(URI.create(deployment + "/login.jsp")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode(), page.body());
            var form = Pattern.compile("<form\\b[^>]*\\bid=\"login-form\"[^>]*>").matcher(page.body());
            assertTrue(form.find(), "The login page must render a form");
            String openingTag = form.group();
            assertTrue(Pattern.compile("\\bmethod=\"post\"", Pattern.CASE_INSENSITIVE).matcher(openingTag).find());
            var action = Pattern.compile("\\baction=\"([^\"]+)\"").matcher(openingTag);
            assertTrue(action.find(), "Login must have an explicit server action");
            URI target = page.uri().resolve(action.group(1));
            assertEquals(URI.create(deployment).getPath() + "/login",
                target.getPath().replaceFirst(";jsessionid=[^/]*", ""));
            assertTrue(page.body().contains("name=\"username\""));
            assertTrue(page.body().contains("name=\"password\""));
            assertTrue(Pattern.compile("/assets/js/login\\.js(?:;jsessionid=[^\"?]*)?\\?v=").matcher(page.body()).find(),
                "Clients must receive the current login script");

            String unsafeUsername = "<img src=x onerror=alert(1)>";
            var invalid = browser.send(HttpRequest.newBuilder(target)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="
                    + URLEncoder.encode(unsafeUsername, java.nio.charset.StandardCharsets.UTF_8) + "&password="))
                .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, invalid.statusCode(), invalid.body());
            var message = Pattern.compile("<p\\b([^>]*\\bid=\"form-message\"[^>]*)>(.*?)</p>", Pattern.DOTALL)
                .matcher(invalid.body());
            assertTrue(message.find(), "Server validation must render inside the login form");
            assertFalse(Pattern.compile("\\bhidden\\b").matcher(message.group(1)).find(),
                "A server error must be visible");
            assertTrue(message.group(2).contains("Vui lòng nhập tên đăng nhập và mật khẩu."));
            assertFalse(invalid.body().contains(unsafeUsername), "The posted username must be escaped");
            assertTrue(invalid.body().contains("&lt;img src=x onerror=alert(1)&gt;"));

            var valid = browser.send(HttpRequest.newBuilder(target)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=webadmin&password=web-test-password-123")).build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(302, valid.statusCode(), valid.body());
            assertTrue(valid.headers().firstValue("Location").orElse("")
                .startsWith(URI.create(deployment).getPath() + "/admin.jsp"));
            var state = browser.send(HttpRequest.newBuilder(URI.create(deployment + "/api/auth")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals("webadmin", JSON.readTree(state.body()).get("user").get("username").asText());
        }
    }

    private static void assertHomepageLinks(String body, String contextPath) {
        assertRenderedJsp(body, contextPath);
        assertTrue(body.contains("class=\"home-page\""), "The welcome page must render the VinFast homepage");
        assertTrue(body.contains("id=\"vehicle-grid\""));
        assertTrue(body.contains("id=\"compare-dialog\""));
        assertTrue(body.contains("href=\"" + contextPath + "/index.jsp\""));
        assertTrue(body.contains("href=\"" + contextPath + "/customer/ai-assistant.jsp\""),
            "Homepage assistant must open the public customer page");
        assertFalse(body.contains("/admin.jsp#ai"));
        assertTrue(body.contains("href=\"" + contextPath + "/login\""),
            "The administration menu must open the workspace login");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/theme.css");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/home.css");
        assertVersionedAsset(body, "src", contextPath + "/assets/js/home.js");
    }

    private static void assertWorkspaceLinks(String body, String contextPath) {
        assertRenderedJsp(body, contextPath);
        assertTrue(body.contains("href=\"" + contextPath + "/index.jsp\""),
            "Workspace brand must return to the shared homepage");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/theme.css");
        assertVersionedAsset(body, "href", contextPath + "/assets/admin-dashboard.css");
        assertTrue(body.contains("src=\"" + contextPath + "/assets/admin-dashboard.js"));
        assertFalse(body.contains("vehicle-assistant.js"), "Admin must not load the customer chat module");
        assertFalse(body.contains("data-view=\"ai\""), "Chat belongs in the customer experience");
    }

    private static void assertVersionedAsset(String body, String attribute, String expectedPath) {
        var resources = Pattern.compile("\\b" + attribute + "=\"([^\"]+)\"").matcher(body);
        while (resources.find()) {
            URI resource = URI.create(resources.group(1));
            if (expectedPath.equals(resource.getPath())) {
                String version = expectedPath.endsWith("/admin-dashboard.css") ? "v=dashboard-20261028"
                    : expectedPath.endsWith("/js/home.js") || expectedPath.endsWith("/css/home.css")
                        ? "v=home-tweaks-20261112" : UI_VERSION;
                assertEquals(version, resource.getQuery(), "Asset cache version: " + expectedPath);
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

    @Test
    void publicCustomerAssistantRendersAndSupportsGuestConversationComparisonAndResetInBothContexts() throws Exception {
        for (String deployment : new String[]{base, showroomBase}) {
            String contextPath = URI.create(deployment).getPath();
            HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            var page = browser.send(HttpRequest.newBuilder(URI.create(deployment
                + "/customer/ai-assistant.jsp?prompt=%3Cscript%3Ealert(1)%3C%2Fscript%3E")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode(), page.body());
            assertRenderedJsp(page.body(), contextPath);
            assertTrue(page.body().contains("data-customer-ai"));
            assertTrue(page.body().contains("id=\"ai-question-form\""));
            assertTrue(page.body().contains("id=\"ai-needs-form\""));
            assertTrue(page.body().contains("id=\"ai-compare-form\""));
            assertTrue(page.body().contains("name=\"budget\" type=\"number\" min=\"0.1\""));
            assertTrue(page.body().contains("name=\"minRange\" type=\"number\" min=\"1\""));
            assertTrue(page.body().contains(contextPath + "/assets/js/customer-ai.js?v=customer-ai-20261028"));
            assertTrue(page.body().contains(contextPath + "/assets/css/customer-ai.css?v=customer-ai-20261028"));
            assertFalse(page.body().contains("<script>alert(1)</script>"), "Prompt query must never be rendered as HTML");
            assertFalse(page.body().contains("Cần kết nối dịch vụ AI"));
            for (String asset : new String[]{"js/customer-ai.js", "css/customer-ai.css"}) {
                assertEquals(200, browser.send(HttpRequest.newBuilder(URI.create(deployment + "/assets/" + asset))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            }
            var auth = browser.send(HttpRequest.newBuilder(URI.create(deployment + "/api/auth")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            JsonNode guest = JSON.readTree(auth.body());
            assertTrue(guest.get("user").isNull());
            String token = guest.get("csrfToken").asText();
            var recommended = assistantPost(browser, deployment, token, "/api/ai",
                "{\"message\":\"Gợi ý xe dưới 600 triệu, 5 chỗ\",\"intent\":\"RECOMMEND\"}");
            assertEquals("EV Test", recommended.get("recommendations").get(0).get("vehicle").get("modelName").asText());
            var followUp = assistantPost(browser, deployment, token, "/api/ai",
                "{\"message\":\"Thông số xe đó\",\"intent\":\"PRODUCT_QA\"}");
            assertEquals(recommended.get("conversationId"), followUp.get("conversationId"));
            assertEquals("EV Test", followUp.get("comparison").get(0).get("modelName").asText());
            var compared = assistantPost(browser, deployment, token, "/api/ai",
                "{\"message\":\"So sánh EV Test và EV Family\",\"intent\":\"COMPARE\",\"modelIds\":[1,2]}");
            assertEquals(2, compared.get("comparison").size());
            var reset = assistantPost(browser, deployment, token, "/api/ai?action=reset", "{}");
            assertTrue(reset.get("cleared").asBoolean());
            assertNotEquals(recommended.get("conversationId"), reset.get("conversationId"));
            var afterReset = assistantPost(browser, deployment, token, "/api/ai",
                "{\"message\":\"Thông số xe đó\",\"intent\":\"PRODUCT_QA\"}");
            assertTrue(afterReset.get("comparison").isEmpty());
        }
    }

    @Test
    void signedInCustomerCanUsePublicAssistantWithoutAdministrationPermission() throws Exception {
        int originalRole;
        try (var connection = fixture.connection(); var statement = connection.createStatement();
            var rows = statement.executeQuery("SELECT RoleID FROM Users WHERE Username='websales'")) {
            assertTrue(rows.next());
            originalRole = rows.getInt("RoleID");
        }
        try {
            try (var connection = fixture.connection(); var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE Users SET RoleID=(SELECT RoleID FROM Roles WHERE RoleName='CUSTOMER')"
                    + " WHERE Username='websales'");
            }
            for (String deployment : new String[]{base, showroomBase}) {
                HttpClient browser = HttpClient.newBuilder()
                    .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
                var auth = browser.send(HttpRequest.newBuilder(URI.create(deployment + "/api/auth")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                String token = JSON.readTree(auth.body()).get("csrfToken").asText();
                var login = assistantPost(browser, deployment, token, "/api/auth",
                    "{\"action\":\"login\",\"username\":\"websales\",\"password\":\"web-test-password-123\"}");
                assertEquals("CUSTOMER", login.get("user").get("role").asText());
                token = login.get("csrfToken").asText();
                var response = assistantPost(browser, deployment, token, "/api/ai",
                    "{\"message\":\"Thông số EV Family\",\"intent\":\"PRODUCT_QA\"}");
                assertEquals("EV Family", response.get("comparison").get(0).get("modelName").asText());
                var page = browser.send(HttpRequest.newBuilder(URI.create(deployment + "/customer/ai-assistant.jsp"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, page.statusCode(), page.body());
                assertTrue(page.body().contains("data-customer-ai"));
            }
        } finally {
            try (var connection = fixture.connection(); var statement = connection.prepareStatement(
                "UPDATE Users SET RoleID=? WHERE Username='websales'")) {
                statement.setInt(1, originalRole);
                statement.executeUpdate();
            }
        }
    }

    private static JsonNode assistantPost(HttpClient browser, String deployment, String token, String path, String body)
        throws Exception {
        var response = browser.send(HttpRequest.newBuilder(URI.create(deployment + path))
            .header("Content-Type", "application/json").header("X-CSRF-Token", token)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return JSON.readTree(response.body());
    }
}
