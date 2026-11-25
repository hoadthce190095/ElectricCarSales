package com.electriccarsales.controller;

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
import com.electriccarsales.model.Account;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.PasswordSession;
import com.electriccarsales.dao.AuthenticationDAO;
import com.electriccarsales.service.AuthorizationService;
import com.electriccarsales.service.SystemConfigurationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebFilter("/api/*")
public class SecurityFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(SecurityFilter.class.getName());
    private final AuthenticationDAO authenticationDAO;

    public SecurityFilter() {
        this(new AuthenticationDAO());
    }

    public SecurityFilter(AuthenticationDAO authenticationDAO) {
        this.authenticationDAO = authenticationDAO;
    }

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Use the mapped path because raw URIs can contain encoded names or matrix parameters.
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        boolean safe = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        HttpSession session = request.getSession(false);
        if (!safe && !validCsrfToken(request, session)) {
            JsonServlet.error(response, 403, "Phiên thao tác không hợp lệ. Tải lại trang rồi thử lại.");
            return;
        }

        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        List<String> permissions = List.of();
        if (user != null && (!"/api/auth".equals(path) || safe)) {
            try (Connection connection = DBContext.openConnection()) {
                Account account = authenticationDAO.byId(connection, user.getUserId());
                int timeout = SystemConfigurationService.integer(connection, "SESSION_TIMEOUT_MINUTES");
                session.setMaxInactiveInterval(timeout * 60);
                request.setAttribute("sessionTimeoutMinutes", timeout);
                if (account == null || !"ACTIVE".equals(account.getStatus()) || !PasswordSession
                    .matches(session.getAttribute(PasswordSession.ATTRIBUTE), account.getPasswordHash())) {
                    // Đổi hoặc đặt lại mật khẩu sẽ làm mọi phiên cũ hết hiệu lực.
                    session.invalidate();
                    user = null;
                } else {
                    user = new SessionUser(account.getUserId(), account.getUsername(), account.getRole());
                    session.setAttribute("user", user);
                    permissions = AuthorizationService.permissions(connection, account.getRoleId(), account.getRole());
                }
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, "Cannot verify account access", exception);
                JsonServlet.error(response, 503, "Không thể xác minh quyền truy cập. Kiểm tra kết nối cơ sở dữ liệu.");
                return;
            }
        }
        request.setAttribute("permissions", permissions);

        String required = requiredPermission(path);
        if (required != null) {
            if (user == null) {
                JsonServlet.error(response, 401, "Bạn cần đăng nhập.");
                return;
            }
            if (!permitted(required, user, permissions)) {
                JsonServlet.error(response, 403, "Bạn không có quyền sử dụng chức năng này.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean validCsrfToken(HttpServletRequest request, HttpSession session) {
        String expected = session == null ? null : (String) session.getAttribute("csrfToken");
        String actual = request.getHeader("X-CSRF-Token");
        return expected != null && actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8));
    }

    private String requiredPermission(String path) {
        if ("/api/reports".equals(path)) {
            return "VIEW_REPORTS";
        }
        if (path.startsWith("/api/admin/")) {
            String section = path.substring("/api/admin/".length()).split("/")[0];
            return switch (section) {
                case "logs" -> "VIEW_LOGS";
                case "promotions" -> "MANAGE_PROMOTIONS";
                case "lookups" -> "LOOKUPS";
                case "roles", "configurations" -> "ADMIN";
                default -> "MANAGE_USERS";
            };
        }
        return null;
    }

    private boolean permitted(String required, SessionUser user, List<String> permissions) {
        return switch (required) {
            case "ADMIN" -> "ADMIN".equals(user.getRole());
            case "LOOKUPS" -> permissions.contains("MANAGE_USERS") || permissions.contains("MANAGE_PROMOTIONS");
            default -> permissions.contains(required);
        };
    }
}
