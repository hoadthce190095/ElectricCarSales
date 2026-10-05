package vn.edu.fpt.electriccarsales.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

class PasswordHasherTest {
    @Test
    void saltedHashesRoundTripAndRejectWrongPasswords() {
        String password = "MậtKhẩu-1234";
        String first = PasswordHasher.hash(password);
        String second = PasswordHasher.hash(password);
        assertNotEquals(first, second);
        assertTrue(first.startsWith("pbkdf2-sha256$"));
        assertFalse(first.contains(password));
        String[] fields = first.split("\\$");
        assertEquals(16, Base64.getDecoder().decode(fields[2]).length);
        assertEquals(32, Base64.getDecoder().decode(fields[3]).length);
        assertTrue(PasswordHasher.verify(password, first));
        assertFalse(PasswordHasher.verify("AnotherPassword", first));
        assertFalse(PasswordHasher.verify(null, first));
        assertFalse(PasswordHasher.verify("x".repeat(129), first));
    }

    @Test
    void creationValidatesLengthIncludingExactBounds() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash("x".repeat(9)));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash("x".repeat(129)));
        assertDoesNotThrow(() -> PasswordHasher.validate("x".repeat(10)));
        assertDoesNotThrow(() -> PasswordHasher.validate("x".repeat(128)));
    }

    @Test
    void invalidStoredHashFailsClosedWithoutUnboundedWorkOrParsingErrors() {
        String salt = Base64.getEncoder().encodeToString(new byte[16]);
        String digest = Base64.getEncoder().encodeToString(new byte[32]);
        for (String iterations : List.of("0", "-1", "99999", "1000001", "9999999999999999999", "invalid")) {
            assertFalse(
                PasswordHasher.verify("validPassword", "pbkdf2-sha256$" + iterations + "$" + salt + "$" + digest));
        }
        for (String malformed : List.of("plainPassword", "sha256$210000$" + salt + "$" + digest,
            "pbkdf2-sha256$210000$not-base64!$" + digest,
            "pbkdf2-sha256$210000$" + Base64.getEncoder().encodeToString(new byte[8]) + "$" + digest,
            "pbkdf2-sha256$210000$" + salt + "$" + Base64.getEncoder().encodeToString(new byte[16]),
            "pbkdf2-sha256$210000$" + salt + "$" + digest + "$extra")) {
            assertFalse(PasswordHasher.verify("validPassword", malformed));
        }
        assertFalse(PasswordHasher.verify("validPassword", null));
    }
}
