package models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class User {
    // Basic user information
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String passwordHash;


    // Status and activity
    private boolean isOnline;
    private long lastSeen;
    private UserStatus status;
    private String customStatus;

    // Profile information
    private String profileImageUrl;
    private long createdAt;
    private Map<String, Object> preferences;

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    // Constructors
    public User() {
        this.preferences = new HashMap<>();
        this.status = UserStatus.OFFLINE;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String userId, String username, String email) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public User(String userId, String username, String email, String firstName, String lastName) {
        this(userId, username, email);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Display methods
    public String getDisplayName() {
        if (firstName != null && lastName != null && !firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        } else if (username != null && !username.isEmpty()) {
            return username;
        } else {
            return "Unknown User";
        }
    }

    public String getInitials() {
        if (firstName != null && lastName != null && !firstName.isEmpty() && !lastName.isEmpty()) {
            return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
        } else if (username != null && !username.isEmpty()) {
            return username.substring(0, Math.min(2, username.length())).toUpperCase();
        } else {
            return "??";
        }
    }

    public String getLastSeenFormatted() {
        if (isOnline) {
            return "Online";
        } else if (lastSeen > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return "Last seen: " + sdf.format(new Date(lastSeen));
        } else {
            return "Never seen";
        }
    }


    public void setOnlineStatus(boolean online) {
        this.isOnline = online;
        if (!online) {
            this.lastSeen = System.currentTimeMillis();
            this.status = UserStatus.OFFLINE;
        } else {
            this.status = UserStatus.ONLINE;
        }
    }

    public void setStatus(UserStatus status) {
        this.status = status;
        if (status == UserStatus.OFFLINE) {
            this.isOnline = false;
            this.lastSeen = System.currentTimeMillis();
        } else {
            this.isOnline = true;
        }
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // Profile methods
    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void updateProfile(String firstName, String lastName, String customStatus) {
        updateProfile(firstName, lastName);
        this.customStatus = customStatus;
    }

    public void setProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }

    // Validation methods
    // Removed validateEmail, validateUsername, isValidEmail, isValidUsername to centralize validation in ValidationUtils

    // Preferences methods
    public void setPreference(String key, Object value) {
        preferences.put(key, value);
    }

    public Object getPreference(String key) {
        return preferences.get(key);
    }

    public Object getPreference(String key, Object defaultValue) {
        return preferences.getOrDefault(key, defaultValue);
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public UserStatus getStatus() { return status; }

    public String getCustomStatus() { return customStatus; }
    public void setCustomStatus(String customStatus) { this.customStatus = customStatus; }

    public String getProfileImageUrl() { return profileImageUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", isOnline=" + isOnline +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return userId != null ? userId.equals(user.userId) : user.userId == null;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }


    private boolean online;

    /**
     * Set whether the user is online
     * @param online Whether the user is online
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Check if the user is online
     * @return true if the user is online
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Check if the user is an admin
     * @return true if the user is an admin
     */
    private boolean admin;

    /**
     * Set whether the user is an admin
     * @param admin Whether the user is an admin
     */
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    /**
     * Check if the user is a creator
     * @return true if the user is a creator
     */
    private boolean creator;

    /**
     * Set whether the user is a creator
     * @param creator Whether the user is a creator
     */
    public void setCreator(boolean creator) {
        this.creator = creator;
    }

    /**
     * Check if the user is a creator
     * @return true if the user is a creator
     */
    public boolean isCreator() {
        return creator;
    }
}