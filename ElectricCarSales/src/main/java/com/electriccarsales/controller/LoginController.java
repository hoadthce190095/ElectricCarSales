package com.electriccarsales.controller;

import com.electriccarsales.dao.UserDAO;
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
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("error", "Please enter username or password");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }
        try {
            User user = new UserDAO().login(username, password);
            if (user == null) {
                req.setAttribute("error",
                        "The login information is incorrect or the account has not been verified/activated.");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
                return;
            }
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null)
                oldSession.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("role", user.getRoleID());
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
        } catch (RuntimeException e) {
            log("Login failed", e);
            req.setAttribute("error", "Login failed");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
