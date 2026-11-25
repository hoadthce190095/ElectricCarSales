package com.electriccarsales.controller;

import com.electriccarsales.dao.AuthenticationDAO;
import com.electriccarsales.model.Account;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.PasswordSession;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.DBContext;

import java.io.IOException;
import java.sql.Connection;
import java.util.Locale;
import java.util.Set;

/** Protects shared operational lists, including direct access to their JSP views. */
@WebFilter(urlPatterns = {
    "/car", "/car.jsp",
    "/contract", "/contract.jsp", "/invoice",
    "/delivery", "/delivery.jsp",
})
public class OperationsPageFilter implements Filter {
    private static final Set<String> STAFF_ROLES = Set.of("ADMIN", "MANAGER", "SALES", "OPERATIONS", "ACCOUNTANT");

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setHeader("X-Content-Type-Options", "nosniff");
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            loginRequired(request, response);
            return;
        }
        try (Connection connection = DBContext.openConnection()) {
            Account account = new AuthenticationDAO().byId(connection, user.getUserId());
            if (account == null || !"ACTIVE".equals(account.getStatus())
                || !PasswordSession.matches(session.getAttribute(PasswordSession.ATTRIBUTE), account.getPasswordHash())) {
                session.invalidate();
                loginRequired(request, response);
                return;
            }
            String role = account.getRole() == null ? "" : account.getRole().toUpperCase(Locale.ROOT);
            session.setAttribute("user", new SessionUser(account.getUserId(), account.getUsername(), account.getRole()));
            session.setAttribute("roleName", account.getRole());
            String path = request.getServletPath();
            boolean allowed;

            if (path.startsWith("/customer/")) {
                if ("/customer/profile".equals(path) && "staff".equalsIgnoreCase(request.getParameter("mode"))) {
                    allowed = Set.of("SALES", "MANAGER", "ADMIN").contains(role);
                } else {
                    allowed = "CUSTOMER".equals(role);
                }
            } else if (path.startsWith("/admin/")) {
                allowed = "ADMIN".equals(role);
            } else if ("/car".equals(path)) {
                allowed = Set.of("MANAGER", "ADMIN").contains(role);
            } else if (path.startsWith("/staff/inventory")) {
                allowed = Set.of("OPERATIONS", "MANAGER", "ADMIN").contains(role);
            } else if ("/contract".equals(path) || "/invoice".equals(path)) {
                // RDS 8.3 & 9.2: Both Customer and Staff/Admin access /contract according to their role scope
                allowed = "CUSTOMER".equals(role) || STAFF_ROLES.contains(role);
            } else {
                allowed = STAFF_ROLES.contains(role);
            }

            if (!allowed) {
                writeError(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Vai trò hiện tại không có quyền truy cập nghiệp vụ này.");
                return;
            }
        } catch (Exception exception) {
            request.getServletContext().log("Cannot verify operational page access", exception);
            writeError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Không thể xác minh quyền truy cập do lỗi kết nối cơ sở dữ liệu.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void loginRequired(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (wantsJson(request)) writeError(request, response, 401, "Vui lòng đăng nhập lại.");
        else response.sendRedirect(request.getContextPath() + "/login");
    }

    private static void writeError(HttpServletRequest request, HttpServletResponse response,
                                   int status, String message) throws IOException {
        if (wantsJson(request)) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + jsonEscape(message) + "\"}");
            return;
        }
        response.sendError(status, message);
    }

    private static boolean wantsJson(HttpServletRequest request) {
        return "json".equalsIgnoreCase(request.getParameter("format"))
            || (request.getHeader("Accept") != null
                && request.getHeader("Accept").contains("application/json"));
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\r", "\\r").replace("\n", "\\n");
    }
}
