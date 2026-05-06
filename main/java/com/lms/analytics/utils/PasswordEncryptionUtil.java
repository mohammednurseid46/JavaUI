package com.lms.analytics.utils;

import org.mindrot.jbcrypt.BCrypt;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordEncryptionUtil {

    // BCrypt workload factor (10-12 is good for most applications)
    private static final int BCRYPT_WORKLOAD = 12;

    /**
     * Hashes a password using BCrypt
     * @param password Plain text password
     * @return Hashed password
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_WORKLOAD));
    }

    /**
     * Verifies a password against a hash
     * @param password Plain text password
     * @param hashedPassword Stored hash
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a secure random token for password reset
     * @return Secure token
     */
    public static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Validates an email address using a proper regex pattern.
     * Checks for: local part @ domain . TLD format
     * Examples accepted:  user@example.com, john.doe@uni.edu.et
     * Examples rejected:  user@, @domain.com, user@domain, plaintext
     *
     * @param email the email string to validate
     * @return true if the email is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        // RFC-5321 simplified regex: local@domain.tld
        String regex = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";
        return email.matches(regex);
    }
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}