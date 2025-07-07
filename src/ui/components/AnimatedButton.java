package ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * AnimatedButton - Custom animated button component
 * Features smooth hover animations, gradient backgrounds, and rounded corners
 */
public class AnimatedButton extends JButton {
    // Colors
    private Color baseColor;
    private Color hoverColor;
    private Color pressedColor;
    private Color textColor;

    // Animation state
    private boolean isHovered = false;
    private boolean isPressed = false;
    private float animationProgress = 0f;
    private Timer animationTimer;

    // Appearance settings
    private int cornerRadius = 10;
    private boolean hasGradient = true;
    private boolean hasShadow = true;
    private boolean hasShineEffect = true;

    // Default EMSI colors
    private static final Color EMSI_GREEN = new Color(0, 150, 70);
    private static final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private static final Color EMSI_GREEN_DARK = new Color(0, 120, 55);

    // Constructors
    public AnimatedButton() {
        this("Button", EMSI_GREEN, EMSI_GREEN_LIGHT, EMSI_GREEN_DARK, Color.WHITE);
    }

    public AnimatedButton(String text) {
        this(text, EMSI_GREEN, EMSI_GREEN_LIGHT, EMSI_GREEN_DARK, Color.WHITE);
    }

    public AnimatedButton(String text, Color baseColor) {
        this(text, baseColor, brighten(baseColor, 0.2f), darken(baseColor, 0.2f), Color.WHITE);
    }

    public AnimatedButton(String text, Color baseColor, Color hoverColor, Color pressedColor) {
        this(text, baseColor, hoverColor, pressedColor, Color.WHITE);
    }

    public AnimatedButton(String text, Color baseColor, Color hoverColor, Color pressedColor, Color textColor) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = hoverColor;
        this.pressedColor = pressedColor;
        this.textColor = textColor;

        initializeButton();
        setupAnimations();
        setupMouseListeners();
    }

    private void initializeButton() {
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setForeground(textColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(120, 40));
    }

    private void setupAnimations() {
        // Timer for smooth animations
        animationTimer = new Timer(16, e -> {
            if (isHovered && animationProgress < 1f) {
                animationProgress += 0.1f;
                if (animationProgress > 1f) animationProgress = 1f;
                repaint();
            } else if (!isHovered && animationProgress > 0f) {
                animationProgress -= 0.1f;
                if (animationProgress < 0f) animationProgress = 0f;
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                animationTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                animationTimer.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate current color based on state
        Color currentColor = getCurrentColor();

        // Draw shadow if enabled
        if (hasShadow && !isPressed) {
            drawShadow(g2d);
        }

        // Draw button background
        drawBackground(g2d, currentColor);

        // Draw shine effect if enabled
        if (hasShineEffect && !isPressed) {
            drawShineEffect(g2d);
        }

        // Draw text
        drawText(g2d);

        g2d.dispose();
    }

    private Color getCurrentColor() {
        if (!isEnabled()) {
            return desaturate(baseColor);
        } else if (isPressed) {
            return pressedColor;
        } else if (isHovered) {
            return interpolateColor(baseColor, hoverColor, animationProgress);
        } else {
            return interpolateColor(hoverColor, baseColor, Math.max(0, 1 - animationProgress));
        }
    }

    private void drawShadow(Graphics2D g2d) {
        // Create shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, cornerRadius, cornerRadius);
    }

    private void drawBackground(Graphics2D g2d, Color color) {
        if (hasGradient) {
            // Create gradient background
            GradientPaint gradient = new GradientPaint(
                    0, 0, color,
                    0, getHeight(), darken(color, 0.1f)
            );
            g2d.setPaint(gradient);
        } else {
            g2d.setColor(color);
        }

        // Draw rounded rectangle
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Draw border
        g2d.setColor(darken(color, 0.2f));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
    }

    private void drawShineEffect(Graphics2D g2d) {
        // Create shine effect at the top
        GradientPaint shine = new GradientPaint(
                0, 0, new Color(255, 255, 255, 60),
                0, getHeight() / 3, new Color(255, 255, 255, 0)
        );
        g2d.setPaint(shine);
        g2d.fillRoundRect(1, 1, getWidth() - 2, getHeight() / 3, cornerRadius - 1, cornerRadius - 1);
    }

    private void drawText(Graphics2D g2d) {
        // Calculate text position
        FontMetrics fm = g2d.getFontMetrics(getFont());
        Rectangle stringBounds = fm.getStringBounds(getText(), g2d).getBounds();

        int textX = (getWidth() - stringBounds.width) / 2;
        int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();

        // Draw text shadow
        if (hasShadow && !isPressed) {
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.setFont(getFont());
            g2d.drawString(getText(), textX + 1, textY + 1);
        }

        // Draw main text
        g2d.setColor(isEnabled() ? getForeground() : getForeground().darker());
        g2d.setFont(getFont());
        g2d.drawString(getText(), textX, textY);
    }

    // Utility methods for color manipulation
    private static Color interpolateColor(Color c1, Color c2, float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        int red = (int) (c1.getRed() + fraction * (c2.getRed() - c1.getRed()));
        int green = (int) (c1.getGreen() + fraction * (c2.getGreen() - c1.getGreen()));
        int blue = (int) (c1.getBlue() + fraction * (c2.getBlue() - c1.getBlue()));
        return new Color(red, green, blue);
    }

    private static Color brighten(Color color, float factor) {
        int red = Math.min(255, (int) (color.getRed() * (1 + factor)));
        int green = Math.min(255, (int) (color.getGreen() * (1 + factor)));
        int blue = Math.min(255, (int) (color.getBlue() * (1 + factor)));
        return new Color(red, green, blue);
    }

    private static Color darken(Color color, float factor) {
        int red = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int green = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int blue = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(red, green, blue);
    }

    private static Color desaturate(Color color) {
        int gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        return new Color(gray, gray, gray);
    }

    // Getter and setter methods
    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setHasGradient(boolean hasGradient) {
        this.hasGradient = hasGradient;
        repaint();
    }

    public boolean hasGradient() {
        return hasGradient;
    }

    public void setHasShadow(boolean hasShadow) {
        this.hasShadow = hasShadow;
        repaint();
    }

    public boolean hasShadow() {
        return hasShadow;
    }

    public void setHasShineEffect(boolean hasShineEffect) {
        this.hasShineEffect = hasShineEffect;
        repaint();
    }

    public boolean hasShineEffect() {
        return hasShineEffect;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor = baseColor;
        repaint();
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public void setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
        repaint();
    }

    public Color getHoverColor() {
        return hoverColor;
    }

    public void setPressedColor(Color pressedColor) {
        this.pressedColor = pressedColor;
        repaint();
    }

    public Color getPressedColor() {
        return pressedColor;
    }
}