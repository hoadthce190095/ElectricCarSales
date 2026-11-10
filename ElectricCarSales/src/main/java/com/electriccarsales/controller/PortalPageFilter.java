
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
    "/admin/*"
})
public class PortalPageFilter implements Filter {

    private static final Set<String> STAFF_ROLES =
        Set.of("SALES", "OPERATIONS", "ACCOUNTANT", "MANAGER");

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

        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");

        if (!(sessionUser instanceof SessionUser)) {
            response.sendRedirect(request.getContextPath() + "/login");
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
                response.sendRedirect(request.getContextPath() + "/login");
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
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        String path = request.getServletPath();

        if (path.startsWith("/customer/") && !"CUSTOMER".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (path.startsWith("/admin/") && !"ADMIN".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (path.startsWith("/staff/inventory")) {
            if (!INVENTORY_ROLES.contains(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } else if (path.startsWith("/staff/")
            && !STAFF_ROLES.contains(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("/customer/my-account.jsp".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/customer/profile");
            return;
        }

        chain.doFilter(request, response);
    }
}
