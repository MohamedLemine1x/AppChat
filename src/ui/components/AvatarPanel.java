package ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * AvatarPanel - Custom avatar component with initials, images, and status indicators
 * Supports different sizes, status indicators, and hover effects
 */
public class AvatarPanel extends JPanel {
    // Avatar properties
    private int size = 40;
    private String initials = "?";
    private String fullName = "";
    private Image avatarImage = null;
    private Color backgroundColor;
    private Color textColor = Color.WHITE;
    private Color borderColor = null;
    private int borderWidth = 0;

    // File path for the current image
    private String currentImagePath = null;
    private BufferedImage currentImage = null;
    
    // Loading state
    private boolean isLoading = false;
    private Timer loadingTimer;
    private int loadingAngle = 0;

    // UI Colors
    private static final Color EMSI_GREEN = new Color(0, 150, 70);
    private static final Color EMSI_GRAY = new Color(108, 117, 125);

    // Status indicator
    private boolean showStatus = false;
    private StatusType status = StatusType.OFFLINE;
    private int statusSize = 12;

    // Hover effects
    private boolean hasHoverEffect = false;
    private boolean isHovered = false;
    private float hoverScale = 1.05f;
    
    // Group icon settings
    private boolean showGroupIcon = false;

    // Change listener interface
    public interface AvatarChangeListener {
        void onImageChanged(String imagePath, BufferedImage image);
        void onImageRemoved();
    }

    // Change listener
    private AvatarChangeListener changeListener;

    // Status types enum
    public enum StatusType {
        ONLINE(new Color(40, 167, 69)),
        AWAY(new Color(255, 193, 7)),
        BUSY(new Color(220, 53, 69)),
        OFFLINE(new Color(108, 117, 125));

        private final Color color;

        StatusType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    // Constructors
    public AvatarPanel() {
        this(40);
    }

    public AvatarPanel(int size) {
        this(size, generateRandomColor());
    }

    public AvatarPanel(int size, Color backgroundColor) {
        this.size = size;
        this.backgroundColor = backgroundColor;

        initializePanel();
    }

    public AvatarPanel(int size, String fullName) {
        this(size, generateColorFromName(fullName));
        setFullName(fullName);
    }

    public AvatarPanel(int size, String fullName, String imagePath) {
        this(size, fullName);
        loadImageFromPath(imagePath);
    }

    private void initializePanel() {
        setPreferredSize(new Dimension(size, size));
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listeners for hover effect
        if (hasHoverEffect) {
            setupHoverListeners();
        }
    }

    private void setupHoverListeners() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate size with hover effect
        int actualSize = size;
        int offsetX = 0, offsetY = 0;

        if (hasHoverEffect && isHovered) {
            actualSize = (int) (size * hoverScale);
            offsetX = (size - actualSize) / 2;
            offsetY = (size - actualSize) / 2;
        }

        // Create circular clip
        Ellipse2D circle = new Ellipse2D.Float(offsetX, offsetY, actualSize, actualSize);
        g2d.setClip(circle);

        // Draw avatar background or image
        if (avatarImage != null) {
            drawAvatarImage(g2d, offsetX, offsetY, actualSize);
        } else {
            drawAvatarBackground(g2d, offsetX, offsetY, actualSize);
            drawInitials(g2d, offsetX, offsetY, actualSize);
        }

        // Reset clip
        g2d.setClip(null);
        
        // Draw loading indicator if loading
        if (isLoading) {
            drawLoadingIndicator(g2d, offsetX, offsetY, actualSize);
        }

        // Draw border
        if (borderColor != null && borderWidth > 0) {
            drawBorder(g2d, offsetX, offsetY, actualSize);
        }

        // Draw status indicator
        if (showStatus) {
            drawStatusIndicator(g2d, offsetX, offsetY, actualSize);
        }

        g2d.dispose();
    }

    private void drawAvatarImage(Graphics2D g2d, int x, int y, int size) {
        // Scale and draw the image to fit the circle
        g2d.drawImage(avatarImage, x, y, size, size, null);
    }

    private void drawAvatarBackground(Graphics2D g2d, int x, int y, int size) {
        // Draw background circle
        g2d.setColor(backgroundColor);
        g2d.fillOval(x, y, size, size);
    }

    private void drawInitials(Graphics2D g2d, int x, int y, int size) {
        if (showGroupIcon) {
            drawGroupIcon(g2d, x, y, size);
        } else {
            drawTextInitials(g2d, x, y, size);
        }
    }
    
    private void drawTextInitials(Graphics2D g2d, int x, int y, int size) {
        if (initials.isEmpty()) return;

        // Calculate font size based on avatar size
        int fontSize = Math.max(size / 3, 12);
        Font font = new Font("Segoe UI", Font.BOLD, fontSize);
        g2d.setFont(font);
        g2d.setColor(textColor);

        // Center the text
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(initials);
        int textHeight = fm.getAscent();

        int textX = x + (size - textWidth) / 2;
        int textY = y + (size - textHeight) / 2 + textHeight;

        g2d.drawString(initials, textX, textY);
    }
    
    private void drawGroupIcon(Graphics2D g2d, int x, int y, int size) {
        g2d.setColor(textColor);
        g2d.setStroke(new BasicStroke(Math.max(size / 20, 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int iconSize = size / 3;
        
        // Draw multiple people silhouettes for group icon
        int personWidth = iconSize / 3;
        int personHeight = iconSize / 2;
        
        // First person (left)
        int person1X = centerX - iconSize / 2;
        int person1Y = centerY - personHeight / 2;
        drawPersonSilhouette(g2d, person1X, person1Y, personWidth, personHeight);
        
        // Second person (center-right, slightly overlapping)
        int person2X = centerX - personWidth / 4;
        int person2Y = centerY - personHeight / 2;
        drawPersonSilhouette(g2d, person2X, person2Y, personWidth, personHeight);
        
        // Third person (right, more overlapping)
        int person3X = centerX + iconSize / 4;
        int person3Y = centerY - personHeight / 2;
        drawPersonSilhouette(g2d, person3X, person3Y, personWidth, personHeight);
    }
    
    private void drawPersonSilhouette(Graphics2D g2d, int x, int y, int width, int height) {
        // Head (circle)
        int headSize = width / 2;
        int headX = x + (width - headSize) / 2;
        int headY = y;
        g2d.fillOval(headX, headY, headSize, headSize);
        
        // Body (rounded rectangle)
        int bodyWidth = width * 3 / 4;
        int bodyHeight = height * 2 / 3;
        int bodyX = x + (width - bodyWidth) / 2;
        int bodyY = y + headSize - 2;
        g2d.fillRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, bodyWidth / 3, bodyWidth / 3);
    }

    private void drawBorder(Graphics2D g2d, int x, int y, int size) {
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawOval(x, y, size - 1, size - 1);
    }

    private void drawStatusIndicator(Graphics2D g2d, int x, int y, int size) {
        // Calculate status indicator position (bottom-right)
        int statusX = x + size - statusSize - 2;
        int statusY = y + size - statusSize - 2;

        // Draw status background (white border)
        g2d.setColor(Color.WHITE);
        g2d.fillOval(statusX - 1, statusY - 1, statusSize + 2, statusSize + 2);

        // Draw status indicator
        g2d.setColor(status.getColor());
        g2d.fillOval(statusX, statusY, statusSize, statusSize);
    }

    // Utility methods for color generation
    private static Color generateRandomColor() {
        Color[] colors = {
                new Color(255, 87, 87),   // Red
                new Color(255, 167, 38),  // Orange
                new Color(255, 206, 84),  // Yellow
                new Color(72, 201, 176),  // Teal
                new Color(116, 185, 255), // Blue
                new Color(162, 155, 254), // Purple
                new Color(223, 117, 20),  // Brown
                new Color(158, 158, 158)  // Gray
        };
        return colors[(int) (Math.random() * colors.length)];
    }

    private static Color generateColorFromName(String name) {
        if (name == null || name.isEmpty()) {
            return generateRandomColor();
        }

        // Generate color based on name hash
        int hash = name.hashCode();
        int r = Math.abs((hash & 0xFF0000) >> 16) % 200 + 55;
        int g = Math.abs((hash & 0x00FF00) >> 8) % 200 + 55;
        int b = Math.abs(hash & 0x0000FF) % 200 + 55;

        return new Color(r, g, b);
    }

    private String generateInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }

        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }

        return initials.toString().toUpperCase();
    }
    
    private static String generateGroupInitials(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return "G";
        }

        String[] parts = groupName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }

        String result = initials.toString().toUpperCase();
        return result.isEmpty() ? "G" : result;
    }

    // Public methods
    public void loadImageFromPath(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                BufferedImage img = ImageIO.read(imageFile);
                setAvatarImage(img);
                this.currentImage = img;
                this.currentImagePath = imagePath;
            }
        } catch (IOException e) {
            System.err.println("Could not load avatar image: " + e.getMessage());
        }
    }

    /**
     * Sets the avatar image using a BufferedImage
     * This method is specifically for ProfileImageEditor compatibility
     */
    public void setAvatarImage(BufferedImage image) {
        this.avatarImage = image;
        this.currentImage = image;
        stopLoading(); // Stop loading when image is set
        repaint();
    }
    
    /**
     * Start loading animation
     */
    public void startLoading() {
        isLoading = true;
        if (loadingTimer == null) {
            loadingTimer = new Timer(50, e -> {
                loadingAngle = (loadingAngle + 10) % 360;
                repaint();
            });
        }
        loadingTimer.start();
    }
    
    /**
     * Stop loading animation
     */
    public void stopLoading() {
        isLoading = false;
        if (loadingTimer != null) {
            loadingTimer.stop();
        }
        repaint();
    }
    
    /**
     * Draw loading indicator
     */
    private void drawLoadingIndicator(Graphics2D g2d, int offsetX, int offsetY, int actualSize) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillOval(offsetX, offsetY, actualSize, actualSize);
        
        // Spinning indicator
        int centerX = offsetX + actualSize / 2;
        int centerY = offsetY + actualSize / 2;
        int radius = actualSize / 4;
        
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        for (int i = 0; i < 8; i++) {
            float alpha = 1.0f - (i * 0.12f);
            if (alpha < 0.2f) alpha = 0.2f;
            
            int angle = (loadingAngle + i * 45) % 360;
            double radians = Math.toRadians(angle);
            
            int x1 = centerX + (int) (Math.cos(radians) * radius * 0.5);
            int y1 = centerY + (int) (Math.sin(radians) * radius * 0.5);
            int x2 = centerX + (int) (Math.cos(radians) * radius);
            int y2 = centerY + (int) (Math.sin(radians) * radius);
            
            g2d.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Clears the current image
     */
    public void clearImage() {
        this.avatarImage = null;
        this.currentImage = null;
        this.currentImagePath = null;
        repaint();
    }

    public boolean hasImage() {
        return avatarImage != null;
    }

    // Getter and setter methods
    public int getAvatarSize() {
        return size;
    }

    public void setAvatarSize(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        repaint();
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials != null ? initials.toUpperCase() : "?";
        repaint();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.initials = generateInitials(fullName);
        repaint();
    }

    @Override
    public void setBackground(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        repaint();
    }

    @Override
    public Color getBackground() {
        return backgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        repaint();
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public void setShowStatus(boolean showStatus) {
        this.showStatus = showStatus;
        repaint();
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
        repaint();
    }

    public int getStatusSize() {
        return statusSize;
    }

    public void setStatusSize(int statusSize) {
        this.statusSize = statusSize;
        repaint();
    }

    public boolean hasHoverEffect() {
        return hasHoverEffect;
    }

    public void setHasHoverEffect(boolean hasHoverEffect) {
        this.hasHoverEffect = hasHoverEffect;
        if (hasHoverEffect) {
            setupHoverListeners();
        }
    }

    public float getHoverScale() {
        return hoverScale;
    }

    public void setHoverScale(float hoverScale) {
        this.hoverScale = hoverScale;
    }
    
    public boolean isShowGroupIcon() {
        return showGroupIcon;
    }
    
    public void setShowGroupIcon(boolean showGroupIcon) {
        this.showGroupIcon = showGroupIcon;
        repaint();
    }

    public void setChangeListener(AvatarChangeListener listener) {
        this.changeListener = listener;
    }

    public String getCurrentImagePath() {
        return currentImagePath;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    // Factory methods for common avatar types
    public static AvatarPanel createUserAvatar(String fullName, int size) {
        AvatarPanel avatar = new AvatarPanel(size, fullName);
        avatar.setShowStatus(true);
        avatar.setHasHoverEffect(true);
        return avatar;
    }

    public static AvatarPanel createGroupAvatar(int size) {
        AvatarPanel avatar = new AvatarPanel(size, new Color(0, 150, 70));
        avatar.setInitials("G");
        avatar.setShowGroupIcon(true);
        return avatar;
    }
    
    public static AvatarPanel createGroupAvatar(String groupName, int size) {
        AvatarPanel avatar = new AvatarPanel(size, generateColorFromName(groupName));
        avatar.setInitials(generateGroupInitials(groupName));
        avatar.setShowGroupIcon(true);
        return avatar;
    }
    
    public static AvatarPanel createGroupAvatarWithImage(String groupName, String imagePath, int size) {
        AvatarPanel avatar = createGroupAvatar(groupName, size);
        if (imagePath != null && !imagePath.isEmpty()) {
            avatar.loadImageFromPath(imagePath);
        }
        return avatar;
    }

    public static AvatarPanel createSystemAvatar(int size) {
        AvatarPanel avatar = new AvatarPanel(size, new Color(0, 150, 70));
        avatar.setInitials("S");
        return avatar;
    }

    public static AvatarPanel createProfileAvatar(String fullName, String imagePath) {
        AvatarPanel avatar = new AvatarPanel(80, fullName, imagePath);
        avatar.setBorderColor(Color.WHITE);
        avatar.setBorderWidth(3);
        avatar.setShowStatus(true);
        avatar.setStatusSize(16);
        return avatar;
    }

    /**
     * Sets a single initial for the avatar
     */
    public void setInitial(String initial) {
        if (initial != null && !initial.isEmpty()) {
            this.initials = initial.substring(0, 1).toUpperCase();
            repaint();
        }
    }
    
    /**
     * Creates a test panel to demonstrate different avatar types
     * This is useful for testing and development
     */
    public static JPanel createTestPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Avatar Test Panel"));
        
        // Test different avatar types
        panel.add(createUserAvatar("John Doe", 60));
        panel.add(createGroupAvatar("Team Alpha", 60));
        panel.add(createGroupAvatarWithImage("Team Beta", null, 60));
        panel.add(createSystemAvatar(60));
        
        return panel;
    }
}