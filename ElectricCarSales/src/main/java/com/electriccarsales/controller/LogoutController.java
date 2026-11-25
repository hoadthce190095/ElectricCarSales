package com.electriccarsales.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logout(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logout(req, resp);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {}
        }

        // Expire session cookie
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
                    cookie.setMaxAge(0);
                    resp.addCookie(cookie);
                }
            }
        }

        // Anti-cache headers to prevent browser back-button recalling authenticated state
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        boolean isJson = "json".equalsIgnoreCase(req.getParameter("format"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        String homeUrl = req.getContextPath() + "/index.jsp";
        if (isJson) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"success\":true,\"message\":\"Đã đăng xuất thành công.\",\"redirectUrl\":\"" + homeUrl + "\"}");
        } else {
            resp.sendRedirect(homeUrl);
        }
    }
}
