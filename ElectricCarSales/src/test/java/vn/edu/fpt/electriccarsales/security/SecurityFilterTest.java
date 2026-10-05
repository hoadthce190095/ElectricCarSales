package vn.edu.fpt.electriccarsales.security;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class SecurityFilterTest {
    @Test
    void anonymousReadsCannotReachAdministrativeAndFinancialControllers() throws Exception {
        for (String route : new String[]{"/api/reports", "/api/admin/users", "/api/admin/logs", "/api/admin/roles"}) {
            Exchange exchange = exchange("GET", route, route.startsWith("/api/admin") ? "/api/admin" : route,
                route.startsWith("/api/admin") ? route.substring("/api/admin".length()) : null, null, null);
            filter(exchange);
            assertEquals(401, exchange.status[0], route);
            assertEquals(0, exchange.passed.get(), route);
        }
    }

    @Test
    void mappedProtectedRoutesRemainProtectedWithPathParametersAndEncodedNames() throws Exception {
        for (String uri : new String[]{"/api/reports;anything=value", "/api/%72eports"}) {
            Exchange exchange = exchange("GET", uri, "/api/reports", null, null, null);
            filter(exchange);
            assertEquals(401, exchange.status[0], uri);
            assertEquals(0, exchange.passed.get(), uri);
        }
        for (String uri : new String[]{"/api/admin;anything=value/users", "/api/%61dmin/users"}) {
            Exchange exchange = exchange("GET", uri, "/api/admin", "/users", null, null);
            filter(exchange);
            assertEquals(401, exchange.status[0], uri);
            assertEquals(0, exchange.passed.get(), uri);
        }
    }

    @Test
    void publicAssistantReadsAreAllowedAndWritesRequireSessionCsrf() throws Exception {
        Exchange publicRead = exchange("GET", "/api/ai", "/api/ai", null, null, null);
        filter(publicRead);
        assertEquals(1, publicRead.passed.get());
        for (String actual : new String[]{null, "wrong", "session-token-extra"}) {
            Exchange invalid = exchange("POST", "/api/ai", "/api/ai", null, "session-token", actual);
            filter(invalid);
            assertEquals(403, invalid.status[0]);
            assertEquals(0, invalid.passed.get());
        }
        Exchange valid = exchange("POST", "/api/ai", "/api/ai", null, "session-token", "session-token");
        filter(valid);
        assertEquals(1, valid.passed.get());
        assertEquals("nosniff", valid.headers.get("X-Content-Type-Options"));
        assertEquals("no-store", valid.headers.get("Cache-Control"));
    }

    @Test
    void loginAndLogoutAreAlsoCsrfProtected() throws Exception {
        Exchange invalid = exchange("POST", "/api/auth", "/api/auth", null, null, null);
        filter(invalid);
        assertEquals(403, invalid.status[0]);
        assertEquals(0, invalid.passed.get());
        Exchange valid = exchange("POST", "/api/auth", "/api/auth", null, "token", "token");
        filter(valid);
        assertEquals(1, valid.passed.get());
    }

    private static void filter(Exchange exchange) throws Exception {
        new SecurityFilter().doFilter(exchange.request, exchange.response,
            (request, response) -> exchange.passed.incrementAndGet());
    }

    private static Exchange exchange(String method, String uri, String servletPath, String pathInfo,
        String expectedToken, String actualToken) {
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> sessionAttributes = new HashMap<>();
        if (expectedToken != null) {
            sessionAttributes.put("csrfToken", expectedToken);
        }
        HttpSession session = (HttpSession) Proxy.newProxyInstance(SecurityFilterTest.class.getClassLoader(),
            new Class<?>[]{HttpSession.class}, (proxy, operation, arguments) -> switch (operation.getName()) {
                case "getAttribute" -> sessionAttributes.get(arguments[0]);
                case "setAttribute" -> {
                    sessionAttributes.put((String) arguments[0], arguments[1]);
                    yield null;
                }
                case "removeAttribute" -> {
                    sessionAttributes.remove(arguments[0]);
                    yield null;
                }
                default -> defaultValue(operation.getReturnType());
            });
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
            SecurityFilterTest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
            (proxy, operation, arguments) -> switch (operation.getName()) {
                case "getMethod" -> method;
                case "getRequestURI" -> uri;
                case "getContextPath" -> "";
                case "getServletPath" -> servletPath;
                case "getPathInfo" -> pathInfo;
                case "getSession" -> expectedToken == null ? null : session;
                case "getHeader" -> "X-CSRF-Token".equals(arguments[0]) ? actualToken : null;
                case "getAttribute" -> attributes.get(arguments[0]);
                case "setAttribute" -> {
                    attributes.put((String) arguments[0], arguments[1]);
                    yield null;
                }
                default -> defaultValue(operation.getReturnType());
            });
        int[] status = {200};
        Map<String, String> headers = new HashMap<>();
        StringWriter body = new StringWriter();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
            SecurityFilterTest.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
            (proxy, operation, arguments) -> switch (operation.getName()) {
                case "setStatus" -> {
                    status[0] = (int) arguments[0];
                    yield null;
                }
                case "getStatus" -> status[0];
                case "setHeader" -> {
                    headers.put((String) arguments[0], (String) arguments[1]);
                    yield null;
                }
                case "getWriter" -> new PrintWriter(body);
                default -> defaultValue(operation.getReturnType());
            });
        return new Exchange(request, response, status, headers, new AtomicInteger());
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }

    private record Exchange(HttpServletRequest request, HttpServletResponse response, int[] status,
        Map<String, String> headers, AtomicInteger passed) {
    }
}
