package com.electriccarsales.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/** Sends registration verification codes through an externally configured SMTP server. */
public class RegistrationEmailService {
    private final SmtpConfig config;

    public RegistrationEmailService() {
        this.config = SmtpConfig.fromEnvironment();
    }

    public boolean isConfigured() {
        return config != null;
    }

    public boolean sendRegistrationOtp(String recipient, String code) {
        if (config == null || recipient == null || code == null || !code.matches("\\d{6}")) return false;
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.host);
        properties.put("mail.smtp.port", Integer.toString(config.port));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        properties.put("mail.smtp.ssl.checkserveridentity", "true");
        // Use modern TLS on STARTTLS connections, matching the verified SMTP flow in the reference.
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        if (config.security.equals("SSL")) {
            properties.put("mail.smtp.ssl.enable", "true");
        } else {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username, config.password);
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.from, true));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, true));
            message.setSubject("Mã xác thực đăng ký ElectricCarSales", StandardCharsets.UTF_8.name());
            String html = "<div style=\"font-family:Arial,sans-serif;max-width:520px;padding:24px;"
                    + "border:1px solid #e5e7eb;border-radius:12px;color:#202124\">"
                    + "<h2 style=\"margin:0 0 18px;color:#171717\">ElectricCarSales</h2>"
                    + "<p>Bạn đã yêu cầu đăng ký tài khoản. Hãy nhập mã xác minh bên dưới:</p>"
                    + "<div style=\"margin:22px 0;padding:16px;text-align:center;background:#f4f5f6;"
                    + "font-size:30px;font-weight:bold;letter-spacing:8px;border:1px dashed #9ca3af\">"
                    + code + "</div>"
                    + "<p>Mã có hiệu lực trong <strong>5 phút</strong> và chỉ sử dụng một lần.</p>"
                    + "<p>Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.</p></div>";
            message.setContent(html, "text/html; charset=" + StandardCharsets.UTF_8.name());
            Transport.send(message);
            return true;
        } catch (MessagingException | RuntimeException e) {
            // Deliberately do not log the OTP, recipient, SMTP settings, or provider exception.
            return false;
        }
    }

    private static final class SmtpConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String from;
        private final String security;

        private SmtpConfig(String host, int port, String username, String password, String from, String security) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.from = from;
            this.security = security;
        }

        private static SmtpConfig fromEnvironment() {
            String host = trimmed("ECARS_SMTP_HOST");
            String portText = trimmed("ECARS_SMTP_PORT");
            String username = trimmed("ECARS_SMTP_USERNAME");
            String password = System.getenv("ECARS_SMTP_PASSWORD");
            String from = trimmed("ECARS_SMTP_FROM");
            String security = trimmed("ECARS_SMTP_SECURITY");
            if (host == null || portText == null || username == null || password == null || password.isBlank()
                    || from == null || security == null) return null;
            try {
                int port = Integer.parseInt(portText);
                String mode = security.toUpperCase(Locale.ROOT);
                if (port < 1 || port > 65535 || !(mode.equals("STARTTLS") || mode.equals("SSL"))) return null;
                InternetAddress sender = new InternetAddress(from, true);
                sender.validate();
                return new SmtpConfig(host, port, username, password, from, mode);
            } catch (Exception e) {
                return null;
            }
        }

        private static String trimmed(String name) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
