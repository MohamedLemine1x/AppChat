package com.vibeapp;

import services.FirebaseService;
import ui.pages.ForgotPasswordPage;
import ui.pages.LoginPage;
import ui.pages.MainChat;
import ui.pages.RegisterPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.awt.geom.AffineTransform;

/**
 * com.vibeapp.VibeApp.java - Simple main class
 * Main application class that serves as the container for all pages
 * and manages navigation between different screens.
 */
public class VibeApp extends JFrame {
    // Singleton instance
    private static VibeApp instance;

    // CardLayout for page navigation
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Application constants
    public static final String APP_NAME = "com.vibeapp.VibeApp";
    public static final String APP_VERSION = "1.0.0";

    // Reference to current active user
    private String currentUserId = null;

    // Private constructor for singleton pattern
    private VibeApp() {
        // Basic frame setup
        setTitle(APP_NAME + " - Communication Platform");
        setSize(950, 650);
        setMinimumSize(new Dimension(800, 600)); // Set minimum size for responsive behavior
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true); // Enable resizing for responsive behavior

        // Set app icon
        try {
            setIconImage(createIcon("pictures/logoVibeApp.png"));
        } catch (Exception e) {
            System.err.println("Failed to load app icon: " + e.getMessage());
        }

        // Create content panel with CardLayout for easy page switching
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        // Initialize the application
        initialize();

        // Add window close listener to handle cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleApplicationExit();
            }
        });
    }

    // Method to get the singleton instance
    public static VibeApp getInstance() {
        if (instance == null) {
            instance = new VibeApp();
        }
        return instance;
    }

    // Method to navigate to a different page
    public void showPage(String pageName) {
        cardLayout.show(mainPanel, pageName);
        revalidate();
        repaint();
    }

    // Method to navigate to MainChat page with userId
    public void showMainChat(String userId) {
        System.out.println("Tentative d'affichage de MainChat pour userId: " + userId);

        this.currentUserId = userId;

        // Create MainChat only when needed to save memory
        // and to ensure we have the current userId
        MainChat mainChat = new MainChat(userId);

        // Remove existing MainChat if any
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof MainChat) {
                mainPanel.remove(comp);
                break;
            }
        }

        // Add new MainChat and show it
        mainPanel.add(mainChat, "mainChat");

        // Show the MainChat page
        showPage("mainChat");

        System.out.println("MainChat affiché avec succès");
    }

    // Main entry point
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Customize UI defaults
            UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("CheckBox.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("TabbedPane.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("ComboBox.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));
            UIManager.put("ComboBox.selectionForeground", new javax.swing.plaf.ColorUIResource(Color.WHITE));
            UIManager.put("TextField.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Show splash window with progress bar first
        SwingUtilities.invokeLater(() -> {
            SplashWindow splash = new SplashWindow();
            splash.setVisible(true);
        });
    }

    // Initialize the application
    private void initialize() {
        // Initialize Firebase first before creating any page
        initializeFirebase();

        // Create and add all pages (no splash)
        mainPanel.add(new LoginPage(), "login");
        mainPanel.add(new RegisterPage(), "register");
        mainPanel.add(new ForgotPasswordPage(), "forgotPassword");

        // Start with the login screen (will be shown after splash)
        showPage("login");
    }

    // Initialize Firebase
    private void initializeFirebase() {
        try {
            FirebaseService.getInstance();
            System.out.println("Firebase initialized in com.vibeapp.VibeApp");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erreur de connexion à Firebase: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Method to create an Image from a file or generate a default one
    private Image createIcon(String path) {
        try {
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                return new ImageIcon(file.getAbsolutePath()).getImage();
            } else {
                return createDefaultIcon();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultIcon();
        }
    }

    // Method to create a default icon if the file isn't found
    private Image createDefaultIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Use EMSI colors
        Color EMSI_GREEN = new Color(0, 150, 70);
        Color EMSI_GRAY = new Color(90, 90, 90);
        Color EMSI_RED = new Color(217, 83, 30);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(EMSI_GRAY);
        g2d.fillRect(10, 10, 40, 20);

        g2d.setColor(EMSI_GREEN);
        g2d.fillOval(12, 15, 25, 25);

        g2d.setColor(EMSI_RED);
        g2d.fillRect(44, 17, 8, 8);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("V", 20, 30);

        g2d.dispose();

        return image;
    }

    // Method to handle application exit
    private void handleApplicationExit() {
        // Perform any cleanup needed
        if (currentUserId != null) {
            try {
                // Update user status to offline
                FirebaseService.getInstance().getDatabase()
                        .getReference("users/" + currentUserId + "/status/online")
                        .setValueAsync(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Wait a bit for Firebase to complete operations
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Exit the application
        System.exit(0);
    }

    // New SplashWindow class with logo and animated progress bar
    private static class SplashWindow extends JWindow {
        private float progress = 0.0f;
        private final Timer progressTimer;
        private final Image logoImg;
        private final int logoWidth = 180;
        private final int logoHeight = 180;
        
        /**
         * Creates a fallback logo when the logo file is not found
         */
        private Image createFallbackLogo() {
            BufferedImage image = new BufferedImage(180, 180, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Background circle
            g2d.setColor(new Color(0, 150, 70));
            g2d.fillOval(10, 10, 160, 160);
            
            // Inner circles
            g2d.setColor(new Color(0, 180, 85));
            g2d.fillOval(30, 30, 120, 120);
            
            // Text
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "VibeApp";
            int x = (180 - fm.stringWidth(text)) / 2;
            int y = (180 - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(text, x, y);
            
            g2d.dispose();
            return image;
        }

        public SplashWindow() {
            setBackground(new Color(0, 0, 0, 0)); // Transparent window
            setSize(400, 300);
            setLocationRelativeTo(null);
            setAlwaysOnTop(true);
            // Load logo with better error handling
            Image img = null;
            try {
                java.io.File logoFile = new java.io.File("pictures/logoVibeApp.png");
                if (logoFile.exists()) {
                    img = new ImageIcon(logoFile.getAbsolutePath()).getImage();
                    System.out.println("Logo loaded successfully from: " + logoFile.getAbsolutePath());
                } else {
                    System.out.println("Logo file not found at: " + logoFile.getAbsolutePath());
                    img = createFallbackLogo();
                }
            } catch (Exception e) {
                System.err.println("Error loading logo: " + e.getMessage());
                img = createFallbackLogo();
            }
            logoImg = img;
            setContentPane(new JPanel() {
                @Override
                public boolean isOpaque() { return false; }
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    // Draw logo centered
                    if (logoImg != null) {
                        int x = (w - logoWidth) / 2;
                        int y = (h - logoHeight) / 2 - 30;
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, progress + 0.2f)));
                        g2d.drawImage(logoImg, x, y, logoWidth, logoHeight, null);
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        // Progress bar centered below logo
                        int barWidth = 180;
                        int barHeight = 12;
                        int barX = (w - barWidth) / 2;
                        int barY = y + logoHeight + 30;
                        int alpha = (int) (Math.min(1f, progress + 0.2f) * 255);
                        g2d.setColor(new Color(220, 220, 220, alpha));
                        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
                        g2d.setColor(new Color(0, 150, 70, alpha));
                        g2d.fillRoundRect(barX, barY, (int) (barWidth * progress), barHeight, 8, 8);
                    } else {
                        String text = "VibeApp";
                        g2d.setFont(new Font("Segoe UI", Font.BOLD, 32));
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(text);
                        int textHeight = fm.getHeight();
                        int x = (w - textWidth) / 2;
                        int y = (h - textHeight) / 2 + fm.getAscent();
                        g2d.setColor(new Color(0, 150, 70));
                        g2d.drawString(text, x, y);
                    }
                    g2d.dispose();
                }
            });
            // Animate progress
            progressTimer = new Timer(30, e -> {
                progress += 0.015f;
                if (progress > 1f) {
                    progress = 1f;
                    repaint();
                    ((Timer) e.getSource()).stop();
                    
                    // Add a small delay before showing main window
                    Timer delayTimer = new Timer(300, event -> {
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("Splash screen completed, showing main app");
                            dispose();
                            VibeApp app = VibeApp.getInstance();
                            app.setVisible(true);
                        });
                        ((Timer) event.getSource()).stop();
                    });
                    delayTimer.setRepeats(false);
                    delayTimer.start();
                }
                repaint();
            });
            progressTimer.start();
        }
    }
}