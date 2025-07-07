package models;

public enum MessageType {
    TEXT("Text", "💬"),
    IMAGE("Image", "📷"),
    FILE("File", "📎"),
    VOICE("Voice", "🎤"),
    VIDEO("Video", "🎥"),
    SYSTEM("System", "ℹ️");

    private final String displayName;
    private final String emoji;

    MessageType(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    @Override
    public String toString() {
        return displayName;
    }
}