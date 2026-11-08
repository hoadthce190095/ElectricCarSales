package com.electriccarsales.controller;

import com.electriccarsales.service.OtpService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import jakarta.servlet.http.HttpSession;
import com.electriccarsales.dao.OTPVerificationDAO;
import com.electriccarsales.service.RegistrationEmailService;

@WebServlet("/verify-otp")
public class OtpController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("registrationUserId") instanceof Integer)) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        req.setAttribute("email", session.getAttribute("registrationEmail"));
        req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("registrationUserId") instanceof Integer userId)) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        req.setAttribute("email", session.getAttribute("registrationEmail"));
        try {
            if ("resend".equals(req.getParameter("action"))) {
                long now = System.currentTimeMillis();
                Object previous = session.getAttribute("otpLastSent");
                if (previous instanceof Long && now - (Long) previous < 60_000L) {
                    req.setAttribute("error", "Vui lòng chờ 60 giây trước khi yêu cầu mã mới.");
                } else {
                    String email = (String) session.getAttribute("registrationEmail");
                    String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
                    RegistrationEmailService mail = new RegistrationEmailService();
                    if (!mail.isConfigured() || !mail.sendRegistrationOtp(email, otp)) {
                        req.setAttribute("error", "Không gửi được email OTP. Kiểm tra SMTP rồi thử lại.");
                    } else if (new OTPVerificationDAO().renewRegistrationOtp(userId, otp)) {
                        session.setAttribute("otpLastSent", now);
                        req.setAttribute("notice", "Đã gửi mã OTP mới. Mã cũ không còn hiệu lực.");
                    } else {
                        req.setAttribute("error", "Tài khoản không còn ở trạng thái chờ xác thực.");
                    }
                }
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            String code = req.getParameter("otp");
            if (!new OtpService().verifyRegistration(userId, code)) {
                req.setAttribute("error", "OTP không đúng hoặc đã hết hạn. Bạn có thể yêu cầu mã mới.");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            session.removeAttribute("registrationUserId");
            session.removeAttribute("registrationEmail");
            session.removeAttribute("otpLastSent");
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
        } catch (RuntimeException e) {
            log("OTP verification failed", e);
            req.setAttribute("error", "Không thể xác thực OTP do lỗi hệ thống.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        }
    }
}
