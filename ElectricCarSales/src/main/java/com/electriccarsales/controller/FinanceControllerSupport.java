package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceCustomerDAO;
import com.electriccarsales.dao.FinanceDAOException;
import com.electriccarsales.model.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Shared request validation for the finance servlets. Identity values must be set by server-side login code. */
final class FinanceControllerSupport {
    static final String CUSTOMER_ROLE = "CUSTOMER";

    private FinanceControllerSupport() { }

    static Integer customerId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof SessionUser user) || user.getRole() == null
                || !CUSTOMER_ROLE.equals(user.getRole().trim().toUpperCase(Locale.ROOT))) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập bằng tài khoản khách hàng.");
            return null;
        }
        try {
            Integer customerId = new FinanceCustomerDAO().findCustomerIdByUserId(user.getUserId());
            if (customerId == null) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa liên kết hồ sơ khách hàng.");
            }
            return customerId;
        } catch (FinanceDAOException e) {
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể xác minh hồ sơ khách hàng. Vui lòng thử lại sau.");
            return null;
        }
    }

    static Integer employeeId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Integer employeeId = positiveSessionInt(session, "employeeId");
        String role = session == null ? null : stringSessionValue(session, "role");
        if (employeeId == null || role == null || !isFinanceStaffRole(role)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xử lý nghiệp vụ tài chính.");
            return null;
        }
        return employeeId;
    }

    static boolean isFinanceStaffRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ACCOUNTANT") || normalized.equals("MANAGER") || normalized.equals("ADMIN");
    }

    static Integer positiveInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    private static Integer positiveSessionInt(HttpSession session, String key) {
        if (session == null) return null;
        Object value = session.getAttribute(key);
        if (value instanceof Number number) {
            long id = number.longValue();
            return id > 0 && id <= Integer.MAX_VALUE ? (int) id : null;
        }
        return value instanceof String text ? positiveInt(text) : null;
    }

    private static String stringSessionValue(HttpSession session, String key) {
        Object value = session == null ? null : session.getAttribute(key);
        return value instanceof String text ? text.trim() : null;
    }

}
