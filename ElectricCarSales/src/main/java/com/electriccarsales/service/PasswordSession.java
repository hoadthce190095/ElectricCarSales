package com.electriccarsales.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PasswordSession {
    public static final String ATTRIBUTE = "passwordFingerprint";

    private PasswordSession() {
    }

    // Chỉ lưu dấu vân tay trong session. Không lưu mật khẩu hoặc trả nó về trình duyệt.
    public static String fingerprint(String passwordHash) {
        if (passwordHash == null) {
            throw new IllegalArgumentException("Thiếu thông tin xác thực tài khoản.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không thể xác minh phiên đăng nhập.", exception);
        }
    }

    public static boolean matches(Object savedFingerprint, String passwordHash) {
        if (!(savedFingerprint instanceof String saved) || passwordHash == null) {
            return false;
        }
        return MessageDigest.isEqual(saved.getBytes(StandardCharsets.UTF_8),
            fingerprint(passwordHash).getBytes(StandardCharsets.UTF_8));
    }
}
