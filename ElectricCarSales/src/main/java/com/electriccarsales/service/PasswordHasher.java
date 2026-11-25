package com.electriccarsales.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 210000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 30
                || !password.matches(".*[A-Z].*") || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9\\s].*")) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 30 ký tự, gồm chữ hoa, chữ số và ký tự đặc biệt.");
        }
    }

    public static String hash(String password) {
        validate(password);
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
            + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || password.length() > 128 || encoded == null) {
            return false;
        }
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4 || !(parts[0].equals("pbkdf2-sha256") || parts[0].equals("PBKDF2"))) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100000 || iterations > 1000000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != 16 || expected.length != 32) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
