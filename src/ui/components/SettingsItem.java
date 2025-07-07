package ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * SettingsItem - A reusable component for individual settings rows
 * Provides consistent layout and styling for different types of settings
 */
public class SettingsItem extends JPanel {

    // Colors
    private final Color TITLE_COLOR = new Color(50, 50, 50);  // Darker title color
    private final Color DESCRIPTION_COLOR = new Color(100, 100, 100);  // Slightly darker description
    private final Color HOVER_COLOR = new Color(245, 247, 250);  // Subtle blue tint on hover
    private final Color PRIMARY_BUTTON_COLOR = new Color(56, 129, 246);  // Modern blue
    private final Color SECONDARY_BUTTON_COLOR = new Color(75, 85, 99);  // Modern gray
    private final Color DANGER_BUTTON_COLOR = new Color(220, 38, 38);    // Modern red

    // Components
    private JLabel iconLabel;
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JPanel controlPanel;

    // Properties
    private boolean hoverable = false;
    private Runnable clickAction;
    private ImageIcon icon;

    /**
     * Creates a settings item with title and description only
     * @param title The setting title
     * @param description The setting description
     */
    public SettingsItem(String title, String description) {
        this(title, description, null);
    }

    /**
     * Creates a settings item with title, description, and icon
     * @param title The setting title
     * @param description The setting description
     * @param iconPath The path to the icon
     */
    public SettingsItem(String title, String description, String iconPath) {
        if (iconPath != null) {
            try {
                ImageIcon originalIcon = new ImageIcon(getClass().getResource("/../../pictures/" + iconPath));
                Image scaledImage = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                this.icon = new ImageIcon(scaledImage);
            } catch (Exception e) {
                System.err.println("Failed to load icon: " + iconPath);
            }
        }
        initializeComponent(title, description);
    }

    /**
     * Initializes the component
     */
    private void initializeComponent(String title, String description) {
        setLayout(new BorderLayout(15, 0));
        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 15, 12, 15));  // Added left and right padding
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredHeight(description)));

        // Create icon and label panel
        JPanel leftPanel = new JPanel(new BorderLayout(10, 0));
        leftPanel.setOpaque(false);

        if (icon != null) {
            iconLabel = new JLabel(icon);
            iconLabel.setPreferredSize(new Dimension(24, 24));
            leftPanel.add(iconLabel, BorderLayout.WEST);
        }

        JPanel labelPanel = createLabelPanel(title, description);
        leftPanel.add(labelPanel, BorderLayout.CENTER);

        // Create control panel
        controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controlPanel.setOpaque(false);

        // Add to layout
        add(leftPanel, BorderLayout.WEST);
        add(controlPanel, BorderLayout.EAST);
    }

    /**
     * Creates the label panel with title and description
     */
    private JPanel createLabelPanel(String title, String description) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Title label
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(TITLE_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);

        // Description label (if provided)
        if (description != null && !description.trim().isEmpty()) {
            panel.add(Box.createVerticalStrut(3));

            descriptionLabel = new JLabel(description);
            descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descriptionLabel.setForeground(DESCRIPTION_COLOR);
            descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(descriptionLabel);
        }

        return panel;
    }

    /**
     * Calculates the preferred height based on content
     */
    private int getPreferredHeight(String description) {
        return description != null && !description.trim().isEmpty() ? 65 : 45;
    }

    /**
     * Adds a control component to the right side
     * @param control The control component (toggle, button, combo box, etc.)
     */
    public void addControl(Component control) {
        controlPanel.add(control);
    }

    /**
     * Sets the control component, replacing any existing controls
     * @param control The control component
     */
    public void setControl(Component control) {
        controlPanel.removeAll();
        controlPanel.add(control);
        revalidate();
        repaint();
    }

    /**
     * Updates the title text
     * @param title The new title
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * Updates the description text
     * @param description The new description
     */
    public void setDescription(String description) {
        if (description != null && !description.trim().isEmpty()) {
            if (descriptionLabel == null) {
                // Create description label if it doesn't exist
                descriptionLabel = new JLabel(description);
                descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                descriptionLabel.setForeground(DESCRIPTION_COLOR);
                descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel labelPanel = (JPanel) ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.WEST);
                labelPanel.add(Box.createVerticalStrut(3));
                labelPanel.add(descriptionLabel);
            } else {
                descriptionLabel.setText(description);
            }
        } else if (descriptionLabel != null) {
            // Remove description label if text is empty
            JPanel labelPanel = (JPanel) ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.WEST);
            labelPanel.remove(descriptionLabel);
            descriptionLabel = null;
        }

        // Update preferred size
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredHeight(description)));

        revalidate();
        repaint();
    }

    /**
     * Sets whether this item should have hover effects
     * @param hoverable true to enable hover effects
     */
    public void setHoverable(boolean hoverable) {
        this.hoverable = hoverable;

        if (hoverable && getMouseListeners().length == 0) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (hoverable) {
                        setBackground(HOVER_COLOR);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoverable) {
                        setBackground(Color.WHITE);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hoverable && clickAction != null) {
                        clickAction.run();
                    }
                }
            });
        }

        setCursor(hoverable ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Sets the click action for hoverable items
     * @param action The action to perform on click
     */
    public void setClickAction(Runnable action) {
        this.clickAction = action;
        setHoverable(true); // Auto-enable hover if click action is set
    }

    /**
     * Creates a settings item with a toggle switch
     * @param title The setting title
     * @param description The setting description
     * @param initialValue The initial toggle value
     * @param changeListener The change listener
     * @return The configured settings item
     */
    public static SettingsItem createToggleItem(String title, String description,
                                                boolean initialValue,
                                                ToggleChangeListener changeListener) {
        SettingsItem item = new SettingsItem(title, description);

        ToggleSwitch toggle = new ToggleSwitch(initialValue);
        toggle.addChangeListener(e -> {
            if (changeListener != null) {
                changeListener.onToggleChanged(toggle.isSelected());
            }
        });

        item.setControl(toggle);
        return item;
    }

    /**
     * Creates a settings item with a text field
     * @param title The setting title
     * @param description The setting description
     * @param initialValue The initial text value
     * @param changeListener The change listener
     * @return The configured settings item
     */
    public static SettingsItem createTextFieldItem(String title, String description,
                                                   String initialValue,
                                                   TextChangeListener changeListener) {
        SettingsItem item = new SettingsItem(title, description);

        JTextField textField = new JTextField(initialValue, 15);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        // Add document listener for real-time changes
        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (changeListener != null) changeListener.onTextChanged(textField.getText());
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (changeListener != null) changeListener.onTextChanged(textField.getText());
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (changeListener != null) changeListener.onTextChanged(textField.getText());
            }
        });

        item.setControl(textField);
        return item;
    }

    /**
     * Creates a settings item with a combo box
     * @param title The setting title
     * @param description The setting description
     * @param options The available options
     * @param initialValue The initial selected value
     * @param changeListener The change listener
     * @return The configured settings item
     */
    public static SettingsItem createComboBoxItem(String title, String description,
                                                  String[] options, String initialValue,
                                                  ComboBoxChangeListener changeListener) {
        SettingsItem item = new SettingsItem(title, description);

        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setSelectedItem(initialValue);

        comboBox.addActionListener(e -> {
            if (changeListener != null) {
                changeListener.onSelectionChanged((String) comboBox.getSelectedItem());
            }
        });

        item.setControl(comboBox);
        return item;
    }

    /**
     * Creates a settings item with a button
     * @param title The setting title
     * @param description The setting description
     * @param buttonText The button text
     * @param clickAction The click action
     * @return The configured settings item
     */
    public static SettingsItem createButtonItem(String title, String description,
                                                String buttonText, Runnable clickAction) {
        return createButtonItem(title, description, buttonText, clickAction, "primary");
    }

    public static SettingsItem createButtonItem(String title, String description,
                                                String buttonText, Runnable clickAction,
                                                String buttonStyle) {
        SettingsItem item = new SettingsItem(title, description, null);

        JButton button = new JButton(buttonText);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setForeground(Color.WHITE);
        
        Color buttonColor;
        switch (buttonStyle.toLowerCase()) {
            case "secondary":
                buttonColor = item.SECONDARY_BUTTON_COLOR;
                break;
            case "danger":
                buttonColor = item.DANGER_BUTTON_COLOR;
                break;
            default:
                buttonColor = item.PRIMARY_BUTTON_COLOR;
        }
        
        button.setBackground(buttonColor);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Make button round
        button.putClientProperty("JButton.buttonType", "roundRect");

        // Add hover and click effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(buttonColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(buttonColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(buttonColor.darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(buttonColor);
                if (clickAction != null) {
                    clickAction.run();
                }
            }
        });

        item.setControl(button);
        return item;
    }

    /**
     * Creates a settings item with an info label (read-only)
     * @param title The setting title
     * @param description The setting description
     * @param value The info value to display
     * @return The configured settings item
     */
    public static SettingsItem createInfoItem(String title, String description, String value) {
        SettingsItem item = new SettingsItem(title, description);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(new Color(90, 90, 90));

        item.setControl(valueLabel);
        return item;
    }

    /**
     * Creates a clickable settings item (like a menu item)
     * @param title The setting title
     * @param description The setting description
     * @param clickAction The click action
     * @return The configured settings item
     */
    public static SettingsItem createClickableItem(String title, String description,
                                                   Runnable clickAction, String iconPath) {
        SettingsItem item = new SettingsItem(title, description, iconPath);
        item.setClickAction(clickAction);

        // Add arrow indicator with animation
        JLabel arrowLabel = new JLabel("›");
        arrowLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        arrowLabel.setForeground(new Color(150, 150, 150));

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                arrowLabel.setForeground(new Color(100, 100, 100));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                arrowLabel.setForeground(new Color(150, 150, 150));
            }
        });

        item.setControl(arrowLabel);
        return item;
    }

    /**
     * Adds a separator line below this item
     */
    public void addSeparator() {
        setBorder(BorderFactory.createCompoundBorder(
                getBorder(),
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240))
        ));
    }

    /**
     * Sets custom colors for the item
     * @param titleColor The title text color
     * @param descriptionColor The description text color
     */
    public void setCustomColors(Color titleColor, Color descriptionColor) {
        if (titleLabel != null) {
            titleLabel.setForeground(titleColor);
        }
        if (descriptionLabel != null) {
            descriptionLabel.setForeground(descriptionColor);
        }
    }

    /**
     * Gets the control component
     * @return The control component
     */
    public Component getControl() {
        return controlPanel.getComponentCount() > 0 ? controlPanel.getComponent(0) : null;
    }

    /**
     * Interface for toggle change events
     */
    @FunctionalInterface
    public interface ToggleChangeListener {
        void onToggleChanged(boolean newValue);
    }

    /**
     * Interface for text change events
     */
    @FunctionalInterface
    public interface TextChangeListener {
        void onTextChanged(String newText);
    }

    /**
     * Interface for combo box change events
     */
    @FunctionalInterface
    public interface ComboBoxChangeListener {
        void onSelectionChanged(String newSelection);
    }
}