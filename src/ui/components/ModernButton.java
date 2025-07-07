package ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * ModernButton - A beautifully designed button component with modern styling
 * Provides consistent design across the entire application
 */
public class ModernButton extends JButton {
    
    // Design Constants
    private static final int DEFAULT_RADIUS = 12;
    private static final int SHADOW_SIZE = 4;
    private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.BOLD, 14);
    
    // EMSI Brand Colors
    private static final Color EMSI_GREEN = new Color(0, 150, 70);
    private static final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private static final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private static final Color EMSI_GRAY = new Color(90, 90, 90);
    private static final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private static final Color EMSI_RED = new Color(217, 83, 30);
    private static final Color EMSI_RED_LIGHT = new Color(237, 103, 50);
    
    // Button Types
    public enum ButtonType {
        PRIMARY(EMSI_GREEN, EMSI_GREEN_LIGHT, EMSI_GREEN_DARK, Color.WHITE),
        SECONDARY(EMSI_GRAY, EMSI_GRAY_LIGHT, EMSI_GRAY.darker(), Color.WHITE),
        DANGER(EMSI_RED, EMSI_RED_LIGHT, EMSI_RED.darker(), Color.WHITE),
        OUTLINE(Color.WHITE, new Color(248, 249, 250), new Color(230, 230, 230), EMSI_GREEN),
        GHOST(new Color(0, 0, 0, 0), new Color(0, 0, 0, 20), new Color(0, 0, 0, 40), EMSI_GREEN);
        
        final Color baseColor;
        final Color hoverColor;
        final Color pressedColor;
        final Color textColor;
        
        ButtonType(Color base, Color hover, Color pressed, Color text) {
            this.baseColor = base;
            this.hoverColor = hover;
            this.pressedColor = pressed;
            this.textColor = text;
        }
    }
    
    public enum ButtonSize {
        SMALL(8, 16, 12),
        MEDIUM(12, 24, 14),
        LARGE(16, 32, 16);
        
        final int verticalPadding;
        final int horizontalPadding;
        final int fontSize;
        
        ButtonSize(int vPad, int hPad, int size) {
            this.verticalPadding = vPad;
            this.horizontalPadding = hPad;
            this.fontSize = size;
        }
    }
    
    // Instance variables
    private ButtonType buttonType;
    private ButtonSize buttonSize;
    private int cornerRadius;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean showShadow = true;
    private Icon icon;
    private int iconTextGap = 8;
    
    /**
     * Creates a primary medium button
     */
    public ModernButton(String text) {
        this(text, ButtonType.PRIMARY, ButtonSize.MEDIUM);
    }
    
    /**
     * Creates a button with specified type and size
     */
    public ModernButton(String text, ButtonType type, ButtonSize size) {
        super(text);
        this.buttonType = type != null ? type : ButtonType.PRIMARY;
        this.buttonSize = size != null ? size : ButtonSize.MEDIUM;
        this.cornerRadius = DEFAULT_RADIUS;
        
        initializeButton();
        setupEventHandlers();
    }
    
    /**
     * Creates an icon-only button
     */
    public ModernButton(Icon icon, ButtonType type, ButtonSize size) {
        super();
        this.icon = icon;
        this.buttonType = type != null ? type : ButtonType.PRIMARY;
        this.buttonSize = size != null ? size : ButtonSize.MEDIUM;
        this.cornerRadius = DEFAULT_RADIUS;
        
        setIcon(icon);
        initializeButton();
        setupEventHandlers();
    }
    
    private void initializeButton() {
        // Ensure buttonSize and buttonType are initialized
        if (buttonSize == null) {
            buttonSize = ButtonSize.MEDIUM;
        }
        if (buttonType == null) {
            buttonType = ButtonType.PRIMARY;
        }
        
        // Remove default button styling
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        
        // Set font and colors
        setFont(new Font("Segoe UI", Font.BOLD, buttonSize.fontSize));
        setForeground(buttonType.textColor);
        
        // Set size based on button size
        updateSize();
        
        // Set cursor
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private void updateSize() {
        if (buttonSize == null) {
            buttonSize = ButtonSize.MEDIUM; // Fallback
        }
        
        int width = 0;
        int height = buttonSize.verticalPadding * 2 + buttonSize.fontSize + 4;
        
        if (getText() != null && !getText().isEmpty()) {
            FontMetrics fm = getFontMetrics(getFont());
            if (fm != null) {
                width = fm.stringWidth(getText()) + buttonSize.horizontalPadding * 2;
                if (icon != null) {
                    width += icon.getIconWidth() + iconTextGap;
                }
            }
        } else if (icon != null) {
            width = icon.getIconWidth() + buttonSize.horizontalPadding * 2;
            height = Math.max(height, icon.getIconHeight() + buttonSize.verticalPadding * 2);
        }
        
        if (width > 0 && height > 0) {
            setPreferredSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
        }
    }
    
    private void setupEventHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = false;
                    repaint();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = false;
                    repaint();
                }
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Get current color based on state
        Color currentColor = getCurrentColor();
        
        // Draw shadow if enabled
        if (showShadow && isEnabled() && !isPressed && buttonType != ButtonType.GHOST) {
            g2d.setColor(new Color(0, 0, 0, 20));
            g2d.fill(new RoundRectangle2D.Float(SHADOW_SIZE, SHADOW_SIZE, 
                width - SHADOW_SIZE, height - SHADOW_SIZE, cornerRadius, cornerRadius));
        }
        
        // Draw main button
        if (buttonType == ButtonType.OUTLINE) {
            // Draw outline button
            g2d.setColor(currentColor);
            g2d.fill(new RoundRectangle2D.Float(0, 0, width - SHADOW_SIZE, height - SHADOW_SIZE, 
                cornerRadius, cornerRadius));
            
            g2d.setColor(buttonType.textColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new RoundRectangle2D.Float(1, 1, width - SHADOW_SIZE - 2, height - SHADOW_SIZE - 2, 
                cornerRadius, cornerRadius));
        } else if (buttonType == ButtonType.GHOST) {
            // Ghost button - only background on hover
            if (isHovered || isPressed) {
                g2d.setColor(currentColor);
                g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));
            }
        } else {
            // Solid button with gradient
            GradientPaint gradient = new GradientPaint(
                0, 0, currentColor,
                0, height, currentColor.darker()
            );
            g2d.setPaint(gradient);
            g2d.fill(new RoundRectangle2D.Float(0, 0, width - SHADOW_SIZE, height - SHADOW_SIZE, 
                cornerRadius, cornerRadius));
        }
        
        // Draw content (icon and/or text)
        drawContent(g2d, width - SHADOW_SIZE, height - SHADOW_SIZE);
        
        g2d.dispose();
    }
    
    private void drawContent(Graphics2D g2d, int width, int height) {
        g2d.setColor(isEnabled() ? buttonType.textColor : buttonType.textColor.darker());
        g2d.setFont(getFont());
        
        FontMetrics fm = g2d.getFontMetrics();
        
        int contentWidth = 0;
        if (icon != null) contentWidth += icon.getIconWidth();
        if (getText() != null && !getText().isEmpty()) {
            contentWidth += fm.stringWidth(getText());
            if (icon != null) contentWidth += iconTextGap;
        }
        
        int startX = (width - contentWidth) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        
        // Draw icon
        if (icon != null) {
            int iconY = (height - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2d, startX, iconY);
            startX += icon.getIconWidth() + iconTextGap;
        }
        
        // Draw text
        if (getText() != null && !getText().isEmpty()) {
            g2d.drawString(getText(), startX, textY);
        }
    }
    
    private Color getCurrentColor() {
        if (!isEnabled()) {
            return new Color(buttonType.baseColor.getRed(), 
                           buttonType.baseColor.getGreen(), 
                           buttonType.baseColor.getBlue(), 100);
        }
        
        if (isPressed) return buttonType.pressedColor;
        if (isHovered) return buttonType.hoverColor;
        return buttonType.baseColor;
    }
    
    // Setters for customization
    public void setButtonType(ButtonType type) {
        this.buttonType = type;
        setForeground(type.textColor);
        repaint();
    }
    
    public void setButtonSize(ButtonSize size) {
        this.buttonSize = size;
        setFont(new Font("Segoe UI", Font.BOLD, size.fontSize));
        updateSize();
        repaint();
    }
    
    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }
    
    public void setShadowEnabled(boolean enabled) {
        this.showShadow = enabled;
        repaint();
    }
    
    public void setIconTextGap(int gap) {
        this.iconTextGap = gap;
        if (buttonSize != null) {
            updateSize();
            repaint();
        }
    }
    
    // Factory methods for common button types
    public static ModernButton createPrimary(String text) {
        return new ModernButton(text, ButtonType.PRIMARY, ButtonSize.MEDIUM);
    }
    
    public static ModernButton createSecondary(String text) {
        return new ModernButton(text, ButtonType.SECONDARY, ButtonSize.MEDIUM);
    }
    
    public static ModernButton createDanger(String text) {
        return new ModernButton(text, ButtonType.DANGER, ButtonSize.MEDIUM);
    }
    
    public static ModernButton createOutline(String text) {
        return new ModernButton(text, ButtonType.OUTLINE, ButtonSize.MEDIUM);
    }
    
    public static ModernButton createGhost(String text) {
        return new ModernButton(text, ButtonType.GHOST, ButtonSize.MEDIUM);
    }
    
    public static ModernButton createIconButton(Icon icon, ButtonType type) {
        return new ModernButton(icon, type, ButtonSize.MEDIUM);
    }
}