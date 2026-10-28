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
        for (String page : new String[]{"/", "/index.jsp"}) {
            var response = get(page);
            assertEquals(200, response.statusCode(), response.body());
            assertHomepageLinks(response.body(), "/ElectricCarSales");
        }
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
        for (String page : new String[]{"/", "/index.jsp", "/admin.jsp"}) {
            var response = showroomClient.send(HttpRequest.newBuilder(URI.create(showroomBase + page)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            if ("/admin.jsp".equals(page)) {
                assertWorkspaceLinks(response.body(), "/showroom");
            } else {
                assertHomepageLinks(response.body(), "/showroom");
            }
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
        assertTrue(body.contains("href=\"" + contextPath + "/admin.jsp?login=1\""),
            "Homepage sign-in must open the workspace login");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/theme.css");
        assertVersionedAsset(body, "href", contextPath + "/assets/css/home.css");
        assertVersionedAsset(body, "src", contextPath + "/assets/js/home.js");
    }

    @Test
    void registrationAndOtpFormsRenderValidationWithoutCreatingAnUnverifiedAccount() throws Exception {
        for (String deployment : new String[]{base, showroomBase}) {
            HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            var page = browser.send(HttpRequest.newBuilder(URI.create(deployment + "/register")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode(), page.body());
            assertTrue(page.body().contains("id=\"register-form\""));
            assertTrue(page.body().contains("action=\"" + URI.create(deployment).getPath() + "/register\""));
            assertTrue(page.body().contains("Kênh gửi OTP chưa được cấu hình"));
            String unsafe = "<img src=x onerror=alert(1)>";
            var invalid = postForm(browser, deployment + "/register", "name="
                + URLEncoder.encode(unsafe, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(200, invalid.statusCode(), invalid.body());
            assertTrue(invalid.body().contains("role=\"alert\""));
            assertFalse(invalid.body().contains(unsafe));
            assertTrue(invalid.body().contains("&lt;img src=x onerror=alert(1)&gt;"));
            var valid = postForm(browser, deployment + "/register",
                "name=Test+Customer&username=pendingcustomer&phone=0900000999&email=pending%40test.local"
                    + "&password=valid-password-123&confirmPassword=valid-password-123");
            assertEquals(200, valid.statusCode(), valid.body());
            assertTrue(valid.body().contains("chưa thể hoàn tất đăng ký"));
            try (var connection = fixture.connection(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM Users WHERE Username='pendingcustomer'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            var otp = postForm(browser, deployment + "/verify-otp", "userId=1&otp=abc");
            assertEquals(200, otp.statusCode(), otp.body());
            assertTrue(otp.body().contains("id=\"otp-form\""));
            assertTrue(otp.body().contains("name=\"userId\" value=\"1\""));
            assertTrue(otp.body().contains("Vui lòng nhập mã OTP gồm 6 chữ số."));
            assertTrue(otp.body().contains("action=\"" + URI.create(deployment).getPath() + "/verify-otp\""));
            var invalidId = postForm(browser, deployment + "/verify-otp", "userId=-1&otp=123456");
            assertEquals(302, invalidId.statusCode());
            assertTrue(invalidId.headers().firstValue("Location").orElse("").endsWith("/register"));
        }
    }

    @Test
    void operationalPagesRenderStoredDataAndRejectGuestsAndCustomers() throws Exception {
        String[] routes = {"/car", "/car.jsp", "/contract", "/contract.jsp", "/delivery", "/delivery.jsp"};
        for (String route : routes) {
            var guest = get(route);
            assertEquals(302, guest.statusCode(), route);
            assertTrue(guest.headers().firstValue("Location").orElse("").endsWith("/login"));
        }
        try (var connection = fixture.connection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO Customers(UserID,CustomerName,Phone)"
                + " SELECT UserID,'Page test customer','0900000888' FROM Users WHERE Username='websales'");
            statement.execute("INSERT INTO Employees(UserID,EmployeeName,Phone)"
                + " SELECT UserID,'Page test employee','0900000887' FROM Users WHERE Username='webmanager'");
            statement.execute("INSERT INTO Orders(CustomerID,EmployeeID,OrderStatusID,PaymentMethodID,OrderDate,"
                + "TotalAmount,DepositAmount,RemainingAmount,Note) SELECT c.CustomerID,e.EmployeeID,s.OrderStatusID,"
                + "p.PaymentMethodID,SYSDATETIME(),500000000,0,500000000,'PAGE-TEST'"
                + " FROM Customers c CROSS JOIN Employees e CROSS JOIN OrderStatuses s CROSS JOIN PaymentMethods p"
                + " WHERE c.Phone='0900000888' AND e.Phone='0900000887' AND s.StatusName='PENDING' AND p.MethodName='CASH'");
            statement.execute("INSERT INTO Contracts(OrderID,CustomerID,EmployeeID,ContractNumber,ContractAmount,Status,Note)"
                + " SELECT OrderID,CustomerID,EmployeeID,'PAGE-CONTRACT',TotalAmount,'DRAFT','<script>alert(1)</script>'"
                + " FROM Orders WHERE Note='PAGE-TEST'");
            statement.execute("INSERT INTO Deliveries(OrderID,CustomerID,EmployeeID,CarID,DeliveryAddress,DeliveryStatus)"
                + " SELECT o.OrderID,o.CustomerID,o.EmployeeID,c.CarID,'<b>Page address</b>','PENDING'"
                + " FROM Orders o CROSS JOIN Cars c WHERE o.Note='PAGE-TEST' AND c.CarCode='TEST1'");
        }
        try {
            for (String deployment : new String[]{base, showroomBase}) {
                HttpClient staff = HttpClient.newBuilder()
                    .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
                assertEquals(302, postForm(staff, deployment + "/login",
                    "username=webadmin&password=web-test-password-123").statusCode());
                for (String route : new String[]{"/car", "/contract", "/delivery"}) {
                    var page = staff.send(HttpRequest.newBuilder(URI.create(deployment + route)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, page.statusCode(), page.body());
                    assertTrue(page.body().contains("<table>"), route);
                    assertTrue(page.body().contains(URI.create(deployment).getPath() + "/assets/css/data-pages.css"));
                    assertFalse(page.body().contains("<c:"), "JSTL must render: " + route);
                    if ("/car".equals(route)) {
                        assertTrue(page.body().contains("TEST1"));
                        assertTrue(page.body().contains("AVAILABLE"));
                    } else if ("/contract".equals(route)) {
                        assertTrue(page.body().contains("PAGE-CONTRACT"));
                        assertTrue(page.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
                    } else {
                        assertTrue(page.body().contains("&lt;b&gt;Page address&lt;/b&gt;"));
                    }
                }
                for (String route : new String[]{"/car.jsp", "/contract.jsp", "/delivery.jsp"}) {
                    var direct = staff.send(HttpRequest.newBuilder(URI.create(deployment + route)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                    assertEquals(302, direct.statusCode(), route);
                    assertTrue(direct.headers().firstValue("Location").orElse("")
                        .endsWith(route.substring(0, route.length() - 4)));
                }
            }
            try (var connection = fixture.connection(); var statement = connection.createStatement()) {
                statement.execute("UPDATE Users SET RoleID=(SELECT RoleID FROM Roles WHERE RoleName='CUSTOMER')"
                    + " WHERE Username='websales'");
            }
            HttpClient customer = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            assertEquals(302, postForm(customer, base + "/login",
                "username=websales&password=web-test-password-123").statusCode());
            for (String route : routes) {
                var denied = customer.send(HttpRequest.newBuilder(URI.create(base + route)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                assertEquals(403, denied.statusCode(), route);
                assertFalse(denied.body().contains("PAGE-CONTRACT"));
            }
        } finally {
            try (var connection = fixture.connection(); var statement = connection.createStatement()) {
                statement.execute("UPDATE Users SET RoleID=(SELECT RoleID FROM Roles WHERE RoleName='SALES')"
                    + " WHERE Username='websales'");
                statement.execute("DELETE FROM Deliveries WHERE OrderID IN (SELECT OrderID FROM Orders WHERE Note='PAGE-TEST')");
                statement.execute("DELETE FROM Contracts WHERE ContractNumber='PAGE-CONTRACT'");
                statement.execute("DELETE FROM Orders WHERE Note='PAGE-TEST'");
                statement.execute("DELETE FROM Customers WHERE Phone='0900000888'");
                statement.execute("DELETE FROM Employees WHERE Phone='0900000887'");
            }
        }
    }

    private static HttpResponse<String> postForm(HttpClient browser, String target, String body) throws Exception {
        return browser.send(HttpRequest.newBuilder(URI.create(target))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
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
                    : expectedPath.endsWith("/js/home.js") ? "v=home-ai-20261028" : UI_VERSION;
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
