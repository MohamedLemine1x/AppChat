package ui.pages;

import ui.components.SettingsPanel;
import ui.components.ProfileImageEditor;
import ui.components.SettingsItem;
import ui.components.SettingsItem.ToggleChangeListener;
import ui.components.SettingsItem.ComboBoxChangeListener;
import ui.components.ToggleSwitch;
import ui.components.LoadingIndicator;
import com.vibeapp.VibeApp;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.swing.*;

import models.UserPreferences;
import services.FirebaseService;
import ui.components.AnimatedButton;
import ui.components.RoundedPanel;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.SwingWorker;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * UserSettingsPage - Improved settings interface for user profile image
 */
public class UserSettingsPage extends JPanel {

    // Constants
    private static final int SIDEBAR_WIDTH = 220;
    private static final int CONTENT_PADDING = 25;
    private static final int CARD_RADIUS = 15;
    private static final float BUTTON_OPACITY = 0.85f;

    // Colors - EMSI Brand Colors
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private final Color EMSI_GRAY_LIGHTER = new Color(240, 240, 240);
    private final Color EMSI_RED = new Color(217, 83, 30);
    private final Color EMSI_RED_HOVER = new Color(237, 103, 50);
    private final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private final Color CARD_BACKGROUND = Color.WHITE;
    private final Color CARD_SHADOW = new Color(0, 0, 0, 30);
    private final Color SECTION_TITLE_COLOR = new Color(60, 60, 60);
    private final Color SECTION_SUBTITLE_COLOR = new Color(100, 100, 100);
    private final Color BUTTON_TEXT_COLOR = new Color(255, 255, 255);
    private final Color LINK_HOVER_COLOR = new Color(0, 140, 60);

    // Fonts
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 22);
    private final Font SIDEBAR_FONT = new Font("Segoe UI", Font.PLAIN, 15);
    private final Font SIDEBAR_SELECTED_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font SECTION_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private final Font SECTION_SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font VALUE_FONT = new Font("Segoe UI Semibold", Font.PLAIN, 14);
    private final Font LINK_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // Image storage constants
    private static final String SHARED_IMAGE_DIR = "resources/profiles";
    private static final String IMAGE_PREFIX = "profile_";

    // Data
    private String currentUserId;
    
    // Auto-save management
    private javax.swing.Timer autoSaveTimer;
    private final int AUTO_SAVE_DELAY = 2000; // 2 seconds delay
    private boolean isUpdatingSettings = false;
    private volatile boolean hasProfileImageChanged = false;

    // Ensure shared image directory exists
    private void ensureSharedImageDirExists() {
        File dir = new File(SHARED_IMAGE_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Created profile images directory: " + dir.getAbsolutePath());
            } else {
                System.err.println("Failed to create profile images directory: " + dir.getAbsolutePath());
            }
        }
    }

    // Services
    private FirebaseService firebaseService;
    private UserPreferences userPreferences;
    private boolean hasUnsavedChanges = false;

    // UI Components
    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private JScrollPane contentScrollPane;
    private JButton saveButton;
    private JButton cancelButton;

    // Enhanced Settings Categories
    private final String[] CATEGORIES = {
            "Profil", "À propos", "Supprimer le compte"
    };

    private final String[] CATEGORY_ICON_PATHS = {
            "pictures/profile.png",
            "pictures/about.png",
            "pictures/close.png"
    };

    private Map<String, ImageIcon> categoryIcons = new HashMap<>();
    private Map<String, JButton> categoryButtons = new HashMap<>();
    private String selectedCategory = "Profil";

    // Listeners for settings saved
    public interface SettingsSavedListener {
        void onSettingsSaved();
    }

    // Add this field to store listeners
    private java.util.List<SettingsSavedListener> settingsSavedListeners = new ArrayList<>();

    public void addSettingsSavedListener(SettingsSavedListener listener) {
        if (settingsSavedListeners == null) {
            settingsSavedListeners = new ArrayList<>();
        }
        settingsSavedListeners.add(listener);
    }

    // Added field
    private JPanel mainPanel;

    /**
     * Constructor
     * @param userId The current user ID
     * @param firebaseService Firebase service instance
     */
    public UserSettingsPage(String userId, FirebaseService firebaseService) {
        this.currentUserId = userId;
        this.firebaseService = firebaseService;

        // Ensure shared image directory exists
        ensureSharedImageDirExists();

        // Load icons
        loadCategoryIcons();

        initializeUI();
        loadUserPreferences();
        
        // Initialize auto-save timer
        initializeAutoSave();

        // Removed problematic responsive listener that causes display bugs
    }

    // Responsive methods removed to fix display bugs

    /**
     * Load modern category icons with universal symbols
     */
    private void loadCategoryIcons() {
        // Use universal symbols that work on all systems
        String[] universalIcons = {
                "●",   // Profile - Filled circle for user
                "i",   // About - Information symbol
                "×"    // Delete - Cross symbol
        };

        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i];
            String iconText = (i < universalIcons.length) ? universalIcons[i] : "•";
            
            // Create enhanced icons with custom drawing
            createCustomIcon(category, i);
        }
    }

    /**
     * Creates custom vector-based icons for each category
     */
    private void createCustomIcon(String category, int iconIndex) {
        // Create a clean 32x32 image
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        
        // Enable high-quality rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int centerX = 16;
        int centerY = 16;
        
        // Set colors
        Color iconColor = EMSI_GREEN;
        Color shadowColor = new Color(0, 0, 0, 20);
        
        // Draw based on icon type
        switch (iconIndex) {
            case 0: // Profile icon
                drawProfileIcon(g2, centerX, centerY, iconColor, shadowColor);
                break;
            case 1: // About icon
                drawAboutIcon(g2, centerX, centerY, iconColor, shadowColor);
                break;
            case 2: // Delete icon
                drawDeleteIcon(g2, centerX, centerY, iconColor, shadowColor);
                break;
            default:
                drawDefaultIcon(g2, centerX, centerY, iconColor, shadowColor);
                break;
        }
        
        g2.dispose();
        categoryIcons.put(category, new ImageIcon(image));
    }
    
    /**
     * Draws a profile/user icon
     */
    private void drawProfileIcon(Graphics2D g2, int centerX, int centerY, Color iconColor, Color shadowColor) {
        // Draw shadow
        g2.setColor(shadowColor);
        g2.fillOval(centerX - 6 + 1, centerY - 10 + 1, 12, 12); // Head shadow
        g2.fillOval(centerX - 10 + 1, centerY + 3 + 1, 20, 16); // Body shadow
        
        // Draw main icon
        g2.setColor(iconColor);
        g2.fillOval(centerX - 6, centerY - 10, 12, 12); // Head
        g2.fillOval(centerX - 10, centerY + 3, 20, 16); // Body/shoulders
    }
    
    /**
     * Draws an info/about icon
     */
    private void drawAboutIcon(Graphics2D g2, int centerX, int centerY, Color iconColor, Color shadowColor) {
        // Draw shadow
        g2.setColor(shadowColor);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(centerX - 12 + 1, centerY - 12 + 1, 24, 24); // Circle shadow
        
        // Draw main icon
        g2.setColor(iconColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(centerX - 12, centerY - 12, 24, 24); // Circle
        
        // Draw "i" letter
        Font font = new Font("Segoe UI", Font.BOLD, 16);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String text = "i";
        int x = centerX - fm.stringWidth(text) / 2;
        int y = centerY + fm.getAscent() / 2 - 2;
        
        // Shadow for text
        g2.setColor(shadowColor);
        g2.drawString(text, x + 1, y + 1);
        
        // Main text
        g2.setColor(iconColor);
        g2.drawString(text, x, y);
    }
    
    /**
     * Draws a delete/trash icon
     */
    private void drawDeleteIcon(Graphics2D g2, int centerX, int centerY, Color iconColor, Color shadowColor) {
        // Use red color for delete
        Color deleteColor = EMSI_RED;
        
        // Draw shadow
        g2.setColor(shadowColor);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(centerX - 8 + 1, centerY - 8 + 1, centerX + 8 + 1, centerY + 8 + 1); // \ shadow
        g2.drawLine(centerX + 8 + 1, centerY - 8 + 1, centerX - 8 + 1, centerY + 8 + 1); // / shadow
        
        // Draw main icon (X)
        g2.setColor(deleteColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(centerX - 8, centerY - 8, centerX + 8, centerY + 8); // \
        g2.drawLine(centerX + 8, centerY - 8, centerX - 8, centerY + 8); // /
    }
    
    /**
     * Draws a default icon
     */
    private void drawDefaultIcon(Graphics2D g2, int centerX, int centerY, Color iconColor, Color shadowColor) {
        // Draw shadow
        g2.setColor(shadowColor);
        g2.fillOval(centerX - 4 + 1, centerY - 4 + 1, 8, 8);
        
        // Draw main icon
        g2.setColor(iconColor);
        g2.fillOval(centerX - 4, centerY - 4, 8, 8);
    }

    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);

        // Create header
        createHeader();

        // Create main content area
        createMainContent();

        // Create bottom buttons
        createBottomPanel();

        // Load default category with delay to ensure proper initialization
        SwingUtilities.invokeLater(() -> {
            showCategory(selectedCategory);
        });
    }

    /**
     * Creates the header panel
     */
    private void createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_BACKGROUND);
        headerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EMSI_GRAY_LIGHTER),
                BorderFactory.createEmptyBorder(18, 25, 18, 25)
        ));

        // Title
        JLabel titleLabel = new JLabel("Paramètres utilisateur");
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(SECTION_TITLE_COLOR);

        // Close button with icon
        JButton closeButton = new JButton();
        ImageIcon closeIcon = new ImageIcon("pictures/close.png");
        if (closeIcon.getIconWidth() > 0) {
            Image img = closeIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            closeButton.setIcon(new ImageIcon(img));
            closeButton.setPreferredSize(new Dimension(44, 44));
            closeButton.setContentAreaFilled(false);
            closeButton.setBorderPainted(false);
            closeButton.setFocusPainted(false);
            closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            closeButton.setText("✕");
            closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        }
        closeButton.addActionListener(e -> closeSettings());
        closeButton.setToolTipText("Fermer");
        
        // Add hover effect for better UX
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(new Color(255, 69, 58)); // Red hover
                closeButton.setContentAreaFilled(true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setContentAreaFilled(false);
            }
        });

        // Logout button with icon
        JButton logoutButton = new JButton();
        ImageIcon logoutIcon = new ImageIcon("pictures/deconnexion.png");
        if (logoutIcon.getIconWidth() > 0) {
            Image img = logoutIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            logoutButton.setIcon(new ImageIcon(img));
            logoutButton.setPreferredSize(new Dimension(44, 44));
            logoutButton.setContentAreaFilled(false);
            logoutButton.setBorderPainted(false);
            logoutButton.setFocusPainted(false);
            logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            logoutButton.setText("⎋");
            logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        }
        logoutButton.setToolTipText("Déconnexion");
        logoutButton.addActionListener(e -> {
            // Log out logic: close both settings dialog and MainChat, then show LoginPage
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                // If the settings are in a dialog, close its owner (MainChat JFrame)
                if (window instanceof JDialog) {
                    Window owner = ((JDialog) window).getOwner();
                    if (owner != null) {
                        owner.dispose(); // Close MainChat JFrame
                    }
                }
                window.dispose(); // Close the settings dialog itself
            }
            // Show login page in a new frame
            JFrame loginFrame = new JFrame("VibeApp - Connexion");
            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setSize(950, 650);
            loginFrame.setLocationRelativeTo(null);
            loginFrame.setContentPane(new LoginPage());
            loginFrame.setVisible(true);
        });

        JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightHeaderPanel.setOpaque(false);
        rightHeaderPanel.add(logoutButton);
        rightHeaderPanel.add(closeButton);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightHeaderPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    /**
     * Creates the main content area with sidebar and content panel
     */
    private void createMainContent() {
        mainPanel = new JPanel(new BorderLayout(15, 0));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Create sidebar
        createSidebar();

        // Create content area
        createContentArea();

        mainPanel.add(sidebarPanel, BorderLayout.WEST);
        mainPanel.add(contentScrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Creates an enhanced sidebar with modern design
     */
    private void createSidebar() {
        sidebarPanel = new RoundedPanel(CARD_RADIUS, CARD_BACKGROUND) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Simple solid background
                g2d.setColor(CARD_BACKGROUND);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS);
                
                // Simple border
                g2d.setColor(EMSI_GRAY_LIGHTER);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS);
                
                g2d.dispose();
            }
        };
        
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));

        // Add some padding at the top
        sidebarPanel.add(Box.createVerticalStrut(10));

        // Add category buttons with consistent spacing
        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i];
            JButton categoryButton = createCategoryButton(category);
            categoryButtons.put(category, categoryButton);
            sidebarPanel.add(categoryButton);
            
            // Add spacing between buttons
            if (i < CATEGORIES.length - 1) {
                sidebarPanel.add(Box.createVerticalStrut(12));
            }
        }

        // Add flexible space at bottom
        sidebarPanel.add(Box.createVerticalGlue());

        // Select first category by default with proper initialization
        if (!categoryButtons.isEmpty()) {
            // Make sure we have a valid selected category
            if (selectedCategory == null || !categoryButtons.containsKey(selectedCategory)) {
                selectedCategory = CATEGORIES[0]; // Default to first category
            }
            
            // Set initial selection state
            JButton selectedButton = categoryButtons.get(selectedCategory);
            if (selectedButton != null) {
                selectedButton.setSelected(true);
                selectedButton.repaint();
            }
        }
    }

    /**
     * Creates the content area for settings
     */
    private void createContentArea() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING));

        contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScrollPane.setBackground(BACKGROUND_COLOR);

        // Make the scrollbar more modern looking
        contentScrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = EMSI_GRAY_LIGHT;
                this.trackColor = EMSI_GRAY_LIGHTER;
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
        });
    }

    /**
     * Creates the bottom panel with save/cancel buttons
     */
    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        bottomPanel.setBackground(CARD_BACKGROUND);
        bottomPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EMSI_GRAY_LIGHTER),
                BorderFactory.createEmptyBorder(18, 25, 18, 25)
        ));

        // Cancel button
        cancelButton = createStyledButton("Annuler", EMSI_GRAY_LIGHT, EMSI_GRAY, EMSI_GRAY_LIGHT);
        cancelButton.addActionListener(e -> cancelChanges());

        // Save button
        saveButton = createStyledButton("Enregistrer", EMSI_GREEN, EMSI_GREEN_LIGHT, EMSI_GREEN_DARK);
        saveButton.addActionListener(e -> saveSettings());
        saveButton.setEnabled(false);

        bottomPanel.add(cancelButton);
        bottomPanel.add(saveButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Custom button class with enhanced styling
     */
    private class StyledButton extends JButton {
        private boolean isHovered = false;
        private boolean isPressed = false;
        private Color baseColor;
        private Color hoverColor;
        private Color pressedColor;
        
        public StyledButton(String text, Color baseColor, Color hoverColor, Color pressedColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            this.pressedColor = pressedColor;
        }
        
        public void setHovered(boolean hovered) {
            this.isHovered = hovered;
        }
        
        public void setPressed(boolean pressed) {
            this.isPressed = pressed;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Get the current button state color with smooth transitions
            Color currentColor = baseColor;
            if (isPressed && isEnabled()) {
                currentColor = pressedColor;
            } else if (isHovered && isEnabled()) {
                currentColor = hoverColor;
            }

            // Apply transparency if disabled
            if (!isEnabled()) {
                currentColor = new Color(
                        currentColor.getRed(),
                        currentColor.getGreen(),
                        currentColor.getBlue(),
                        (int)(255 * BUTTON_OPACITY)
                );
            }

            // Enhanced shadow with multiple layers for depth
            if (isEnabled() && !isPressed) {
                // Outer shadow (softer, larger)
                g2d.setColor(new Color(0, 0, 0, 8));
                g2d.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 14, 14);
                
                // Inner shadow (sharper, smaller)
                g2d.setColor(new Color(0, 0, 0, 15));
                g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 13, 13);
            }

            // Enhanced gradient with more depth
            GradientPaint gradient;
            if (isPressed) {
                // Inverted gradient when pressed
                gradient = new GradientPaint(
                    0, 0, currentColor.darker(),
                    0, getHeight(), currentColor
                );
            } else {
                // Normal gradient with highlight
                Color lightColor = new Color(
                    Math.min(255, currentColor.getRed() + 20),
                    Math.min(255, currentColor.getGreen() + 20),
                    Math.min(255, currentColor.getBlue() + 20)
                );
                gradient = new GradientPaint(
                    0, 0, lightColor,
                    0, getHeight(), currentColor
                );
            }
            
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            // Enhanced border with subtle inner highlight
            if (isEnabled()) {
                // Main border
                g2d.setColor(currentColor.darker());
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                
                // Inner highlight (top edge)
                if (!isPressed) {
                    g2d.setColor(new Color(255, 255, 255, 40));
                    g2d.setStroke(new BasicStroke(1f));
                    g2d.drawRoundRect(1, 1, getWidth()-3, getHeight()/3, 8, 8);
                }
            }

            // Enhanced text rendering with better shadow
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(getText())) / 2;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            
            // Text shadow (multiple layers for smoothness)
            if (isEnabled()) {
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.drawString(getText(), textX + 1, textY + 1);
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.drawString(getText(), textX + 1, textY);
            }

            // Main text with enhanced color
            Color textColor = BUTTON_TEXT_COLOR;
            if (!isEnabled()) {
                textColor = new Color(255, 255, 255, 150);
            } else if (isPressed) {
                textColor = new Color(240, 240, 240);
            }
            
            g2d.setColor(textColor);
            g2d.drawString(getText(), textX, textY);

            g2d.dispose();
        }
    }

    /**
     * Creates a modern styled button with enhanced effects and animations
     */
    private JButton createStyledButton(String text, Color baseColor, Color hoverColor, Color pressedColor) {
        StyledButton button = new StyledButton(text, baseColor, hoverColor, pressedColor);

        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setBackground(baseColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 44)); // Slightly larger for better touch targets
        
        // Add enhanced hover and press effects with smooth transitions
        button.addMouseListener(new MouseAdapter() {
            private Timer hoverAnimation;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setHovered(true);
                    startHoverAnimation(button, true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setHovered(false);
                    startHoverAnimation(button, false);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setPressed(true);
                    button.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setPressed(false);
                    button.repaint();
                }
            }
            
            private void startHoverAnimation(JButton btn, boolean entering) {
                if (hoverAnimation != null && hoverAnimation.isRunning()) {
                    hoverAnimation.stop();
                }
                
                hoverAnimation = new Timer(20, event -> {
                    btn.repaint();
                    // Animation duration control
                    Timer timer = (Timer) event.getSource();
                    if (timer.getDelay() > 150) { // Stop after reasonable time
                        timer.stop();
                    }
                });
                hoverAnimation.start();
            }
        });

        return button;
    }

    /**
     * Creates an enhanced category button with modern design and animations
     */
    private JButton createCategoryButton(String category) {
        JButton button = new JButton(category) {
            private boolean selected = false;
            private float animationProgress = 0.0f;
            private Timer animationTimer;

            @Override
            public void setSelected(boolean selected) {
                this.selected = selected;
                updateAppearance();
                startAnimation();
            }

            @Override
            public boolean isSelected() {
                return selected;
            }

            private void startAnimation() {
                // Simple immediate update without complex animations
                animationProgress = selected ? 1.0f : 0.0f;
                repaint();
            }

            private void updateAppearance() {
                if (selected) {
                    setForeground(EMSI_GREEN_DARK);
                    setFont(SIDEBAR_SELECTED_FONT);
                } else {
                    setForeground(EMSI_GRAY);
                    setFont(SIDEBAR_FONT);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Calculate colors based on state
                Color backgroundColor = CARD_BACKGROUND;
                Boolean isHovered = (Boolean) getClientProperty("hovered");
                boolean hovered = isHovered != null && isHovered;
                
                if (selected) {
                    // Selected state with green tint
                    backgroundColor = new Color(230, 245, 235);
                } else if (hovered) {
                    // Hover state with subtle blue tint
                    backgroundColor = new Color(248, 252, 255);
                }

                // Draw background
                g2d.setColor(backgroundColor);
                if (selected || hovered) {
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else {
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }

                // Draw selection indicator
                if (selected) {
                    g2d.setColor(EMSI_GREEN);
                    g2d.fillRoundRect(0, (getHeight() - 40) / 2, 4, 40, 2, 2);
                }

                // Draw icon and text
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };

        // Set enhanced icon with better spacing
        ImageIcon icon = categoryIcons.get(category);
        if (icon != null) {
            button.setIcon(icon);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIconTextGap(20); // Better spacing
        }

        button.setFont(SIDEBAR_FONT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64)); // Slightly taller
        button.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 64));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 16)); // Better padding

        // Simplified hover effects to avoid display issues
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.putClientProperty("hovered", true);
                if (!button.isSelected()) {
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.putClientProperty("hovered", false);
                if (!button.isSelected()) {
                    button.repaint();
                }
            }
        });

        // Add click listener
        button.addActionListener(e -> selectCategory(category));

        return button;
    }

    /**
     * Creates an icon button
     */
    private JButton createIconButton(String text, int size) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, size));
        button.setPreferredSize(new Dimension(44, 44));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(EMSI_GRAY);

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(EMSI_RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(EMSI_GRAY);
            }
        });

        return button;
    }

    /**
     * Selects a category and updates the UI
     */
    private void selectCategory(String category) {
        if (category == null || category.equals(selectedCategory)) {
            return; // No change needed
        }
        
        // Update button states with explicit repaint
        categoryButtons.forEach((name, button) -> {
            boolean isSelected = name.equals(category);
            button.setSelected(isSelected);
            button.repaint(); // Force repaint of each button
        });

        selectedCategory = category;
        showCategory(category);
        
        // Force complete refresh with multiple passes to ensure proper display
        Timer refreshTimer = new Timer(100, e -> {
            forceRefresh();
            ((Timer) e.getSource()).stop();
        });
        refreshTimer.start();
        
        // Additional refresh for complex layouts like Profile
        if ("Profil".equals(category)) {
            Timer profileRefreshTimer = new Timer(200, e -> {
                forceRefresh();
                ((Timer) e.getSource()).stop();
            });
            profileRefreshTimer.start();
        }
    }

    /**
     * Shows the content for the selected category
     */
    private void showCategory(String category) {
        // Use SwingUtilities to ensure thread safety
        SwingUtilities.invokeLater(() -> {
            contentPanel.removeAll();

            switch (category) {
                case "Profil":
                    createProfileSettings();
                    break;
                case "À propos":
                    createAboutSettings();
                    break;
                case "Supprimer le compte":
                    createDeleteAccountSettings();
                    break;
                default:
                    // Fallback to profile if category not found
                    createProfileSettings();
                    break;
            }

            // Force complete refresh of the content area
            contentPanel.revalidate();
            contentPanel.repaint();
            
            // Also refresh the parent container
            if (contentScrollPane != null) {
                contentScrollPane.revalidate();
                contentScrollPane.repaint();
            }
            
            // Force repaint of entire main panel
            if (mainPanel != null) {
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
    }

    /**
     * Creates profile settings panel
     */
    private void createProfileSettings() {
        // Create enhanced panel with shadow
        RoundedPanel profileSection = new RoundedPanel(CARD_RADIUS, CARD_BACKGROUND);
        profileSection.setLayout(new BoxLayout(profileSection, BoxLayout.Y_AXIS));
        profileSection.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        profileSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section title
        JLabel titleLabel = new JLabel("Profil");
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(SECTION_TITLE_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section subtitle
        JLabel subtitleLabel = new JLabel("Personnalisez votre profil");
        subtitleLabel.setFont(SECTION_SUBTITLE_FONT);
        subtitleLabel.setForeground(SECTION_SUBTITLE_COLOR);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileSection.add(titleLabel);
        profileSection.add(Box.createVerticalStrut(5));
        profileSection.add(subtitleLabel);
        profileSection.add(Box.createVerticalStrut(25));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setForeground(EMSI_GRAY_LIGHTER);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        profileSection.add(separator);
        profileSection.add(Box.createVerticalStrut(25));

        // Create a simplified panel for the profile image section
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Removed fixed sizes that cause layout issues

        // Add title for image section
        JLabel imageTitle = new JLabel("Photo de profil");
        imageTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        imageTitle.setForeground(EMSI_GRAY);

        JPanel imageTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imageTitlePanel.setOpaque(false);
        imageTitlePanel.add(imageTitle);

        // Profile image editor with improved size
        ProfileImageEditor imageEditor = new ProfileImageEditor(120); // Larger avatar size

        // Set user initial if available
        if (userPreferences != null && userPreferences.getDisplayName() != null && !userPreferences.getDisplayName().isEmpty()) {
            imageEditor.setUserInitial(userPreferences.getDisplayName().substring(0, 1));
        }

        // Set current profile image if exists
        if (userPreferences != null && userPreferences.getProfileImageUrl() != null && !userPreferences.getProfileImageUrl().isEmpty()) {
            imageEditor.setImagePath(userPreferences.getProfileImageUrl());
        }

        // Set change listener to handle image changes with non-blocking approach
        imageEditor.setChangeListener(new ProfileImageEditor.ProfileImageChangeListener() {
            @Override
            public void onImageChanged(String imagePath, BufferedImage image) {
                // Use profile-specific safe update to prevent auto-save conflicts
                safeUpdateProfileImage(() -> {
                    if (image != null) {
                        handleImageChangeAsync(image);
                    }
                });
            }

            @Override
            public void onImageRemoved() {
                // Use profile-specific safe update to prevent auto-save conflicts
                safeUpdateProfileImage(() -> {
                    handleImageRemovalAsync();
                });
            }
        });

        // Simplified layout without wrapper panels
        profileSection.add(imageTitlePanel);
        profileSection.add(Box.createVerticalStrut(10));
        
        // Create centered panel for image editor
        JPanel editorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        editorPanel.setOpaque(false);
        editorPanel.add(imageEditor);
        editorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        profileSection.add(editorPanel);

        // Add simplified helper text
        profileSection.add(Box.createVerticalStrut(10));
        
        JLabel helperText = new JLabel("Cliquez sur l'image pour la modifier ou la supprimer");
        helperText.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        helperText.setForeground(EMSI_GRAY_LIGHT);
        helperText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        profileSection.add(helperText);

        // Add the section to contentPanel
        contentPanel.add(profileSection);

        // Add some space below
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Force layout refresh after profile creation
        SwingUtilities.invokeLater(() -> {
            profileSection.revalidate();
            profileSection.repaint();
        });
    }

    /**
     * Creates about settings panel
     */
    private void createAboutSettings() {
        // Create enhanced panel with shadow
        RoundedPanel aboutSection = new RoundedPanel(CARD_RADIUS, CARD_BACKGROUND);
        aboutSection.setLayout(new BoxLayout(aboutSection, BoxLayout.Y_AXIS));
        aboutSection.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        aboutSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section title with icon
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        titlePanel.setOpaque(false);
        try {
            ImageIcon aboutIcon = new ImageIcon("pictures/about.png");
            if (aboutIcon.getIconWidth() > 0) {
                Image img = aboutIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                JLabel iconLabel = new JLabel(new ImageIcon(img));
                iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
                titlePanel.add(iconLabel);
            }
        } catch (Exception e) {
            // fallback: no icon
        }
        JLabel titleLabel = new JLabel("À propos de VibeApp");
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(SECTION_TITLE_COLOR);
        titleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        titlePanel.add(titleLabel);
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        aboutSection.add(titlePanel);
        aboutSection.add(Box.createVerticalStrut(20));

        // App info section
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Info items with enhanced styling
        addEnhancedInfoItem(infoPanel, "Version", "1.0.0");
        addEnhancedInfoItem(infoPanel, "Build", "2025.05.27");
        addEnhancedInfoItem(infoPanel, "Développé par", "Équipe EMSI");

        aboutSection.add(infoPanel);

        // Separator
        JSeparator separator = new JSeparator();
        separator.setForeground(EMSI_GRAY_LIGHTER);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        aboutSection.add(separator);
        aboutSection.add(Box.createVerticalStrut(20));

        // Links section title
        JLabel linksTitle = new JLabel("Liens utiles");
        linksTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        linksTitle.setForeground(EMSI_GRAY);
        linksTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        aboutSection.add(linksTitle);
        aboutSection.add(Box.createVerticalStrut(15));

        // Links panel
        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new BoxLayout(linksPanel, BoxLayout.Y_AXIS));
        linksPanel.setOpaque(false);
        linksPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create link buttons with text-based icons
        addEnhancedLinkButton(linksPanel, "🌐 Site web", "https://vibeapp.com");
        addEnhancedLinkButton(linksPanel, "📧 Support", "mailto:support@vibeapp.com");
        addEnhancedLinkButton(linksPanel, "📄 Conditions d'utilisation", "https://vibeapp.com/terms");
        addEnhancedLinkButton(linksPanel, "🔒 Politique de confidentialité", "https://vibeapp.com/privacy");

        aboutSection.add(linksPanel);

        // App logo/branding (centered)
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        JLabel logoLabel = new JLabel("VibeApp");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoLabel.setForeground(EMSI_GREEN);

        logoPanel.add(logoLabel);

        aboutSection.add(logoPanel);

        // Copyright at bottom
        JPanel copyrightPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        copyrightPanel.setOpaque(false);
        copyrightPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel copyrightLabel = new JLabel("© 2025 VibeApp. Tous droits réservés.");
        copyrightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyrightLabel.setForeground(EMSI_GRAY_LIGHT);

        copyrightPanel.add(copyrightLabel);

        aboutSection.add(copyrightPanel);

        contentPanel.add(aboutSection);
        contentPanel.add(Box.createVerticalStrut(20));
    }

    /**
     * Adds an enhanced info item to a section
     */
    private void addEnhancedInfoItem(JPanel section, String label, String value) {
        JPanel itemPanel = new JPanel(new BorderLayout(15, 0));
        itemPanel.setOpaque(false);
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        itemPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(LABEL_FONT);
        labelComponent.setForeground(EMSI_GRAY);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(VALUE_FONT);
        valueComponent.setForeground(EMSI_GRAY);

        itemPanel.add(labelComponent, BorderLayout.WEST);
        itemPanel.add(valueComponent, BorderLayout.EAST);

        section.add(itemPanel);
    }

    /**
     * Adds an enhanced link button to a panel
     */
    private void addEnhancedLinkButton(JPanel panel, String text, String url) {
        JButton linkButton = new JButton(text);
        linkButton.setFont(LINK_FONT);
        linkButton.setForeground(EMSI_GREEN);
        linkButton.setHorizontalAlignment(SwingConstants.LEFT);
        linkButton.setContentAreaFilled(false);
        linkButton.setBorderPainted(false);
        linkButton.setFocusPainted(false);
        linkButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        linkButton.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        linkButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Impossible d'ouvrir le lien: " + url,
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Hover effect
        linkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                linkButton.setForeground(LINK_HOVER_COLOR);
                linkButton.setText("→ " + text);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                linkButton.setForeground(EMSI_GREEN);
                linkButton.setText(text);
            }
        });

        panel.add(linkButton);
        panel.add(Box.createVerticalStrut(5));
    }

    /**
     * Creates delete account settings panel
     */
    private void createDeleteAccountSettings() {
        // Create enhanced panel with shadow and warning styling
        RoundedPanel deleteAccountSection = new RoundedPanel(CARD_RADIUS, CARD_BACKGROUND);
        deleteAccountSection.setLayout(new BoxLayout(deleteAccountSection, BoxLayout.Y_AXIS));
        deleteAccountSection.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        deleteAccountSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Warning icon and title in same row
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);

        // Warning icon (text-based fallback)
        JLabel warningIcon = new JLabel("⚠️");
        warningIcon.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        headerPanel.add(warningIcon);

        // Section title
        JLabel titleLabel = new JLabel("Supprimer le compte");
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(new Color(180, 0, 0));

        headerPanel.add(titleLabel);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        deleteAccountSection.add(headerPanel);
        deleteAccountSection.add(Box.createVerticalStrut(15));

        // Warning message
        JTextArea warningText = new JTextArea(
                "Cette action est irréversible. Une fois votre compte supprimé, toutes vos données seront définitivement perdues et ne pourront pas être récupérées."
        );
        warningText.setFont(SECTION_SUBTITLE_FONT);
        warningText.setForeground(EMSI_GRAY);
        warningText.setWrapStyleWord(true);
        warningText.setLineWrap(true);
        warningText.setOpaque(false);
        warningText.setEditable(false);
        warningText.setFocusable(false);
        warningText.setBorder(BorderFactory.createEmptyBorder());
        warningText.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        deleteAccountSection.add(warningText);
        deleteAccountSection.add(Box.createVerticalStrut(30));

        // Create delete button panel (for centering)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create styled delete button
        JButton deleteButton = new JButton("Supprimer mon compte") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded rectangle with gradient
                g2d.setPaint(new GradientPaint(
                        0, 0, EMSI_RED,
                        0, getHeight(), new Color(180, 50, 20)
                ));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                // Draw border
                g2d.setColor(new Color(150, 40, 20));
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);

                // Draw text with shadow
                g2d.setColor(new Color(0, 0, 0, 50));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2 + 1;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + 1;
                g2d.drawString(getText(), textX, textY);

                g2d.setColor(Color.WHITE);
                textX = (getWidth() - fm.stringWidth(getText())) / 2;
                textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(getText(), textX, textY);

                g2d.dispose();
            }
        };

        deleteButton.setFont(BUTTON_FONT);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.setPreferredSize(new Dimension(220, 45));

        // Add hover and press effects
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                deleteButton.setForeground(new Color(255, 220, 220));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                deleteButton.setForeground(Color.WHITE);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                deleteButton.setForeground(new Color(200, 200, 200));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (deleteButton.contains(e.getPoint())) {
                    deleteButton.setForeground(new Color(255, 220, 220));
                } else {
                    deleteButton.setForeground(Color.WHITE);
                }
            }
        });

        // Add action with simplified confirmation dialog
        deleteButton.addActionListener(e -> {
            // Simple confirmation dialog that should always work
            String message = "⚠️ ATTENTION ⚠️\n\n" +
                           "Êtes-vous sûr de vouloir supprimer votre compte?\n\n" +
                           "Cette action est IRRÉVERSIBLE et toutes vos données\n" +
                           "seront définitivement perdues.\n\n" +
                           "Cliquez sur OUI pour confirmer la suppression.";

            // Find the parent window for proper dialog positioning
            Window parentWindow = SwingUtilities.getWindowAncestor(UserSettingsPage.this);
            
            int result = JOptionPane.showConfirmDialog(
                    parentWindow,  // Use parent window instead of 'this'
                    message,
                    "⚠️ Supprimer le compte - Confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                // Show processing dialog
                JOptionPane.showMessageDialog(
                    parentWindow,
                    "Suppression du compte en cours...",
                    "Suppression",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // Delete user account from Firebase
                try {
                    firebaseService.deleteUser(currentUserId, new FirebaseService.FirebaseEmailCallback() {
                        @Override
                        public void onSuccess() {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(
                                        parentWindow,
                                        "Votre compte a été supprimé avec succès.",
                                        "Compte supprimé",
                                        JOptionPane.INFORMATION_MESSAGE
                                );

                                // Close the settings window
                                closeSettings();

                                // Close the main window
                                if (parentWindow != null) {
                                    parentWindow.dispose();
                                }

                                // Show login page
                                try {
                                    VibeApp.getInstance().showPage("login");
                                    VibeApp.getInstance().setVisible(true);
                                } catch (Exception ex) {
                                    System.err.println("Error showing login page: " + ex.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(
                                        parentWindow,
                                        "Erreur lors de la suppression du compte:\n" + errorMessage,
                                        "Erreur",
                                        JOptionPane.ERROR_MESSAGE
                                );
                            });
                        }
                    });
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        parentWindow,
                        "Erreur lors de la suppression du compte:\n" + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        buttonPanel.add(deleteButton);
        deleteAccountSection.add(buttonPanel);

        // Add more spacing at the bottom
        deleteAccountSection.add(Box.createVerticalStrut(20));

        // Add further warning message
        JPanel finalWarningPanel = new JPanel();
        finalWarningPanel.setLayout(new BoxLayout(finalWarningPanel, BoxLayout.Y_AXIS));
        finalWarningPanel.setOpaque(false);
        finalWarningPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        finalWarningPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EMSI_GRAY_LIGHTER),
                BorderFactory.createEmptyBorder(20, 0, 0, 0)
        ));

        JTextArea finalWarningText = new JTextArea(
                "Note: La suppression de votre compte entraînera également la suppression de tous vos messages, conversations et données associées à votre profil."
        );
        finalWarningText.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        finalWarningText.setForeground(EMSI_GRAY_LIGHT);
        finalWarningText.setWrapStyleWord(true);
        finalWarningText.setLineWrap(true);
        finalWarningText.setOpaque(false);
        finalWarningText.setEditable(false);
        finalWarningText.setFocusable(false);
        finalWarningText.setAlignmentX(Component.LEFT_ALIGNMENT);

        finalWarningPanel.add(finalWarningText);
        deleteAccountSection.add(finalWarningPanel);

        contentPanel.add(deleteAccountSection);
        contentPanel.add(Box.createVerticalStrut(20));
    }

    // ===== DATA MANAGEMENT METHODS =====

    /**
     * Gets the profile image path for a user from the properties file
     * @param userId The user ID
     * @return The profile image path or null if not found
     */
    private String getProfileImagePath(String userId) {
        try {
            File mappingFile = new File("resources/user_profiles.properties");
            if (mappingFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(mappingFile)) {
                    props.load(fis);
                    return props.getProperty(userId);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading user profile mapping: " + e.getMessage());
        }
        return null;
    }

    /**
     * Saves a mapping of user ID to profile image path in a properties file
     * This allows other parts of the application to find profile images
     *
     * @param userId The user ID
     * @param imagePath The relative path to the image
     */
    private void saveUserProfileMapping(String userId, String imagePath) {
        try {
            // Create a properties file to store user-to-image mappings
            File mappingFile = new File("resources/user_profiles.properties");

            // Create parent directory if needed
            if (!mappingFile.getParentFile().exists()) {
                mappingFile.getParentFile().mkdirs();
            }

            // Create file if it doesn't exist
            if (!mappingFile.exists()) {
                mappingFile.createNewFile();
            }

            // Load existing properties
            java.util.Properties props = new java.util.Properties();
            if (mappingFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(mappingFile)) {
                    props.load(fis);
                }
            }

            // Update the user's image path
            props.setProperty(userId, imagePath);

            // Save the properties back to the file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(mappingFile)) {
                props.store(fos, "User Profile Image Mappings");
            }

            System.out.println("Updated user profile mapping for " + userId + ": " + imagePath);

        } catch (IOException e) {
            System.err.println("Error saving user profile mapping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves the profile image to Firebase so other users can see it
     * @param userId The user ID
     * @param imagePath The local image path
     */
    private void saveProfileImageToFirebase(String userId, String imagePath) {
        System.out.println("Saving profile image to Firebase for user: " + userId + ", path: " + imagePath);
        try {
            // Update the user's profile image URL in Firebase
            DatabaseReference userRef = firebaseService.getDatabase()
                    .getReference("users/" + userId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageUrl", imagePath);

            userRef.updateChildren(updates, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        System.err.println("Error saving profile image to Firebase: " + error.getMessage());
                    } else {
                        System.out.println("Profile image successfully saved to Firebase for user: " + userId + " with path: " + imagePath);
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Error saving profile image to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads user preferences from Firebase
     */
    private void loadUserPreferences() {
        // Create default preferences
        userPreferences = new UserPreferences();
        userPreferences.setUserId(currentUserId);

        // Try to load the profile image path from the properties file
        String savedImagePath = getProfileImagePath(currentUserId);
        if (savedImagePath != null && !savedImagePath.isEmpty()) {
            userPreferences.setProfileImageUrl(savedImagePath);
            System.out.println("Loaded profile image path: " + savedImagePath);
        }

        refreshCurrentCategory();
    }

    /**
     * Refreshes the current category display
     */
    private void refreshCurrentCategory() {
        showCategory(selectedCategory);
    }
    
    /**
     * Forces a complete refresh of the interface
     */
    private void forceRefresh() {
        SwingUtilities.invokeLater(() -> {
            // Refresh sidebar buttons
            if (sidebarPanel != null) {
                sidebarPanel.revalidate();
                sidebarPanel.repaint();
            }
            
            // Refresh content
            if (contentPanel != null) {
                contentPanel.revalidate();
                contentPanel.repaint();
            }
            
            // Refresh scroll pane
            if (contentScrollPane != null) {
                contentScrollPane.revalidate();
                contentScrollPane.repaint();
            }
            
            // Refresh main panel
            if (mainPanel != null) {
                mainPanel.revalidate();
                mainPanel.repaint();
            }
            
            // Refresh entire component
            revalidate();
            repaint();
        });
    }

    /**
     * Marks settings as changed with debounced auto-save (thread-safe)
     */
    private void markAsChanged() {
        // Prevent recursive calls and ensure thread safety
        if (isUpdatingSettings) {
            return;
        }
        
        // Use invokeLater for all UI updates to ensure thread safety
        SwingUtilities.invokeLater(() -> {
            if (!isUpdatingSettings) {
                hasUnsavedChanges = true;
                
                if (saveButton != null) {
                    saveButton.setEnabled(true);
                }
                
                // Reset auto-save timer safely
                try {
                    if (autoSaveTimer != null && autoSaveTimer.isRunning()) {
                        autoSaveTimer.restart();
                    } else {
                        startAutoSaveTimer();
                    }
                } catch (Exception e) {
                    System.err.println("Error with auto-save timer: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Starts the auto-save timer with debouncing
     */
    private void startAutoSaveTimer() {
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
        }
        
        autoSaveTimer = new javax.swing.Timer(AUTO_SAVE_DELAY, e -> {
            if (hasUnsavedChanges && !isUpdatingSettings) {
                autoSaveSettings();
            }
        });
        autoSaveTimer.setRepeats(false);
        autoSaveTimer.start();
    }
    
    /**
     * Initializes auto-save functionality
     */
    private void initializeAutoSave() {
        // Add shutdown hook to clean up timer
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (autoSaveTimer != null && autoSaveTimer.isRunning()) {
                autoSaveTimer.stop();
            }
        }));
    }
    
    /**
     * Cleanup method to be called when disposing the component
     */
    public void dispose() {
        if (autoSaveTimer != null && autoSaveTimer.isRunning()) {
            autoSaveTimer.stop();
        }
        isUpdatingSettings = false;
    }
    
    /**
     * Safe setter helper to prevent event cascading (enhanced thread safety)
     */
    private void safeUpdatePreference(Runnable updateAction) {
        if (updateAction == null) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                if (userPreferences != null && !isUpdatingSettings) {
                    updateAction.run();
                    markAsChanged();
                }
            } catch (Exception e) {
                System.err.println("Error in safeUpdatePreference: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Safe setter for profile image changes (no auto-save to prevent conflicts)
     */
    private void safeUpdateProfileImage(Runnable updateAction) {
        SwingUtilities.invokeLater(() -> {
            if (userPreferences != null && !isUpdatingSettings) {
                updateAction.run();
                hasProfileImageChanged = true;
                // Don't trigger auto-save for profile images as they handle their own saving
                hasUnsavedChanges = true;
                if (saveButton != null) {
                    saveButton.setEnabled(true);
                }
            }
        });
    }
    
    /**
     * Handles image change asynchronously without blocking UI (completely non-blocking)
     */
    private void handleImageChangeAsync(BufferedImage image) {
        // Immediate UI update
        if (userPreferences != null) {
            String tempPath = "profiles/temp_" + currentUserId + ".png";
            userPreferences.setProfileImageUrl(tempPath);
            hasUnsavedChanges = false;
            hasProfileImageChanged = false;
            if (saveButton != null) {
                saveButton.setEnabled(false);
            }
        }
        
        // Perform all operations asynchronously without blocking
        CompletableFuture.runAsync(() -> {
            try {
                // Generate filename
                String fileName = IMAGE_PREFIX + currentUserId + ".png";
                String fullPath = SHARED_IMAGE_DIR + File.separator + fileName;
                String relativePath = "profiles/" + fileName;

                // Create directory and save image
                new File(SHARED_IMAGE_DIR).mkdirs();
                File outputFile = new File(fullPath);
                ImageIO.write(image, "png", outputFile);

                // Update preferences on UI thread
                SwingUtilities.invokeLater(() -> {
                    if (userPreferences != null) {
                        userPreferences.setProfileImageUrl(relativePath);
                    }
                });

                // Save to properties file (non-blocking)
                CompletableFuture.runAsync(() -> {
                    try {
                        saveUserProfileMapping(currentUserId, relativePath);
                    } catch (Exception e) {
                        System.err.println("Error saving profile mapping: " + e.getMessage());
                    }
                });

                // Save to Firebase (non-blocking)
                CompletableFuture.runAsync(() -> {
                    try {
                        saveProfileImageToFirebase(currentUserId, relativePath);
                    } catch (Exception e) {
                        System.err.println("Error saving to Firebase: " + e.getMessage());
                    }
                });

                System.out.println("Profile image saved: " + relativePath);
            } catch (Exception e) {
                System.err.println("Error saving profile image: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    if (saveButton != null) {
                        saveButton.setEnabled(true);
                        hasUnsavedChanges = true;
                    }
                });
            }
        });
    }
    
    /**
     * Handles image removal asynchronously without blocking UI (completely non-blocking)
     */
    private void handleImageRemovalAsync() {
        // Immediate UI update
        if (userPreferences != null) {
            userPreferences.setProfileImageUrl("");
            hasUnsavedChanges = false;
            hasProfileImageChanged = false;
            if (saveButton != null) {
                saveButton.setEnabled(false);
            }
        }
        
        // Perform all cleanup operations asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Get the old image path
                String oldImageRelativePath = userPreferences != null ? userPreferences.getProfileImageUrl() : "";

                // Delete the file if it exists
                if (oldImageRelativePath != null && !oldImageRelativePath.isEmpty() && !oldImageRelativePath.equals("")) {
                    String fullPath = SHARED_IMAGE_DIR + File.separator +
                            oldImageRelativePath.substring(oldImageRelativePath.lastIndexOf('/') + 1);
                    File oldImage = new File(fullPath);
                    if (oldImage.exists()) {
                        oldImage.delete();
                    }
                }

                // Update profile mapping (non-blocking)
                CompletableFuture.runAsync(() -> {
                    try {
                        saveUserProfileMapping(currentUserId, "");
                    } catch (Exception e) {
                        System.err.println("Error updating profile mapping: " + e.getMessage());
                    }
                });

                // Update Firebase (non-blocking)
                CompletableFuture.runAsync(() -> {
                    try {
                        saveProfileImageToFirebase(currentUserId, "");
                    } catch (Exception e) {
                        System.err.println("Error updating Firebase: " + e.getMessage());
                    }
                });

                System.out.println("Profile image removed for user: " + currentUserId);
            } catch (Exception e) {
                System.err.println("Error removing profile image: " + e.getMessage());
            }
        });
    }

    /**
     * Auto-saves settings in background without UI dialogs (completely non-blocking)
     */
    private void autoSaveSettings() {
        if (userPreferences == null || isUpdatingSettings) {
            return;
        }
        
        isUpdatingSettings = true;
        
        // Immediate UI feedback
        SwingUtilities.invokeLater(() -> {
            hasUnsavedChanges = false;
            if (saveButton != null) {
                saveButton.setEnabled(false);
            }
        });
        
        // Perform save completely asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Simulate or perform actual save operations
                // All Firebase operations should already be async with callbacks
                Thread.sleep(50); // Minimal delay
            } catch (Exception e) {
                System.err.println("Error in auto-save: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    isUpdatingSettings = false;
                });
            }
        });
    }

    /**
     * Saves the current settings
     */
    private void saveSettings() {
        if (userPreferences == null) {
            return;
        }

        // Disable save button during save
        saveButton.setEnabled(false);
        saveButton.setText("Enregistrement...");

        // The profile image is already saved when it's changed,
        // so here we just need to finalize the save operation
        try {
            SwingUtilities.invokeLater(() -> {
                hasUnsavedChanges = false;
                saveButton.setText("Enregistrer");
                saveButton.setEnabled(false);

                // Create a success dialog with custom styling
                JPanel successPanel = new JPanel();
                successPanel.setLayout(new BoxLayout(successPanel, BoxLayout.Y_AXIS));
                successPanel.setBackground(CARD_BACKGROUND);

                JLabel successIcon = new JLabel("✓");
                successIcon.setFont(new Font("Segoe UI", Font.BOLD, 36));
                successIcon.setForeground(EMSI_GREEN);
                successIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel successText = new JLabel("Paramètres enregistrés avec succès!");
                successText.setFont(new Font("Segoe UI", Font.BOLD, 16));
                successText.setForeground(EMSI_GRAY);
                successText.setAlignmentX(Component.CENTER_ALIGNMENT);

                successPanel.add(Box.createVerticalStrut(10));
                successPanel.add(successIcon);
                successPanel.add(Box.createVerticalStrut(10));
                successPanel.add(successText);
                successPanel.add(Box.createVerticalStrut(10));

                // Custom JOptionPane properties
                UIManager.put("OptionPane.background", CARD_BACKGROUND);
                UIManager.put("Panel.background", CARD_BACKGROUND);

                JOptionPane.showMessageDialog(
                        UserSettingsPage.this,
                        successPanel,
                        "Succès",
                        JOptionPane.PLAIN_MESSAGE
                );

                // Reset UIManager properties
                UIManager.put("OptionPane.background", null);
                UIManager.put("Panel.background", null);

                // Notify listeners that settings have been saved
                notifySettingsSaved();
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(UserSettingsPage.this,
                        "Erreur lors de l'enregistrement: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);

                saveButton.setText("Enregistrer");
                saveButton.setEnabled(true);
            });

            e.printStackTrace();
        }
    }

    /**
     * Cancels changes and closes settings
     */
    private void cancelChanges() {
        if (hasUnsavedChanges) {
            // Custom styled confirmation dialog
            JPanel confirmPanel = new JPanel();
            confirmPanel.setLayout(new BoxLayout(confirmPanel, BoxLayout.Y_AXIS));
            confirmPanel.setBackground(CARD_BACKGROUND);

            JLabel confirmIcon = new JLabel("⚠️");
            confirmIcon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            confirmIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel confirmTitle = new JLabel("Modifications non enregistrées");
            confirmTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            confirmTitle.setForeground(EMSI_GRAY);
            confirmTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel confirmText = new JLabel("Voulez-vous vraiment fermer sans enregistrer?");
            confirmText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            confirmText.setForeground(EMSI_GRAY);
            confirmText.setAlignmentX(Component.CENTER_ALIGNMENT);

            confirmPanel.add(Box.createVerticalStrut(10));
            confirmPanel.add(confirmIcon);
            confirmPanel.add(Box.createVerticalStrut(10));
            confirmPanel.add(confirmTitle);
            confirmPanel.add(Box.createVerticalStrut(10));
            confirmPanel.add(confirmText);
            confirmPanel.add(Box.createVerticalStrut(10));

            // Custom JOptionPane properties
            UIManager.put("OptionPane.background", CARD_BACKGROUND);
            UIManager.put("Panel.background", CARD_BACKGROUND);
            UIManager.put("OptionPane.messageForeground", EMSI_GRAY);
            UIManager.put("Button.background", EMSI_GRAY_LIGHT);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", BUTTON_FONT);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    confirmPanel,
                    "Confirmer",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            // Reset UIManager properties
            UIManager.put("OptionPane.background", null);
            UIManager.put("Panel.background", null);
            UIManager.put("OptionPane.messageForeground", null);
            UIManager.put("Button.background", null);
            UIManager.put("Button.foreground", null);
            UIManager.put("Button.font", null);

            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        closeSettings();
    }

    /**
     * Closes the settings dialog
     */
    private void closeSettings() {
        // Always close the parent window (JFrame/JDialog) if possible
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            boolean canFade = true;
            if (window instanceof Dialog) {
                canFade = ((Dialog) window).isUndecorated();
            }
            if (canFade && window.isDisplayable() && window.isOpaque()) {
                // Fade-out animation for undecorated windows
                Timer timer = new Timer(20, null);
                final float[] opacity = {1.0f};
                final float step = 0.05f;
                timer.addActionListener(e -> {
                    opacity[0] -= step;
                    if (opacity[0] <= 0) {
                        timer.stop();
                        window.dispose();
                    } else {
                        window.setOpacity(opacity[0]);
                    }
                });
                timer.start();
            } else {
                // Just close immediately for decorated dialogs/windows
                window.dispose();
            }
        } else {
            // Fallback - just make this component invisible
            setVisible(false);
            // Try to find and close parent container
            Container container = getParent();
            if (container != null) {
                container.remove(this);
                container.revalidate();
                container.repaint();
            }
        }
    }

    /**
     * Utility method to add this settings page to a JTabbedPane
     * @param tabbedPane The tabbed pane to add to
     * @param title The title for the tab (default: "Paramètres")
     */
    public void addToTabbedPane(JTabbedPane tabbedPane, String title) {
        if (title == null || title.isEmpty()) {
            title = "Paramètres";
        }
        
        // Load settings icon for tab
        ImageIcon tabIcon = null;
        try {
            ImageIcon icon = new ImageIcon("pictures/settings.png");
            if (icon.getIconWidth() > 0) {
                Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                tabIcon = new ImageIcon(img);
            }
        } catch (Exception e) {
            // Use text fallback
        }
        
        if (tabIcon != null) {
            tabbedPane.addTab(title, tabIcon, this, "Paramètres utilisateur");
        } else {
            tabbedPane.addTab(title, this);
        }
        
        // Select this tab
        tabbedPane.setSelectedComponent(this);
    }
    



    /**
     * Utility method to add this settings page to a JTabbedPane with default title
     * @param tabbedPane The tabbed pane to add to
     */
    public void addToTabbedPane(JTabbedPane tabbedPane) {
        addToTabbedPane(tabbedPane, "Paramètres");
    }

    /**
     * Notifies all registered listeners that settings have been saved
     */
    private void notifySettingsSaved() {
        for (SettingsSavedListener listener : settingsSavedListeners) {
            listener.onSettingsSaved();
        }
    }
}