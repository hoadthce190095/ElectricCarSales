package com.electriccarsales.controller;

import com.electriccarsales.dao.OTPVerificationDAO;
import com.electriccarsales.dao.UserDAO;
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
    private static final String RESET_AUTH_UNTIL = "resetPasswordAuthorizedUntil";
    private static final long RESET_AUTH_WINDOW_MILLIS = 10 * 60 * 1000L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("forgot".equalsIgnoreCase(req.getParameter("mode"))) {
            req.setAttribute("otpMode", "forgot");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        if ("resetPassword".equalsIgnoreCase(req.getParameter("mode"))) {
            HttpSession resetSession = req.getSession(false);
            if (isResetAuthorized(resetSession)) {
                req.getRequestDispatcher("/auth/reset-password.jsp").forward(req, resp);
            } else {
                if (resetSession != null) {
                    resetSession.removeAttribute(RESET_AUTH_UNTIL);
                    resetSession.removeAttribute("resetPasswordUserId");
                }
                req.setAttribute("otpMode", "forgot");
                req.setAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại mã OTP.");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            }
            return;
        }
        if ("resetOtp".equalsIgnoreCase(req.getParameter("mode"))) {
            HttpSession resetSession = req.getSession(false);
            if (resetSession != null && resetSession.getAttribute("resetPasswordUserId") instanceof Integer) {
                req.setAttribute("otpMode", "resetOtp");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
        }
        HttpSession session = req.getSession(false);
        Integer pendingUserId = session == null ? null : (Integer) session.getAttribute("pendingRegistrationUserId");
        if (pendingUserId != null && pendingUserId > 0) {
            req.setAttribute("userId", pendingUserId);
            req.setAttribute("email", session.getAttribute("pendingRegistrationEmail"));
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/register");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        if ("forgotRequest".equalsIgnoreCase(action)) {
            handleForgotRequest(req, resp);
            return;
        }
        if ("resetPassword".equalsIgnoreCase(action)) {
            handleResetPassword(req, resp);
            return;
        }
        if ("verifyResetOtp".equalsIgnoreCase(action)) {
            handleVerifyResetOtp(req, resp);
            return;
        }
        if ("resendResetOtp".equalsIgnoreCase(action)) {
            handleResendResetOtp(req, resp);
            return;
        }

        int userId = resolveUserId(req);
        if (userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        req.setAttribute("userId", userId);
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("pendingRegistrationEmail") != null) {
            req.setAttribute("email", session.getAttribute("pendingRegistrationEmail"));
        }

        if ("resend".equalsIgnoreCase(action)) {
            try {
                String email = new OtpService().resendRegistrationOtp(userId);
                req.setAttribute("email", email);
                req.setAttribute("notice", "Đã phát hành mã OTP mới. Vui lòng kiểm tra email");
            } catch (RuntimeException e) {
                req.setAttribute("error", e.getMessage());
            }
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }

        try {
            String code = req.getParameter("otp");
            if (code == null || !code.trim().matches("[0-9]{6}")) {
                req.setAttribute("error", "Vui lòng nhập mã OTP gồm 6 chữ số.");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            if (!new OtpService().verifyRegistration(userId, code.trim())) {
                req.setAttribute("error", "OTP không đúng hoặc đã hết hạn. Bạn có thể bấm Gửi lại mã xác thực.");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            if (session != null) {
                session.removeAttribute("pendingRegistrationUserId");
                session.removeAttribute("pendingRegistrationEmail");
            }
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
        } catch (RuntimeException e) {
            log("OTP verification failed", e);
            req.setAttribute("error", "Không thể xác thực OTP do lỗi dữ liệu.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        }
    }

    private void handleForgotRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String identifier = req.getParameter("identifier");
        req.setAttribute("otpMode", "forgot");
        if (identifier == null || identifier.isBlank()) {
            req.setAttribute("error", "Vui lòng nhập Email, Số điện thoại hoặc Tên đăng nhập.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        try {
            OTPVerificationDAO dao = new OTPVerificationDAO();
            Integer userId = dao.findUserIdByEmailOrPhone(identifier.trim());
            if (userId == null || userId <= 0) {
                req.setAttribute("error", "Không tìm thấy tài khoản khớp với thông tin đã nhập.");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            new OtpService().issuePurposeOtp(userId, "RESET_PASSWORD");
            req.getSession().setAttribute("resetPasswordUserId", userId);
            req.setAttribute("otpMode", "resetOtp");
            req.setAttribute("notice", "Đã tạo mã OTP đặt lại mật khẩu (hiệu lực 5 phút). Vui lòng kiểm tra email.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            log("Forgot password OTP request failed", e);
            req.setAttribute("error", "Không thể tạo yêu cầu đặt lại mật khẩu: " + e.getMessage());
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        }
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("resetPasswordUserId");
        if (userId == null || userId <= 0 || !isResetAuthorized(session)) {
            if (session != null) {
                session.removeAttribute(RESET_AUTH_UNTIL);
                session.removeAttribute("resetPasswordUserId");
            }
            req.setAttribute("otpMode", "forgot");
            req.setAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại mã OTP.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
            req.setAttribute("error", "Mật khẩu mới phải có từ 8 đến 72 ký tự.");
            req.getRequestDispatcher("/auth/reset-password.jsp").forward(req, resp);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            req.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            req.getRequestDispatcher("/auth/reset-password.jsp").forward(req, resp);
            return;
        }
        try {
            UserDAO userDAO = new UserDAO();
            if (userDAO.passwordMatches(userId, newPassword)) {
                req.setAttribute("error", "Mật khẩu mới không được trùng với mật khẩu hiện tại. Vui lòng nhập mật khẩu khác.");
                req.getRequestDispatcher("/auth/reset-password.jsp").forward(req, resp);
                return;
            }
            new OTPVerificationDAO().updatePasswordHash(userId, userDAO.hashPassword(newPassword));
            session.removeAttribute("resetPasswordUserId");
            session.removeAttribute(RESET_AUTH_UNTIL);
            resp.sendRedirect(req.getContextPath() + "/login?reset=1");
        } catch (RuntimeException e) {
            log("Reset password failed", e);
            req.setAttribute("error", "Không thể đặt lại mật khẩu.");
            req.getRequestDispatcher("/auth/reset-password.jsp").forward(req, resp);
        }
    }

    private void handleVerifyResetOtp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("resetPasswordUserId");
        String code = req.getParameter("otp");
        req.setAttribute("otpMode", "resetOtp");
        if (userId == null || userId <= 0) {
            req.setAttribute("otpMode", "forgot");
            req.setAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại mã OTP.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        if (code == null || !code.trim().matches("[0-9]{6}")) {
            req.setAttribute("error", "Vui lòng nhập mã OTP gồm 6 chữ số.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        try {
            if (!new OtpService().verifyPurposeOtp(userId, "RESET_PASSWORD", code.trim())) {
                req.setAttribute("error", "Mã OTP không đúng hoặc đã hết hạn.");
                req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
                return;
            }
            session.setAttribute(RESET_AUTH_UNTIL, System.currentTimeMillis() + RESET_AUTH_WINDOW_MILLIS);
            resp.sendRedirect(req.getContextPath() + "/verify-otp?mode=resetPassword");
        } catch (RuntimeException e) {
            log("Reset password OTP verification failed", e);
            req.setAttribute("error", "Không thể xác thực OTP.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
        }
    }

    private void handleResendResetOtp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("resetPasswordUserId");
        req.setAttribute("otpMode", "resetOtp");
        if (userId == null || userId <= 0) {
            req.setAttribute("otpMode", "forgot");
            req.setAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại mã OTP.");
            req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
            return;
        }
        try {
            new OtpService().issuePurposeOtp(userId, "RESET_PASSWORD");
            session.removeAttribute(RESET_AUTH_UNTIL);
            req.setAttribute("notice", "Đã gửi yêu cầu mã OTP mới. Mã cũ đã hết hiệu lực; vui lòng kiểm tra email hoặc Console của IntelliJ.");
        } catch (RuntimeException e) {
            log("Reset password OTP resend failed", e);
            req.setAttribute("error", "Không thể gửi lại mã OTP: " + e.getMessage());
        }
        req.getRequestDispatcher("/auth/otp.jsp").forward(req, resp);
    }

    private boolean isResetAuthorized(HttpSession session) {
        if (session == null) return false;
        Object deadline = session.getAttribute(RESET_AUTH_UNTIL);
        return deadline instanceof Long && (Long) deadline > System.currentTimeMillis();
    }

    private int resolveUserId(HttpServletRequest req) {
        String param = req.getParameter("userId");
        if (param != null && !param.isBlank()) {
            try {
                int parsed = Integer.parseInt(param.trim());
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {
            }
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object pending = session.getAttribute("pendingRegistrationUserId");
            if (pending instanceof Integer id && id > 0) {
                return id;
            }
        }
        return -1;
    }
}
