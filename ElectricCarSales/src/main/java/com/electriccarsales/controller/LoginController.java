package com.electriccarsales.controller;

import com.electriccarsales.service.AuthenticationService;
import com.electriccarsales.model.Account;
import com.electriccarsales.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBContext;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/login")
public class LoginController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("1".equals(req.getParameter("registered"))) req.setAttribute("success", "Đăng ký thành công. Bạn có thể đăng nhập.");
        if ("1".equals(req.getParameter("reset"))) req.setAttribute("success", "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.");
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
                req.setAttribute("error", "Tên đăng nhập, mật khẩu hoặc trạng thái tài khoản không hợp lệ.");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
                return;
            }
            String roleName = authenticationService.getRoleName(user.getRoleID());
            Account account = new Account(user.getUserID(), user.getUsername(), user.getPasswordHash(),
                user.getRoleID(), roleName, "ACTIVE");
            try (Connection connection = DBContext.openConnection()) {
                AuthenticationController.createLoginSession(req, account, List.of(), connection);
            }
            String role = roleName.toUpperCase(java.util.Locale.ROOT);

            if ("CUSTOMER".equals(role)) {
                resp.sendRedirect(req.getContextPath() + "/index.jsp");
            } else if ("ADMIN".equals(role)) {
                resp.sendRedirect(req.getContextPath() + "/admin.jsp");
            } else if (List.of("SALES", "OPERATIONS", "ACCOUNTANT", "MANAGER").contains(role)) {
                resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (SQLException | RuntimeException e) {
            log("Login failed", e);
            req.setAttribute("error", "Không thể đăng nhập do lỗi kết nối hoặc dữ liệu. Vui lòng thử lại.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
