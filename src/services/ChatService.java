package services;

import com.google.firebase.database.*;
import models.Chat;
import models.Message;
import models.User;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ChatService - Handles chat operations
 * Manages chat creation, message sending, and chat loading functionality
 */
public class ChatService {
    private FirebaseService firebaseService;
    private static ChatService instance;

    // Private constructor for singleton pattern
    private ChatService() {
        try {
            this.firebaseService = FirebaseService.getInstance();
        } catch (Exception e) {
            System.err.println("Error initializing ChatService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Singleton pattern to get the instance
    public static synchronized ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }

    /**
     * Load all chats for a specific user
     *
     * @param userId User ID
     * @return List of Chat objects
     */
    public List<Chat> loadUserChats(String userId) {
        try {
            List<Chat> chats = new ArrayList<>();
            DatabaseReference userChatsRef = firebaseService.getDatabase()
                    .getReference("users/" + userId + "/chats");

            CountDownLatch latch = new CountDownLatch(1);

            userChatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<String> chatIds = new ArrayList<>();

                    // Collect all chat IDs
                    for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        if (chatId != null) {
                            chatIds.add(chatId);
                        }
                    }

                    // Load chat details for each chat ID
                    if (chatIds.isEmpty()) {
                        latch.countDown();
                        return;
                    }

                    CountDownLatch chatLatch = new CountDownLatch(chatIds.size());

                    for (String chatId : chatIds) {
                        loadChatDetails(chatId, new ChatLoadCallback() {
                            @Override
                            public void onChatLoaded(Chat chat) {
                                if (chat != null) {
                                    synchronized (chats) {
                                        chats.add(chat);
                                    }
                                }
                                chatLatch.countDown();
                            }

                            @Override
                            public void onError(String error) {
                                System.err.println("Error loading chat " + chatId + ": " + error);
                                chatLatch.countDown();
                            }
                        });
                    }

                    // Wait for all chats to load
                    try {
                        chatLatch.await(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted while loading chats: " + e.getMessage());
                    }

                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading user chats: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(30, TimeUnit.SECONDS);

            // Sort chats by last message time (most recent first)
            chats.sort((c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

            return chats;

        } catch (Exception e) {
            System.err.println("Error loading user chats: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Send a message to a chat
     *
     * @param chatId   Chat ID
     * @param content  Message content
     * @param senderId Sender's user ID
     * @return true if message sent successfully
     */
    public boolean sendMessage(String chatId, String content, String senderId) {
        try {
            // Create message object
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("userId", senderId);
            messageData.put("text", content);
            messageData.put("timestamp", ServerValue.TIMESTAMP);
            messageData.put("read", false);

            // Reference to the chat's messages
            DatabaseReference messagesRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages");

            String messageId = messagesRef.push().getKey();

            if (messageId != null) {
                CountDownLatch latch = new CountDownLatch(1);
                final boolean[] success = {false};

                messagesRef.child(messageId).setValue(messageData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error == null) {
                            success[0] = true;

                            // Update chat's last message info
                            updateChatLastMessage(chatId, content, System.currentTimeMillis());
                        } else {
                            System.err.println("Error sending message: " + error.getMessage());
                        }
                        latch.countDown();
                    }
                });

                latch.await(10, TimeUnit.SECONDS);
                return success[0];
            }

        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Create a new chat with multiple participants
     *
     * @param participants List of user IDs to include in the chat
     * @return Chat ID if created successfully, null otherwise
     */
    public String createChat(List<String> participants) {
        try {
            if (participants == null || participants.size() < 2) {
                System.err.println("Chat must have at least 2 participants");
                return null;
            }

            // Create chat data
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("users", participants);
            chatData.put("createdAt", ServerValue.TIMESTAMP);
            chatData.put("createdBy", participants.get(0)); // First participant is the creator
            chatData.put("lastMessageText", "");
            chatData.put("lastMessageTime", ServerValue.TIMESTAMP);

            // Create the chat
            DatabaseReference chatsRef = firebaseService.getDatabase().getReference("chats");
            String chatId = chatsRef.push().getKey();

            if (chatId != null) {
                CountDownLatch latch = new CountDownLatch(1);
                final boolean[] success = {false};

                chatsRef.child(chatId).setValue(chatData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error == null) {
                            success[0] = true;

                            // Add chat to each participant's chat list
                            addChatToUsers(chatId, participants);
                        } else {
                            System.err.println("Error creating chat: " + error.getMessage());
                        }
                        latch.countDown();
                    }
                });

                latch.await(10, TimeUnit.SECONDS);

                if (success[0]) {
                    return chatId;
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating chat: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a group chat with a name
     *
     * @param groupName    Name of the group
     * @param participants List of user IDs to include in the group
     * @return Chat ID if created successfully, null otherwise
     */
    public String createGroupChat(String groupName, List<String> participants) {
        try {
            if (groupName == null || groupName.trim().isEmpty()) {
                System.err.println("Group name cannot be empty");
                return null;
            }

            // Create the chat first
            String chatId = createChat(participants);

            if (chatId != null) {
                // Update the chat to make it a group with a name
                DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + chatId);

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", groupName.trim());
                updates.put("isGroup", true);

                CountDownLatch latch = new CountDownLatch(1);
                final boolean[] success = {false};

                chatRef.updateChildren(updates, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        success[0] = (error == null);
                        if (error != null) {
                            System.err.println("Error updating group info: " + error.getMessage());
                        }
                        latch.countDown();
                    }
                });

                latch.await(10, TimeUnit.SECONDS);

                if (success[0]) {
                    // Send system message
                    sendSystemMessage(chatId, "Group \"" + groupName + "\" was created");
                    return chatId;
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating group chat: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Add a user to an existing group
     *
     * @param chatId    Group chat ID
     * @param newUserId ID of user to add
     * @param addedBy   ID of user performing the action
     * @return true if added successfully
     */
    public boolean addUserToGroup(String chatId, String newUserId, String addedBy) {
        try {
            Chat chat = loadChatById(chatId);
            if (chat == null || !chat.isGroupChat()) {
                return false;
            }

            // Check if the user performing action is in the group
            if (!chat.hasParticipant(addedBy)) {
                return false;
            }

            // Check if new user is already in the group
            if (chat.hasParticipant(newUserId)) {
                return false;
            }

            // Add user to participants list
            List<String> updatedParticipants = new ArrayList<>(chat.getParticipants());
            updatedParticipants.add(newUserId);

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            // Update chat participants
            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + chatId);
            chatRef.child("users").setValue(updatedParticipants, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error == null) {
                        success[0] = true;

                        // Add chat to new user's chat list
                        DatabaseReference userChatsRef = firebaseService.getDatabase()
                                .getReference("users/" + newUserId + "/chats");
                        userChatsRef.child(chatId).setValueAsync(true);

                    } else {
                        System.err.println("Error adding user to group: " + error.getMessage());
                    }
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                // Send system message
                sendSystemMessage(chatId, newUserId + " was added to the group");
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Error adding user to group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a user from a group
     *
     * @param chatId         Group chat ID
     * @param userIdToRemove ID of user to remove
     * @param removedBy      ID of user performing the action
     * @return true if removed successfully
     */
    public boolean removeUserFromGroup(String chatId, String userIdToRemove, String removedBy) {
        try {
            Chat chat = loadChatById(chatId);
            if (chat == null || !chat.isGroupChat()) {
                return false;
            }

            // Check if the user performing action is in the group
            if (!chat.hasParticipant(removedBy)) {
                return false;
            }

            // Check if user to remove is in the group
            if (!chat.hasParticipant(userIdToRemove)) {
                return false;
            }

            // Remove user from participants list
            List<String> updatedParticipants = new ArrayList<>(chat.getParticipants());
            updatedParticipants.remove(userIdToRemove);

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            // Update chat participants
            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + chatId);
            chatRef.child("users").setValue(updatedParticipants, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error == null) {
                        success[0] = true;

                        // Remove chat from user's chat list
                        DatabaseReference userChatsRef = firebaseService.getDatabase()
                                .getReference("users/" + userIdToRemove + "/chats/" + chatId);
                        userChatsRef.removeValueAsync();

                    } else {
                        System.err.println("Error removing user from group: " + error.getMessage());
                    }
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                // Send system message
                if (userIdToRemove.equals(removedBy)) {
                    sendSystemMessage(chatId, userIdToRemove + " left the group");
                } else {
                    sendSystemMessage(chatId, userIdToRemove + " was removed from the group");
                }
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Error removing user from group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load messages for a specific chat
     *
     * @param chatId Chat ID
     * @return List of Message objects
     */
    public List<Message> loadMessages(String chatId) {
        try {
            List<Message> messages = new ArrayList<>();
            DatabaseReference messagesRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages");

            CountDownLatch latch = new CountDownLatch(1);

            messagesRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        try {
                            Message message = new Message();
                            message.setMessageId(messageSnapshot.getKey());
                            message.setChatId(chatId);
                            message.setSenderId(messageSnapshot.child("userId").getValue(String.class));
                            message.setContent(messageSnapshot.child("text").getValue(String.class));

                            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                            message.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());

                            Boolean read = messageSnapshot.child("read").getValue(Boolean.class);
                            message.setRead(read != null ? read : false);

                            messages.add(message);
                        } catch (Exception e) {
                            System.err.println("Error parsing message: " + e.getMessage());
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading messages: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(15, TimeUnit.SECONDS);
            return messages;

        } catch (Exception e) {
            System.err.println("Error loading messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Mark messages as read for a specific user in a chat
     *
     * @param chatId Chat ID
     * @param userId User ID
     */
    public void markMessagesAsRead(String chatId, String userId) {
        try {
            DatabaseReference messagesRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages");

            messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        String senderId = messageSnapshot.child("userId").getValue(String.class);
                        Boolean read = messageSnapshot.child("read").getValue(Boolean.class);

                        // Mark as read if message is from another user and not already read
                        if (senderId != null && !senderId.equals(userId) && (read == null || !read)) {
                            messageSnapshot.getRef().child("read").setValueAsync(true);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error marking messages as read: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("Error marking messages as read: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Delete a message (mark as deleted)
     *
     * @param chatId    Chat ID
     * @param messageId Message ID
     * @param userId    User ID (must be sender)
     * @return true if deleted successfully
     */
    public boolean deleteMessage(String chatId, String messageId, String userId) {
        try {
            DatabaseReference messageRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages/" + messageId);

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            // First check if user is the sender
            messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String senderId = dataSnapshot.child("userId").getValue(String.class);

                        if (userId.equals(senderId)) {
                            // Mark as deleted instead of removing completely
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("deleted", true);
                            updates.put("deletedAt", ServerValue.TIMESTAMP);
                            updates.put("text", "Message deleted");

                            dataSnapshot.getRef().updateChildren(updates, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError error, DatabaseReference ref) {
                                    success[0] = (error == null);
                                    if (error != null) {
                                        System.err.println("Error deleting message: " + error.getMessage());
                                    }
                                    latch.countDown();
                                }
                            });
                        } else {
                            System.err.println("User not authorized to delete this message");
                            latch.countDown();
                        }
                    } else {
                        System.err.println("Message not found");
                        latch.countDown();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error checking message ownership: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("Error deleting message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a chat by ID
     *
     * @param chatId Chat ID
     * @return Chat object or null if not found
     */
    public Chat loadChatById(String chatId) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final Chat[] result = {null};

            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + chatId);
            chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        try {
                            Chat chat = new Chat();
                            chat.setChatId(chatId);

                            // Get participants
                            Object usersObj = dataSnapshot.child("users").getValue();
                            List<String> participants = new ArrayList<>();

                            if (usersObj instanceof ArrayList) {
                                participants = (ArrayList<String>) usersObj;
                            } else if (usersObj instanceof HashMap) {
                                HashMap<String, String> usersMap = (HashMap<String, String>) usersObj;
                                participants.addAll(usersMap.values());
                            }

                            chat.setParticipants(participants);

                            // Get other chat info
                            Long createdAt = dataSnapshot.child("createdAt").getValue(Long.class);
                            chat.setCreatedAt(createdAt != null ? createdAt : 0);

                            String createdBy = dataSnapshot.child("createdBy").getValue(String.class);
                            chat.setCreatedBy(createdBy);

                            String lastMessageText = dataSnapshot.child("lastMessageText").getValue(String.class);
                            chat.setLastMessage(lastMessageText != null ? lastMessageText : "");

                            Long lastMessageTime = dataSnapshot.child("lastMessageTime").getValue(Long.class);
                            chat.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);

                            String chatName = dataSnapshot.child("name").getValue(String.class);
                            chat.setChatName(chatName);

                            Boolean isActive = dataSnapshot.child("isActive").getValue(Boolean.class);
                            chat.setActive(isActive != null ? isActive : true);

                            String chatImageUrl = dataSnapshot.child("chatImageUrl").getValue(String.class);
                            chat.setChatImageUrl(chatImageUrl);

                            result[0] = chat;

                        } catch (Exception e) {
                            System.err.println("Error parsing chat data: " + e.getMessage());
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading chat: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            return result[0];

        } catch (Exception e) {
            System.err.println("Error loading chat: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Send a system message to a chat
     *
     * @param chatId  Chat ID
     * @param content System message content
     * @return true if sent successfully
     */
    public boolean sendSystemMessage(String chatId, String content) {
        try {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("userId", "system");
            messageData.put("text", content);
            messageData.put("timestamp", ServerValue.TIMESTAMP);
            messageData.put("type", "SYSTEM");
            messageData.put("read", true);

            DatabaseReference messagesRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages");

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            messagesRef.push().setValue(messageData, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error == null) {
                        success[0] = true;
                        // Update chat's last message
                        updateChatLastMessage(chatId, content, System.currentTimeMillis());
                    } else {
                        System.err.println("Error sending system message: " + error.getMessage());
                    }
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("Error sending system message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Private helper methods

    /**
     * Load detailed information for a specific chat
     */
    private void loadChatDetails(String chatId, ChatLoadCallback callback) {
        try {
            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + chatId);

            chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        try {
                            Chat chat = new Chat();
                            chat.setChatId(chatId);

                            // Get participants
                            Object usersObj = dataSnapshot.child("users").getValue();
                            List<String> participants = new ArrayList<>();

                            if (usersObj instanceof ArrayList) {
                                participants = (ArrayList<String>) usersObj;
                            } else if (usersObj instanceof HashMap) {
                                HashMap<String, String> usersMap = (HashMap<String, String>) usersObj;
                                participants.addAll(usersMap.values());
                            }

                            chat.setParticipants(participants);

                            // Get other chat info
                            Long createdAt = dataSnapshot.child("createdAt").getValue(Long.class);
                            chat.setCreatedAt(createdAt != null ? createdAt : 0);

                            String createdBy = dataSnapshot.child("createdBy").getValue(String.class);
                            chat.setCreatedBy(createdBy);

                            String lastMessageText = dataSnapshot.child("lastMessageText").getValue(String.class);
                            chat.setLastMessage(lastMessageText != null ? lastMessageText : "");

                            Long lastMessageTime = dataSnapshot.child("lastMessageTime").getValue(Long.class);
                            chat.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);

                            String chatName = dataSnapshot.child("name").getValue(String.class);
                            chat.setChatName(chatName);

                            callback.onChatLoaded(chat);

                        } catch (Exception e) {
                            callback.onError("Error parsing chat data: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Chat not found");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError("Database error: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Update chat's last message information
     */
    public void updateChatLastMessage(String chatId, String messageText, long timestamp) {
        try {
            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + chatId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessageText", messageText);
            updates.put("lastMessageTime", timestamp);

            chatRef.updateChildren(updates, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        System.err.println("Error updating last message: " + error.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Error updating last message: " + e.getMessage());
        }
    }

    /**
     * Add chat to each participant's chat list
     */
    private void addChatToUsers(String chatId, List<String> participants) {
        try {
            for (String userId : participants) {
                DatabaseReference userChatsRef = firebaseService.getDatabase()
                        .getReference("users/" + userId + "/chats");

                userChatsRef.child(chatId).setValueAsync(true);
            }
        } catch (Exception e) {
            System.err.println("Error adding chat to users: " + e.getMessage());
        }
    }

    // Callback interface for asynchronous chat loading
    private interface ChatLoadCallback {
        void onChatLoaded(Chat chat);

        void onError(String error);
    }
}