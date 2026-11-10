package com.electriccarsales.service;

import com.electriccarsales.dao.OTPVerificationDAO;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
import java.util.Properties;

public class OtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final OTPVerificationDAO otpDAO = new OTPVerificationDAO();

    public boolean verifyRegistration(int userId, String code) {
        if (userId <= 0 || code == null || !code.matches("\\d{6}")) return false;
        return otpDAO.verifyRegistrationOtp(userId, code);
    }
    public static boolean isSmtpConfigured() {
        for (String key : new String[]{"ECARS_SMTP_HOST", "ECARS_SMTP_PORT",
            "ECARS_SMTP_SECURITY", "ECARS_SMTP_USERNAME",
            "ECARS_SMTP_PASSWORD", "ECARS_SMTP_FROM"}) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                System.err.println("THIEU BIEN SMTP: " + key);
                return false;
            }        }
        return true;
    }

    public String generateRegistrationCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public void sendRegistrationOtp(String email, String code) {
        if (!isSmtpConfigured())
            throw new IllegalStateException("Chưa cấu hình SMTP.");

        String host = System.getenv("ECARS_SMTP_HOST").trim();
        String username = System.getenv("ECARS_SMTP_USERNAME").trim();
        String password = System.getenv("ECARS_SMTP_PASSWORD").replaceAll("\\s+", "");
        String from = System.getenv("ECARS_SMTP_FROM").trim();

        if (!"STARTTLS".equalsIgnoreCase(System.getenv("ECARS_SMTP_SECURITY").trim()))
            throw new IllegalStateException("SMTP hiện chỉ hỗ trợ STARTTLS.");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", System.getenv("ECARS_SMTP_PORT").trim());
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("Ma xac thuc Electric Car Sales", "UTF-8");
            message.setText("Ma OTP dang ky cua ban: " + code
                + "\nMa co hieu luc trong 5 phut.", "UTF-8");

            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không gửi được OTP qua SMTP.", e);
        }
    }

    public String resendRegistrationOtp(int userId) {
        String email = otpDAO.findPendingRegistrationEmail(userId);

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Không tìm thấy tài khoản đang chờ xác thực.");

        String code = generateRegistrationCode();

        if (!otpDAO.issueRegistrationOtp(userId, code))
            throw new IllegalStateException("Không lưu được mã OTP mới.");

        try {
            sendRegistrationOtp(email, code);
        } catch (RuntimeException e) {
            otpDAO.expireFailedRegistrationOtp(userId, code);
            throw e;
        }

        return email;
    }
}
