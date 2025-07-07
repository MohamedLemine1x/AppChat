package ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * ToggleSwitch - A modern toggle switch component
 * Provides smooth animations and customizable appearance
 */
public class ToggleSwitch extends JComponent {

    // Colors
    private final Color ENABLED_COLOR = new Color(0, 150, 70);
    private final Color ENABLED_COLOR_HOVER = new Color(0, 180, 85);
    private final Color DISABLED_COLOR = new Color(180, 180, 180);
    private final Color DISABLED_COLOR_HOVER = new Color(160, 160, 160);
    private final Color HANDLE_COLOR = Color.WHITE;
    private final Color HANDLE_SHADOW = new Color(0, 0, 0, 30);

    // State
    private boolean selected = false;
    private boolean enabled = true;
    private boolean hover = false;

    // Animation
    private float animationProgress = 0.0f;
    private Timer animationTimer;
    private final int ANIMATION_DURATION = 200; // milliseconds

    // Dimensions
    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 25;
    private static final int HANDLE_MARGIN = 2;

    // Listeners
    private List<ChangeListener> changeListeners = new ArrayList<>();

    /**
     * Creates a new toggle switch
     */
    public ToggleSwitch() {
        this(false);
    }

    /**
     * Creates a new toggle switch with initial state
     * @param selected Initial selected state
     */
    public ToggleSwitch(boolean selected) {
        this.selected = selected;
        this.animationProgress = selected ? 1.0f : 0.0f;

        setupComponent();
        setupAnimation();
        setupMouseListeners();
    }

    /**
     * Sets up basic component properties
     */
    private void setupComponent() {
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFocusable(true);
    }

    /**
     * Sets up animation timer
     */
    private void setupAnimation() {
        animationTimer = new Timer(16, e -> { // ~60 FPS
            float targetProgress = selected ? 1.0f : 0.0f;
            float speed = 4.0f / (ANIMATION_DURATION / 16.0f); // Animation speed

            if (Math.abs(animationProgress - targetProgress) < 0.01f) {
                animationProgress = targetProgress;
                animationTimer.stop();
            } else {
                animationProgress += (targetProgress - animationProgress) * speed;
                repaint();
            }
        });
    }

    /**
     * Sets up mouse event listeners
     */
    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (enabled) {
                    toggle();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        };

        addMouseListener(mouseAdapter);

        // Keyboard support
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (enabled && (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE ||
                        e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)) {
                    toggle();
                }
            }
        });
    }

    /**
     * Toggles the switch state
     */
    public void toggle() {
        setSelected(!selected);
    }

    /**
     * Sets the selected state
     * @param selected The selected state
     */
    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;

            // Start animation
            if (!animationTimer.isRunning()) {
                animationTimer.start();
            }

            // Fire change event
            fireStateChanged();
        }
    }

    /**
     * Gets the selected state
     * @return true if selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the enabled state
     * @param enabled The enabled state
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setCursor(enabled ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /**
     * Gets the enabled state
     * @return true if enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Adds a change listener
     * @param listener The change listener
     */
    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes a change listener
     * @param listener The change listener
     */
    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    /**
     * Fires change event to all listeners
     */
    private void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(event);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background track
        Color trackColor;
        if (!enabled) {
            trackColor = DISABLED_COLOR;
        } else if (selected) {
            trackColor = hover ? ENABLED_COLOR_HOVER : ENABLED_COLOR;
        } else {
            trackColor = hover ? DISABLED_COLOR_HOVER : DISABLED_COLOR;
        }

        g2.setColor(trackColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));

        // Handle
        int handleSize = height - (HANDLE_MARGIN * 2);
        int handleX = (int) (HANDLE_MARGIN + (animationProgress * (width - handleSize - (HANDLE_MARGIN * 2))));
        int handleY = HANDLE_MARGIN;

        // Handle shadow
        g2.setColor(HANDLE_SHADOW);
        g2.fillOval(handleX + 1, handleY + 1, handleSize, handleSize);

        // Handle
        g2.setColor(enabled ? HANDLE_COLOR : new Color(240, 240, 240));
        g2.fillOval(handleX, handleY, handleSize, handleSize);

        g2.dispose();
    }
}