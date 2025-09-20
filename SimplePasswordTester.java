import java.util.Scanner;

/**
 * Simple BCrypt Password Tester
 *
 * This tool provides basic BCrypt hash analysis and common password testing.
 * For actual verification, use the Spring Boot application or a proper BCrypt library.
 *
 * Compile: javac SimplePasswordTester.java
 * Run: java SimplePasswordTester
 */
public class SimplePasswordTester {

    public static void main(String[] args) {
        String targetHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";

        System.out.println("==========================================");
        System.out.println("    Simple BCrypt Password Analyzer");
        System.out.println("==========================================\n");

        if (args.length > 0) {
            targetHash = args[0];
        }

        System.out.println("Target Hash: " + targetHash);
        System.out.println();

        // Analyze hash structure
        analyzeBCryptHash(targetHash);

        // Show common passwords to test
        showCommonPasswords();

        // Interactive mode
        interactiveMode();
    }

    /**
     * Analyze BCrypt hash structure
     */
    private static void analyzeBCryptHash(String hash) {
        System.out.println("Hash Analysis:");
        System.out.println("==============");

        if (hash == null || hash.isEmpty()) {
            System.out.println("❌ Empty hash");
            return;
        }

        System.out.println("Length: " + hash.length());

        if (!hash.startsWith("$2")) {
            System.out.println("❌ Not a BCrypt hash (should start with $2)");
            return;
        }

        try {
            String[] parts = hash.split("\\$");
            if (parts.length < 4) {
                System.out.println("❌ Invalid hash format");
                return;
            }

            String version = parts[1];
            String rounds = parts[2];
            String saltAndHash = parts[3];

            System.out.println("✓ Valid BCrypt format");
            System.out.println("  Algorithm: BCrypt");
            System.out.println("  Version: $" + version);
            System.out.println("  Rounds: " + rounds + " (2^" + rounds + " = " + (1 << Integer.parseInt(rounds)) + " iterations)");

            if (saltAndHash.length() >= 22) {
                String salt = saltAndHash.substring(0, 22);
                String hashPart = saltAndHash.substring(22);
                System.out.println("  Salt: " + salt);
                System.out.println("  Hash: " + hashPart.substring(0, Math.min(10, hashPart.length())) + "...");
            }

            // Security analysis
            int roundsInt = Integer.parseInt(rounds);
            System.out.println("\nSecurity Analysis:");
            if (roundsInt < 10) {
                System.out.println("⚠️  Low security: Rounds < 10 (easily crackable)");
            } else if (roundsInt < 12) {
                System.out.println("⚡ Medium security: Rounds 10-11 (moderate protection)");
            } else {
                System.out.println("🔒 High security: Rounds >= 12 (strong protection)");
            }

        } catch (Exception e) {
            System.out.println("❌ Error parsing hash: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Show common passwords that should be tested
     */
    private static void showCommonPasswords() {
        System.out.println("Common Passwords to Test:");
        System.out.println("=========================");

        String[] categories = {
            "Basic passwords:",
            "password, 123456, password123, admin, qwerty, letmein, welcome",
            "",
            "Application specific:",
            "chitchat, chitchat123, ChitChat, ChitChat123, mobile, app, backend",
            "",
            "Admin passwords:",
            "admin, admin123, root, user, test, guest, demo, default",
            "",
            "Variations:",
            "Password1, password1, Secret123, Login123, temp123, new123"
        };

        for (String line : categories) {
            System.out.println("  " + line);
        }

        System.out.println("\n💡 To actually test these passwords:");
        System.out.println("   Use the Spring Boot application endpoints:");
        System.out.println("   POST /api/users/admin/password/verify");
        System.out.println("   POST /api/users/admin/password/info");
        System.out.println();
    }

    /**
     * Interactive mode for manual testing guidance
     */
    private static void interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Interactive Mode ===");
        System.out.println("This tool cannot verify passwords directly.");
        System.out.println("Use the curl commands below to test passwords:\n");

        while (true) {
            System.out.print("Enter a password to generate curl command (or 'quit' to exit): ");
            String input = scanner.nextLine().trim();

            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            generateCurlCommand(input);
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    /**
     * Generate curl command for password testing
     */
    private static void generateCurlCommand(String password) {
        String targetHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";

        System.out.println("\nCurl command to test password '" + password + "':");
        System.out.println("================================================");
        System.out.println("curl -X POST http://localhost:9101/api/users/admin/password/verify \\");
        System.out.println("  -H \"Authorization: Bearer YOUR_JWT_TOKEN\" \\");
        System.out.println("  -H \"Content-Type: application/json\" \\");
        System.out.println("  -d '{");
        System.out.println("    \"hashedPassword\": \"" + targetHash + "\",");
        System.out.println("    \"plainPassword\": \"" + password + "\"");
        System.out.println("  }'");
        System.out.println();

        System.out.println("Alternative - Test common passwords:");
        System.out.println("curl -X POST http://localhost:9101/api/users/admin/password/verify \\");
        System.out.println("  -H \"Authorization: Bearer YOUR_JWT_TOKEN\" \\");
        System.out.println("  -H \"Content-Type: application/json\" \\");
        System.out.println("  -d '{");
        System.out.println("    \"hashedPassword\": \"" + targetHash + "\"");
        System.out.println("  }'");
        System.out.println();
    }
}