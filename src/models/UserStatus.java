package models;

public enum UserStatus {
    ONLINE("Online", "#00C851"),
    AWAY("Away", "#FFB347"),
    BUSY("Busy", "#FF4444"),
    INVISIBLE("Invisible", "#9E9E9E"),
    OFFLINE("Offline", "#9E9E9E");

    private final String displayName;
    private final String colorCode;

    UserStatus(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}