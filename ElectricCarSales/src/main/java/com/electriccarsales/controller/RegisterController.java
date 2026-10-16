package com.electriccarsales.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegisterController extends HttpServlet {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{5,50}");
    private static final Pattern PHONE = Pattern.compile("0[0-9]{9,10}");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String name = trim(req.getParameter("name"));
        String username = trim(req.getParameter("username"));
        String phone = trim(req.getParameter("phone"));
        String email = trim(req.getParameter("email")).toLowerCase();
        String address = trim(req.getParameter("address"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        req.setAttribute("name", name); req.setAttribute("username", username);
        req.setAttribute("phone", phone); req.setAttribute("email", email); req.setAttribute("address", address);

        String error = validate(name, username, phone, email, password, confirm);
        if (error != null) {
            req.setAttribute("error", error);
            req.getRequestDispatcher("/auth/register.jsp").forward(req, resp);
            return;
        }

        // Registration creates an inactive account and is not safe to persist until OTP delivery is available.
        req.setAttribute("error", "Hệ thống chưa cấu hình kênh gửi OTP nên chưa thể hoàn tất đăng ký. "
                + "Vui lòng liên hệ quản trị viên.");
        req.getRequestDispatcher("/auth/register.jsp").forward(req, resp);
    }

    private String validate(String name, String username, String phone, String email, String password, String confirm) {
        if (name.isBlank() || name.length() > 100) return "Vui lòng nhập họ tên hợp lệ.";
        if (!USERNAME.matcher(username).matches()) return "Tên đăng nhập phải có 5–50 ký tự, chỉ gồm chữ, số và dấu gạch dưới.";
        if (!PHONE.matcher(phone).matches()) return "Số điện thoại phải bắt đầu bằng 0 và có 10 hoặc 11 chữ số.";
        if (email.isBlank() || email.length() > 100 || !EMAIL.matcher(email).matches()) return "Email không hợp lệ.";
        if (password == null || password.length() < 8 || password.length() > 72)
            return "Mật khẩu phải có từ 8 đến 72 ký tự.";
        if (confirm == null || !password.equals(confirm)) return "Mật khẩu nhập lại không khớp.";
        return null;
    }
    private String trim(String value) { return value == null ? "" : value.trim(); }
}
