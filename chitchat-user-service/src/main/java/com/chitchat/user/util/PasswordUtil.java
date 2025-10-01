package com.chitchat.user.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for BCrypt password operations and testing
 * 
 * Provides tools for:
 * - Password encoding with BCrypt
 * - Password verification against hashes
 * - Hash format validation
 * - Hash information extraction
 * - Common password testing (security audit)
 * 
 * Use Cases:
 * - Admin panel password management
 * - Security auditing (finding weak passwords)
 * - Password migration and testing
 * - Troubleshooting authentication issues
 * 
 * BCrypt Overview:
 * - Industry-standard password hashing algorithm
 * - Includes random salt (prevents rainbow table attacks)
 * - Configurable work factor (rounds) for computational cost
 * - Secure against brute force attacks
 * 
 * Hash Format: $2a$10$N.zmdr9k7uOCQb376NoUnu.TJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi
 * Structure: $[version]$[rounds]$[salt + hash]
 * 
 * Note: Although ChitChat uses SMS/OTP, this utility is useful for:
 * - Admin accounts (username/password)
 * - Testing and development
 * - Legacy system migration
 */
@Slf4j
@Component
public class PasswordUtil {

    /**
     * BCrypt password encoder instance
     * Thread-safe and reusable across all operations
     */
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Regex pattern for valid BCrypt hash format
     * 
     * Pattern Breakdown:
     * - \A - Start of string
     * - \$2[abxy]? - Version identifier ($2a, $2b, $2x, $2y)
     * - \$ - Delimiter
     * - \d+ - Rounds (cost factor, typically 4-31)
     * - \$ - Delimiter
     * - [./0-9A-Za-z]{53} - Base64-encoded salt (22 chars) + hash (31 chars)
     * 
     * Example: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi
     */
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2[abxy]?\\$\\d+\\$[./0-9A-Za-z]{53}");

    /**
     * Verifies if a plain text password matches a BCrypt hash
     * 
     * This is the core password verification function.
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * Process:
     * 1. Validate hash format
     * 2. Extract salt from hash
     * 3. Hash the plain password with extracted salt
     * 4. Compare resulting hash with stored hash
     * 5. Return match result
     * 
     * Security:
     * - Constant-time comparison (prevents timing attacks)
     * - Handles all BCrypt versions (2a, 2b, 2x, 2y)
     * - Safe against invalid input
     * 
     * @param plainPassword The password to verify
     * @param hashedPassword The BCrypt hash to verify against
     * @return true if password matches hash, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            // First validate the hash format
            if (!isBCryptHash(hashedPassword)) {
                log.error("Invalid BCrypt hash format: {}", hashedPassword);
                return false;
            }

            // Use BCrypt's secure comparison function
            // Automatically handles salt extraction and hashing
            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            log.info("Password verification for hash {}: {}",
                    hashedPassword.substring(0, 20) + "...", matches);
            return matches;

        } catch (Exception e) {
            // Never throw exception on password verification
            // Always return false for security
            log.error("Error verifying password: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Encodes a plain text password using BCrypt
     * 
     * Generates a BCrypt hash with:
     * - Random salt (different for each password)
     * - Default cost factor (10 rounds = 2^10 iterations)
     * - Secure hashing algorithm
     * 
     * Same password encoded twice produces different hashes (due to salt).
     * 
     * @param plainPassword The password to encode
     * @return BCrypt hash string
     * @throws RuntimeException if encoding fails
     */
    public static String encodePassword(String plainPassword) {
        try {
            // Generate BCrypt hash with random salt
            String encoded = passwordEncoder.encode(plainPassword);
            log.info("Password encoded successfully");
            return encoded;
        } catch (Exception e) {
            log.error("Error encoding password: {}", e.getMessage());
            throw new RuntimeException("Failed to encode password", e);
        }
    }

    /**
     * Validates if a string is a properly formatted BCrypt hash
     * 
     * Checks format using regex pattern.
     * Does NOT verify if password is correct, only if format is valid.
     * 
     * Valid format: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi
     * Invalid formats: "password", "md5hash", "$1$old$format"
     * 
     * @param hash String to validate
     * @return true if valid BCrypt hash format, false otherwise
     */
    public static boolean isBCryptHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        // Match against BCrypt pattern
        return BCRYPT_PATTERN.matcher(hash).matches();
    }

    /**
     * Extracts detailed information from a BCrypt hash
     * 
     * Parses the BCrypt hash string and returns structured information:
     * - Version (2a, 2b, 2x, 2y)
     * - Rounds (cost factor, typically 10)
     * - Salt (22 characters)
     * - Hash value (31 characters)
     * 
     * BCrypt Hash Structure:
     * $2a$10$N.zmdr9k7uOCQb376NoUnu.TJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi
     * |   |  |                                                        |
     * ver rnd salt (22 chars) + hash (31 chars)
     * 
     * Used for:
     * - Security auditing (checking cost factor)
     * - Debugging hashing issues
     * - Understanding hash properties
     * 
     * @param hash BCrypt hash string to parse
     * @return BCryptInfo object with parsed components, or null if invalid
     */
    public static BCryptInfo getBCryptInfo(String hash) {
        // Validate hash format first
        if (!isBCryptHash(hash)) {
            return null;
        }

        try {
            // Split hash by $ delimiter
            // Example: $2a$10$salt+hash -> ["", "2a", "10", "salt+hash"]
            String[] parts = hash.split("\\$");
            if (parts.length < 4) {
                return null;
            }

            // Extract components
            String version = parts[1];                    // e.g., "2a"
            int rounds = Integer.parseInt(parts[2]);      // e.g., 10 (2^10 = 1024 iterations)
            String salt = parts[3].substring(0, 22);      // First 22 chars are salt
            String hashValue = parts[3].substring(22);    // Remaining 31 chars are hash

            return BCryptInfo.builder()
                    .version(version)
                    .rounds(rounds)
                    .salt(salt)
                    .hash(hashValue)
                    .fullHash(hash)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing BCrypt hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Tests a BCrypt hash against common weak passwords
     * 
     * Security Audit Tool:
     * Tests if a BCrypt hash was created from a common/weak password.
     * Useful for:
     * - Security audits (identifying weak admin passwords)
     * - Password policy enforcement
     * - Migration from other systems
     * - Testing and debugging
     * 
     * Common Passwords Tested (25 total):
     * - Generic: password, 123456, admin, qwerty, etc.
     * - Application-specific: chitchat, chitchat123, etc.
     * - Development: test, demo, sample, default
     * 
     * WARNING: This is computationally expensive
     * - Each BCrypt comparison takes ~100ms
     * - Testing 25 passwords takes ~2-3 seconds
     * - Use sparingly, not in hot paths
     * 
     * @param hashedPassword BCrypt hash to test
     * @return PasswordTestResult with match details
     */
    public static PasswordTestResult testCommonPasswords(String hashedPassword) {
        // List of common/weak passwords to test
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "1234567890", "abc123",
            "Password1", "password1", "admin123", "root", "user",
            "test", "guest", "demo", "sample", "default",
            "chitchat", "chitchat123", "ChitChat", "ChitChat123",
            "mobile", "mobile123", "app", "app123", "backend"
        };

        // Initialize result object
        PasswordTestResult result = new PasswordTestResult();
        result.setHashedPassword(hashedPassword);
        result.setTotalTested(commonPasswords.length);

        // Test each common password
        // Stop on first match (security issue found)
        for (String password : commonPasswords) {
            if (verifyPassword(password, hashedPassword)) {
                result.setMatchFound(true);
                result.setMatchedPassword(password);
                log.info("MATCH FOUND! Password '{}' matches the hash", password);
                break;  // Stop testing once we find a match
            }
        }

        if (!result.isMatchFound()) {
            log.info("No common password matches found for the given hash");
        }

        return result;
    }

    /**
     * Data class containing BCrypt hash components
     * 
     * Provides structured access to BCrypt hash information:
     * - version: BCrypt version (2a, 2b, 2x, 2y)
     * - rounds: Cost factor (4-31, typically 10)
     * - salt: Random salt value (22 base64 chars)
     * - hash: Actual password hash (31 base64 chars)
     * - fullHash: Complete BCrypt string
     * 
     * Example Output:
     * Version: 2a
     * Rounds: 10
     * Salt: N.zmdr9k7uOCQb376NoUnu
     * Hash: TJ8iAt6Z5E...
     */
    @lombok.Builder
    @lombok.Data
    public static class BCryptInfo {
        private String version;
        private int rounds;
        private String salt;
        private String hash;
        private String fullHash;

        @Override
        public String toString() {
            return String.format(
                "BCrypt Info:\n" +
                "  Version: %s\n" +
                "  Rounds: %d\n" +
                "  Salt: %s\n" +
                "  Hash: %s...\n" +
                "  Full: %s",
                version, rounds, salt, hash.substring(0, 10), fullHash
            );
        }
    }

    /**
     * Data class containing password test results
     * 
     * Used by testCommonPasswords() to return results.
     * Contains:
     * - hashedPassword: The hash that was tested
     * - matchFound: Whether a match was found
     * - matchedPassword: The password that matched (if any)
     * - totalTested: Number of passwords tested
     * 
     * Example Output (match found):
     * Hash: $2a$10$...
     * Match Found: YES
     * Matched Password: 'password123'
     * Total Tested: 25
     */
    @lombok.Data
    public static class PasswordTestResult {
        private String hashedPassword;
        private boolean matchFound = false;
        private String matchedPassword;
        private int totalTested;

        @Override
        public String toString() {
            if (matchFound) {
                return String.format(
                    "Password Test Result:\n" +
                    "  Hash: %s\n" +
                    "  Match Found: YES\n" +
                    "  Matched Password: '%s'\n" +
                    "  Total Tested: %d",
                    hashedPassword, matchedPassword, totalTested
                );
            } else {
                return String.format(
                    "Password Test Result:\n" +
                    "  Hash: %s\n" +
                    "  Match Found: NO\n" +
                    "  Total Tested: %d",
                    hashedPassword, totalTested
                );
            }
        }
    }

    /**
     * Main method for standalone testing/debugging
     * 
     * Can be run from command line to test BCrypt hashes:
     * 
     * Usage:
     * java PasswordUtil                           # Test with default hash
     * java PasswordUtil password1 password2       # Test custom passwords
     * 
     * Features:
     * 1. Validates hash format
     * 2. Extracts and displays hash information
     * 3. Tests against common passwords
     * 4. Tests custom passwords if provided as arguments
     * 
     * Example Output:
     * === BCrypt Password Utility ===
     * 1. Hash Format Validation: Valid
     * 2. Hash Information: Version 2a, Rounds 10, ...
     * 3. Testing Common Passwords: Match found - 'password123'
     * 4. Custom Password Testing: ...
     */
    public static void main(String[] args) {
        String testHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";

        System.out.println("=== BCrypt Password Utility ===\n");

        // Validate hash format
        System.out.println("1. Hash Format Validation:");
        boolean isValid = isBCryptHash(testHash);
        System.out.println("   Is valid BCrypt hash: " + isValid);
        System.out.println();

        if (isValid) {
            // Extract hash information
            System.out.println("2. Hash Information:");
            BCryptInfo info = getBCryptInfo(testHash);
            if (info != null) {
                System.out.println("   " + info.toString().replace("\n", "\n   "));
            }
            System.out.println();

            // Test common passwords
            System.out.println("3. Testing Common Passwords:");
            PasswordTestResult result = testCommonPasswords(testHash);
            System.out.println("   " + result.toString().replace("\n", "\n   "));
            System.out.println();

            // Interactive testing (if args provided)
            if (args.length > 0) {
                System.out.println("4. Custom Password Testing:");
                for (String password : args) {
                    boolean matches = verifyPassword(password, testHash);
                    System.out.printf("   Password '%s': %s%n", password, matches ? "MATCH!" : "No match");
                }
            } else {
                System.out.println("4. Custom Password Testing:");
                System.out.println("   Run with arguments to test custom passwords:");
                System.out.println("   java PasswordUtil password1 password2 ...");
            }
        }
    }
}