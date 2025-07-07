package ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * RoundedPanel - Custom panel with rounded corners and gradient backgrounds
 * Supports shadows, borders, and various styling options
 */
public class RoundedPanel extends JPanel {
    // Appearance properties
    private int cornerRadius = 10;
    private Color backgroundColor;
    private Color borderColor;
    private int borderWidth = 1;
    private boolean hasShadow = false;
    private boolean hasGradient = false;
    private Color gradientColor;
    private int shadowOffset = 3;
    private Color shadowColor = new Color(0, 0, 0, 50);

    // Gradient direction
    public enum GradientDirection {
        VERTICAL, HORIZONTAL, DIAGONAL_UP, DIAGONAL_DOWN
    }
    private GradientDirection gradientDirection = GradientDirection.VERTICAL;

    // Constructors
    public RoundedPanel() {
        this(10, Color.WHITE);
    }

    public RoundedPanel(int radius) {
        this(radius, Color.WHITE);
    }

    public RoundedPanel(Color backgroundColor) {
        this(10, backgroundColor);
    }

    public RoundedPanel(int radius, Color backgroundColor) {
        this.cornerRadius = radius;
        this.backgroundColor = backgroundColor;

        setOpaque(false);
        initializePanel();
    }

    public RoundedPanel(int radius, Color backgroundColor, Color borderColor) {
        this(radius, backgroundColor);
        this.borderColor = borderColor;
    }

    public RoundedPanel(int radius, Color backgroundColor, Color borderColor, int borderWidth) {
        this(radius, backgroundColor, borderColor);
        this.borderWidth = borderWidth;
    }

    private void initializePanel() {
        // Set default properties
        setBackground(backgroundColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Adjust dimensions for shadow
        int paintWidth = hasShadow ? width - shadowOffset : width;
        int paintHeight = hasShadow ? height - shadowOffset : height;

        // Draw shadow
        if (hasShadow) {
            drawShadow(g2d, width, height);
        }

        // Create rounded rectangle shape
        RoundRectangle2D roundRect = new RoundRectangle2D.Float(
                0, 0, paintWidth - 1, paintHeight - 1, cornerRadius, cornerRadius);

        // Fill background
        if (hasGradient && gradientColor != null) {
            drawGradientBackground(g2d, roundRect, paintWidth, paintHeight);
        } else {
            g2d.setColor(backgroundColor);
            g2d.fill(roundRect);
        }

        // Draw border
        if (borderColor != null && borderWidth > 0) {
            drawBorder(g2d, roundRect);
        }

        g2d.dispose();
    }

    private void drawShadow(Graphics2D g2d, int width, int height) {
        // Create shadow shape
        RoundRectangle2D shadowRect = new RoundRectangle2D.Float(
                shadowOffset, shadowOffset,
                width - shadowOffset - 1, height - shadowOffset - 1,
                cornerRadius, cornerRadius);

        g2d.setColor(shadowColor);
        g2d.fill(shadowRect);
    }

    private void drawGradientBackground(Graphics2D g2d, RoundRectangle2D roundRect, int width, int height) {
        GradientPaint gradient;

        switch (gradientDirection) {
            case HORIZONTAL:
                gradient = new GradientPaint(0, 0, backgroundColor, width, 0, gradientColor);
                break;
            case DIAGONAL_UP:
                gradient = new GradientPaint(0, height, backgroundColor, width, 0, gradientColor);
                break;
            case DIAGONAL_DOWN:
                gradient = new GradientPaint(0, 0, backgroundColor, width, height, gradientColor);
                break;
            case VERTICAL:
            default:
                gradient = new GradientPaint(0, 0, backgroundColor, 0, height, gradientColor);
                break;
        }

        g2d.setPaint(gradient);
        g2d.fill(roundRect);
    }

    private void drawBorder(Graphics2D g2d, RoundRectangle2D roundRect) {
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.draw(roundRect);
    }

    // Getter and setter methods
    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    @Override
    public void setBackground(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        super.setBackground(backgroundColor);
        repaint();
    }

    @Override
    public Color getBackground() {
        return backgroundColor;
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

    public boolean hasShadow() {
        return hasShadow;
    }

    public void setHasShadow(boolean hasShadow) {
        this.hasShadow = hasShadow;
        repaint();
    }

    public int getShadowOffset() {
        return shadowOffset;
    }

    public void setShadowOffset(int shadowOffset) {
        this.shadowOffset = shadowOffset;
        repaint();
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
        repaint();
    }

    public boolean hasGradient() {
        return hasGradient;
    }

    public void setHasGradient(boolean hasGradient) {
        this.hasGradient = hasGradient;
        repaint();
    }

    public Color getGradientColor() {
        return gradientColor;
    }

    public void setGradientColor(Color gradientColor) {
        this.gradientColor = gradientColor;
        this.hasGradient = (gradientColor != null);
        repaint();
    }

    public GradientDirection getGradientDirection() {
        return gradientDirection;
    }

    public void setGradientDirection(GradientDirection gradientDirection) {
        this.gradientDirection = gradientDirection;
        repaint();
    }

    // Convenience methods for setting gradients
    public void setVerticalGradient(Color startColor, Color endColor) {
        setBackground(startColor);
        setGradientColor(endColor);
        setGradientDirection(GradientDirection.VERTICAL);
    }

    public void setHorizontalGradient(Color startColor, Color endColor) {
        setBackground(startColor);
        setGradientColor(endColor);
        setGradientDirection(GradientDirection.HORIZONTAL);
    }

    // Factory methods for common panel types
    public static RoundedPanel createCard() {
        RoundedPanel panel = new RoundedPanel(12, Color.WHITE);
        panel.setBorderColor(new Color(230, 230, 230));
        panel.setBorderWidth(1);
        panel.setHasShadow(true);
        panel.setShadowColor(new Color(0, 0, 0, 20));
        panel.setShadowOffset(2);
        return panel;
    }

    public static RoundedPanel createMessageBubble(Color backgroundColor) {
        RoundedPanel panel = new RoundedPanel(18, backgroundColor);
        panel.setHasShadow(true);
        panel.setShadowColor(new Color(0, 0, 0, 15));
        panel.setShadowOffset(1);
        return panel;
    }

    public static RoundedPanel createToolbar() {
        RoundedPanel panel = new RoundedPanel(8, new Color(248, 249, 250));
        panel.setBorderColor(new Color(220, 220, 220));
        panel.setBorderWidth(1);
        return panel;
    }

    public static RoundedPanel createSidebar() {
        RoundedPanel panel = new RoundedPanel(0, Color.WHITE);
        panel.setBorderColor(new Color(220, 220, 220));
        panel.setBorderWidth(1);
        return panel;
    }

    public static RoundedPanel createGradientPanel(Color startColor, Color endColor) {
        RoundedPanel panel = new RoundedPanel(10, startColor);
        panel.setGradientColor(endColor);
        return panel;
    }

    public static RoundedPanel createEMSICard() {
        RoundedPanel panel = new RoundedPanel(12, Color.WHITE);
        panel.setBorderColor(new Color(0, 150, 70));
        panel.setBorderWidth(2);
        panel.setHasShadow(true);
        panel.setShadowColor(new Color(0, 150, 70, 30));
        panel.setShadowOffset(3);
        return panel;
    }

    public static RoundedPanel createEMSIGradientPanel() {
        RoundedPanel panel = new RoundedPanel(10, new Color(0, 150, 70));
        panel.setGradientColor(new Color(0, 180, 85));
        return panel;
    }
}