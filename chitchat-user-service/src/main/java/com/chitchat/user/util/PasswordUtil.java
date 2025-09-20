package com.chitchat.user.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for BCrypt password operations
 */
@Slf4j
@Component
public class PasswordUtil {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2[abxy]?\\$\\d+\\$[./0-9A-Za-z]{53}");

    /**
     * Verify if a plain text password matches the BCrypt hash
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            if (!isBCryptHash(hashedPassword)) {
                log.error("Invalid BCrypt hash format: {}", hashedPassword);
                return false;
            }

            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            log.info("Password verification for hash {}: {}",
                    hashedPassword.substring(0, 20) + "...", matches);
            return matches;

        } catch (Exception e) {
            log.error("Error verifying password: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Encode a plain text password using BCrypt
     */
    public static String encodePassword(String plainPassword) {
        try {
            String encoded = passwordEncoder.encode(plainPassword);
            log.info("Password encoded successfully");
            return encoded;
        } catch (Exception e) {
            log.error("Error encoding password: {}", e.getMessage());
            throw new RuntimeException("Failed to encode password", e);
        }
    }

    /**
     * Check if the string is a valid BCrypt hash
     */
    public static boolean isBCryptHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        return BCRYPT_PATTERN.matcher(hash).matches();
    }

    /**
     * Extract BCrypt hash information
     */
    public static BCryptInfo getBCryptInfo(String hash) {
        if (!isBCryptHash(hash)) {
            return null;
        }

        try {
            String[] parts = hash.split("\\$");
            if (parts.length < 4) {
                return null;
            }

            String version = parts[1];
            int rounds = Integer.parseInt(parts[2]);
            String salt = parts[3].substring(0, 22);
            String hashValue = parts[3].substring(22);

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
     * Test multiple common passwords against the hash
     */
    public static PasswordTestResult testCommonPasswords(String hashedPassword) {
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "1234567890", "abc123",
            "Password1", "password1", "admin123", "root", "user",
            "test", "guest", "demo", "sample", "default",
            "chitchat", "chitchat123", "ChitChat", "ChitChat123",
            "mobile", "mobile123", "app", "app123", "backend"
        };

        PasswordTestResult result = new PasswordTestResult();
        result.setHashedPassword(hashedPassword);
        result.setTotalTested(commonPasswords.length);

        for (String password : commonPasswords) {
            if (verifyPassword(password, hashedPassword)) {
                result.setMatchFound(true);
                result.setMatchedPassword(password);
                log.info("MATCH FOUND! Password '{}' matches the hash", password);
                break;
            }
        }

        if (!result.isMatchFound()) {
            log.info("No common password matches found for the given hash");
        }

        return result;
    }

    /**
     * BCrypt hash information container
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
     * Password test result container
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
     * Main method for standalone testing
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