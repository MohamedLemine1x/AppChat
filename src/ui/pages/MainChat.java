package ui.pages;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Query;

import services.FirebaseService;
import ui.components.*;
import ui.components.ModernButton;
import models.Group;
import models.User;
import services.GroupService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.awt.geom.RoundRectangle2D;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MainChat extends JPanel {
    // Constants
    private static final int EXPANDED_SIDEBAR_WIDTH = 280;
    private static final int COLLAPSED_SIDEBAR_WIDTH = 80;
    private static final int TYPING_TIMEOUT = 2000; // milliseconds


    // Colors
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private final Color EMSI_GRAY_LIGHTER = new Color(240, 240, 240);
    private final Color EMSI_RED = new Color(217, 83, 30);
    private final Color EMSI_RED_LIGHT = new Color(230, 110, 50);
    private final Color MESSAGE_BG_MINE = new Color(220, 248, 230);
    private final Color MESSAGE_BG_OTHERS = new Color(240, 240, 240);

    // Firebase
    private FirebaseService firebaseService;
    private String currentUserId;
    private String currentUsername;
    private String currentChatId;

    // Optimized Cache with size limits
    private static final int MAX_CACHE_SIZE = 100;
    private Map<String, UserInfo> userInfoCache = new LinkedHashMap<String, UserInfo>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UserInfo> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private Map<String, ChatInfo> chatInfoCache = new LinkedHashMap<String, ChatInfo>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ChatInfo> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private Map<String, String> userCache = new LinkedHashMap<String, String>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private Map<String, BufferedImage> imageCache = new LinkedHashMap<String, BufferedImage>(50, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > 50; // Smaller cache for images as they use more memory
        }
    };

    // UI Components
    private JPanel sidebarPanel;
    private JPanel chatListPanel;
    private JPanel chatPanel;
    private JTextPane messageArea;
    private JScrollPane messageScrollPane;
    private JPanel activeUserPanel;
    private JTextField messageField;
    private JButton sendButton;
    private JButton attachButton;
    private JButton emojiButton;
    private ModernButton addChatButton;
    private JTextField chatSearchField;

    private JPopupMenu chatContextMenu;
    private JMenuItem addMembersMenuItem;
    private JMenuItem groupInfoMenuItem;
    private JMenuItem leaveGroupMenuItem;

    // State
    private boolean isSidebarCollapsed = false;
    private boolean isTyping = false;
    private boolean lastTypingStatus = false;
    private javax.swing.Timer typingTimer;
    private javax.swing.Timer timer;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private GroupService groupService;

    private ValueEventListener currentMessagesListener = null;

    /**
     * Creates a new MainChat panel for the specified user
     *
     * @param userId The ID of the current user
     */
    public MainChat(String userId) {
        this.currentUserId = userId;
        setLayout(new BorderLayout());

        initializeFirebase();
        initializeGroupService();
        loadCurrentUserInfoSync();
        initUI();
        loadChats();
        setupTypingTimer();
        
        // Add responsive behavior
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResponsiveLayout();
            }
        });
    }

    /**
     * Initializes Firebase service
     */
    private void initializeFirebase() {
        try {
            firebaseService = FirebaseService.getInstance();
            System.out.println("Firebase initialized in MainChat");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur de connexion à Firebase: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeGroupService() {
        this.groupService = GroupService.getInstance();
    }

    /**
     * Loads current user information from Firebase
     */
    private void loadCurrentUserInfoSync() {
        try {
            DatabaseReference userRef = firebaseService.getDatabase()
                    .getReference("users/" + currentUserId);

            CountDownLatch latch = new CountDownLatch(1);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        currentUsername = dataSnapshot.child("username").getValue(String.class);
                        if (currentUsername == null || currentUsername.isEmpty()) {
                            // If username is not set, use the combination of nom and prenom
                            String nom = dataSnapshot.child("nom").getValue(String.class);
                            String prenom = dataSnapshot.child("prenom").getValue(String.class);
                            currentUsername = prenom + " " + nom;
                        }

                        // Store user info in cache
                        userInfoCache.put(currentUserId, new UserInfo(
                                currentUserId,
                                currentUsername,
                                dataSnapshot.child("email").getValue(String.class)
                        ));

                        // Update online status
                        updateOnlineStatus(true);
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading user info: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading user info: " + e.getMessage());
        }
    }

    private void loadCurrentUserInfo() {
        try {
            DatabaseReference userRef = firebaseService.getDatabase()
                    .getReference("users/" + currentUserId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        currentUsername = dataSnapshot.child("username").getValue(String.class);
                        if (currentUsername == null || currentUsername.isEmpty()) {
                            // If username is not set, use the combination of nom and prenom
                            String nom = dataSnapshot.child("nom").getValue(String.class);
                            String prenom = dataSnapshot.child("prenom").getValue(String.class);
                            currentUsername = prenom + " " + nom;
                        }

                        // Store user info in cache
                        userInfoCache.put(currentUserId, new UserInfo(
                                currentUserId,
                                currentUsername,
                                dataSnapshot.child("email").getValue(String.class)
                        ));

                        // Update online status and UI on EDT
                        SwingUtilities.invokeLater(() -> {
                            updateOnlineStatus(true);
                            updateProfileDisplay();
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading user info: " + databaseError.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading user info: " + e.getMessage());
        }
    }

    /**
     * Updates the user's online status in Firebase
     *
     * @param isOnline Whether the user is online
     */
    private void updateOnlineStatus(boolean isOnline) {
        try {
            DatabaseReference userStatusRef = firebaseService.getDatabase()
                    .getReference("users/" + currentUserId + "/status");

            Map<String, Object> status = new HashMap<>();
            status.put("online", isOnline);
            status.put("lastSeen", ServerValue.TIMESTAMP);

            userStatusRef.updateChildren(status, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        System.err.println("Error updating status: " + error.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the UI components
     */
    private void initUI() {
        // Set up main layout
        setLayout(new BorderLayout());

        // Create sidebar
        createSidebar();

        // Create chat panel
        createChatPanel();

        // Add main panels to frame
        add(sidebarPanel, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);
    }

    /**
     * Creates the sidebar panel with user profile and chat list
     */
    private void createSidebar() {
        sidebarPanel = new JPanel(new BorderLayout());
        
        // Make sidebar width responsive
        int sidebarWidth = getWidth() < 800 ? COLLAPSED_SIDEBAR_WIDTH : EXPANDED_SIDEBAR_WIDTH;
        sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, 0));
        sidebarPanel.setMinimumSize(new Dimension(COLLAPSED_SIDEBAR_WIDTH, 0));
        sidebarPanel.setBackground(Color.WHITE);
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

        // Create top profile panel
        JPanel profilePanel = createProfilePanel();

        // Create search panel
        JPanel searchPanel = createSearchPanel();

        // Create chat list
        createChatList();

        // Create buttons panel at bottom of sidebar
        JPanel buttonsPanel = createSidebarButtonsPanel();

        // Add components to sidebar
        sidebarPanel.add(profilePanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(createScrollPane(chatListPanel), BorderLayout.CENTER);

        sidebarPanel.add(centerPanel, BorderLayout.CENTER);
        sidebarPanel.add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a JScrollPane with modern scrollbar
     *
     * @param component The component to add to the scroll pane
     * @return The styled scroll pane
     */
    private JScrollPane createScrollPane(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    /**
     * Creates the profile panel at the top of the sidebar
     *
     * @return The profile panel
     */
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // User avatar
        AvatarPanel avatarPanel = new AvatarPanel(45, generateAvatarColor(currentUserId));

        // Set initials from username
        if (currentUsername != null && !currentUsername.isEmpty()) {
            avatarPanel.setInitials(getInitials(currentUsername));
        }

        // Check if user has a profile image and load it
        loadUserProfileImage(avatarPanel, currentUserId);

        // User info panel
        JPanel userInfoPanel = new JPanel(new BorderLayout(4, 0));
        userInfoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(currentUsername);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(new Color(50, 50, 50));

        JLabel statusLabel = new JLabel("● En ligne");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(EMSI_GREEN);

        userInfoPanel.add(nameLabel, BorderLayout.NORTH);
        userInfoPanel.add(statusLabel, BorderLayout.CENTER);

        // Create button panel for toggle and settings
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        // Settings button with enhanced styling
        JButton settingsButton = createEnhancedButton(loadImageIcon("pictures/settings.png", 18, 18), 
                                                    "Paramètres", EMSI_GRAY);
        settingsButton.addActionListener(e -> openUserSettings());

        // Toggle sidebar button with enhanced styling and dynamic icon
        JButton toggleButton = createEnhancedButton(loadImageIcon("pictures/menu.png", 18, 18), 
                                                  "Menu", EMSI_GRAY);
        toggleButton.addActionListener(e -> {
            toggleSidebar();
            // Update button icon based on sidebar state
            updateToggleButtonIcon(toggleButton);
        });

        buttonPanel.add(settingsButton);
        buttonPanel.add(toggleButton);

        // Add components to panel
        panel.add(avatarPanel, BorderLayout.WEST);
        panel.add(userInfoPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Creates an enhanced button with better styling
     */
    private JButton createEnhancedButton(ImageIcon icon, String tooltip, Color color) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(36, 36));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Enhanced hover effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60), 1),
                    BorderFactory.createEmptyBorder(7, 7, 7, 7)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
                button.setBorder(BorderFactory.createEmptyBorder());
            }
        });
        
        return button;
    }

    private void loadUserProfileImage(AvatarPanel avatarPanel, String userId) {
        if (userId == null || avatarPanel == null) {
            System.err.println("Invalid parameters for loading profile image");
            return;
        }
        
        System.out.println("Loading profile image for user: " + userId);
        
        try {
            DatabaseReference userRef = firebaseService.getDatabase()
                    .getReference("users/" + userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get user info for fallback
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String nom = dataSnapshot.child("nom").getValue(String.class);
                        String prenom = dataSnapshot.child("prenom").getValue(String.class);
                        
                        // Use username or fallback to nom+prenom
                        String displayName = username;
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
                            displayName = displayName.trim();
                        }
                        if (displayName.isEmpty()) {
                            displayName = "User";
                        }
                        
                        // Set initials as fallback
                        avatarPanel.setInitials(getInitials(displayName));
                        
                        // Check for profile image URL
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                        System.out.println("Profile image URL from Firebase for " + userId + ": " + profileImageUrl);

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            loadImageFromUrl(avatarPanel, profileImageUrl, userId);
                        } else {
                            System.out.println("No profile image URL found for user: " + userId + ", using initials");
                        }
                    } else {
                        System.out.println("User data not found in Firebase for: " + userId);
                        avatarPanel.setInitials("?");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading user profile image: " + databaseError.getMessage());
                    avatarPanel.setInitials("?");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            avatarPanel.setInitials("?");
        }
    }
    
    /**
     * Loads an image from various URL types (remote, local, absolute)
     */
    private void loadImageFromUrl(AvatarPanel avatarPanel, String profileImageUrl, String userId) {
        // Check cache first
        String cacheKey = userId + ":" + profileImageUrl;
        BufferedImage cachedImage = imageCache.get(cacheKey);
        if (cachedImage != null) {
            avatarPanel.setAvatarImage(cachedImage);
            System.out.println("Loaded cached image for user: " + userId);
            return;
        }
        
        // Show loading indicator
        avatarPanel.startLoading();
        
        SwingWorker<BufferedImage, Void> imageLoader = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                BufferedImage image = null;
                
                // Try different loading strategies
                if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
                    // Remote URL (Firebase Storage, etc.)
                    System.out.println("Loading remote image: " + profileImageUrl);
                    try {
                        java.net.URL url = new java.net.URL(profileImageUrl);
                        image = ImageIO.read(url);
                    } catch (Exception e) {
                        System.err.println("Failed to load remote image: " + e.getMessage());
                    }
                } else if (profileImageUrl.startsWith("profiles/")) {
                    // Relative path - try multiple locations
                    String[] possiblePaths = {
                        "resources/" + profileImageUrl,
                        profileImageUrl,
                        "src/resources/" + profileImageUrl,
                        System.getProperty("user.dir") + "/" + profileImageUrl
                    };
                    
                    for (String path : possiblePaths) {
                        try {
                            File imageFile = new File(path);
                            System.out.println("Trying image path: " + path + " (exists: " + imageFile.exists() + ")");
                            if (imageFile.exists() && imageFile.isFile()) {
                                image = ImageIO.read(imageFile);
                                if (image != null) {
                                    System.out.println("Successfully loaded image from: " + path);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load image from " + path + ": " + e.getMessage());
                        }
                    }
                } else {
                    // Absolute file path
                    try {
                        File imageFile = new File(profileImageUrl);
                        if (imageFile.exists() && imageFile.isFile()) {
                            image = ImageIO.read(imageFile);
                            System.out.println("Loaded image from absolute path: " + profileImageUrl);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load image from absolute path: " + e.getMessage());
                    }
                }
                
                return image;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        // Cache the image for future use
                        imageCache.put(cacheKey, image);
                        
                        SwingUtilities.invokeLater(() -> {
                            avatarPanel.setAvatarImage(image);
                            System.out.println("Successfully set profile image for user: " + userId);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            avatarPanel.stopLoading();
                            System.err.println("Failed to load any image for user: " + userId + ", keeping initials");
                        });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        avatarPanel.stopLoading();
                        System.err.println("Error in image loading worker: " + e.getMessage());
                    });
                }
            }
        };
        
        imageLoader.execute();
    }
    
    /**
     * Generates initials from a display name
     */
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }
        
        return initials.length() > 0 ? initials.toString().toUpperCase() : "?";
    }
    
    /**
     * Clears the image cache to free memory
     */
    public void clearImageCache() {
        imageCache.clear();
        System.out.println("Image cache cleared");
    }
    
    /**
     * Removes a specific user's cached image
     */
    public void clearUserImageCache(String userId) {
        imageCache.entrySet().removeIf(entry -> entry.getKey().startsWith(userId + ":"));
        System.out.println("Cleared cached images for user: " + userId);
    }



    /**
     * Opens the user settings dialog
     */
    private void openUserSettings() {
        JDialog settingsDialog = new JDialog(
                (Frame)SwingUtilities.getWindowAncestor(this),
                "Paramètres utilisateur",
                true
        );
        settingsDialog.setSize(800, 600);
        settingsDialog.setLocationRelativeTo(this);

        // Create and set the UserSettingsPage
        UserSettingsPage settingsPage = new UserSettingsPage(currentUserId, firebaseService);

        // Add a listener to refresh the UI when settings are saved
        settingsPage.addSettingsSavedListener(new UserSettingsPage.SettingsSavedListener() {
            @Override
            public void onSettingsSaved() {
                // Refresh the profile panel to reflect any changes
                refreshUserInterface();
            }
        });

        settingsDialog.setContentPane(settingsPage);
        settingsDialog.setVisible(true);
    }

    private void refreshUserInterface() {
        // Reload user info asynchronously
        loadCurrentUserInfo();
    }
    
    private void updateProfileDisplay() {
        // Rebuild the sidebar to reflect any profile changes
        sidebarPanel.removeAll();
        createSidebar();
        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    /**
     * Creates the search panel for the sidebar
     *
     * @return The search panel
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 20, 20, 20));
        panel.setOpaque(false);

        // Create search field with enhanced styling
        chatSearchField = new JTextField();
        chatSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatSearchField.setBorder(new EmptyBorder(12, 45, 12, 15));
        chatSearchField.setBackground(new Color(248, 249, 250));
        chatSearchField.setForeground(new Color(50, 50, 50));
        chatSearchField.setCaretColor(EMSI_GREEN);

        // Wrap in a panel for the magnifying glass icon
        JPanel searchWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                
                // Draw subtle border
                g2.setColor(new Color(220, 220, 220));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchWrapper.setBackground(new Color(248, 249, 250));
        searchWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));
        searchWrapper.setPreferredSize(new Dimension(0, 42));

        // Create and add the magnifying glass icon
        JLabel searchIcon = new JLabel(loadImageIcon("pictures/search.png", 18, 18));
        searchIcon.setBorder(new EmptyBorder(0, 15, 0, 0));
        searchIcon.setForeground(new Color(120, 120, 120));

        searchWrapper.add(searchIcon, BorderLayout.WEST);
        searchWrapper.add(chatSearchField, BorderLayout.CENTER);

        // Add search functionality
        chatSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterChats(chatSearchField.getText());
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterChats(chatSearchField.getText());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterChats(chatSearchField.getText());
            }
        });

        // Add focus effects
        chatSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                searchWrapper.setBackground(new Color(255, 255, 255));
                searchWrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(EMSI_GREEN, 2),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
                searchWrapper.repaint();
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                searchWrapper.setBackground(new Color(248, 249, 250));
                searchWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                searchWrapper.repaint();
            }
        });

        // Add placeholder text
        chatSearchField.addCaretListener(e -> {
            if (chatSearchField.getText().isEmpty() && !chatSearchField.hasFocus()) {
                chatSearchField.setForeground(new Color(150, 150, 150));
            } else {
                chatSearchField.setForeground(new Color(50, 50, 50));
            }
        });

        // Add the search wrapper to panel
        panel.add(searchWrapper, BorderLayout.CENTER);

        return panel;
    }
    
    private void filterChats(String searchText) {
        String filter = searchText != null ? searchText.toLowerCase().trim() : "";
        
        // If empty filter, show all chats from cache without reloading
        if (filter.isEmpty()) {
            showAllChatsFromCache();
            return;
        }
        
        // Filter existing chat items without clearing
        SwingUtilities.invokeLater(() -> {
            for (Component comp : chatListPanel.getComponents()) {
                if (comp instanceof ChatItem) {
                    ChatItem chatItem = (ChatItem) comp;
                    boolean matches = false;
                    
                    // Check if chat name matches
                    if (chatItem.getChatName() != null && 
                        chatItem.getChatName().toLowerCase().contains(filter)) {
                        matches = true;
                    }
                    
                    // Check participant names for direct chats if needed
                    if (!matches) {
                        ChatInfo chatInfo = chatInfoCache.get(chatItem.getChatId());
                        if (chatInfo != null) {
                            for (String userId : chatInfo.users) {
                                if (!userId.equals(currentUserId)) {
                                    UserInfo userInfo = userInfoCache.get(userId);
                                    if (userInfo != null && userInfo.username != null && 
                                        userInfo.username.toLowerCase().contains(filter)) {
                                        matches = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Show/hide based on match
                    comp.setVisible(matches);
                }
            }
            
            chatListPanel.revalidate();
            chatListPanel.repaint();
        });
    }
    
    private void showAllChatsFromCache() {
        SwingUtilities.invokeLater(() -> {
            // Make all chat items visible
            for (Component comp : chatListPanel.getComponents()) {
                comp.setVisible(true);
            }
            
            chatListPanel.revalidate();
            chatListPanel.repaint();
        });
    }

    /**
     * Creates the chat list panel with optimized layout
     */
    private void createChatList() {
        chatListPanel = new JPanel();

        // Use BoxLayout for better performance with dynamic components
        chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
        chatListPanel.setBackground(Color.WHITE);
        chatListPanel.setBorder(BorderFactory.createEmptyBorder());

        // Add custom repaint manager to reduce unnecessary repaints
        RepaintManager repaintManager = RepaintManager.currentManager(chatListPanel);
        repaintManager.setDoubleBufferingEnabled(true);
    }


    /**
     * Creates the buttons panel at the bottom of the sidebar
     *
     * @return The buttons panel
     */
    private JPanel createSidebarButtonsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // Add new chat button with modern icon design
        Icon plusIcon = createPlusIcon();
        addChatButton = new ModernButton(plusIcon, ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.LARGE);
        addChatButton.setCornerRadius(28); // Make it circular
        addChatButton.setPreferredSize(new Dimension(56, 56));
        addChatButton.setMinimumSize(new Dimension(40, 40));
        addChatButton.setMaximumSize(new Dimension(64, 64));
        addChatButton.addActionListener(e -> showNewChatDialog());

        // Optimized hover effects for add button
        addChatButton.addMouseListener(new MouseAdapter() {
            private boolean isHovered = false;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isHovered) {
                    isHovered = true;
                    addChatButton.repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (isHovered) {
                    isHovered = false;
                    addChatButton.repaint();
                }
            }
        });

        // Create a wrapper panel to properly center the button
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonWrapper.setBackground(Color.WHITE);
        buttonWrapper.add(addChatButton);
        
        panel.add(buttonWrapper, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the main chat panel
     */
    private void createChatPanel() {
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(250, 250, 250));
        chatPanel.setBorder(BorderFactory.createEmptyBorder());

        // Create a placeholder panel for when no chat is selected
        JPanel placeholderPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw subtle background pattern
                g2.setColor(new Color(245, 245, 245));
                for (int i = 0; i < getWidth(); i += 40) {
                    for (int j = 0; j < getHeight(); j += 40) {
                        g2.drawOval(i, j, 2, 2);
                    }
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        placeholderPanel.setBackground(new Color(250, 250, 250));

        // Create welcome message
        JLabel welcomeLabel = new JLabel("Bienvenue sur VibeApp");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(80, 80, 80));

        JLabel subtitleLabel = new JLabel("Sélectionnez une conversation pour commencer");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(120, 120, 120));

        // Create icon
        JLabel iconLabel;
        File logoFile = new File("pictures/logoVibeApp.png");
        if (logoFile.exists()) {
            try {
                BufferedImage logoImage = ImageIO.read(logoFile);
                Image scaledLogo = logoImage.getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                iconLabel = new JLabel(new ImageIcon(scaledLogo));
            } catch (Exception e) {
                iconLabel = new JLabel("VibeApp");
                iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 64));
                iconLabel.setForeground(EMSI_GREEN);
            }
        } else {
            iconLabel = new JLabel("VibeApp");
            iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 64));
            iconLabel.setForeground(EMSI_GREEN);
        }

        // Layout components
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        placeholderPanel.add(iconLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        placeholderPanel.add(welcomeLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        placeholderPanel.add(subtitleLabel, gbc);

        chatPanel.add(placeholderPanel, BorderLayout.CENTER);
    }

    /**
     * Sets up the chat panel for a specific chat
     *
     * @param chatId The ID of the chat
     * @param chatName The name of the chat
     */
    private void setupChatPanel(String chatId, String chatName) {
        chatPanel.removeAll();

        currentChatId = chatId;

        // Create top chat info panel
        JPanel chatInfoPanel = createChatInfoPanel(chatId, chatName);

        // Create message area
        createMessageArea();

        // Create message input panel
        JPanel inputPanel = createMessageInputPanel();

        // Add components to chat panel
        chatPanel.add(chatInfoPanel, BorderLayout.NORTH);
        chatPanel.add(messageScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        chatPanel.revalidate();
        chatPanel.repaint();

        // Load messages
        loadMessages(chatId);
    }


    /**
     * Creates the chat info panel at the top of the chat panel
     *
     * @param chatId The ID of the chat
     * @param chatName The name of the chat
     * @return The chat info panel
     */
    private JPanel createChatInfoPanel(String chatId, String chatName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(15, 20, 15, 20)
        ));
        panel.setBackground(Color.WHITE);

        // Chat avatar - use group avatar for group chats
        AvatarPanel avatarPanel;
        if (isGroupChat(chatId)) {
            avatarPanel = createGroupAvatarWithImage(chatId, chatName, 40);
        } else {
            avatarPanel = new AvatarPanel(40, generateAvatarColor(chatId));
            if (chatName != null && !chatName.isEmpty()) {
                avatarPanel.setInitials(getInitials(chatName));
            }
        }

        // Chat info
        JPanel chatInfoPanel = new JPanel(new BorderLayout());
        chatInfoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(chatName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(EMSI_GRAY);

        // Active users panel
        activeUserPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        activeUserPanel.setOpaque(false);

        JLabel activeLabel = new JLabel("Actif maintenant");
        activeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        activeLabel.setForeground(EMSI_GREEN);

        activeUserPanel.add(activeLabel);

        chatInfoPanel.add(nameLabel, BorderLayout.NORTH);
        chatInfoPanel.add(activeUserPanel, BorderLayout.CENTER);

        // Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton infoButton = new RoundButton(loadImageIcon("pictures/more.png", 20, 20));
        infoButton.addActionListener(e -> showContactProfileDetails(chatId, chatName));

        actionsPanel.add(infoButton);

        // Add components to panel
        panel.add(avatarPanel, BorderLayout.WEST);
        panel.add(chatInfoPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.EAST);

        // Add typing indicator
        listenForTyping(chatId);

        return panel;
    }
    
    private void showContactProfileDetails(String chatId, String chatName) {
        try {
            ChatInfo chatInfo = chatInfoCache.get(chatId);
            if (chatInfo == null) return;
            
            // Check if it's a group chat
            if (isGroupChat(chatId)) {
                showGroupDetails(chatId, chatName);
            } else {
                // For direct chat, show the other user's profile
                String otherUserId = null;
                for (String userId : chatInfo.users) {
                    if (!userId.equals(currentUserId)) {
                        otherUserId = userId;
                        break;
                    }
                }
                
                if (otherUserId != null) {
                    showUserProfileDetails(otherUserId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error showing contact profile details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showUserProfileDetails(String userId) {
        // Create profile details dialog
        JDialog profileDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                           "Profil du contact", true);
        profileDialog.setSize(400, 500);
        profileDialog.setLocationRelativeTo(this);
        profileDialog.setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header with avatar and basic info
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // Get user info
        UserInfo userInfo = userInfoCache.get(userId);
        String displayName = userInfo != null ? userInfo.username : "Utilisateur";
        
        // Avatar
        AvatarPanel avatarPanel = new AvatarPanel(80, generateAvatarColor(userId));
        if (displayName != null) {
            avatarPanel.setInitials(getInitials(displayName));
        }
        avatarPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Name and info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLabel.setForeground(new Color(50, 50, 50));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        if (userInfo != null && userInfo.email != null) {
            JLabel emailLabel = new JLabel(userInfo.email);
            emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emailLabel.setForeground(new Color(120, 120, 120));
            emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            infoPanel.add(emailLabel);
        }
        
        infoPanel.add(nameLabel);
        
        headerPanel.add(avatarPanel, BorderLayout.NORTH);
        headerPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        
        JButton closeButton = createDialogButton("Fermer", EMSI_GRAY, Color.WHITE);
        closeButton.addActionListener(e -> profileDialog.dispose());
        
        buttonPanel.add(closeButton);
        
        mainPanel.add(headerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        profileDialog.setContentPane(mainPanel);
        profileDialog.setVisible(true);
    }
    
    private void showGroupDetails(String groupId, String groupName) {
        // Create group details dialog
        JDialog groupDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                         "Détails du groupe", true);
        groupDialog.setSize(450, 600);
        groupDialog.setLocationRelativeTo(this);
        groupDialog.setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header with group info
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // Group avatar
        AvatarPanel groupAvatar = createGroupAvatarWithImage(groupId, groupName, 80);
        groupAvatar.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Group name
        JLabel nameLabel = new JLabel(groupName != null ? groupName : "Groupe");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLabel.setForeground(new Color(50, 50, 50));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        headerPanel.add(groupAvatar, BorderLayout.NORTH);
        headerPanel.add(nameLabel, BorderLayout.CENTER);
        
        // Members section
        JPanel membersPanel = new JPanel(new BorderLayout());
        membersPanel.setOpaque(false);
        
        JLabel membersLabel = new JLabel("Membres du groupe");
        membersLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        membersLabel.setForeground(new Color(50, 50, 50));
        membersLabel.setBorder(new EmptyBorder(20, 0, 10, 0));
        
        JPanel membersList = new JPanel();
        membersList.setLayout(new BoxLayout(membersList, BoxLayout.Y_AXIS));
        membersList.setOpaque(false);
        
        // Load group members
        ChatInfo chatInfo = chatInfoCache.get(groupId);
        if (chatInfo != null) {
            for (String userId : chatInfo.users) {
                UserInfo userInfo = userInfoCache.get(userId);
                String memberName = userInfo != null ? userInfo.username : "Utilisateur";
                
                JLabel memberLabel = new JLabel(memberName);
                memberLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                memberLabel.setForeground(new Color(70, 70, 70));
                memberLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
                
                if (userId.equals(currentUserId)) {
                    memberLabel.setText(memberName + " (Vous)");
                    memberLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                }
                
                membersList.add(memberLabel);
            }
        }
        
        JScrollPane membersScroll = new JScrollPane(membersList);
        membersScroll.setPreferredSize(new Dimension(0, 200));
        membersScroll.setBorder(null);
        
        membersPanel.add(membersLabel, BorderLayout.NORTH);
        membersPanel.add(membersScroll, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        
        JButton addMembersButton = createDialogButton("Ajouter des membres", Color.WHITE, EMSI_GREEN);
        addMembersButton.addActionListener(e -> showAddMembersDialog(groupId, groupName, groupDialog));
        
        JButton closeButton = createDialogButton("Fermer", EMSI_GRAY, Color.WHITE);
        closeButton.addActionListener(e -> groupDialog.dispose());
        
        buttonPanel.add(addMembersButton);
        buttonPanel.add(closeButton);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(membersPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        groupDialog.setContentPane(mainPanel);
        groupDialog.setVisible(true);
    }
    
    private void showAddMembersDialog(String groupId, String groupName, JDialog parentDialog) {
        // Create modern add members dialog
        JDialog addDialog = new JDialog(parentDialog, "Ajouter des membres", true);
        addDialog.setSize(500, 650);
        addDialog.setLocationRelativeTo(parentDialog);
        addDialog.setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        // Header with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        
        // Icon
        JLabel iconLabel = new JLabel(loadImageIcon("pictures/group.png", 40, 40));
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
        
        // Title and subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Ajouter des membres");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(40, 40, 40));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("à " + groupName);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(120, 120, 120));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitleLabel);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        
        // Search section
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(new EmptyBorder(10, 0, 20, 0));
        
        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 15, 12, 15)
        ));
        searchField.setBackground(new Color(250, 250, 250));
        
        JLabel searchIcon = new JLabel(loadImageIcon("pictures/search.png", 20, 20));
        searchIcon.setBorder(new EmptyBorder(0, 10, 0, 10));
        
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        // User list with modern styling
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        List<String> selectedUsers = new ArrayList<>();
        
        // Search functionality
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterAvailableUsers(searchField.getText(), userListPanel, selectedUsers, groupId);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterAvailableUsers(searchField.getText(), userListPanel, selectedUsers, groupId);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterAvailableUsers(searchField.getText(), userListPanel, selectedUsers, groupId);
            }
        });
        
        // Action buttons with modern styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JButton cancelButton = createDialogButton("Annuler", EMSI_GRAY, Color.WHITE);
        cancelButton.addActionListener(e -> addDialog.dispose());
        
        JButton addButton = createDialogButton("Ajouter", Color.WHITE, EMSI_GREEN);
        addButton.addActionListener(e -> {
            if (selectedUsers.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog,
                    "Veuillez sélectionner au moins un membre à ajouter",
                    "Aucun membre sélectionné", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Add members to group
            boolean success = groupService.addMembersToGroup(groupId, selectedUsers, currentUserId);
            if (success) {
                JOptionPane.showMessageDialog(addDialog,
                    selectedUsers.size() + " membre(s) ajouté(s) avec succès!",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
                addDialog.dispose();
                parentDialog.dispose();
                // Refresh group details
                showGroupDetails(groupId, groupName);
            } else {
                JOptionPane.showMessageDialog(addDialog,
                    "Erreur lors de l'ajout des membres",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(addButton);
        
        // Layout
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Load available users
        loadAvailableUsers(userListPanel, selectedUsers, groupId);
        
        addDialog.setContentPane(mainPanel);
        addDialog.setVisible(true);
    }
    
    private void loadAvailableUsers(JPanel userListPanel, List<String> selectedUsers, String groupId) {
        try {
            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userListPanel.removeAll();
                    
                    // Get current group members
                    Set<String> groupMembers = new HashSet<>();
                    ChatInfo chatInfo = chatInfoCache.get(groupId);
                    if (chatInfo != null) {
                        groupMembers.addAll(chatInfo.users);
                    }
                    
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        
                        // Skip if user is already in the group
                        if (userId != null && !groupMembers.contains(userId)) {
                            String username = userSnapshot.child("username").getValue(String.class);
                            if (username == null || username.isEmpty()) {
                                String nom = userSnapshot.child("nom").getValue(String.class);
                                String prenom = userSnapshot.child("prenom").getValue(String.class);
                                username = prenom + " " + nom;
                            }
                            
                            String email = userSnapshot.child("email").getValue(String.class);
                            
                            ModernUserSelectItem userItem = new ModernUserSelectItem(userId, username, email);
                            userItem.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    userItem.toggleSelection();
                                    if (userItem.isSelected()) {
                                        selectedUsers.add(userId);
                                    } else {
                                        selectedUsers.remove(userId);
                                    }
                                }
                            });
                            
                            userListPanel.add(userItem);
                        }
                    }
                    
                    userListPanel.revalidate();
                    userListPanel.repaint();
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading users: " + databaseError.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error loading available users: " + e.getMessage());
        }
    }
    
    private void filterAvailableUsers(String searchText, JPanel userListPanel, List<String> selectedUsers, String groupId) {
        String filter = searchText != null ? searchText.toLowerCase().trim() : "";
        
        for (Component comp : userListPanel.getComponents()) {
            if (comp instanceof ModernUserSelectItem) {
                ModernUserSelectItem userItem = (ModernUserSelectItem) comp;
                boolean matches = filter.isEmpty() || 
                    (userItem.getUsername() != null && userItem.getUsername().toLowerCase().contains(filter)) ||
                    (userItem.getEmail() != null && userItem.getEmail().toLowerCase().contains(filter));
                
                comp.setVisible(matches);
            }
        }
        
        userListPanel.revalidate();
        userListPanel.repaint();
    }
    
    private class ModernUserSelectItem extends JPanel {
        private final String userId;
        private final String username;
        private final String email;
        private final String nom;
        private final String prenom;
        private boolean isSelected = false;
        private final JPanel selectionIndicator;
        
        public ModernUserSelectItem(String userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.nom = null;
            this.prenom = null;
            
            setLayout(new BorderLayout(15, 0));
            setPreferredSize(new Dimension(0, 70));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(245, 245, 245)),
                new EmptyBorder(15, 20, 15, 20)
            ));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Selection indicator (left border)
            selectionIndicator = new JPanel();
            selectionIndicator.setPreferredSize(new Dimension(4, 40));
            selectionIndicator.setBackground(new Color(0, 0, 0, 0));
            selectionIndicator.setOpaque(true);
            
            // Avatar
            AvatarPanel avatarPanel = new AvatarPanel(45, generateAvatarColor(userId));
            if (username != null && !username.isEmpty()) {
                avatarPanel.setInitials(getInitials(username));
            }
            
            // User info panel
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
            
            // Username
            JLabel nameLabel = new JLabel(username != null ? username : "Utilisateur");
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            nameLabel.setForeground(new Color(40, 40, 40));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Email
            JLabel emailLabel = new JLabel(email != null ? email : "");
            emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            emailLabel.setForeground(new Color(120, 120, 120));
            emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(3));
            infoPanel.add(emailLabel);
            
            // Checkbox indicator
            JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            checkboxPanel.setOpaque(false);
            checkboxPanel.setPreferredSize(new Dimension(30, 30));
            
            JLabel checkboxLabel = new JLabel("○");
            checkboxLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            checkboxLabel.setForeground(new Color(180, 180, 180));
            checkboxPanel.add(checkboxLabel);
            
            // Layout - combine avatar and selection indicator in left panel
            JPanel leftPanel = new JPanel(new BorderLayout(0, 0));
            leftPanel.setOpaque(false);
            leftPanel.add(selectionIndicator, BorderLayout.WEST);
            leftPanel.add(avatarPanel, BorderLayout.CENTER);
            
            add(leftPanel, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
            add(checkboxPanel, BorderLayout.EAST);
            
            // Hover effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(new Color(248, 250, 252));
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(Color.WHITE);
                    }
                }
            });
        }
        
        public void toggleSelection() {
            isSelected = !isSelected;
            updateAppearance();
        }
        
        private void updateAppearance() {
            if (isSelected) {
                setBackground(new Color(236, 248, 244));
                selectionIndicator.setBackground(EMSI_GREEN);
                
                // Update checkbox
                Component[] components = ((JPanel) getComponent(3)).getComponents();
                if (components.length > 0 && components[0] instanceof JLabel) {
                    JLabel checkboxLabel = (JLabel) components[0];
                    checkboxLabel.setText("●");
                    checkboxLabel.setForeground(EMSI_GREEN);
                }
            } else {
                setBackground(Color.WHITE);
                selectionIndicator.setBackground(new Color(0, 0, 0, 0));
                
                // Update checkbox
                Component[] components = ((JPanel) getComponent(3)).getComponents();
                if (components.length > 0 && components[0] instanceof JLabel) {
                    JLabel checkboxLabel = (JLabel) components[0];
                    checkboxLabel.setText("○");
                    checkboxLabel.setForeground(new Color(180, 180, 180));
                }
            }
            repaint();
        }
        
        public boolean isSelected() {
            return isSelected;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getEmail() {
            return email;
        }
    }

    /**
     * Creates the message area for displaying chat messages
     */
    private void createMessageArea() {
        messageArea = new JTextPane();
        messageArea.setEditable(false);
        messageArea.setBackground(Color.WHITE);
        messageArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Allow inserting components like our custom message bubbles
        messageArea.setEditorKit(new javax.swing.text.StyledEditorKit());

        // Create scroll pane with custom scrollbar
        messageScrollPane = createScrollPane(messageArea);

        // Set auto-scroll policy
        messageScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!e.getValueIsAdjusting()) {
                // Auto-scroll to bottom when new content is added
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            }
        });
    }

    /**
     * Creates the message input panel
     */
    private JPanel createMessageInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        // Create message field with modern rounded styling
        messageField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                
                // Draw border
                if (hasFocus()) {
                    g2.setColor(EMSI_GREEN);
                    g2.setStroke(new BasicStroke(2));
                } else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.setStroke(new BasicStroke(1));
                }
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        messageField.setBackground(new Color(248, 249, 250));
        messageField.setForeground(new Color(50, 50, 50));
        messageField.setCaretColor(EMSI_GREEN);
        messageField.setOpaque(false);

        // Add focus effects to message field
        messageField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                messageField.setBackground(Color.WHITE);
                messageField.repaint();
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                messageField.setBackground(new Color(248, 249, 250));
                messageField.repaint();
            }
        });

        // Create button panel with improved spacing
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // Attach button with enhanced styling
        attachButton = createMessageButton(loadImageIcon("pictures/attachment.png", 20, 20), "Joindre un fichier");
        attachButton.addActionListener(e -> {
            // TODO: Implement file attachment
            JOptionPane.showMessageDialog(this, "Fonctionnalité de pièce jointe à venir", "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        // Emoji button with enhanced styling
        emojiButton = createMessageButton(loadImageIcon("pictures/emoji.png", 20, 20), "Emoji");
        emojiButton.addActionListener(e -> {
            // TODO: Implement emoji picker
            JOptionPane.showMessageDialog(this, "Sélecteur d'emoji à venir", "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        // Send button with modern icon design
        ImageIcon sendIcon = loadImageIcon("pictures/send.png", 20, 20);
        sendButton = new JButton(sendIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw circular background
                Color bgColor;
                if (getModel().isPressed()) {
                    bgColor = EMSI_GREEN_DARK;
                } else if (getModel().isRollover()) {
                    bgColor = EMSI_GREEN_LIGHT;
                } else {
                    bgColor = EMSI_GREEN;
                }
                
                g2.setColor(bgColor);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 20, 20);
                
                // Add subtle shadow
                g2.setColor(new Color(0, 0, 0, 30));
                g2.drawRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 18, 18);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        sendButton.setToolTipText("Envoyer le message");
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(EMSI_GREEN);
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        sendButton.setContentAreaFilled(false);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setPreferredSize(new Dimension(44, 44));
        sendButton.addActionListener(e -> sendMessage());

        // Optimized hover effects for send button
        sendButton.addMouseListener(new MouseAdapter() {
            private boolean isHovered = false;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isHovered) {
                    isHovered = true;
                    sendButton.repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (isHovered) {
                    isHovered = false;
                    sendButton.repaint();
                }
            }
        });

        // Add components to button panel
        buttonPanel.add(attachButton);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);

        // Add components to main panel
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        // Add enhanced key listener for keyboard shortcuts
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Enter to send message
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
                // Shift+Enter for new line (allow default behavior)
                else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                    // Allow default behavior for new line
                }
                // Escape to clear message field
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    e.consume();
                    messageField.setText("");
                    updateTypingStatus(false);
                }
                // Ctrl+A to select all text
                else if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                    e.consume();
                    messageField.selectAll();
                }
            }
        });

        // Add document listener for typing indicator
        messageField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateTypingStatus(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateTypingStatus(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateTypingStatus(true);
            }
        });

        return panel;
    }

    /**
     * Creates a styled message button
     */
    private JButton createMessageButton(ImageIcon icon, String tooltip) {
        JButton button = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded background on hover
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2.setColor(new Color(EMSI_GREEN.getRed(), EMSI_GREEN.getGreen(), EMSI_GREEN.getBlue(), 
                                          getModel().isPressed() ? 40 : 25));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
                    
                    // Draw subtle border
                    g2.setColor(new Color(EMSI_GREEN.getRed(), EMSI_GREEN.getGreen(), EMSI_GREEN.getBlue(), 80));
                    g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
                }
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(44, 44));
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add subtle animation effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
        
        return button;
    }

    /**
     * Loads the list of chats from Firebase
     */
    private void loadChats() {
        try {
            // Reference to users/<userId>/chats to get the list of chat IDs
            DatabaseReference userChatsRef = firebaseService.getDatabase()
                    .getReference("users/" + currentUserId + "/chats");

            userChatsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Store current chats to avoid clearing if no changes
                    Set<String> newChatIds = new HashSet<>();
                    for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        if (chatId != null) {
                            newChatIds.add(chatId);
                        }
                    }
                    
                    // Only clear and reload if there are actual changes
                    Set<String> currentChatIds = new HashSet<>();
                    for (Component comp : chatListPanel.getComponents()) {
                        if (comp instanceof ChatItem) {
                            currentChatIds.add(((ChatItem) comp).getChatId());
                        }
                    }
                    
                    if (!newChatIds.equals(currentChatIds)) {
                        SwingUtilities.invokeLater(() -> {
                            chatListPanel.removeAll();
                            chatListPanel.revalidate();
                            chatListPanel.repaint();
                        });
                        
                        for (String chatId : newChatIds) {
                            loadChatInfo(chatId);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading chats: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading chats: " + e.getMessage());
        }
    }

    /**
     * Loads information about a specific chat from Firebase
     *
     * @param chatId The ID of the chat
     */
    /**
     * Loads information about a specific chat from Firebase with improved caching
     *
     * @param chatId The ID of the chat
     */
    private void loadChatInfo(String chatId) {
        try {
            // Reference to the chat data
            DatabaseReference chatRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId);

            chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get users list
                        ArrayList<String> users = new ArrayList<>();
                        String chatName = null;

                        // Handle different data structures based on how users are stored
                        Object usersObj = dataSnapshot.child("users").getValue();
                        if (usersObj instanceof ArrayList) {
                            // If users is an ArrayList
                            ArrayList<String> usersList = (ArrayList<String>) usersObj;
                            users.addAll(usersList);
                        } else if (usersObj instanceof HashMap) {
                            // If users is a HashMap
                            HashMap<String, String> usersMap = (HashMap<String, String>) usersObj;
                            users.addAll(usersMap.values());
                        }

                        // Get other info
                        long createdAt = dataSnapshot.child("createdAt").getValue(Long.class) != null ?
                                dataSnapshot.child("createdAt").getValue(Long.class) : 0;

                        // Get chat name
                        chatName = dataSnapshot.child("name").getValue(String.class);

                        // Get last message
                        String lastMessageText = "";
                        long lastMessageTime = 0;

                        // Check if there are messages
                        DataSnapshot messagesSnapshot = dataSnapshot.child("messages");
                        if (messagesSnapshot.exists() && messagesSnapshot.getChildrenCount() > 0) {
                            // Get the last message - more efficient approach
                            DataSnapshot lastMessageSnapshot = null;

                            // Use Query to get last message ordered by timestamp
                            for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                                lastMessageSnapshot = messageSnapshot;
                            }

                            if (lastMessageSnapshot != null) {
                                lastMessageText = lastMessageSnapshot.child("text").getValue(String.class);
                                lastMessageTime = lastMessageSnapshot.child("timestamp").getValue(Long.class) != null ?
                                        lastMessageSnapshot.child("timestamp").getValue(Long.class) : 0;
                            }
                        }

                        // Store chat info in cache with chat name
                        final ChatInfo chatInfo = new ChatInfo(chatId, users, createdAt, lastMessageText, lastMessageTime);
                        chatInfo.chatName = chatName;
                        chatInfoCache.put(chatId, chatInfo);

                        // Get other user info for display (for private chats)
                        if (users.size() == 2) {
                            String otherUserId = users.get(0).equals(currentUserId) ? users.get(1) : users.get(0);
                            loadUserInfo(otherUserId, userInfo ->
                                    createOrUpdateChatItem(chatId, userInfo.username, chatInfo.lastMessageText, chatInfo.lastMessageTime, otherUserId)
                            );
                        } else {
                            // For group chats
                            if (chatName == null || chatName.isEmpty()) {
                                chatName = "Discussion de groupe";
                            }
                            final String finalChatName = chatName;
                            createOrUpdateChatItem(chatId, finalChatName, chatInfo.lastMessageText, chatInfo.lastMessageTime, null);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading chat info: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading chat info: " + e.getMessage());
        }
    }

    /**
     * Opens a chat with the specified ID
     * @param chatId The chat ID to open
     */
    private void openChat(String chatId) {
        // Update selection state for all chat items
        for (Component comp : chatListPanel.getComponents()) {
            if (comp instanceof ChatItem) {
                ChatItem chatItem = (ChatItem) comp;
                if (chatItem.getChatId().equals(chatId)) {
                    chatItem.setSelected(true);
                    // Get the chat name from the selected item
                    String chatName = chatItem.getChatName();
                    setupChatPanel(chatId, chatName);
                } else {
                    chatItem.setSelected(false);
                }
            }
        }
    }

    /**
     * Loads user contacts
     * @return List of contact users
     */
    private List<User> loadContacts() {
        List<User> contacts = new ArrayList<>();

        try {
            // Reference to all users except current user
            DatabaseReference usersRef = firebaseService.getDatabase()
                    .getReference("users");

            CountDownLatch latch = new CountDownLatch(1);

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();

                        // Skip current user
                        if (userId != null && !userId.equals(currentUserId)) {
                            String username = userSnapshot.child("username").getValue(String.class);

                            // If username is not set, use nom + prenom
                            if (username == null || username.isEmpty()) {
                                String nom = userSnapshot.child("nom").getValue(String.class);
                                String prenom = userSnapshot.child("prenom").getValue(String.class);
                                username = prenom + " " + nom;
                            }

                            String email = userSnapshot.child("email").getValue(String.class);

                            User user = new User(userId, username, email);
                            contacts.add(user);
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contacts;
    }

    /**
     * Gets a user by ID
     * @param userId The user ID
     * @return The User object
     */
    private User getUserById(String userId) {
        if (userInfoCache.containsKey(userId)) {
            UserInfo userInfo = userInfoCache.get(userId);
            return new User(userInfo.userId, userInfo.username, userInfo.email);
        }

        // Default return if not found
        return new User(userId, "Unknown User", "");
    }

    /**
     * Loads information about a specific user from Firebase
     *
     * @param userId The ID of the user
     * @param callback Callback to be called when user info is loaded
     */
    private void loadUserInfo(String userId, UserInfoCallback callback) {
        // Check if we already have this user's info
        if (userInfoCache.containsKey(userId)) {
            callback.onUserInfoLoaded(userInfoCache.get(userId));
            return;
        }

        try {
            DatabaseReference userRef = firebaseService.getDatabase()
                    .getReference("users/" + userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);

                        // If username is not set, use the combination of nom and prenom
                        if (username == null || username.isEmpty()) {
                            String nom = dataSnapshot.child("nom").getValue(String.class);
                            String prenom = dataSnapshot.child("prenom").getValue(String.class);
                            username = prenom + " " + nom;
                        }

                        String email = dataSnapshot.child("email").getValue(String.class);

                        UserInfo userInfo = new UserInfo(userId, username, email);
                        userInfoCache.put(userId, userInfo);

                        callback.onUserInfoLoaded(userInfo);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading user info: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading user info: " + e.getMessage());
        }
    }

    /**
     * Creates or updates a chat item in the chat list
     *
     * @param chatId The ID of the chat
     * @param chatName The name of the chat
     * @param lastMessage The last message in the chat
     * @param timestamp The timestamp of the last message
     * @param userId The user ID for loading profile image (for private chats)
     */
    private void createOrUpdateChatItem(String chatId, String chatName, String lastMessage, long timestamp, String userId) {
        // Synchronize to prevent race conditions with UI updates
        SwingUtilities.invokeLater(() -> {
            // Remove existing chat item if it exists
            Component[] components = chatListPanel.getComponents();
            for (int i = components.length - 1; i >= 0; i--) {
                Component comp = components[i];
                if (comp instanceof ChatItem && ((ChatItem) comp).getChatId().equals(chatId)) {
                    chatListPanel.remove(comp);
                    break;
                }
            }

        // Use the chat name as is - the avatar will show if it's a group
        final String finalChatName = chatName;

        // Create new chat item
        ChatItem chatItem = new ChatItem(chatId, finalChatName, lastMessage, timestamp, userId);

        // Add click listener
        chatItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Component comp : chatListPanel.getComponents()) {
                    if (comp instanceof ChatItem) {
                        ((ChatItem) comp).setSelected(false);
                    }
                }
                chatItem.setSelected(true);
                setupChatPanel(chatId, finalChatName);
            }
        });

        // Optimized insertion - only sort when necessary
        if (timestamp > 0) {
            boolean added = false;
            Component[] chatComponents = chatListPanel.getComponents();

            // First check if this should be the very first item (most recent)
            if (chatComponents.length > 0 && chatComponents[0] instanceof ChatItem) {
                ChatItem firstItem = (ChatItem) chatComponents[0];
                if (timestamp > firstItem.getTimestamp()) {
                    chatListPanel.add(chatItem, 0);
                    added = true;
                }
            }

            // If not added at the beginning, insert in correct position
            if (!added) {
                for (int i = 0; i < chatComponents.length; i++) {
                    if (chatComponents[i] instanceof ChatItem) {
                        ChatItem existingItem = (ChatItem) chatComponents[i];
                        if (timestamp > existingItem.getTimestamp()) {
                            chatListPanel.add(chatItem, i);
                            added = true;
                            break;
                        }
                    }
                }

                // If still not added, add at the end
                if (!added) {
                    chatListPanel.add(chatItem);
                }
            }
        } else {
            // If no timestamp, just add at the end
            chatListPanel.add(chatItem);
            }

            chatListPanel.revalidate();
            chatListPanel.repaint();
        });
    }

    /**
     * Loads messages for a chat from Firebase
     *
     * @param chatId The ID of the chat
     */
    /**
     * Loads messages for a chat from Firebase
     *
     * @param chatId The ID of the chat
     */
    private void loadMessages(String chatId) {
        try {
            // Remove previous listener if exists
            if (currentMessagesListener != null && currentChatId != null) {
                DatabaseReference oldRef = firebaseService.getDatabase()
                    .getReference("chats/" + currentChatId + "/messages");
                oldRef.removeEventListener(currentMessagesListener);
            }

            // Reference to the chat messages           
            DatabaseReference messagesRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages");
            
            Query orderedMessagesQuery = messagesRef.orderByChild("timestamp");

            currentMessagesListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    SwingUtilities.invokeLater(() -> {
                        // Clear message area
                        messageArea.setText("");

                    // Process all messages
                    List<MessageInfo> messages = new ArrayList<>();

                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        try {
                            String messageId = messageSnapshot.getKey();
                            String messageText = messageSnapshot.child("text").getValue(String.class);
                            String senderId = messageSnapshot.child("userId").getValue(String.class);
                            Long timestampObj = messageSnapshot.child("timestamp").getValue(Long.class);
                            long timestamp = (timestampObj != null) ? timestampObj : System.currentTimeMillis();

                            // Skip messages with missing required fields
                            if (messageId == null || messageText == null || senderId == null) {
                                System.err.println("Skipping message with missing fields: " + messageId);
                                continue;
                            }

                            messages.add(new MessageInfo(messageId, messageText, senderId, timestamp));
                        } catch (Exception e) {
                            System.err.println("Error processing message: " + e.getMessage());
                        }
                    }

                    // Sort messages by timestamp
                    messages.sort(Comparator.comparing(MessageInfo::getTimestamp));

                    // Display messages immediately with cached or placeholder user info
                    for (MessageInfo message : messages) {
                        UserInfo userInfo = userInfoCache.get(message.senderId);
                        if (userInfo != null) {
                            addMessageToDisplay(messageArea.getStyledDocument(), message, userInfo);
                        } else {
                            // Use a placeholder UserInfo (e.g., "Loading..." as username)
                            UserInfo placeholder = new UserInfo(message.senderId, "Loading...", "");
                            addMessageToDisplay(messageArea.getStyledDocument(), message, placeholder);

                            // Asynchronously load user info, but do NOT refresh all messages
                            loadUserInfo(message.senderId, loadedUserInfo -> {
                                // Optionally, you could update just this message bubble if you keep references
                                // For now, do nothing to avoid disorder
                            });
                        }
                    }

                        SwingUtilities.invokeLater(() -> {
                            JScrollBar vertical = messageScrollPane.getVerticalScrollBar();
                            vertical.setValue(vertical.getMaximum());
                        });
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading messages: " + databaseError.getMessage());
                }
            };

            orderedMessagesQuery.addValueEventListener(currentMessagesListener);
            currentChatId = chatId; // update the current chat id

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading messages: " + e.getMessage());
        }
    }

    /**
     * Adds a message to the message display
     *
     * @param doc The styled document to add the message to
     * @param message The message to add
     * @param sender The sender of the message
     */
    private void addMessageToDisplay(StyledDocument doc, MessageInfo message, UserInfo sender) {
        try {
            boolean isMyMessage = message.senderId.equals(currentUserId);

            // Create custom message bubble panel with rounded corners
            JPanel bubblePanel = new JPanel(new BorderLayout(5, 2));
            bubblePanel.setOpaque(false);

            // Set different background colors based on sender
            JPanel messagePanel = new RoundedPanel(10, isMyMessage ? MESSAGE_BG_MINE : MESSAGE_BG_OTHERS);
            messagePanel.setLayout(new BorderLayout(5, 0));
            messagePanel.setBorder(new EmptyBorder(8, 12, 8, 12));

            // Sender name at top if not the current user
            if (!isMyMessage) {
                JLabel nameLabel = new JLabel(sender.username);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                nameLabel.setForeground(EMSI_GRAY);
                messagePanel.add(nameLabel, BorderLayout.NORTH);
            }

            // Message content
            JTextArea textArea = new JTextArea(message.text);
            textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setBackground(new Color(0, 0, 0, 0)); // Transparent
            textArea.setBorder(null);

            // Optimize sizing calculation
            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            int maxWidth = 300; // Reasonable max width for messages

            // Split by line breaks for proper height calculation
            String[] lines = message.text.split("\n");
            int width = 10; // Minimum padding

            // Calculate required width based on longest line
            for (String line : lines) {
                int lineWidth = fm.stringWidth(line) + 20; // Add padding
                width = Math.max(width, Math.min(lineWidth, maxWidth));
            }

            // Calculate height - consider word wrapping
            int lineCount = 0;
            for (String line : lines) {
                int lineWidth = fm.stringWidth(line);
                if (lineWidth <= width - 20) {
                    lineCount++;
                } else {
                    // Estimate wrapped lines
                    lineCount += Math.ceil((double)lineWidth / (width - 20));
                }
            }

            int height = fm.getHeight() * Math.max(lineCount, 1) + 10;

            textArea.setPreferredSize(new Dimension(width, height));
            messagePanel.add(textArea, BorderLayout.CENTER);

            // Time at bottom
            JLabel timeLabel = new JLabel(timeFormat.format(new Date(message.timestamp)));
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(EMSI_GRAY_LIGHT);
            timeLabel.setHorizontalAlignment(isMyMessage ? SwingConstants.RIGHT : SwingConstants.LEFT);
            messagePanel.add(timeLabel, BorderLayout.SOUTH);

            // Avatar with profile image
            AvatarPanel avatarPanel = new AvatarPanel(30, generateAvatarColor(message.senderId));
            avatarPanel.setInitials(getInitials(sender.username));
            
            // Load user profile image
            loadUserProfileImage(avatarPanel, message.senderId);

            // Layout different for my messages vs others
            if (isMyMessage) {
                bubblePanel.add(Box.createHorizontalStrut(50), BorderLayout.WEST);
                bubblePanel.add(messagePanel, BorderLayout.CENTER);
                bubblePanel.add(avatarPanel, BorderLayout.EAST);
            } else {
                bubblePanel.add(avatarPanel, BorderLayout.WEST);
                bubblePanel.add(messagePanel, BorderLayout.CENTER);
                bubblePanel.add(Box.createHorizontalStrut(50), BorderLayout.EAST);
            }

            // Add spacing between messages
            bubblePanel.setBorder(new EmptyBorder(5, 10, 5, 10));

            // Insert the component into document
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setOpaque(false);
            wrapperPanel.add(bubblePanel, isMyMessage ? BorderLayout.EAST : BorderLayout.WEST);

            // Use a separate thread for UI updates to prevent blocking
            SwingUtilities.invokeLater(() -> {
                try {
                    // Insert into document
                    messageArea.setCaretPosition(doc.getLength());
                    messageArea.insertComponent(wrapperPanel);
                    doc.insertString(doc.getLength(), "\n", null);

                    // Scroll to bottom - important to do this after component insertion
                    JScrollBar vertical = messageScrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Debouncing for send messages
    private long lastSendTime = 0;
    private static final long SEND_DEBOUNCE_MS = 500;
    
    /**
     * Sends a message to the current chat (optimized)
     */
    private void sendMessage() {
        // Debouncing to prevent spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSendTime < SEND_DEBOUNCE_MS) {
            return;
        }
        lastSendTime = currentTime;
        
        String message = messageField.getText().trim();
        
        // Quick validation
        if (message.isEmpty()) {
            messageField.requestFocus();
            return;
        }
        
        if (currentChatId == null) {
            return;
        }

        // Immediate UI feedback
        String messageToSend = message;
        messageField.setText("");
        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        
        // Stop typing status immediately
        updateTypingStatus(false);
        
        // Perform Firebase operations asynchronously
        SwingWorker<Void, Void> sendWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Create message object
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("userId", currentUserId);
                    messageData.put("username", currentUsername);
                    messageData.put("text", messageToSend);
                    messageData.put("timestamp", ServerValue.TIMESTAMP);
                    messageData.put("type", "text");

                    // Send to Firebase
                    DatabaseReference messagesRef = firebaseService.getDatabase()
                            .getReference("chats/" + currentChatId + "/messages");
                    
                    String messageId = messagesRef.push().getKey();
                    if (messageId != null) {
                        messagesRef.child(messageId).updateChildren(messageData, (error, ref) -> {
                            if (error != null) {
                                System.err.println("Error sending message: " + error.getMessage());
                            }
                        });
                        
                        // Update last message info in background
                        Map<String, Object> lastMessageUpdate = new HashMap<>();
                        lastMessageUpdate.put("lastMessageText", messageToSend);
                        lastMessageUpdate.put("lastMessageTime", ServerValue.TIMESTAMP);
                        lastMessageUpdate.put("lastMessageUserId", currentUserId);

                        firebaseService.getDatabase()
                                .getReference("chats/" + currentChatId)
                                .updateChildren(lastMessageUpdate, (error, ref) -> {
                                    if (error != null) {
                                        System.err.println("Error updating last message info: " + error.getMessage());
                                    }
                                });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                return null;
            }
            
            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    messageField.setEnabled(true);
                    sendButton.setEnabled(true);
                    messageField.requestFocus();
                    
                    try {
                        get(); // Check for exceptions
                    } catch (Exception e) {
                        // Restore message on error
                        messageField.setText(messageToSend);
                        System.err.println("Error sending message: " + e.getMessage());
                    }
                });
            }
        };
        sendWorker.execute();
    }

    /**
     * Sets up the typing timer (optimized)
     */
    private void setupTypingTimer() {
        if (typingTimer != null) {
            typingTimer.stop();
        }
        typingTimer = new javax.swing.Timer(TYPING_TIMEOUT, e -> {
            updateTypingStatus(false);
        });
        typingTimer.setRepeats(false);
    }
    
    /**
     * Cleanup method to prevent memory leaks
     */
    public void cleanup() {
        if (typingTimer != null) {
            typingTimer.stop();
            typingTimer = null;
        }
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        
        // Clear caches
        userInfoCache.clear();
        chatInfoCache.clear();
        userCache.clear();
        imageCache.clear();
        
        // Remove Firebase listeners
        if (currentMessagesListener != null && firebaseService != null) {
            try {
                firebaseService.getDatabase()
                    .getReference("chats/" + currentChatId + "/messages")
                    .removeEventListener(currentMessagesListener);
            } catch (Exception e) {
                System.err.println("Error removing message listener: " + e.getMessage());
            }
            currentMessagesListener = null;
        }
    }

    /**
     * Handles responsive layout changes based on window size
     */
    private void handleResponsiveLayout() {
        SwingUtilities.invokeLater(() -> {
            int width = getWidth();
            int height = getHeight();
            
            // Enhanced responsive breakpoints
            if (width > 0 && height > 0) { // Ensure valid dimensions
                // Auto-collapse sidebar based on screen size  
                if (width < 800) { // Mobile/tablet threshold - consistent with sidebar creation
                    if (!isSidebarCollapsed) {
                        toggleSidebar();
                    }
                } else if (width > 1000) { // Desktop threshold
                    if (isSidebarCollapsed) {
                        toggleSidebar();
                    }
                }
                
                // Adjust component sizes for different screen sizes
                adjustComponentSizes(width, height);
                
                // Adjust padding and margins for small screens
                adjustSpacing(width, height);
                
                revalidate();
                repaint();
            }
        });
    }

    /**
     * Adjusts component sizes based on screen dimensions
     */
    private void adjustComponentSizes(int width, int height) {
        // Define size breakpoints for better responsive design
        boolean isSmall = width < 600;    // Mobile
        boolean isMedium = width < 900;   // Tablet
        boolean isLarge = width >= 1200;  // Desktop
        
        // Adjust message field font size with more granular control
        if (messageField != null) {
            int fontSize = isSmall ? 12 : isMedium ? 13 : 14;
            messageField.setFont(new Font("Segoe UI", Font.PLAIN, fontSize));
        }
        
        // Adjust message area font size
        if (messageArea != null) {
            int fontSize = isSmall ? 11 : isMedium ? 12 : 13;
            Font currentFont = messageArea.getFont();
            if (currentFont != null) {
                messageArea.setFont(new Font(currentFont.getName(), currentFont.getStyle(), fontSize));
            }
        }
        
        // Adjust button sizes for touch-friendly interface
        int buttonSize = isSmall ? 48 : isMedium ? 46 : 44;
        
        if (sendButton != null) {
            sendButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        }
        
        if (attachButton != null) {
            attachButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        }
        
        if (emojiButton != null) {
            emojiButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        }
    }
    
    /**
     * Adjusts spacing and padding based on screen size
     */
    private void adjustSpacing(int width, int height) {
        // This method can be expanded to adjust margins and padding
        // For now, it's a placeholder for future spacing adjustments
        boolean isSmall = width < 600;
        
        // Adjust sidebar minimum width for very small screens
        if (isSmall && sidebarPanel != null) {
            // Could reduce minimum sidebar width for very small screens
            sidebarPanel.setMinimumSize(new Dimension(COLLAPSED_SIDEBAR_WIDTH - 10, 0));
        }
    }

    /**
     * Updates the user's typing status in Firebase (optimized)
     *
     * @param isTyping Whether the user is typing
     */
    private void updateTypingStatus(boolean isTyping) {
        this.isTyping = isTyping;

        if (isTyping) {
            if (typingTimer != null) {
                typingTimer.restart();
            }
        } else {
            if (typingTimer != null) {
                typingTimer.stop();
            }
        }

        // Only update Firebase if status actually changed
        if (lastTypingStatus != isTyping && currentChatId != null) {
            lastTypingStatus = isTyping;
            
            // Update typing status in Firebase asynchronously
            SwingWorker<Void, Void> typingWorker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        DatabaseReference typingRef = firebaseService.getDatabase()
                                .getReference("chats/" + currentChatId + "/typing/" + currentUserId);
                        typingRef.setValue(isTyping, (error, ref) -> {
                            if (error != null) {
                                System.err.println("Error updating typing status: " + error.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Error updating typing status: " + e.getMessage());
                    }
                    return null;
                }
            };
            typingWorker.execute();
        }
    }

    /**
     * Listens for typing status changes in Firebase
     *
     * @param chatId The ID of the chat
     */
    private void listenForTyping(String chatId) {
        try {
            DatabaseReference typingRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/typing");

            typingRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean someoneTyping = false;
                    List<String> typingUsers = new ArrayList<>();

                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        Boolean isTyping = userSnapshot.getValue(Boolean.class);

                        if (userId != null && !userId.equals(currentUserId) &&
                                isTyping != null && isTyping) {
                            someoneTyping = true;
                            typingUsers.add(userId);
                        }
                    }

                    // Update typing indicator in UI
                    updateTypingIndicator(someoneTyping, typingUsers);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error listening for typing: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the typing indicator in the UI
     *
     * @param someoneTyping Whether someone is typing
     * @param typingUsers List of users who are typing
     */
    private void updateTypingIndicator(boolean someoneTyping, List<String> typingUsers) {
        // Remove existing typing indicator
        for (Component comp : activeUserPanel.getComponents()) {
            if (comp instanceof JLabel && ((JLabel) comp).getText().contains("en train d'écrire")) {
                activeUserPanel.remove(comp);
            }
        }

        // Add new typing indicator if someone is typing
        if (someoneTyping && typingUsers.size() > 0) {
            StringBuilder typingText = new StringBuilder();

            if (typingUsers.size() == 1) {
                String userId = typingUsers.get(0);
                UserInfo userInfo = userInfoCache.get(userId);
                String username = userInfo != null ? userInfo.username : "Quelqu'un";
                typingText.append(username).append(" est en train d'écrire...");
            } else {
                typingText.append("Plusieurs personnes sont en train d'écrire...");
            }

            JLabel typingLabel = new JLabel(typingText.toString());
            typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            typingLabel.setForeground(EMSI_GREEN);

            activeUserPanel.add(typingLabel);
        }

        activeUserPanel.revalidate();
        activeUserPanel.repaint();
    }

    /**
     * Toggles the sidebar between expanded and collapsed states
     */
    private void toggleSidebar() {
        isSidebarCollapsed = !isSidebarCollapsed;

        int targetWidth = isSidebarCollapsed ? COLLAPSED_SIDEBAR_WIDTH : EXPANDED_SIDEBAR_WIDTH;

        // Show or hide components based on sidebar state
        setSidebarComponentsVisibility(!isSidebarCollapsed);

        // Animate width change
        new Thread(() -> {
            int currentWidth = sidebarPanel.getPreferredSize().width;
            int step = (targetWidth - currentWidth) / 10;

            for (int i = 1; i <= 10; i++) {
                final int width = currentWidth + (step * i);
                SwingUtilities.invokeLater(() -> {
                    sidebarPanel.setPreferredSize(new Dimension(width, 0));
                    sidebarPanel.revalidate();
                });

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Ensure final size is exact
            SwingUtilities.invokeLater(() -> {
                sidebarPanel.setPreferredSize(new Dimension(targetWidth, 0));
                sidebarPanel.revalidate();
            });
        }).start();
    }

    /**
     * Sets the visibility of sidebar components based on the collapsed state
     * @param visible true to show components, false to hide them
     */
    private void setSidebarComponentsVisibility(boolean visible) {
        System.out.println("Setting sidebar components visibility to: " + visible);
        
        // Get all components in the sidebar
        Component[] components = sidebarPanel.getComponents();
        System.out.println("Found " + components.length + " components in sidebar");
        
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                setPanelComponentsVisibility((JPanel) comp, visible);
            }
        }
        
        // Force immediate repaint
        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    /**
     * Recursively sets visibility of components in a panel
     * @param panel The panel to process
     * @param visible true to show components, false to hide them
     */
    private void setPanelComponentsVisibility(JPanel panel, boolean visible) {
        Component[] components = panel.getComponents();
        
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                // Recursively process nested panels
                setPanelComponentsVisibility((JPanel) comp, visible);
            } else if (comp instanceof JLabel || comp instanceof JTextField || 
                       comp instanceof JButton || comp instanceof JScrollPane || 
                       comp instanceof AvatarPanel) {
                // Show/hide text components, labels, buttons, and scroll panes
                // Handle all components including AvatarPanel for responsive behavior
                if (!(comp instanceof RoundButton)) {
                    boolean shouldBeVisible = visible;
                    
                    // In collapsed mode, show only essential components
                    if (!visible) {
                        // Keep toggle button visible even when collapsed
                        if (comp instanceof JButton && 
                            (((JButton)comp).getToolTipText() != null && 
                             (((JButton)comp).getToolTipText().contains("Menu") || 
                              ((JButton)comp).getToolTipText().contains("Développer") || 
                              ((JButton)comp).getToolTipText().contains("Réduire")))) {
                            shouldBeVisible = true;
                        } else {
                            // Hide all other components including profile avatar in collapsed mode
                            shouldBeVisible = false;
                        }
                    }
                    
                    comp.setVisible(shouldBeVisible);
                }
            }
        }
    }

    /**
     * Shows the dialog for creating a new chat
     */
    private void showNewChatDialog() {
        // Create modern dialog with better styling
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Nouvelle conversation", true);
        dialog.setSize(520, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        // Create main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(0, 25)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Subtle gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(250, 252, 255),
                    0, getHeight(), new Color(240, 245, 250)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Enhanced header with welcome message
        JPanel headerPanel = createModernHeader();
        
        // Enhanced options panel
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setOpaque(false);

        // Option 1: Create Group with modern card design
        JPanel groupOptionPanel = createEnhancedOptionCard(
                "Créer un groupe",
                "Rassemblez plusieurs personnes dans une discussion de groupe",
                "👥",
                EMSI_GREEN,
                () -> {
                    dialog.dispose();
                    try {
                        showCreateGroupDialog();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                true // isPrimary
        );

        // Option 2: Direct Chat with modern card design  
        JPanel chatOptionPanel = createEnhancedOptionCard(
                "Discussion privée",
                "Commencez une conversation individuelle avec un contact",
                "💬",
                new Color(74, 144, 226),
                () -> showDirectChatCreator(dialog),
                false // isPrimary
        );

        optionsPanel.add(groupOptionPanel);
        optionsPanel.add(Box.createVerticalStrut(20));
        optionsPanel.add(chatOptionPanel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(optionsPanel, BorderLayout.CENTER);

        // Close button at bottom with modern design
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton closeButton = new JButton("Annuler") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Modern button background
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(240, 240, 240));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(248, 248, 248));
                } else {
                    g2d.setColor(Color.WHITE);
                }
                
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Border
                g2d.setColor(new Color(220, 220, 220));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        closeButton.setForeground(EMSI_GRAY);
        closeButton.setPreferredSize(new Dimension(120, 40));
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dialog.dispose());

        footerPanel.add(closeButton);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Creates the modern header panel for the new chat dialog
     */
    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(0, 15));
        headerPanel.setOpaque(false);
        
        // Main title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        titlePanel.setOpaque(false);
        
        // Chat icon
        JLabel iconLabel = new JLabel("💬");
        iconLabel.setFont(new Font("Apple Color Emoji", Font.PLAIN, 32));
        
        // Title
        JLabel titleLabel = new JLabel("Nouvelle conversation");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(new Color(45, 45, 45));
        
        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        
        // Subtitle/description
        JLabel subtitleLabel = new JLabel("Choisissez le type de conversation que vous souhaitez créer");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    /**
     * Creates an enhanced option card with modern design
     */
    private JPanel createEnhancedOptionCard(String title, String description, String emoji, 
                                           Color accentColor, Runnable action, boolean isPrimary) {
        JPanel cardPanel = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Enhanced card background with shadow effect
                if (isPrimary) {
                    // Primary card - filled background
                    GradientPaint gradient = new GradientPaint(
                        0, 0, accentColor,
                        0, getHeight(), accentColor.darker()
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 16, 16);
                    
                    // Subtle shadow
                    g2d.setColor(new Color(0, 0, 0, 15));
                    g2d.fillRoundRect(6, 6, getWidth() - 8, getHeight() - 8, 16, 16);
                } else {
                    // Secondary card - outline style
                    g2d.setColor(Color.WHITE);
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
                    
                    // Border
                    g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
                    
                    // Light shadow
                    g2d.setColor(new Color(0, 0, 0, 8));
                    g2d.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 16, 16);
                }
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        cardPanel.setOpaque(false);
        cardPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        cardPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cardPanel.setPreferredSize(new Dimension(0, 100));
        cardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        // Icon panel with emoji
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(60, 60));
        
        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("Apple Color Emoji", Font.PLAIN, 28));
        iconPanel.add(emojiLabel);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(isPrimary ? Color.WHITE : accentColor);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Description
        JLabel descLabel = new JLabel("<html><div style='width: 280px;'>" + description + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(isPrimary ? new Color(255, 255, 255, 220) : new Color(100, 100, 100));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(descLabel);
        
        // Arrow indicator
        JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        arrowPanel.setOpaque(false);
        arrowPanel.setPreferredSize(new Dimension(30, 30));
        
        JLabel arrowLabel = new JLabel("›");
        arrowLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        arrowLabel.setForeground(isPrimary ? Color.WHITE : accentColor);
        arrowPanel.add(arrowLabel);
        
        // Assembly
        cardPanel.add(iconPanel, BorderLayout.WEST);
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        cardPanel.add(arrowPanel, BorderLayout.EAST);
        
        // Enhanced hover effects
        cardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                // Brighten colors on hover
                if (isPrimary) {
                    titleLabel.setForeground(Color.WHITE);
                    arrowLabel.setForeground(Color.WHITE);
                } else {
                    titleLabel.setForeground(accentColor.brighter());
                    arrowLabel.setForeground(accentColor.brighter());
                }
                cardPanel.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // Restore original colors
                if (isPrimary) {
                    titleLabel.setForeground(Color.WHITE);
                    arrowLabel.setForeground(Color.WHITE);
                } else {
                    titleLabel.setForeground(accentColor);
                    arrowLabel.setForeground(accentColor);
                }
                cardPanel.repaint();
            }
        });
        
        return cardPanel;
    }

    // Update createOptionPanel to accept iconPath
    private JPanel createOptionPanel(String title, String description, Color accentColor, Runnable action, String iconPath) {
        JPanel panel = new JPanel(new BorderLayout(12, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background with subtle border
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 50));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.setPreferredSize(new Dimension(0, 80));

        // Icon (if provided)
        JLabel iconLabel = null;
        if (iconPath != null && !iconPath.isEmpty()) {
            ImageIcon icon = loadImageIcon(iconPath, 40, 40);
            iconLabel = new JLabel(icon);
            iconLabel.setBorder(new EmptyBorder(0, 0, 0, 12));
        }

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(accentColor);

        // Description
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(EMSI_GRAY);

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout(0, 4));
        infoPanel.setOpaque(false);
        infoPanel.add(titleLabel, BorderLayout.NORTH);
        infoPanel.add(descLabel, BorderLayout.CENTER);

        // Arrow icon
        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        arrowLabel.setForeground(accentColor);

        if (iconLabel != null) {
            panel.add(iconLabel, BorderLayout.WEST);
        }
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(arrowLabel, BorderLayout.EAST);

        // Hover effects
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                titleLabel.setForeground(accentColor.brighter());
                arrowLabel.setForeground(accentColor.brighter());
                panel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                titleLabel.setForeground(accentColor);
                arrowLabel.setForeground(accentColor);
                panel.repaint();
            }
        });

        return panel;
    }

    private void showDirectChatCreator(JDialog parentDialog) {
        parentDialog.dispose();

        // Create simple dialog for direct chat
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Discussion privée", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Choisir un contact");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(EMSI_GRAY);

        // Search field
        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(new EmptyBorder(8, 10, 8, 10));
        searchField.setBackground(EMSI_GRAY_LIGHTER);

        // User list
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);
        
        List<String> selectedUsers = new ArrayList<>();
        
        // Add search functionality
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterUsers(searchField.getText(), userListPanel, selectedUsers);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterUsers(searchField.getText(), userListPanel, selectedUsers);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterUsers(searchField.getText(), userListPanel, selectedUsers);
            }
        });

        JScrollPane scrollPane = createScrollPane(userListPanel);

        // Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.setOpaque(false);

        JButton cancelButton = createDialogButton("Annuler", EMSI_GRAY, Color.WHITE);
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton createButton = createDialogButton("Créer", Color.WHITE, EMSI_GREEN);

        createButton.addActionListener(e -> {
            if (selectedUsers.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Veuillez sélectionner un utilisateur",
                        "Erreur", JOptionPane.WARNING_MESSAGE);
                return;
            }
            createNewChat(selectedUsers, dialog);
        });

        bottomPanel.add(cancelButton);
        bottomPanel.add(createButton);

        // Create center panel to hold search and user list
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(searchField, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);

        // Load users for direct chat
        loadUsers(userListPanel, selectedUsers);

        dialog.setVisible(true);
    }

    /**
     * Shows the dialog for creating a new group
     */
    private void showCreateGroupDialog() throws IOException {
        // Create and show the simplified group creator dialog
        SimpleGroupCreator groupCreator = new SimpleGroupCreator(
                (Frame) SwingUtilities.getWindowAncestor(this),
                currentUserId
        );
        groupCreator.setVisible(true);
    }

    /**
     * Checks if a chat is a group chat
     * @param chatId The chat ID to check
     * @return true if it's a group chat
     */
    private boolean isGroupChat(String chatId) {
        // First check if we have this chat in the cache
        if (chatInfoCache.containsKey(chatId)) {
            ChatInfo chatInfo = chatInfoCache.get(chatId);
            // Check if it has more than 2 participants or has a name
            return chatInfo.users.size() > 2 || (chatInfo.chatName != null && !chatInfo.chatName.isEmpty());
        }

        // If not in cache, check with groupService
        try {
            if (groupService != null) {
                Group group = groupService.loadGroup(chatId);
                return group != null;
            }
        } catch (Exception e) {
            // If error, it's probably not a group
        }
        return false;
    }
    
    private AvatarPanel createGroupAvatarWithImage(String groupId, String groupName, int size) {
        try {
            if (groupService != null) {
                Group group = groupService.loadGroup(groupId);
                if (group != null && group.getGroupImageUrl() != null && !group.getGroupImageUrl().isEmpty()) {
                    return AvatarPanel.createGroupAvatarWithImage(groupName, group.getGroupImageUrl(), size);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading group image: " + e.getMessage());
        }
        
        // Fallback to default group avatar
        return AvatarPanel.createGroupAvatar(groupName, size);
    }


    private List<UserData> allUsers = new ArrayList<>();
    
    private class UserData {
        String userId, username, email, nom, prenom;
        
        UserData(String userId, String username, String email, String nom, String prenom) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.nom = nom;
            this.prenom = prenom;
        }
        
        boolean matchesSearch(String searchText) {
            if (searchText == null || searchText.trim().isEmpty()) return true;
            String search = searchText.toLowerCase();
            return (username != null && username.toLowerCase().contains(search)) ||
                   (email != null && email.toLowerCase().contains(search)) ||
                   (nom != null && nom.toLowerCase().contains(search)) ||
                   (prenom != null && prenom.toLowerCase().contains(search));
        }
    }

    /**
     * Creates a dialog button with modern styling
     */
    private JButton createDialogButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Button background
                if (getModel().isPressed()) {
                    g2d.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(bgColor.brighter());
                } else {
                    g2d.setColor(bgColor);
                }
                
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2d.setColor(bgColor.darker());
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(fgColor);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 36));
        
        return button;
    }
    
    /**
     * Filters users in the list based on search text
     */
    private void filterUsers(String searchText, JPanel userListPanel, List<String> selectedUsers) {
        String filter = searchText != null ? searchText.toLowerCase().trim() : "";
        
        for (Component comp : userListPanel.getComponents()) {
            if (comp instanceof UserSelectItem) {
                UserSelectItem userItem = (UserSelectItem) comp;
                boolean matches = filter.isEmpty() || 
                    (userItem.getUsername() != null && userItem.getUsername().toLowerCase().contains(filter)) ||
                    (userItem.getEmail() != null && userItem.getEmail().toLowerCase().contains(filter));
                
                comp.setVisible(matches);
            }
        }
        
        userListPanel.revalidate();
        userListPanel.repaint();
    }

    /**
     * Loads users from Firebase for the new chat dialog
     *
     * @param userListPanel The panel to add users to
     * @param selectedUsers List to store selected users
     */
    private void loadUsers(JPanel userListPanel, List<String> selectedUsers) {
        try {
            DatabaseReference usersRef = firebaseService.getDatabase()
                    .getReference("users");

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    allUsers.clear();
                    userListPanel.removeAll();

                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();

                        // Skip current user
                        if (userId != null && !userId.equals(currentUserId)) {
                            String username = userSnapshot.child("username").getValue(String.class);
                            String nom = userSnapshot.child("nom").getValue(String.class);
                            String prenom = userSnapshot.child("prenom").getValue(String.class);

                            // If username is not set, use the combination of nom and prenom
                            if (username == null || username.isEmpty()) {
                                username = prenom + " " + nom;
                            }

                            // Get email for display
                            String email = userSnapshot.child("email").getValue(String.class);

                            // Store user data for searching
                            allUsers.add(new UserData(userId, username, email, nom, prenom));

                            // Create user item
                            UserSelectItem userItem = new UserSelectItem(userId, username, email);

                            // Add click listener
                            userItem.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    userItem.toggleSelected();

                                    if (userItem.isSelected()) {
                                        selectedUsers.add(userId);
                                    } else {
                                        selectedUsers.remove(userId);
                                    }
                                }
                            });

                            userListPanel.add(userItem);
                        }
                    }

                    userListPanel.revalidate();
                    userListPanel.repaint();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading users: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    /**
     * Creates a new chat with selected users
     *
     * @param selectedUsers List of selected user IDs
     * @param dialog The dialog to close on success
     */
    private void createNewChat(List<String> selectedUsers, JDialog dialog) {
        try {
            // Add current user to the list
            List<String> allUsers = new ArrayList<>(selectedUsers);
            allUsers.add(currentUserId);

            // Create chat data
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("users", allUsers);
            chatData.put("createdAt", ServerValue.TIMESTAMP);
            chatData.put("createdBy", currentUserId);

            // Create chat
            DatabaseReference chatsRef = firebaseService.getDatabase()
                    .getReference("chats");

            String chatId = chatsRef.push().getKey();
            if (chatId != null) {
                chatsRef.child(chatId).updateChildren(chatData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error != null) {
                            System.err.println("Error creating chat: " + error.getMessage());
                            return;
                        }

                        // Add chat to each user's chats list
                        for (String userId : allUsers) {
                            DatabaseReference userChatsRef = firebaseService.getDatabase()
                                    .getReference("users/" + userId + "/chats");

                            Map<String, Object> value = new HashMap<>();
                            value.put("value", true);
                            userChatsRef.child(chatId).updateChildren(value, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError error, DatabaseReference ref) {
                                    if (error != null) {
                                        System.err.println("Error adding chat to user: " + error.getMessage());
                                    }
                                }
                            });
                        }

                        // Close dialog
                        dialog.dispose();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error creating chat: " + e.getMessage());

            JOptionPane.showMessageDialog(dialog,
                    "Erreur lors de la création de la discussion: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads an image from a file
     *
     * @param path The path to the image file
     * @return The loaded image
     */
    private Image loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createDefaultImage();
    }


    /**
     * Creates a default image for when image loading fails
     *
     * @return The default image
     */
    private Image createDefaultImage() {
        BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(EMSI_GREEN);
        g2d.fillOval(0, 0, 48, 48);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("V", 16, 32);

        g2d.dispose();

        return image;
    }

    /**
     * Loads an image icon, resizing it to the specified dimensions
     *
     * @param path The path to the image file
     * @param width The desired width
     * @param height The desired height
     * @return The loaded and resized image icon
     */
    private ImageIcon loadImageIcon(String path, int width, int height) {
        try {
            // Check if file exists first
            java.io.File iconFile = new java.io.File(path);
            if (!iconFile.exists()) {
                System.out.println("Icon file does not exist: " + iconFile.getAbsolutePath());
                return createFallbackIcon(path, width, height);
            }
            
            System.out.println("Loading icon: " + path + " (exists: " + iconFile.exists() + ", size: " + iconFile.length() + " bytes)");
            
            ImageIcon icon = new ImageIcon(path);
            if (icon.getIconWidth() <= 0) {
                // File found but icon failed to load - create appropriate fallback icon based on path
                System.out.println("Icon failed to load properly: " + path + ", creating fallback icon");
                return createFallbackIcon(path, width, height);
            }

            // Resize the icon to the specified dimensions
            Image img = icon.getImage();
            Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            System.out.println("Icon loaded successfully: " + path + " (" + icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
            return new ImageIcon(resizedImg);
        } catch (Exception e) {
            System.err.println("Error loading icon: " + path + " - " + e.getMessage());
            e.printStackTrace();
            return createFallbackIcon(path, width, height);
        }
    }
    
    /**
     * Creates appropriate fallback icons based on the original file path
     */
    private ImageIcon createFallbackIcon(String originalPath, int width, int height) {
        if (originalPath.contains("menu.png")) {
            return createMenuIcon(width, height);
        } else if (originalPath.contains("settings.png")) {
            return createSettingsIcon(width, height);
        } else if (originalPath.contains("search.png")) {
            return createSearchIcon(width, height);
        } else if (originalPath.contains("send.png")) {
            return createSendIcon(width, height);
        } else if (originalPath.contains("attachment.png")) {
            return createAttachmentIcon(width, height);
        } else if (originalPath.contains("emoji.png")) {
            return createEmojiIcon(width, height);
        } else if (originalPath.contains("more.png")) {
            return createMoreIcon(width, height);
        } else {
            return createDefaultIcon("?", EMSI_GRAY, Math.max(width, height));
        }
    }
    
    /**
     * Creates a menu icon (hamburger menu)
     */
    private ImageIcon createMenuIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        int lineWidth = width * 2 / 3;
        int startX = (width - lineWidth) / 2;
        int spacing = height / 4;
        
        // Three horizontal lines
        g2d.drawLine(startX, spacing, startX + lineWidth, spacing);
        g2d.drawLine(startX, height / 2, startX + lineWidth, height / 2);
        g2d.drawLine(startX, height - spacing, startX + lineWidth, height - spacing);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates a settings icon (gear)
     */
    private ImageIcon createSettingsIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        g2d.setStroke(new BasicStroke(1.5f));
        
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;
        
        // Outer gear teeth
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int x1 = centerX + (int) (radius * 1.2 * Math.cos(angle));
            int y1 = centerY + (int) (radius * 1.2 * Math.sin(angle));
            int x2 = centerX + (int) (radius * 0.8 * Math.cos(angle));
            int y2 = centerY + (int) (radius * 0.8 * Math.sin(angle));
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Center circle
        g2d.drawOval(centerX - radius / 2, centerY - radius / 2, radius, radius);
        g2d.fillOval(centerX - radius / 4, centerY - radius / 4, radius / 2, radius / 2);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates a search icon (magnifying glass)
     */
    private ImageIcon createSearchIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        int circleSize = Math.min(width, height) * 2 / 3;
        int circleX = width / 6;
        int circleY = height / 6;
        
        // Magnifying glass circle
        g2d.drawOval(circleX, circleY, circleSize, circleSize);
        
        // Handle
        int handleStartX = circleX + circleSize * 3 / 4;
        int handleStartY = circleY + circleSize * 3 / 4;
        int handleEndX = width - 2;
        int handleEndY = height - 2;
        g2d.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates a send icon (paper plane)
     */
    private ImageIcon createSendIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(Color.WHITE);
        
        // Paper plane shape
        int[] xPoints = {2, width - 2, width / 2, 2};
        int[] yPoints = {height / 2, 2, height / 2, height - 2};
        
        g2d.fillPolygon(xPoints, yPoints, 4);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates an attachment icon (paperclip)
     */
    private ImageIcon createAttachmentIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Paperclip shape
        int margin = 3;
        g2d.drawArc(margin, margin, width / 2, height / 2, 180, 180);
        g2d.drawLine(width / 4 + margin, height / 4 + margin, width / 4 + margin, height - margin);
        g2d.drawArc(width / 4, height / 2, width / 2, height / 2 - margin, 0, 180);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates an emoji icon (smiley face)
     */
    private ImageIcon createEmojiIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 2 - 2;
        
        // Face circle
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // Eyes
        g2d.fillOval(centerX - radius / 2, centerY - radius / 3, radius / 4, radius / 4);
        g2d.fillOval(centerX + radius / 4, centerY - radius / 3, radius / 4, radius / 4);
        
        // Smile
        g2d.drawArc(centerX - radius / 2, centerY - radius / 4, radius, radius / 2, 0, -180);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates a more icon (three dots)
     */
    private ImageIcon createMoreIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        
        int dotSize = Math.min(width, height) / 5;
        int spacing = width / 4;
        int startX = (width - spacing * 2) / 2;
        int centerY = height / 2;
        
        // Three dots
        g2d.fillOval(startX, centerY - dotSize / 2, dotSize, dotSize);
        g2d.fillOval(startX + spacing, centerY - dotSize / 2, dotSize, dotSize);
        g2d.fillOval(startX + spacing * 2, centerY - dotSize / 2, dotSize, dotSize);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates a collapse icon (left arrow)
     */
    private ImageIcon createCollapseIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Left arrow
        int centerY = height / 2;
        int arrowSize = Math.min(width, height) / 3;
        g2d.drawLine(width * 2 / 3, centerY - arrowSize, width / 3, centerY);
        g2d.drawLine(width / 3, centerY, width * 2 / 3, centerY + arrowSize);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Creates an expand icon (right arrow)
     */
    private ImageIcon createExpandIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(EMSI_GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Right arrow
        int centerY = height / 2;
        int arrowSize = Math.min(width, height) / 3;
        g2d.drawLine(width / 3, centerY - arrowSize, width * 2 / 3, centerY);
        g2d.drawLine(width * 2 / 3, centerY, width / 3, centerY + arrowSize);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Updates the toggle button icon based on sidebar state
     */
    private void updateToggleButtonIcon(JButton toggleButton) {
        ImageIcon newIcon;
        String tooltip;
        
        if (isSidebarCollapsed) {
            newIcon = createExpandIcon(18, 18);
            tooltip = "Développer le menu";
        } else {
            newIcon = createCollapseIcon(18, 18);
            tooltip = "Réduire le menu";
        }
        
        toggleButton.setIcon(newIcon);
        toggleButton.setToolTipText(tooltip);
    }

    /**
     * Creates a default icon with text when image loading fails
     *
     * @param text The text to display in the icon
     * @param color The background color
     * @param size The size of the icon
     * @return The created icon
     */
    private ImageIcon createDefaultIcon(String text, Color color, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, size/2));

        FontMetrics fm = g2d.getFontMetrics();
        int x = (size - fm.stringWidth(text)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();

        g2d.drawString(text, x, y);
        g2d.dispose();

        return new ImageIcon(image);
    }

    /**
     * Generates a color based on a user ID for avatars
     *
     * @param id The user ID
     * @return The generated color
     */
    private Color generateAvatarColor(String id) {
        if (id == null || id.isEmpty()) {
            return EMSI_GRAY;
        }

        // Generate a color based on the ID
        int hash = id.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        // Ensure the color is not too dark or too light
        r = Math.max(Math.min(r, 200), 50);
        g = Math.max(Math.min(g, 200), 50);
        b = Math.max(Math.min(b, 200), 50);

        return new Color(r, g, b);
    }

    /**
     * Checks if two dates are on the same day
     *
     * @param date1 The first date
     * @param date2 The second date
     * @return True if the dates are on the same day
     */
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Formats a timestamp for display
     *
     * @param timestamp The timestamp to format
     * @return The formatted time string
     */
    private String formatTime(long timestamp) {
        if (timestamp == 0) {
            return "";
        }

        Date date = new Date(timestamp);
        Date now = new Date();

        // Same day
        if (isSameDay(date, now)) {
            return timeFormat.format(date);
        }

        // Yesterday
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterday = calendar.getTime();

        if (isSameDay(date, yesterday)) {
            return "Hier";
        }

        // This week
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date lastWeek = calendar.getTime();

        if (date.after(lastWeek)) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
            return dayFormat.format(date);
        }

        // Older
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(date);
    }

    // ----- Inner Classes -----

    /**
     * Chat info data class
     */
    private static class ChatInfo {
        private final String chatId;
        private final List<String> users;
        private final long createdAt;
        private final String lastMessageText;
        private final long lastMessageTime;
        private String chatName; // Added chatName field

        public ChatInfo(String chatId, List<String> users, long createdAt, String lastMessageText, long lastMessageTime) {
            this.chatId = chatId;
            this.users = users;
            this.createdAt = createdAt;
            this.lastMessageText = lastMessageText;
            this.lastMessageTime = lastMessageTime;
        }
    }

    /**
     * Message info data class
     */
    private static class MessageInfo {
        private final String messageId;
        private final String text;
        private final String senderId;
        private final long timestamp;

        public MessageInfo(String messageId, String text, String senderId, long timestamp) {
            this.messageId = messageId;
            this.text = text;
            this.senderId = senderId;
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * User info data class
     */
    private static class UserInfo {
        private final String userId;
        private final String username;
        private final String email;

        public UserInfo(String userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
        }
    }

    /**
     * User info callback interface
     */
    private interface UserInfoCallback {
        void onUserInfoLoaded(UserInfo userInfo);
    }

    /**
     * Text bubble border for message fields
     */
    private class TextBubbleBorder extends AbstractBorder {
        private Color color;
        private int thickness;
        private int radii;
        private int pointerSize;
        private Insets insets;
        private BasicStroke stroke;
        private int strokePad;
        RenderingHints hints;

        TextBubbleBorder(Color color, int thickness, int radii, int pointerSize) {
            this.color = color;
            this.thickness = thickness;
            this.radii = radii;
            this.pointerSize = pointerSize;

            stroke = new BasicStroke(thickness);
            strokePad = thickness / 2;

            hints = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = radii + strokePad;
            insets = new Insets(pad, pad, pad, pad);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = this.insets.left;
            insets.top = this.insets.top;
            insets.right = this.insets.right;
            insets.bottom = this.insets.bottom;
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHints(hints);

            int bottomLineY = height - thickness;

            RoundRectangle2D.Double bubble = new RoundRectangle2D.Double(
                    strokePad, strokePad,
                    width - thickness, height - thickness,
                    radii, radii);

            g2.setColor(color);
            g2.setStroke(stroke);
            g2.draw(bubble);
        }
    }

    /**
     * Round button with hover effects
     */
    private class RoundButton extends JButton {
        public RoundButton(Icon icon) {
            super(icon);

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(36, 36));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(EMSI_GRAY_LIGHTER);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(null);
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getBackground() != null) {
                g2d.setColor(getBackground());
                g2d.fillOval(0, 0, getWidth(), getHeight());
            }

            super.paintComponent(g2d);
            g2d.dispose();
        }

        @Override
        public boolean contains(int x, int y) {
            return new Ellipse2D.Float(0, 0, getWidth(), getHeight()).contains(x, y);
        }
    }

    /**
     * Modern scroll bar UI
     */
    private class ModernScrollBarUI extends BasicScrollBarUI {
        private final Color thumbColor;
        private final Color trackColor;

        public ModernScrollBarUI(Color thumbColor, Color trackColor) {
            this.thumbColor = thumbColor;
            this.trackColor = trackColor;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(trackColor);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 10, 10);

            g2.dispose();
        }

        @Override
        protected void setThumbBounds(int x, int y, int width, int height) {
            super.setThumbBounds(x, y, width, height);
            scrollbar.repaint();
        }
    }

    /**
     * Chat item for sidebar
     */
    private class ChatItem extends JPanel {
        private final String chatId;
        private final String chatName;
        private final long timestamp;
        private boolean isSelected = false;
        private final String userId;

        public ChatItem(String chatId, String chatName, String lastMessage, long timestamp, String userId) {
            this.chatId = chatId;
            this.chatName = chatName;
            this.timestamp = timestamp;
            this.userId = userId;

            setLayout(new BorderLayout(12, 0));
            setPreferredSize(new Dimension(0, 80));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                    BorderFactory.createEmptyBorder(15, 20, 15, 20)
            ));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Avatar with enhanced styling
            AvatarPanel avatarPanel;
            if (userId == null || isGroupChat(chatId)) {
                // Group chat - create group avatar
                avatarPanel = AvatarPanel.createGroupAvatar(chatName, 50);
            } else {
                // Private chat - create user avatar for the OTHER user (not current user)
                // Skip creating chat item if userId is the current user (should not happen)
                if (userId.equals(currentUserId)) {
                    System.err.println("Warning: Attempting to create chat item with current user ID: " + userId);
                    return; // Don't create chat item for current user
                }
                
                avatarPanel = new AvatarPanel(50, generateAvatarColor(userId));
                if (chatName != null && !chatName.isEmpty()) {
                    avatarPanel.setInitials(getInitials(chatName));
                }
                // Load profile image if userId is provided
                loadUserProfileImage(avatarPanel, userId);
            }

            // Chat info panel
            JPanel infoPanel = new JPanel(new BorderLayout(6, 0));
            infoPanel.setOpaque(false);

            // Chat name with enhanced styling
            JLabel nameLabel = new JLabel(chatName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            nameLabel.setForeground(new Color(40, 40, 40));

            // Last message with enhanced styling
            JLabel messageLabel = new JLabel(lastMessage != null ? lastMessage : "Aucun message");
            messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            messageLabel.setForeground(new Color(120, 120, 120));

            // Time label with enhanced styling
            JLabel timeLabel = new JLabel(formatTime(timestamp));
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            timeLabel.setForeground(new Color(150, 150, 150));
            timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            infoPanel.add(nameLabel, BorderLayout.NORTH);
            infoPanel.add(messageLabel, BorderLayout.CENTER);

            // Time panel
            JPanel timePanel = new JPanel(new BorderLayout());
            timePanel.setOpaque(false);
            timePanel.add(timeLabel, BorderLayout.NORTH);

            // Add components to main panel
            add(avatarPanel, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
            add(timePanel, BorderLayout.EAST);

            // Enhanced hover effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openChat(chatId);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(248, 249, 250));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)
                    ));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(Color.WHITE);
                        setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                                BorderFactory.createEmptyBorder(15, 20, 15, 20)
                        ));
                    }
                }
            });
        }

        public String getChatId() {
            return chatId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getChatName() {
            return chatName;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;

            if (isSelected) {
                setBackground(new Color(230, 245, 240));
            } else {
                setBackground(Color.WHITE);
            }

            repaint();
        }
    }

    /**
     * User select item for new chat dialog
     */
    private class UserSelectItem extends JPanel {
        private final String userId;
        private final String username;
        private final String email;
        private boolean isSelected = false;

        public UserSelectItem(String userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;

            setLayout(new BorderLayout(10, 0));
            setPreferredSize(new Dimension(0, 60));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Avatar
            AvatarPanel avatarPanel = new AvatarPanel(40, generateAvatarColor(userId));
            if (username != null && !username.isEmpty()) {
                avatarPanel.setInitials(getInitials(username));
            }

            // Load profile image for this user
            loadUserProfileImage(avatarPanel, userId);

            // Info panel
            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(username);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(EMSI_GRAY);

            JLabel emailLabel = new JLabel(email);
            emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            emailLabel.setForeground(EMSI_GRAY_LIGHT);

            infoPanel.add(nameLabel, BorderLayout.NORTH);
            infoPanel.add(emailLabel, BorderLayout.CENTER);

            // Checkbox
            JCheckBox checkBox = new JCheckBox();
            checkBox.setOpaque(false);
            checkBox.setFocusPainted(false);

            // Add components
            add(avatarPanel, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
            add(checkBox, BorderLayout.EAST);

            // Add hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(new Color(245, 245, 245));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(Color.WHITE);
                    }
                }
            });
        }

        public void toggleSelected() {
            isSelected = !isSelected;

            if (isSelected) {
                setBackground(new Color(230, 245, 240));
            } else {
                setBackground(Color.WHITE);
            }

            // Update checkbox
            JCheckBox checkBox = (JCheckBox) getComponent(2);
            checkBox.setSelected(isSelected);

            repaint();
        }

        public boolean isSelected() {
            return isSelected;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getEmail() {
            return email;
        }
    }



    /**
     * Creates a plus icon for the add chat button
     */
    private Icon createPlusIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                int centerX = x + getIconWidth() / 2;
                int centerY = y + getIconHeight() / 2;
                int size = Math.min(getIconWidth(), getIconHeight()) / 3;
                
                // Horizontal line
                g2.drawLine(centerX - size/2, centerY, centerX + size/2, centerY);
                // Vertical line
                g2.drawLine(centerX, centerY - size/2, centerX, centerY + size/2);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 24; }
            
            @Override
            public int getIconHeight() { return 24; }
        };
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        try {
            // Set look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Customize UI defaults
            UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("CheckBox.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("ComboBox.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));
            UIManager.put("ComboBox.selectionForeground", new javax.swing.plaf.ColorUIResource(Color.WHITE));
            UIManager.put("TextField.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Launch app
        SwingUtilities.invokeLater(() -> {
            // Show splash screen
            JFrame splashFrame = new JFrame();
            splashFrame.setUndecorated(true);
            splashFrame.setBackground(new Color(0, 0, 0, 0));

            JPanel splashPanel = new JPanel() {
                private float progress = 0f;
                private javax.swing.Timer timer;

                {
                    timer = new javax.swing.Timer(20, e -> {
                        progress += 0.05f;
                        if (progress > 1f) {
                            ((javax.swing.Timer) e.getSource()).stop();
                            splashFrame.dispose();

                            // Open main chat window (for testing only; in production this would be called from ui.pages.LoginPage)
                            JFrame frame = new JFrame("VibeApp Chat");
                            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            frame.setSize(1000, 700);
                            frame.setLocationRelativeTo(null);
                            frame.setContentPane(new MainChat("user_1234"));
                            frame.setVisible(true);
                        }
                        repaint();
                    });
                    timer.start();
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Draw logo
                    int alpha = (int) (progress * 255);

                    // Draw VibeApp logo
                    g2d.setColor(new Color(0, 150, 70, alpha));
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 32));
                    g2d.drawString("VibeApp", 50, 80);

                    g2d.setColor(new Color(90, 90, 90, alpha));
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    g2d.drawString("Chat", 150, 80);

                    // Draw loading bar
                    g2d.setColor(new Color(220, 220, 220, alpha));
                    g2d.fillRoundRect(50, 100, 150, 4, 2, 2);

                    g2d.setColor(new Color(0, 150, 70, alpha));
                    g2d.fillRoundRect(50, 100, (int) (150 * progress), 4, 2, 2);

                    g2d.dispose();
                }
            };

            splashPanel.setPreferredSize(new Dimension(250, 150));
            splashPanel.setOpaque(false);

            splashFrame.setContentPane(splashPanel);
            splashFrame.pack();
            splashFrame.setLocationRelativeTo(null);
            splashFrame.setVisible(true);
        });
    }
}