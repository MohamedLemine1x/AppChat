package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtils - Utility class for password hashing and verification
 * Provides secure password hashing using SHA-256 with salt
 */
public class PasswordUtils {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * Hash a password using SHA-256 with salt
     * @param password Plain text password
     * @return Hashed password with salt
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        // Generate a random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        // Hash the password with salt
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(salt);
        byte[] hashedPassword = md.digest(password.getBytes());

        // Combine salt and hash
        byte[] combined = new byte[salt.length + hashedPassword.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

        // Return as Base64 encoded string
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Hash a password with a specific salt (for compatibility with existing hashes)
     * @param password Plain text password
     * @param salt Salt bytes
     * @return Hashed password
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public static String hashPasswordWithSalt(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(salt);
        byte[] hashedPassword = md.digest(password.getBytes());

        // Combine salt and hash
        byte[] combined = new byte[salt.length + hashedPassword.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Verify a password against a stored hash
     * @param password Plain text password to verify
     * @param storedHash Stored password hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Decode the stored hash
            byte[] decoded = Base64.getDecoder().decode(storedHash);

            // Extract salt (first 16 bytes)
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(decoded, 0, salt, 0, SALT_LENGTH);

            // Hash the provided password with the extracted salt
            String hashedPassword = hashPasswordWithSalt(password, salt);

            // Compare with stored hash
            return hashedPassword.equals(storedHash);

        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Simple hash for backward compatibility (not recommended for production)
     * @param password Plain text password
     * @return Simple hash of password
     */
    public static String simpleHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedPassword = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedPassword) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // Fallback to basic hashCode if SHA-256 is not available
            return String.valueOf(password.hashCode());
        }
    }

    /**
     * Generate a random password
     * @param length Length of the password
     * @return Random password string
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());
            password.append(chars.charAt(randomIndex));
        }

        return password.toString();
    }

    /**
     * Generate a random salt
     * @return Random salt bytes
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Check if a password meets basic strength requirements
     * @param password Password to check
     * @return true if password is strong enough
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }

        // Require at least 3 out of 4 character types
        int typeCount = 0;
        if (hasUpperCase) typeCount++;
        if (hasLowerCase) typeCount++;
        if (hasDigit) typeCount++;
        if (hasSpecialChar) typeCount++;

        return typeCount >= 3;
    }

    /**
     * Get password strength description
     * @param password Password to evaluate
     * @return Strength description
     */
    public static String getPasswordStrengthDescription(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < 6) {
            return "Too short (minimum 6 characters)";
        }

        if (password.length() < 8) {
            return "Weak - should be at least 8 characters";
        }

        if (!isPasswordStrong(password)) {
            return "Medium - add uppercase, lowercase, numbers, or special characters";
        }

        return "Strong";
    }
}