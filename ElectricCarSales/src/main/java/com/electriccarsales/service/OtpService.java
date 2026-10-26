package com.electriccarsales.service;

import com.electriccarsales.dao.OTPVerificationDAO;

public class OtpService {
    private final OTPVerificationDAO otpDAO = new OTPVerificationDAO();

    public boolean verifyRegistration(int userId, String code) {
        if (userId <= 0 || code == null || !code.matches("\\d{6}")) return false;
        return otpDAO.verifyRegistrationOtp(userId, code);
    }
}
