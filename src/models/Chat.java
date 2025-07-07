package models;

import java.text.SimpleDateFormat;
import java.util.*;

public class Chat {
    // Basic chat information
    private String chatId;
    private String chatName;
    private List<String> participants;
    private String createdBy;
    private long createdAt;

    // Message information
    private String lastMessage;
    private long lastMessageTime;
    private String lastMessageSenderId;

    // Chat status and activity
    private boolean isActive;
    private boolean isGroupChat;
    private String chatImageUrl;

    // Typing indicators and read status
    private Map<String, Boolean> typing;
    private Map<String, Integer> unreadCount;
    private Map<String, Long> lastReadTime;

    // Chat settings
    private Map<String, Object> settings;

    // Constructors
    public Chat() {
        this.participants = new ArrayList<>();
        this.typing = new HashMap<>();
        this.unreadCount = new HashMap<>();
        this.lastReadTime = new HashMap<>();
        this.settings = new HashMap<>();
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }

    public Chat(String chatId, List<String> participants) {
        this();
        this.chatId = chatId;
        this.participants = new ArrayList<>(participants);
        this.isGroupChat = participants.size() > 2;

        // Initialize unread count for all participants
        for (String participantId : participants) {
            this.unreadCount.put(participantId, 0);
            this.lastReadTime.put(participantId, System.currentTimeMillis());
        }
    }

    public Chat(String chatId, List<String> participants, String createdBy) {
        this(chatId, participants);
        this.createdBy = createdBy;
    }

    // Display methods
    public String getDisplayName(String currentUserId) {
        if (isGroupChat) {
            return chatName != null && !chatName.isEmpty() ? chatName : "Group Chat";
        } else {
            // For private chats, return the other participant's name
            String otherParticipant = getOtherParticipant(currentUserId);
            return otherParticipant != null ? otherParticipant : "Private Chat";
        }
    }

    public String getOtherParticipant(String currentUserId) {
        if (isGroupChat || participants.size() != 2) {
            return null;
        }

        for (String participantId : participants) {
            if (!participantId.equals(currentUserId)) {
                return participantId;
            }
        }
        return null;
    }

    public String getFormattedLastMessageTime() {
        if (lastMessageTime <= 0) {
            return "";
        }

        Date messageDate = new Date(lastMessageTime);
        Date now = new Date();

        // Same day - show time
        if (isSameDay(messageDate, now)) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            return timeFormat.format(messageDate);
        }

        // Yesterday
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterday = calendar.getTime();

        if (isSameDay(messageDate, yesterday)) {
            return "Yesterday";
        }

        // This week - show day name
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date lastWeek = calendar.getTime();

        if (messageDate.after(lastWeek)) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
            return dayFormat.format(messageDate);
        }

        // Older - show date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(messageDate);
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    // Participant management
    public boolean addParticipant(String userId) {
        if (!participants.contains(userId)) {
            participants.add(userId);
            unreadCount.put(userId, 0);
            lastReadTime.put(userId, System.currentTimeMillis());

            // Update group chat status
            if (participants.size() > 2) {
                isGroupChat = true;
            }
            return true;
        }
        return false;
    }

    public boolean removeParticipant(String userId) {
        boolean removed = participants.remove(userId);
        if (removed) {
            unreadCount.remove(userId);
            lastReadTime.remove(userId);
            typing.remove(userId);

            // Update group chat status
            if (participants.size() <= 2) {
                isGroupChat = false;
            }
        }
        return removed;
    }

    public boolean hasParticipant(String userId) {
        return participants.contains(userId);
    }

    public int getParticipantCount() {
        return participants.size();
    }

    // Message management
    public void updateLastMessage(String message, String senderId, long timestamp) {
        this.lastMessage = message;
        this.lastMessageSenderId = senderId;
        this.lastMessageTime = timestamp;

        // Increment unread count for all participants except sender
        for (String participantId : participants) {
            if (!participantId.equals(senderId)) {
                int currentUnread = unreadCount.getOrDefault(participantId, 0);
                unreadCount.put(participantId, currentUnread + 1);
            }
        }
    }

    public void markAsRead(String userId) {
        unreadCount.put(userId, 0);
        lastReadTime.put(userId, System.currentTimeMillis());
    }

    public int getUnreadCount(String userId) {
        return unreadCount.getOrDefault(userId, 0);
    }

    public boolean hasUnreadMessages(String userId) {
        return getUnreadCount(userId) > 0;
    }

    // Typing indicators
    public void setUserTyping(String userId, boolean typing) {
        if (typing) {
            this.typing.put(userId, true);
        } else {
            this.typing.remove(userId);
        }
    }

    public boolean isUserTyping(String userId) {
        return typing.getOrDefault(userId, false);
    }

    public List<String> getTypingUsers() {
        List<String> typingUsers = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : typing.entrySet()) {
            if (entry.getValue()) {
                typingUsers.add(entry.getKey());
            }
        }
        return typingUsers;
    }

    public List<String> getTypingUsersExcept(String excludeUserId) {
        List<String> typingUsers = getTypingUsers();
        typingUsers.remove(excludeUserId);
        return typingUsers;
    }

    // Chat type methods
    public boolean isGroupChat() {
        return isGroupChat;
    }

    public boolean isPrivateChat() {
        return !isGroupChat;
    }

    // Settings methods
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public Object getSetting(String key, Object defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    // Archive/Activate methods
    public void archive() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    // Getters and Setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getChatName() { return chatName; }
    public void setChatName(String chatName) {
        this.chatName = chatName;
        if (chatName != null && !chatName.isEmpty()) {
            this.isGroupChat = true;
        }
    }

    public List<String> getParticipants() { return new ArrayList<>(participants); }
    public void setParticipants(List<String> participants) {
        this.participants = new ArrayList<>(participants);
        this.isGroupChat = participants.size() > 2;
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getChatImageUrl() { return chatImageUrl; }
    public void setChatImageUrl(String chatImageUrl) { this.chatImageUrl = chatImageUrl; }

    public Map<String, Object> getSettings() { return settings; }
    public void setSettings(Map<String, Object> settings) { this.settings = settings; }

    @Override
    public String toString() {
        return "Chat{" +
                "chatId='" + chatId + '\'' +
                ", chatName='" + chatName + '\'' +
                ", participants=" + participants.size() +
                ", isGroupChat=" + isGroupChat +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Chat chat = (Chat) obj;
        return chatId != null ? chatId.equals(chat.chatId) : chat.chatId == null;
    }

    @Override
    public int hashCode() {
        return chatId != null ? chatId.hashCode() : 0;
    }
}