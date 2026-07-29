package com.bank.trading.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {

    @Test
    @DisplayName("Hash generation should produce a valid Argon2id string with OWASP parameters")
    void testHashFormat() {
        String password = "Admin123";
        String hash = PasswordService.hash(password);

        assertNotNull(hash, "Hash should not be null");
        assertTrue(hash.startsWith("$argon2id$"), "Hash must start with $argon2id$");
        assertTrue(hash.contains("m=19456"), "Hash must specify memory cost 19456 (19 MiB)");
        assertTrue(hash.contains("t=2"), "Hash must specify time cost / iterations = 2");
        assertTrue(hash.contains("p=1"), "Hash must specify parallelism = 1");
    }

    @Test
    @DisplayName("Verification should succeed for correct password and fail for incorrect password")
    void testVerifySuccessAndFailure() {
        String password = "Admin123";
        String hash = PasswordService.hash(password);

        assertTrue(PasswordService.verify(hash, "Admin123"), "Verification must succeed for matching password");
        assertFalse(PasswordService.verify(hash, "WrongPassword"), "Verification must fail for incorrect password");
        assertFalse(PasswordService.verify(hash, "admin123"), "Verification must be case-sensitive");
    }

    @Test
    @DisplayName("Salts should be unique for each hash operation")
    void testUniqueSalts() {
        String password = "Admin123";
        String hash1 = PasswordService.hash(password);
        String hash2 = PasswordService.hash(password);

        assertNotEquals(hash1, hash2, "Hashes for the same password must produce different outputs due to random salt");
        assertTrue(PasswordService.verify(hash1, password));
        assertTrue(PasswordService.verify(hash2, password));
    }

    @Test
    @DisplayName("needsRehash should return false for current OWASP parameters and true for outdated parameters")
    void testNeedsRehash() {
        String password = "Admin123";
        String hashCurrent = PasswordService.hash(password);

        assertFalse(PasswordService.needsRehash(hashCurrent), "Current Argon2id hash should not need rehash");
        assertTrue(PasswordService.needsRehash("plaintext"), "Plaintext password requires rehash");
        assertTrue(PasswordService.needsRehash("$argon2id$v=19$m=65536,t=3,p=4$salt$hash"), "Different params require rehash");
        assertTrue(PasswordService.needsRehash(null), "Null hash requires rehash");
    }

    @Test
    @DisplayName("Null or empty password inputs should be handled safely")
    void testNullAndEmptyInputs() {
        assertFalse(PasswordService.verify(null, "Admin123"));
        assertFalse(PasswordService.verify("somehash", null));
        assertThrows(IllegalArgumentException.class, () -> PasswordService.hash(null));
    }
}
