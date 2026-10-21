package com.electriccarsales.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.Test;

class PasswordFormatCompatibilityTest {
    @Test
    void acceptsMainPasswordEncodingWithoutPaddingAndRejectsWrongPasswords() throws Exception {
        String password = "main-password-123";
        byte[] salt = new byte[16];
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
        byte[] derived;
        try {
            derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
        String encoded = "PBKDF2$120000$" + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
            + Base64.getEncoder().withoutPadding().encodeToString(derived);
        assertTrue(PasswordHasher.verify(password, encoded));
        assertFalse(PasswordHasher.verify("wrong-password-123", encoded));
    }

    @Test
    void mainEncodingKeepsExistingIterationAndLengthGuards() {
        String salt = Base64.getEncoder().encodeToString(new byte[16]);
        String digest = Base64.getEncoder().encodeToString(new byte[32]);
        for (String iterations : List.of("0", "-1", "99999", "1000001", "9999999999999999999", "invalid")) {
            assertFalse(PasswordHasher.verify("validPassword", "PBKDF2$" + iterations + "$" + salt + "$" + digest));
        }
        assertFalse(PasswordHasher.verify("validPassword", "PBKDF2$120000$"
            + Base64.getEncoder().encodeToString(new byte[8]) + "$" + digest));
        assertFalse(PasswordHasher.verify("validPassword", "PBKDF2$120000$" + salt + "$"
            + Base64.getEncoder().encodeToString(new byte[16])));
        assertFalse(PasswordHasher.verify("validPassword", "PBKDF2$120000$not-base64!$" + digest));
    }
}
