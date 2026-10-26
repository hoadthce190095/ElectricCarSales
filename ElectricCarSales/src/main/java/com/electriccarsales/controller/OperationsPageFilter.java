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
@WebFilter(urlPatterns = {"/car", "/car.jsp", "/contract", "/contract.jsp", "/delivery", "/delivery.jsp"})
public class OperationsPageFilter implements Filter {
    private static final Set<String> STAFF_ROLES = Set.of("ADMIN", "MANAGER", "SALES", "OPERATIONS", "ACCOUNTANT");

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try (Connection connection = DBContext.openConnection()) {
            Account account = new AuthenticationDAO().byId(connection, user.getUserId());
            if (account == null || !"ACTIVE".equals(account.getStatus())
                || !PasswordSession.matches(session.getAttribute(PasswordSession.ATTRIBUTE), account.getPasswordHash())) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            String role = account.getRole() == null ? "" : account.getRole().toUpperCase(Locale.ROOT);
            session.setAttribute("user", new SessionUser(account.getUserId(), account.getUsername(), account.getRole()));
            session.setAttribute("roleName", account.getRole());
            if (!STAFF_ROLES.contains(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem danh sách nghiệp vụ.");
                return;
            }
        } catch (Exception exception) {
            request.getServletContext().log("Cannot verify operational page access", exception);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể xác minh quyền truy cập.");
            return;
        }
        chain.doFilter(request, response);
    }
}
