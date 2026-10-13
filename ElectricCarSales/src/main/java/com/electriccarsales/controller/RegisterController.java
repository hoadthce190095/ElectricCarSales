package com.electriccarsales.controller;

import com.electriccarsales.dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegisterController extends HttpServlet {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^0[0-9]{9,10}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");
        String name = trim(req.getParameter("name"));
        String email = trim(req.getParameter("email")).toLowerCase();
        String phone = trim(req.getParameter("phone"));
        String address = trim(req.getParameter("address"));
        req.setAttribute("username", username);
        req.setAttribute("name", name);
        req.setAttribute("email", email);
        req.setAttribute("phone", phone);
        req.setAttribute("address", address);

        String error = validate(username, password, confirm, name, email, phone);
        if (error != null) {
            showError(req, resp, error);
            return;
        }
        try {
            if (new UserDAO().isUsernameExist(username)) {
                showError(req, resp, "The username is already in use.");
                return;
            }
            if (new UserDAO().isEmailExist(email)) {
                showError(req, resp, "The email is already in use.");
                return;
            }
            if (new UserDAO().isPhoneExist(phone)) {
                showError(req, resp, "The phone number has already been used.");
                return;
            }
            String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
            int userId = new UserDAO().registerCustomer(username, password, email, phone, name, address, otp);
            req.setAttribute("userId", userId);
            req.setAttribute("otpForTesting", otp);
            req.setAttribute("success", "Registration has been recorded. Enter the OTP to activate your account.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            log("Registration failed", e);
            showError(req, resp, "Registration failed.");
        }
    }

    private String validate(String username, String password, String confirm, String name, String email, String phone) {
        if (username.isEmpty() || password == null || password.isEmpty() || name.isEmpty() || email.isEmpty()
                || phone.isEmpty())
            return "Please enter your full name, username, email, phone number, and password.";
        if (!USERNAME.matcher(username).matches())
            return "The username must be 5–50 characters long and consist only of letters, numbers, or underscores.";
        if (password.length() < 8 || password.length() > 72 || password.trim().length() != password.length())
            return "The password must be 8–72 characters long and contain no leading or trailing spaces.";
        if (!password.equals(confirm))
            return "The confirmation password does not match.";
        if (!EMAIL.matcher(email).matches())
            return "Invalid email.";
        if (!PHONE.matcher(phone).matches())
            return "Vietnamese phone numbers must start with 0 and consist of 10 or 11 digits.";
        if (name.length() > 100)
            return "Full name: maximum 100 characters.";
        return null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        req.getRequestDispatcher("/auth/register.jsp").forward(req, resp);
    }
}
