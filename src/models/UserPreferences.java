package models;

import java.util.HashMap;
import java.util.Map;

/**
 * UserPreferences model class for storing user settings and preferences
 * This class handles all user customization options including profile, privacy, notifications, and appearance settings
 */
public class UserPreferences {

    // User Identity
    private String userId;
    private String displayName;
    private String bio;
    private String statusMessage;
    private String profileImageUrl;
    private String phoneNumber;

    // Privacy Settings
    private boolean onlineStatusVisible;
    private boolean lastSeenVisible;
    private boolean readReceiptsEnabled;
    private boolean profilePhotoVisible;
    private boolean allowGroupInvites;

    // Notification Settings
    private boolean soundEnabled;
    private boolean notificationsEnabled;
    private boolean messagePreviewEnabled;
    private boolean vibrationEnabled;
    private String notificationSound;
    private boolean groupNotificationsEnabled;

    // Appearance Settings
    private String theme; // "light", "dark", "auto"
    private String fontSize; // "small", "medium", "large"
    private String chatWallpaper;
    private boolean showAvatars;
    private boolean showTimestamps;
    private String dateFormat; // "12h", "24h"

    // Communication Settings
    private boolean typingIndicatorEnabled;
    private boolean autoDownloadImages;
    private boolean autoDownloadVideos;
    private boolean compressImages;
    private String language; // "fr", "en", "ar", etc.

    // Advanced Settings
    private boolean dataUsageOptimization;
    private boolean backupEnabled;
    private String backupFrequency; // "daily", "weekly", "monthly"
    private boolean syncAcrossDevices;

    // Metadata
    private long createdAt;
    private long lastUpdated;
    private String version; // Settings version for migration

    /**
     * Default constructor with default values
     */
    public UserPreferences() {
        this.displayName = "";
        this.bio = "Salut! J'utilise VibeApp.";
        this.statusMessage = "";
        this.profileImageUrl = "";
        this.phoneNumber = "";

        // Privacy defaults
        this.onlineStatusVisible = true;
        this.lastSeenVisible = true;
        this.readReceiptsEnabled = true;
        this.profilePhotoVisible = true;
        this.allowGroupInvites = true;

        // Notification defaults
        this.soundEnabled = true;
        this.notificationsEnabled = true;
        this.messagePreviewEnabled = true;
        this.vibrationEnabled = true;
        this.notificationSound = "default";
        this.groupNotificationsEnabled = true;

        // Appearance defaults
        this.theme = "light";
        this.fontSize = "medium";
        this.chatWallpaper = "default";
        this.showAvatars = true;
        this.showTimestamps = true;
        this.dateFormat = "24h";

        // Communication defaults
        this.typingIndicatorEnabled = true;
        this.autoDownloadImages = true;
        this.autoDownloadVideos = false;
        this.compressImages = true;
        this.language = "fr";

        // Advanced defaults
        this.dataUsageOptimization = false;
        this.backupEnabled = true;
        this.backupFrequency = "weekly";
        this.syncAcrossDevices = true;

        // Metadata
        this.createdAt = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
        this.version = "1.0";
    }

    /**
     * Constructor with user ID
     * @param userId The user ID
     */
    public UserPreferences(String userId) {
        this();
        this.userId = userId;
    }

    /**
     * Full constructor
     */
    public UserPreferences(String userId, String displayName, String bio, String profileImageUrl) {
        this(userId);
        this.displayName = displayName != null ? displayName : "";
        this.bio = bio != null ? bio : "Salut! J'utilise VibeApp.";
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : "";
    }

    // ===== GETTERS AND SETTERS =====

    // User Identity
    public String getUserId() { return userId; }
    public void setUserId(String userId) {
        this.userId = userId;
        updateTimestamp();
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
        updateTimestamp();
    }

    public String getBio() { return bio; }
    public void setBio(String bio) {
        this.bio = bio != null ? bio : "";
        updateTimestamp();
    }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage != null ? statusMessage : "";
        updateTimestamp();
    }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : "";
        updateTimestamp();
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        updateTimestamp();
    }

    // Privacy Settings
    public boolean isOnlineStatusVisible() { return onlineStatusVisible; }
    public void setOnlineStatusVisible(boolean onlineStatusVisible) {
        this.onlineStatusVisible = onlineStatusVisible;
        updateTimestamp();
    }

    public boolean isLastSeenVisible() { return lastSeenVisible; }
    public void setLastSeenVisible(boolean lastSeenVisible) {
        this.lastSeenVisible = lastSeenVisible;
        updateTimestamp();
    }

    public boolean isReadReceiptsEnabled() { return readReceiptsEnabled; }
    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) {
        this.readReceiptsEnabled = readReceiptsEnabled;
        updateTimestamp();
    }

    public boolean isProfilePhotoVisible() { return profilePhotoVisible; }
    public void setProfilePhotoVisible(boolean profilePhotoVisible) {
        this.profilePhotoVisible = profilePhotoVisible;
        updateTimestamp();
    }

    public boolean isAllowGroupInvites() { return allowGroupInvites; }
    public void setAllowGroupInvites(boolean allowGroupInvites) {
        this.allowGroupInvites = allowGroupInvites;
        updateTimestamp();
    }

    // Notification Settings
    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
        updateTimestamp();
    }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
        updateTimestamp();
    }

    public boolean isMessagePreviewEnabled() { return messagePreviewEnabled; }
    public void setMessagePreviewEnabled(boolean messagePreviewEnabled) {
        this.messagePreviewEnabled = messagePreviewEnabled;
        updateTimestamp();
    }

    public boolean isVibrationEnabled() { return vibrationEnabled; }
    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
        updateTimestamp();
    }

    public String getNotificationSound() { return notificationSound; }
    public void setNotificationSound(String notificationSound) {
        this.notificationSound = notificationSound != null ? notificationSound : "default";
        updateTimestamp();
    }

    public boolean isGroupNotificationsEnabled() { return groupNotificationsEnabled; }
    public void setGroupNotificationsEnabled(boolean groupNotificationsEnabled) {
        this.groupNotificationsEnabled = groupNotificationsEnabled;
        updateTimestamp();
    }

    // Appearance Settings
    public String getTheme() { return theme; }
    public void setTheme(String theme) {
        this.theme = theme != null ? theme : "light";
        updateTimestamp();
    }

    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) {
        this.fontSize = fontSize != null ? fontSize : "medium";
        updateTimestamp();
    }

    public String getChatWallpaper() { return chatWallpaper; }
    public void setChatWallpaper(String chatWallpaper) {
        this.chatWallpaper = chatWallpaper != null ? chatWallpaper : "default";
        updateTimestamp();
    }

    public boolean isShowAvatars() { return showAvatars; }
    public void setShowAvatars(boolean showAvatars) {
        this.showAvatars = showAvatars;
        updateTimestamp();
    }

    public boolean isShowTimestamps() { return showTimestamps; }
    public void setShowTimestamps(boolean showTimestamps) {
        this.showTimestamps = showTimestamps;
        updateTimestamp();
    }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat != null ? dateFormat : "24h";
        updateTimestamp();
    }

    // Communication Settings
    public boolean isTypingIndicatorEnabled() { return typingIndicatorEnabled; }
    public void setTypingIndicatorEnabled(boolean typingIndicatorEnabled) {
        this.typingIndicatorEnabled = typingIndicatorEnabled;
        updateTimestamp();
    }

    public boolean isAutoDownloadImages() { return autoDownloadImages; }
    public void setAutoDownloadImages(boolean autoDownloadImages) {
        this.autoDownloadImages = autoDownloadImages;
        updateTimestamp();
    }

    public boolean isAutoDownloadVideos() { return autoDownloadVideos; }
    public void setAutoDownloadVideos(boolean autoDownloadVideos) {
        this.autoDownloadVideos = autoDownloadVideos;
        updateTimestamp();
    }

    public boolean isCompressImages() { return compressImages; }
    public void setCompressImages(boolean compressImages) {
        this.compressImages = compressImages;
        updateTimestamp();
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) {
        this.language = language != null ? language : "fr";
        updateTimestamp();
    }

    // Advanced Settings
    public boolean isDataUsageOptimization() { return dataUsageOptimization; }
    public void setDataUsageOptimization(boolean dataUsageOptimization) {
        this.dataUsageOptimization = dataUsageOptimization;
        updateTimestamp();
    }

    public boolean isBackupEnabled() { return backupEnabled; }
    public void setBackupEnabled(boolean backupEnabled) {
        this.backupEnabled = backupEnabled;
        updateTimestamp();
    }

    public String getBackupFrequency() { return backupFrequency; }
    public void setBackupFrequency(String backupFrequency) {
        this.backupFrequency = backupFrequency != null ? backupFrequency : "weekly";
        updateTimestamp();
    }

    public boolean isSyncAcrossDevices() { return syncAcrossDevices; }
    public void setSyncAcrossDevices(boolean syncAcrossDevices) {
        this.syncAcrossDevices = syncAcrossDevices;
        updateTimestamp();
    }

    // Metadata
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version != null ? version : "1.0"; }

    // ===== UTILITY METHODS =====

    /**
     * Updates the lastUpdated timestamp
     */
    private void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Converts UserPreferences to a Map for Firebase storage
     * @return Map representation of user preferences
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        // User Identity
        map.put("userId", userId);
        map.put("displayName", displayName);
        map.put("bio", bio);
        map.put("statusMessage", statusMessage);
        map.put("profileImageUrl", profileImageUrl);
        map.put("phoneNumber", phoneNumber);

        // Privacy Settings
        map.put("onlineStatusVisible", onlineStatusVisible);
        map.put("lastSeenVisible", lastSeenVisible);
        map.put("readReceiptsEnabled", readReceiptsEnabled);
        map.put("profilePhotoVisible", profilePhotoVisible);
        map.put("allowGroupInvites", allowGroupInvites);

        // Notification Settings
        map.put("soundEnabled", soundEnabled);
        map.put("notificationsEnabled", notificationsEnabled);
        map.put("messagePreviewEnabled", messagePreviewEnabled);
        map.put("vibrationEnabled", vibrationEnabled);
        map.put("notificationSound", notificationSound);
        map.put("groupNotificationsEnabled", groupNotificationsEnabled);

        // Appearance Settings
        map.put("theme", theme);
        map.put("fontSize", fontSize);
        map.put("chatWallpaper", chatWallpaper);
        map.put("showAvatars", showAvatars);
        map.put("showTimestamps", showTimestamps);
        map.put("dateFormat", dateFormat);

        // Communication Settings
        map.put("typingIndicatorEnabled", typingIndicatorEnabled);
        map.put("autoDownloadImages", autoDownloadImages);
        map.put("autoDownloadVideos", autoDownloadVideos);
        map.put("compressImages", compressImages);
        map.put("language", language);

        // Advanced Settings
        map.put("dataUsageOptimization", dataUsageOptimization);
        map.put("backupEnabled", backupEnabled);
        map.put("backupFrequency", backupFrequency);
        map.put("syncAcrossDevices", syncAcrossDevices);

        // Metadata
        map.put("createdAt", createdAt);
        map.put("lastUpdated", lastUpdated);
        map.put("version", version);

        return map;
    }

    /**
     * Creates UserPreferences from a Firebase Map
     * @param map The map from Firebase
     * @return UserPreferences object
     */
    public static UserPreferences fromMap(Map<String, Object> map) {
        if (map == null) {
            return new UserPreferences();
        }

        UserPreferences prefs = new UserPreferences();

        // User Identity
        prefs.userId = getStringValue(map, "userId", "");
        prefs.displayName = getStringValue(map, "displayName", "");
        prefs.bio = getStringValue(map, "bio", "Salut! J'utilise VibeApp.");
        prefs.statusMessage = getStringValue(map, "statusMessage", "");
        prefs.profileImageUrl = getStringValue(map, "profileImageUrl", "");
        prefs.phoneNumber = getStringValue(map, "phoneNumber", "");

        // Privacy Settings
        prefs.onlineStatusVisible = getBooleanValue(map, "onlineStatusVisible", true);
        prefs.lastSeenVisible = getBooleanValue(map, "lastSeenVisible", true);
        prefs.readReceiptsEnabled = getBooleanValue(map, "readReceiptsEnabled", true);
        prefs.profilePhotoVisible = getBooleanValue(map, "profilePhotoVisible", true);
        prefs.allowGroupInvites = getBooleanValue(map, "allowGroupInvites", true);

        // Notification Settings
        prefs.soundEnabled = getBooleanValue(map, "soundEnabled", true);
        prefs.notificationsEnabled = getBooleanValue(map, "notificationsEnabled", true);
        prefs.messagePreviewEnabled = getBooleanValue(map, "messagePreviewEnabled", true);
        prefs.vibrationEnabled = getBooleanValue(map, "vibrationEnabled", true);
        prefs.notificationSound = getStringValue(map, "notificationSound", "default");
        prefs.groupNotificationsEnabled = getBooleanValue(map, "groupNotificationsEnabled", true);

        // Appearance Settings
        prefs.theme = getStringValue(map, "theme", "light");
        prefs.fontSize = getStringValue(map, "fontSize", "medium");
        prefs.chatWallpaper = getStringValue(map, "chatWallpaper", "default");
        prefs.showAvatars = getBooleanValue(map, "showAvatars", true);
        prefs.showTimestamps = getBooleanValue(map, "showTimestamps", true);
        prefs.dateFormat = getStringValue(map, "dateFormat", "24h");

        // Communication Settings
        prefs.typingIndicatorEnabled = getBooleanValue(map, "typingIndicatorEnabled", true);
        prefs.autoDownloadImages = getBooleanValue(map, "autoDownloadImages", true);
        prefs.autoDownloadVideos = getBooleanValue(map, "autoDownloadVideos", false);
        prefs.compressImages = getBooleanValue(map, "compressImages", true);
        prefs.language = getStringValue(map, "language", "fr");

        // Advanced Settings
        prefs.dataUsageOptimization = getBooleanValue(map, "dataUsageOptimization", false);
        prefs.backupEnabled = getBooleanValue(map, "backupEnabled", true);
        prefs.backupFrequency = getStringValue(map, "backupFrequency", "weekly");
        prefs.syncAcrossDevices = getBooleanValue(map, "syncAcrossDevices", true);

        // Metadata
        prefs.createdAt = getLongValue(map, "createdAt", System.currentTimeMillis());
        prefs.lastUpdated = getLongValue(map, "lastUpdated", System.currentTimeMillis());
        prefs.version = getStringValue(map, "version", "1.0");

        return prefs;
    }

    // Helper methods for safe type conversion from Firebase
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    private static boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private static long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Creates a copy of the current preferences
     * @return A new UserPreferences object with same values
     */
    public UserPreferences copy() {
        return UserPreferences.fromMap(this.toMap());
    }

    /**
     * Validates the preferences data
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * Resets preferences to default values
     */
    public void resetToDefaults() {
        UserPreferences defaults = new UserPreferences(this.userId);

        // Keep user identity but reset everything else
        String currentUserId = this.userId;
        String currentDisplayName = this.displayName;
        String currentProfileImageUrl = this.profileImageUrl;

        // Copy all defaults
        UserPreferences.fromMap(defaults.toMap());

        // Restore identity
        this.userId = currentUserId;
        this.displayName = currentDisplayName;
        this.profileImageUrl = currentProfileImageUrl;

        updateTimestamp();
    }

    @Override
    public String toString() {
        return "UserPreferences{" +
                "userId='" + userId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", theme='" + theme + '\'' +
                ", language='" + language + '\'' +
                ", notificationsEnabled=" + notificationsEnabled +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserPreferences that = (UserPreferences) obj;
        return userId != null ? userId.equals(that.userId) : that.userId == null;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}