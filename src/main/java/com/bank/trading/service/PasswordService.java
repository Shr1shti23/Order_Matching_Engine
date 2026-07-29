package com.bank.trading.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * Dedicated service for Argon2id password hashing, verification, and parameter checks.
 *
 * <p>Security Parameters (OWASP aligned):</p>
 * <ul>
 *   <li>Algorithm: Argon2id</li>
 *   <li>Memory Cost: 19456 KB (19 MiB)</li>
 *   <li>Iterations (Time Cost): 2</li>
 *   <li>Parallelism: 1 thread</li>
 *   <li>Salt Length: 16 bytes</li>
 *   <li>Hash Output Length: 32 bytes</li>
 * </ul>
 */
public final class PasswordService {

    // Named OWASP-aligned security parameters
    public static final int MEMORY_COST_KB  = 19456; // 19 MiB
    public static final int ITERATIONS      = 2;
    public static final int PARALLELISM     = 1;
    public static final int SALT_LENGTH     = 16;   // 16 bytes
    public static final int HASH_LENGTH     = 32;   // 32 bytes

    private static final Argon2 ARGON2_INSTANCE = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            SALT_LENGTH,
            HASH_LENGTH
    );

    private PasswordService() {
        // Utility class
    }

    /**
     * Hashes a plaintext password using Argon2id with OWASP-aligned parameters.
     *
     * @param plaintextPassword the raw password string
     * @return encoded Argon2id hash string including parameters and salt
     */
    public static String hash(String plaintextPassword) {
        if (plaintextPassword == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        char[] chars = plaintextPassword.toCharArray();
        try {
            return ARGON2_INSTANCE.hash(ITERATIONS, MEMORY_COST_KB, PARALLELISM, chars);
        } finally {
            ARGON2_INSTANCE.wipeArray(chars);
        }
    }

    /**
     * Verifies a plaintext password against an encoded Argon2id hash string in constant-time.
     *
     * @param storedHash        the encoded Argon2id hash string from database
     * @param plaintextPassword the candidate plaintext password supplied by user
     * @return true if password matches stored Argon2id hash, false otherwise
     */
    public static boolean verify(String storedHash, String plaintextPassword) {
        if (storedHash == null || storedHash.trim().isEmpty() || plaintextPassword == null) {
            return false;
        }

        // If stored string is not an Argon2id hash (e.g. legacy plain text)
        if (!isArgon2Hash(storedHash)) {
            return storedHash.equals(plaintextPassword);
        }

        char[] chars = plaintextPassword.toCharArray();
        try {
            return ARGON2_INSTANCE.verify(storedHash, chars);
        } catch (Throwable t) {
            return false;
        } finally {
            try {
                ARGON2_INSTANCE.wipeArray(chars);
            } catch (Throwable ignore) {}
        }
    }

    /**
     * Checks if a stored password hash was created using outdated parameters or algorithm.
     * Used for transparent rehashing on successful login.
     *
     * @param storedHash the encoded hash string
     * @return true if rehash is needed, false otherwise
     */
    public static boolean needsRehash(String storedHash) {
        if (storedHash == null || !isArgon2Hash(storedHash)) {
            return true;
        }
        // Expected parameter signature snippet for current configuration: m=19456,t=2,p=1
        String expectedParams = String.format("m=%d,t=%d,p=%d", MEMORY_COST_KB, ITERATIONS, PARALLELISM);
        return !storedHash.contains(expectedParams);
    }

    /**
     * Returns true if the string is formatted as an Argon2id hash.
     *
     * @param hash candidate string
     * @return true if starts with $argon2id$
     */
    public static boolean isArgon2Hash(String hash) {
        return hash != null && hash.startsWith("$argon2id$");
    }
}
