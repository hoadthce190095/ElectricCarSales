package com.electriccarsales.controller;

import com.electriccarsales.dao.OTPVerificationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/verify-otp")
public class OtpController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/register");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        try {
            int userId = Integer.parseInt(req.getParameter("userId"));
            String code = req.getParameter("otp");
            if (code == null || !code.matches("\\d{6}")
                    || !new OTPVerificationDAO().verifyRegistrationOtp(userId, code)) {
                req.setAttribute("error", "OTP không đúng hoặc đã hết hạn. Hãy đăng ký lại để nhận mã mới.");
                req.setAttribute("userId", userId);
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/register");
        } catch (RuntimeException e) {
            log("OTP verification failed", e);
            req.setAttribute("error", "Không thể xác thực OTP do lỗi dữ liệu.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        }
    }
}
