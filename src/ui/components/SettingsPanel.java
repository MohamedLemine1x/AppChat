package ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * SettingsPanel - A reusable container for grouping related settings
 * Provides a clean, modern card-like appearance for settings sections
 */
public class SettingsPanel extends JPanel {

    // Colors
    private final Color CARD_BACKGROUND = new Color(255, 255, 255);
    private final Color BORDER_COLOR = new Color(230, 235, 240);  // Subtle blue tint
    private final Color TITLE_COLOR = new Color(30, 41, 59);      // Modern slate
    private final Color SUBTITLE_COLOR = new Color(100, 116, 139); // Modern slate
    private final Color SHADOW_COLOR = new Color(0, 0, 0, 15);    // Transparent black for shadow

    private int cornerRadius = 16;  // Slightly larger corner radius
    private String title;
    private String subtitle;
    private JPanel contentPanel;
    private boolean showShadow = true;

    /**
     * Creates a settings panel with title
     * @param title The panel title
     */
    public SettingsPanel(String title) {
        this(title, null);
    }

    /**
     * Creates a settings panel with title and subtitle
     * @param title The panel title
     * @param subtitle The panel subtitle (optional)
     */
    public SettingsPanel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;

        initializePanel();
    }

    /**
     * Initializes the panel layout and components
     */
    private void initializePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(10, 0, 10, 0));

        // Create header if title is provided
        if (title != null && !title.trim().isEmpty()) {
            JPanel headerPanel = createHeader();
            add(headerPanel, BorderLayout.NORTH);
        }

        // Create content area with modern styling
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(15, 25, 25, 25));  // Increased padding

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Creates the header panel with title and subtitle
     */
    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(25, 25, title != null && subtitle != null ? 5 : 15, 25));  // Increased padding

        // Title label with modern font
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));  // Larger font size
        titleLabel.setForeground(TITLE_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleLabel);

        // Subtitle label with modern styling
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            headerPanel.add(Box.createVerticalStrut(8));  // Increased spacing

            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));  // Larger font size
            subtitleLabel.setForeground(SUBTITLE_COLOR);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerPanel.add(subtitleLabel);

            headerPanel.setBorder(new EmptyBorder(25, 25, 15, 25));
        }

        return headerPanel;
    }

    /**
     * Adds a component to the settings panel
     * @param component The component to add
     */
    public void addSetting(Component component) {
        contentPanel.add(component);
        contentPanel.add(Box.createVerticalStrut(5));
    }

    /**
     * Adds a component with custom spacing
     * @param component The component to add
     * @param spacing The spacing after the component
     */
    public void addSetting(Component component, int spacing) {
        contentPanel.add(component);
        if (spacing > 0) {
            contentPanel.add(Box.createVerticalStrut(spacing));
        }
    }

    /**
     * Adds a separator line
     */
    public void addSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER_COLOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(separator);
        contentPanel.add(Box.createVerticalStrut(10));
    }

    /**
     * Sets the corner radius for the panel
     * @param radius The corner radius
     */
    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw shadow if enabled
        if (showShadow) {
            for (int i = 0; i < 4; i++) {
                g2.setColor(new Color(SHADOW_COLOR.getRed(),
                                    SHADOW_COLOR.getGreen(),
                                    SHADOW_COLOR.getBlue(),
                                    SHADOW_COLOR.getAlpha() / (i + 2)));
                g2.fill(new RoundRectangle2D.Float(i, i, width - (i * 2), height - (i * 2), cornerRadius, cornerRadius));
            }
        }

        // Fill background with rounded corners
        g2.setColor(CARD_BACKGROUND);
        g2.fill(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));

        // Draw border with subtle gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(), 100),
            width, height, new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(), 50)
        );
        g2.setPaint(gradient);
        g2.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Sets whether to show the shadow effect
     * @param show true to show shadow, false to hide
     */
    public void setShowShadow(boolean show) {
        this.showShadow = show;
        repaint();
    }
}