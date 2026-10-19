package com.electriccarsales.controller;

import com.electriccarsales.service.AuthenticationService;
import com.electriccarsales.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("1".equals(req.getParameter("registered"))) req.setAttribute("success", "Đăng ký thành công. Bạn có thể đăng nhập.");
        if ("1".equals(req.getParameter("loggedOut"))) req.setAttribute("success", "Bạn đã đăng xuất thành công.");
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("error", "Vui lòng nhập tên đăng nhập và mật khẩu.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }
        try {
            AuthenticationService authenticationService = new AuthenticationService();
            User user = authenticationService.authenticate(username.trim(), password);
            if (user == null) {
                req.setAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác.");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
                return;
            }
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("userID", user.getUserID());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("roleID", user.getRoleID());
            session.setAttribute("roleName", authenticationService.getRoleName(user.getRoleID()));
            session.setAttribute("email", user.getEmail());
            session.setAttribute("phone", user.getPhone());

            String role = ((String) session.getAttribute("roleName")).toUpperCase(java.util.Locale.ROOT);
            if ("CUSTOMER".equals(role)) resp.sendRedirect(req.getContextPath() + "/index.jsp");
            else if ("ADMIN".equals(role)) resp.sendRedirect(req.getContextPath() + "/admin/admin.html");
            else resp.sendRedirect(req.getContextPath() + "/staff/dashboard.jsp");
        } catch (RuntimeException e) {
            log("Login failed", e);
            req.setAttribute("error", "Không thể đăng nhập do lỗi kết nối hoặc dữ liệu. Vui lòng thử lại.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
