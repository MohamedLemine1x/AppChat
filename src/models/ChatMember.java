package models;

import java.util.Objects;

/**
 * ChatMember model class
 * Represents a member of a chat with additional status information
 */
public class ChatMember {
    private String userId;
    private String username;
    private String email;
    private boolean isAdmin;
    private boolean isCreator;
    private boolean isOnline;
    private long lastActive;

    /**
     * Default constructor
     */
    public ChatMember() {
    }

    /**
     * Constructor with basic properties
     * @param userId User ID
     * @param username Username
     * @param email Email address
     * @param isAdmin Whether the user is an admin
     * @param isCreator Whether the user is the creator
     */
    public ChatMember(String userId, String username, String email, boolean isAdmin, boolean isCreator) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isCreator = isCreator;
    }

    /**
     * Get the user ID
     * @return User ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set the user ID
     * @param userId User ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Get the username
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username
     * @param username Username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the email address
     * @return Email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the email address
     * @param email Email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Check if the user is an admin
     * @return true if the user is an admin
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Set whether the user is an admin
     * @param admin Whether the user is an admin
     */
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    /**
     * Check if the user is the creator
     * @return true if the user is the creator
     */
    public boolean isCreator() {
        return isCreator;
    }

    /**
     * Set whether the user is the creator
     * @param creator Whether the user is the creator
     */
    public void setCreator(boolean creator) {
        isCreator = creator;
    }

    /**
     * Check if the user is online
     * @return true if the user is online
     */
    public boolean isOnline() {
        return isOnline;
    }

    /**
     * Set whether the user is online
     * @param online Whether the user is online
     */
    public void setOnline(boolean online) {
        isOnline = online;
    }

    /**
     * Get the timestamp of last activity
     * @return Last activity timestamp
     */
    public long getLastActive() {
        return lastActive;
    }

    /**
     * Set the timestamp of last activity
     * @param lastActive Last activity timestamp
     */
    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    /**
     * Get the role display name (Creator, Admin, or Member)
     * @return Role display name
     */
    public String getRoleDisplayName() {
        if (isCreator) {
            return "Creator";
        } else if (isAdmin) {
            return "Admin";
        } else {
            return "Member";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMember that = (ChatMember) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "ChatMember{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", isAdmin=" + isAdmin +
                ", isCreator=" + isCreator +
                ", isOnline=" + isOnline +
                '}';
    }
}