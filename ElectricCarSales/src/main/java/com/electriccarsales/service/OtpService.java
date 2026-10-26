package com.electriccarsales.service;

import com.electriccarsales.dao.OTPVerificationDAO;
import com.electriccarsales.service.RegistrationEmailService;

import java.security.SecureRandom;
import java.util.Locale;

public class OtpService {
    private final OTPVerificationDAO otpDAO = new OTPVerificationDAO();
    private static final SecureRandom RANDOM = new SecureRandom();

    public boolean verifyRegistration(int userId, String code) {
        if (userId <= 0 || code == null || !code.matches("\\d{6}")) return false;
        return otpDAO.verifyRegistrationOtp(userId, code);
    }

    public boolean resendRegistrationOtp(int userId) {
        RegistrationEmailService emailService = new RegistrationEmailService();
        if (!emailService.isConfigured()) throw new IllegalStateException("SMTP chưa được cấu hình.");
        String email = otpDAO.findPendingRegistrationEmail(userId);
        if (email == null || email.isBlank()) return false;
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        if (!otpDAO.issueRegistrationOtp(userId, code)) return false;
        return emailService.sendRegistrationOtp(email, code);
    }
}
