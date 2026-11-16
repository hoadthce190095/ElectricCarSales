
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

@WebFilter(urlPatterns = {
    "/customer/*",
    "/staff/*",
    "/admin",
    "/admin/*",
    "/admin.jsp",
    "/orders",
    "/payment/*"
})
public class PortalPageFilter implements Filter {

    private static final Set<String> STAFF_ROLES =
        Set.of("SALES", "OPERATIONS", "ACCOUNTANT", "MANAGER", "ADMIN");

    private static final Set<String> INVENTORY_ROLES =
        Set.of("OPERATIONS", "MANAGER", "ADMIN");

    @Override
    public void doFilter(ServletRequest rawRequest,
                         ServletResponse rawResponse,
                         FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;

        response.setHeader("Cache-Control", "no-store");

        String path = request.getServletPath();
        boolean assistant = "/customer/ai-assistant.jsp".equals(path);
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");

        if (!(sessionUser instanceof SessionUser)) {
            if (assistant) {
                chain.doFilter(request, response);
                return;
            }
            loginRequired(request, response);
            return;
        }

        SessionUser user = (SessionUser) sessionUser;
        String role;

        try (Connection connection = DBContext.openConnection()) {
            Account account = new AuthenticationDAO()
                .byId(connection, user.getUserId());

            if (account == null
                || !"ACTIVE".equalsIgnoreCase(account.getStatus())
                || !PasswordSession.matches(
                session.getAttribute(PasswordSession.ATTRIBUTE),
                account.getPasswordHash())) {

                session.invalidate();
                loginRequired(request, response);
                return;
            }

            role = account.getRole() == null
                ? ""
                : account.getRole().toUpperCase(Locale.ROOT);

            session.setAttribute("user",
                new SessionUser(account.getUserId(),
                    account.getUsername(), account.getRole()));
            session.setAttribute("roleName", role);

        } catch (Exception e) {
            request.getServletContext().log("Page authorization failed", e);
            writeError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Không thể xác minh quyền truy cập do lỗi kết nối cơ sở dữ liệu.");
            return;
        }

        boolean salesCustomerList = "/customer/profile".equals(path)
            && "staff".equalsIgnoreCase(request.getParameter("mode"))
            && Set.of("SALES", "MANAGER", "ADMIN").contains(role);

        if (!assistant && path.startsWith("/customer/") && !"CUSTOMER".equals(role) && !salesCustomerList) {
            writeError(request, response, HttpServletResponse.SC_FORBIDDEN,
                "Vai trò hiện tại không có quyền truy cập trang khách hàng này.");
            return;
        }

        if (("/admin".equals(path) || path.startsWith("/admin/") || "/admin.jsp".equals(path)) && !"ADMIN".equals(role)) {
            writeError(request, response, HttpServletResponse.SC_FORBIDDEN,
                "Chỉ Admin được truy cập trang này.");
            return;
        }

        if ("/admin".equals(path) || "/admin/".equals(path) || "/admin/admin.html".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/admin.jsp");
            return;
        }

        if (path.startsWith("/staff/inventory")) {
            if (!INVENTORY_ROLES.contains(role)) {
                writeError(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Operations Officer, Manager hoặc Admin được truy cập tồn kho.");
                return;
            }
        } else if (path.startsWith("/staff/")
            && !STAFF_ROLES.contains(role)) {
            writeError(request, response, HttpServletResponse.SC_FORBIDDEN,
                "Vai trò hiện tại không có quyền truy cập khu vực nhân viên.");
            return;
        }

        if ("/customer/my-account.jsp".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/customer/profile");
            return;
        }

        if ("/customer/my-profile.jsp".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/customer/profile?view=profile");
            return;
        }

        chain.doFilter(request, response);
    }

    private static void loginRequired(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"))) {
            writeError(request, response, 401, "Vui lòng đăng nhập lại.");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private static void writeError(HttpServletRequest request, HttpServletResponse response,
                                   int status, String message) throws IOException {
        boolean json = "json".equalsIgnoreCase(request.getParameter("format"))
            || (request.getHeader("Accept") != null
                && request.getHeader("Accept").contains("application/json"));
        if (json) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + message
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"}");
        } else {
            response.sendError(status, message);
        }
    }
}
