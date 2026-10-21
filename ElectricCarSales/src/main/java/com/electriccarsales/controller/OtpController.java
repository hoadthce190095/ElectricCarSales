package com.electriccarsales.controller;

import com.electriccarsales.service.OtpService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/verify-otp")
public class OtpController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object pending = session == null ? null : session.getAttribute("pendingRegistrationUserId");
        if (!(pending instanceof Integer userId) || userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        req.setAttribute("userId", userId);
        req.setAttribute("email", session.getAttribute("pendingRegistrationEmail"));
        req.setAttribute("notice", session.getAttribute("otpNotice"));
        session.removeAttribute("otpNotice");
        req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        Object pending = session == null ? null : session.getAttribute("pendingRegistrationUserId");
        if (!(pending instanceof Integer userId) || userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        String action = req.getParameter("action");
        if ("resend".equals(action)) {
            try {
                boolean sent = new OtpService().resendRegistrationOtp(userId);
                req.setAttribute(sent ? "notice" : "error", sent
                        ? "Mã xác thực mới đã được gửi." : "Không thể gửi mã. Tài khoản có thể đã được xác minh.");
            } catch (RuntimeException e) {
                log("Registration OTP resend failed");
                req.setAttribute("error", "Không gửi được email xác thực. Vui lòng thử lại.");
            }
            req.setAttribute("userId", userId);
            req.setAttribute("email", session.getAttribute("pendingRegistrationEmail"));
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        try {
            String code = req.getParameter("otp");
            if (!new OtpService().verifyRegistration(userId, code)) {
                req.setAttribute("error", "OTP không đúng, đã hết hạn hoặc đã được thay thế.");
                req.setAttribute("userId", userId);
                req.setAttribute("email", session.getAttribute("pendingRegistrationEmail"));
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            session.removeAttribute("pendingRegistrationUserId");
            session.removeAttribute("pendingRegistrationEmail");
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
        } catch (RuntimeException e) {
            log("OTP verification failed");
            req.setAttribute("error", "Không thể xác thực OTP do lỗi dữ liệu.");
            req.setAttribute("userId", userId);
            req.setAttribute("email", session.getAttribute("pendingRegistrationEmail"));
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        }
    }
}
