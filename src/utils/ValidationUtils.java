package utils;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * ValidationUtils - Utility class for input validation
 * Provides validation methods for common input types
 */
public class ValidationUtils {

    // Regex patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?[1-9]\\d{1,14}|0[1-9]\\d{8,9})$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]{2,50}$");

    /**
     * Validate email address
     * @param email Email to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate username
     * @param username Username to validate
     * @return true if valid username format
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Validate phone number
     * @param phone Phone number to validate
     * @return true if valid phone format
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleanPhone = phone.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Validate name (first name, last name)
     * @param name Name to validate
     * @return true if valid name format
     */
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Validate password strength
     * @param password Password to validate
     * @return true if password meets minimum requirements
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        // Check for at least one letter and one number
        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        return hasLetter && hasDigit;
    }

    /**
     * Check if string is null or empty
     * @param str String to check
     * @return true if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string has minimum length
     * @param str String to check
     * @param minLength Minimum required length
     * @return true if string meets minimum length
     */
    public static boolean hasMinLength(String str, int minLength) {
        return str != null && str.trim().length() >= minLength;
    }

    /**
     * Check if string has maximum length
     * @param str String to check
     * @param maxLength Maximum allowed length
     * @return true if string is within maximum length
     */
    public static boolean hasMaxLength(String str, int maxLength) {
        return str != null && str.trim().length() <= maxLength;
    }

    /**
     * Validate string length range
     * @param str String to check
     * @param minLength Minimum length
     * @param maxLength Maximum length
     * @return true if string length is within range
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        return hasMinLength(str, minLength) && hasMaxLength(str, maxLength);
    }

    /**
     * Clean and sanitize input string
     * @param input Input string to clean
     * @return Cleaned string
     */
    public static String cleanInput(String input) {
        if (input == null) {
            return "";
        }

        // Remove leading/trailing whitespace and normalize
        String cleaned = input.trim();

        // Remove any potentially dangerous characters for basic XSS prevention
        cleaned = cleaned.replaceAll("[<>\"'&]", "");

        return cleaned;
    }

    /**
     * Validate date format (dd/MM/yyyy)
     * @param date Date string to validate
     * @return true if valid date format
     */
    public static boolean isValidDate(String date) {
        if (date == null || date.length() != 10) {
            return false;
        }

        Pattern datePattern = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
        if (!datePattern.matcher(date).matches()) {
            return false;
        }

        String[] parts = date.split("/");
        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Basic date validation
            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;
            if (year < 1900 || year > 2100) return false;

            // More precise validation for days in month
            int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

            // Check for leap year
            if (isLeapYear(year)) {
                daysInMonth[1] = 29;
            }

            return day <= daysInMonth[month - 1];

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if year is a leap year
     * @param year Year to check
     * @return true if leap year
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Validate age (must be at least minimum age)
     * @param birthDate Birth date in dd/MM/yyyy format
     * @param minimumAge Minimum required age
     * @return true if person is at least minimum age
     */
    public static boolean isValidAge(String birthDate, int minimumAge) {
        if (!isValidDate(birthDate)) {
            return false;
        }

        try {
            String[] parts = birthDate.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            java.util.Calendar birthCal = java.util.Calendar.getInstance();
            birthCal.set(year, month - 1, day);

            java.util.Calendar now = java.util.Calendar.getInstance();

            int age = now.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR);

            // Adjust if birthday hasn't occurred this year
            if (now.get(java.util.Calendar.DAY_OF_YEAR) < birthCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age >= minimumAge;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get validation error message for email
     * @param email Email to validate
     * @return Error message or null if valid
     */
    public static String getEmailError(String email) {
        if (isEmpty(email)) {
            return "Email is required";
        }
        if (!isValidEmail(email)) {
            return "Invalid email format";
        }
        return null;
    }

    /**
     * Get validation error message for username
     * @param username Username to validate
     * @return Error message or null if valid
     */
    public static String getUsernameError(String username) {
        if (isEmpty(username)) {
            return "Username is required";
        }
        if (!isValidLength(username, 3, 20)) {
            return "Username must be 3-20 characters long";
        }
        if (!isValidUsername(username)) {
            return "Username can only contain letters, numbers, and underscores";
        }
        return null;
    }

    /**
     * Get validation error message for password
     * @param password Password to validate
     * @return Error message or null if valid
     */
    public static String getPasswordError(String password) {
        if (isEmpty(password)) {
            return "Password is required";
        }
        if (!hasMinLength(password, 6)) {
            return "Password must be at least 6 characters long";
        }
        if (!isValidPassword(password)) {
            return "Password must contain at least one letter and one number";
        }
        return null;
    }

    /**
     * Get validation error message for name
     * @param name Name to validate
     * @param fieldName Name of the field (e.g., "First name", "Last name")
     * @return Error message or null if valid
     */
    public static String getNameError(String name, String fieldName) {
        if (isEmpty(name)) {
            return fieldName + " is required";
        }
        if (!isValidLength(name, 2, 50)) {
            return fieldName + " must be 2-50 characters long";
        }
        if (!isValidName(name)) {
            return fieldName + " contains invalid characters";
        }
        return null;
    }

    /**
     * Validate group name
     * @param groupName Group name to validate
     * @return true if valid
     */
    public static boolean isValidGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return false;
        }

        String trimmed = groupName.trim();

        // Check length (3-50 characters)
        if (trimmed.length() < 3 || trimmed.length() > 50) {
            return false;
        }

        // Check for inappropriate characters (basic check)
        if (trimmed.contains("@") || trimmed.contains("#") || trimmed.contains("$")) {
            return false;
        }

        return true;
    }

    /**
     * Validate group description
     * @param description Group description to validate
     * @return true if valid
     */
    public static boolean isValidGroupDescription(String description) {
        if (description == null) {
            return true; // Description is optional
        }

        String trimmed = description.trim();

        // Empty description is valid
        if (trimmed.isEmpty()) {
            return true;
        }

        // Check length (max 200 characters)
        return trimmed.length() <= 200;
    }

    /**
     * Validate invite code format
     * @param inviteCode Invite code to validate
     * @return true if valid format
     */
    public static boolean isValidInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return false;
        }

        String trimmed = inviteCode.trim();

        // Check length (6-12 characters)
        if (trimmed.length() < 6 || trimmed.length() > 12) {
            return false;
        }

        // Check format (alphanumeric only)
        return trimmed.matches("^[A-Za-z0-9]+$");
    }

    /**
     * Validate member count for group
     * @param memberCount Number of members
     * @param maxMembers Maximum allowed members
     * @return true if valid
     */
    public static boolean isValidMemberCount(int memberCount, int maxMembers) {
        return memberCount >= 2 && memberCount <= maxMembers;
    }

    /**
     * Validate group member list
     * @param members List of member IDs
     * @return ValidationResult with details
     */
    public static ValidationResult validateGroupMembers(List<String> members) {
        if (members == null || members.isEmpty()) {
            return new ValidationResult(false, "Member list cannot be empty");
        }

        if (members.size() < 2) {
            return new ValidationResult(false, "Group must have at least 2 members");
        }

        if (members.size() > 100) {
            return new ValidationResult(false, "Group cannot have more than 100 members");
        }

        // Check for duplicates
        Set<String> uniqueMembers = new HashSet<>(members);
        if (uniqueMembers.size() != members.size()) {
            return new ValidationResult(false, "Duplicate members found in list");
        }

        // Validate each member ID
        for (String memberId : members) {
            if (memberId == null || memberId.trim().isEmpty()) {
                return new ValidationResult(false, "Invalid member ID found");
            }
        }

        return new ValidationResult(true, "Valid member list");
    }

    /**
     * Get group name validation error message
     * @param groupName Group name to check
     * @return Error message or null if valid
     */
    public static String getGroupNameValidationError(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return "Group name is required";
        }

        String trimmed = groupName.trim();

        if (trimmed.length() < 3) {
            return "Group name must be at least 3 characters long";
        }

        if (trimmed.length() > 50) {
            return "Group name cannot exceed 50 characters";
        }

        if (trimmed.contains("@") || trimmed.contains("#") || trimmed.contains("$")) {
            return "Group name contains invalid characters";
        }

        return null; // Valid
    }

    /**
     * Get group description validation error message
     * @param description Group description to check
     * @return Error message or null if valid
     */
    public static String getGroupDescriptionValidationError(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null; // Description is optional
        }

        if (description.trim().length() > 200) {
            return "Description cannot exceed 200 characters";
        }

        return null; // Valid
    }

    // ValidationResult helper class
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}