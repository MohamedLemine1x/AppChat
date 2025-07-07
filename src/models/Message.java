package models;

import java.text.SimpleDateFormat;
import java.util.*;

public class Message {
    // Basic message information
    private String messageId;
    private String chatId;
    private String senderId;
    private String content;
    private long timestamp;

    // Message type and status
    private MessageType type;
    private boolean isRead;
    private boolean isEdited;
    private boolean isDeleted;

    // Read status tracking
    private List<String> readBy;
    private Map<String, Long> readTimestamps;

    // Reply and attachment information
    private String replyToMessageId;
    private String attachmentUrl;
    private String attachmentName;
    private long attachmentSize;

    // Message metadata
    private Map<String, Object> metadata;
    private long editedAt;
    private long deletedAt;

    // Constructors
    public Message() {
        this.readBy = new ArrayList<>();
        this.readTimestamps = new HashMap<>();
        this.metadata = new HashMap<>();
        this.type = MessageType.TEXT;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String senderId, String content) {
        this();
        this.senderId = senderId;
        this.content = content;
    }

    public Message(String messageId, String chatId, String senderId, String content) {
        this(senderId, content);
        this.messageId = messageId;
        this.chatId = chatId;
    }

    public Message(String senderId, String content, MessageType type) {
        this(senderId, content);
        this.type = type;
    }

    // Display methods
    public String getFormattedTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        return timeFormat.format(new Date(timestamp));
    }

    public String getFormattedDateTime() {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return dateTimeFormat.format(new Date(timestamp));
    }

    public String getDisplayContent() {
        if (isDeleted) {
            return "This message was deleted";
        }

        switch (type) {
            case TEXT:
                return content != null ? content : "";
            case IMAGE:
                return "📷 Image" + (content != null && !content.isEmpty() ? ": " + content : "");
            case FILE:
                return "📎 File" + (attachmentName != null ? ": " + attachmentName : "");
            case VOICE:
                return "🎤 Voice message";
            case VIDEO:
                return "🎥 Video" + (content != null && !content.isEmpty() ? ": " + content : "");
            case SYSTEM:
                return content != null ? content : "System message";
            default:
                return content != null ? content : "";
        }
    }

    public String getDisplayContentPreview(int maxLength) {
        String displayContent = getDisplayContent();
        if (displayContent.length() <= maxLength) {
            return displayContent;
        }
        return displayContent.substring(0, maxLength - 3) + "...";
    }

    // Message status methods
    public boolean isFromUser(String userId) {
        return senderId != null && senderId.equals(userId);
    }

    public void markAsRead(String userId) {
        if (!readBy.contains(userId)) {
            readBy.add(userId);
            readTimestamps.put(userId, System.currentTimeMillis());
        }

        // If this is the sender marking their own message as read, set global read status
        if (isFromUser(userId)) {
            this.isRead = true;
        }
    }

    public boolean isReadBy(String userId) {
        return readBy.contains(userId);
    }

    public long getReadTimeBy(String userId) {
        return readTimestamps.getOrDefault(userId, 0L);
    }

    public int getReadCount() {
        return readBy.size();
    }

    public List<String> getReadBy() {
        return new ArrayList<>(readBy);
    }

    // Permission methods
    public boolean canEdit(String userId) {
        // Only sender can edit, and only text messages, and not system messages
        return isFromUser(userId) &&
                type == MessageType.TEXT &&
                !isDeleted &&
                !isSystemMessage();
    }

    public boolean canDelete(String userId) {
        // Sender can always delete, system messages cannot be deleted
        return isFromUser(userId) && !isSystemMessage();
    }

    public boolean canReply() {
        return !isDeleted && !isSystemMessage();
    }

    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }

    // Edit and delete methods
    public void editContent(String newContent) {
        if (type == MessageType.TEXT && !isDeleted) {
            this.content = newContent;
            this.isEdited = true;
            this.editedAt = System.currentTimeMillis();
        }
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = System.currentTimeMillis();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = 0L;
    }

    // Reply methods
    public void setReplyTo(String messageId) {
        this.replyToMessageId = messageId;
    }

    public boolean isReply() {
        return replyToMessageId != null && !replyToMessageId.isEmpty();
    }

    // Attachment methods
    public void setAttachment(String url, String name, long size) {
        this.attachmentUrl = url;
        this.attachmentName = name;
        this.attachmentSize = size;
    }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.isEmpty();
    }

    public String getFormattedAttachmentSize() {
        if (attachmentSize <= 0) return "";

        if (attachmentSize < 1024) {
            return attachmentSize + " B";
        } else if (attachmentSize < 1024 * 1024) {
            return String.format("%.1f KB", attachmentSize / 1024.0);
        } else if (attachmentSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", attachmentSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", attachmentSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // Metadata methods
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Object getMetadata(String key, Object defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    // Factory methods for different message types
    public static Message createTextMessage(String senderId, String content) {
        return new Message(senderId, content, MessageType.TEXT);
    }

    public static Message createSystemMessage(String content) {
        Message message = new Message("system", content, MessageType.SYSTEM);
        message.isRead = true; // System messages are always considered read
        return message;
    }

    public static Message createImageMessage(String senderId, String imageUrl, String caption) {
        Message message = new Message(senderId, caption, MessageType.IMAGE);
        message.setAttachment(imageUrl, "image", 0);
        return message;
    }

    public static Message createFileMessage(String senderId, String fileUrl, String fileName, long fileSize) {
        Message message = new Message(senderId, "", MessageType.FILE);
        message.setAttachment(fileUrl, fileName, fileSize);
        return message;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public long getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(long attachmentSize) { this.attachmentSize = attachmentSize; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public long getEditedAt() { return editedAt; }
    public void setEditedAt(long editedAt) { this.editedAt = editedAt; }

    public long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(long deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", type=" + type +
                ", content='" + getDisplayContentPreview(50) + '\'' +
                ", timestamp=" + getFormattedTime() +
                ", isRead=" + isRead +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Message message = (Message) obj;
        return messageId != null ? messageId.equals(message.messageId) : message.messageId == null;
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }
}