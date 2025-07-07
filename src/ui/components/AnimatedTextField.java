package ui.components;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * AnimatedTextField - Custom animated text field component
 * Features floating labels, smooth focus animations, and modern styling
 */
public class AnimatedTextField extends JPanel {
    // Components
    private JTextField textField;
    private JLabel floatingLabel;
    private JLabel hintLabel;

    // Properties
    private String labelText;
    private String hintText;
    private boolean isRequired = false;
    private boolean isValid = true;
    private String errorMessage = "";

    // Colors
    private Color normalBorderColor = new Color(200, 200, 200);
    private Color focusedBorderColor = new Color(0, 150, 70);
    private Color errorBorderColor = new Color(220, 53, 69);
    private Color labelColor = new Color(108, 117, 125);
    private Color focusedLabelColor = new Color(0, 150, 70);
    private Color errorLabelColor = new Color(220, 53, 69);
    private Color hintColor = new Color(108, 117, 125);

    // Animation state
    private boolean isFocused = false;
    private boolean hasContent = false;
    private float labelAnimationProgress = 0f;
    private Timer animationTimer;

    // Appearance settings
    private int cornerRadius = 8;
    private boolean hasFloatingLabel = true;

    // Constructors
    public AnimatedTextField() {
        this("", "");
    }

    public AnimatedTextField(String labelText) {
        this(labelText, "");
    }

    public AnimatedTextField(String labelText, String hintText) {
        this.labelText = labelText;
        this.hintText = hintText;

        initializeComponents();
        setupLayout();
        setupAnimations();
        setupEventListeners();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, hasFloatingLabel ? 70 : 50));
        setOpaque(false);

        // Create text field
        textField = new JTextField();
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(new RoundedBorder());
        textField.setOpaque(false);

        // Create floating label
        if (hasFloatingLabel && !labelText.isEmpty()) {
            floatingLabel = new JLabel(labelText + (isRequired ? " *" : ""));
            floatingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            floatingLabel.setForeground(labelColor);
        }

        // Create hint label
        if (!hintText.isEmpty()) {
            hintLabel = new JLabel(hintText);
            hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            hintLabel.setForeground(hintColor);
        }
    }

    private void setupLayout() {
        // Main panel for text field and floating label
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new OverlayLayout(mainPanel));
        mainPanel.setOpaque(false);
        mainPanel.setPreferredSize(new Dimension(0, 40));

        // Text field panel with padding
        JPanel textFieldPanel = new JPanel(new BorderLayout());
        textFieldPanel.setOpaque(false);
        textFieldPanel.setBorder(BorderFactory.createEmptyBorder(
                hasFloatingLabel ? 12 : 8, 12, 8, 12));
        textFieldPanel.add(textField, BorderLayout.CENTER);

        mainPanel.add(textFieldPanel);

        // Add floating label if enabled
        if (floatingLabel != null) {
            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
            labelPanel.setOpaque(false);
            labelPanel.add(floatingLabel);
            mainPanel.add(labelPanel);
        }

        add(mainPanel, BorderLayout.CENTER);

        // Add hint label at bottom if present
        if (hintLabel != null) {
            JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            hintPanel.setOpaque(false);
            hintPanel.add(hintLabel);
            add(hintPanel, BorderLayout.SOUTH);
        }
    }

    private void setupAnimations() {
        animationTimer = new Timer(16, e -> {
            boolean shouldAnimate = (isFocused || hasContent) && labelAnimationProgress < 1f;
            boolean shouldDeAnimate = !(isFocused || hasContent) && labelAnimationProgress > 0f;

            if (shouldAnimate) {
                labelAnimationProgress += 0.15f;
                if (labelAnimationProgress > 1f) labelAnimationProgress = 1f;
                updateFloatingLabel();
            } else if (shouldDeAnimate) {
                labelAnimationProgress -= 0.15f;
                if (labelAnimationProgress < 0f) labelAnimationProgress = 0f;
                updateFloatingLabel();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
    }

    private void setupEventListeners() {
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                animationTimer.start();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                hasContent = !textField.getText().trim().isEmpty();
                animationTimer.start();
                repaint();
            }
        });

        // Listen for text changes
        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateContentState();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateContentState();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateContentState();
            }
        });
    }

    private void updateContentState() {
        boolean newHasContent = !textField.getText().trim().isEmpty();
        if (newHasContent != hasContent) {
            hasContent = newHasContent;
            if (!isFocused) {
                animationTimer.start();
            }
        }
    }

    private void updateFloatingLabel() {
        if (floatingLabel != null) {
            // Animate label position and size
            float scale = 1f - (labelAnimationProgress * 0.25f); // Scale down by 25%
            int yOffset = (int) (labelAnimationProgress * -8); // Move up by 8 pixels

            Font originalFont = new Font("Segoe UI", Font.PLAIN, 12);
            Font scaledFont = originalFont.deriveFont(originalFont.getSize() * scale);
            floatingLabel.setFont(scaledFont);

            // Update colors
            Color currentColor;
            if (!isValid) {
                currentColor = errorLabelColor;
            } else if (isFocused) {
                currentColor = focusedLabelColor;
            } else {
                currentColor = labelColor;
            }
            floatingLabel.setForeground(currentColor);

            repaint();
        }
    }

    // Custom border class
    private class RoundedBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Determine border color
            Color borderColor;
            if (!isValid) {
                borderColor = errorBorderColor;
            } else if (isFocused) {
                borderColor = focusedBorderColor;
            } else {
                borderColor = normalBorderColor;
            }

            // Draw border
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(isFocused ? 2f : 1f));
            g2d.drawRoundRect(x, y, width - 1, height - 1, cornerRadius, cornerRadius);

            // Draw background
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(x + 1, y + 1, width - 2, height - 2, cornerRadius, cornerRadius);

            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = 2;
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }

    // Public methods
    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
        updateContentState();
        if (hasFloatingLabel) {
            animationTimer.start();
        }
    }

    public JTextField getTextField() {
        return textField;
    }

    // Method to replace the internal text field (for password fields)
    private void replaceTextField(JTextField newTextField) {
        // Find the parent panel containing the text field
        Container parent = textField.getParent();
        if (parent != null) {
            // Remove old text field
            parent.remove(textField);

            // Add new text field
            parent.add(newTextField, BorderLayout.CENTER);

            // Update reference
            this.textField = newTextField;

            // Re-setup event listeners for the new field
            setupEventListenersForField(newTextField);

            // Refresh the component
            parent.revalidate();
            parent.repaint();
        }
    }

    private void setupEventListenersForField(JTextField field) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                animationTimer.start();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                hasContent = !field.getText().trim().isEmpty();
                animationTimer.start();
                repaint();
            }
        });

        // Listen for text changes
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateContentState();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateContentState();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateContentState();
            }
        });
    }

    public void setRequired(boolean required) {
        this.isRequired = required;
        if (floatingLabel != null) {
            floatingLabel.setText(labelText + (isRequired ? " *" : ""));
        }
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setValid(boolean valid) {
        setValid(valid, "");
    }

    public void setValid(boolean valid, String errorMessage) {
        this.isValid = valid;
        this.errorMessage = errorMessage;

        if (hintLabel != null) {
            if (!valid && !errorMessage.isEmpty()) {
                hintLabel.setText(errorMessage);
                hintLabel.setForeground(errorLabelColor);
            } else {
                hintLabel.setText(hintText);
                hintLabel.setForeground(hintColor);
            }
        }

        updateFloatingLabel();
        repaint();
    }

    public boolean isValid() {
        return isValid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
        if (floatingLabel != null) {
            floatingLabel.setText(labelText + (isRequired ? " *" : ""));
        }
    }

    public String getLabelText() {
        return labelText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
        if (hintLabel != null && isValid) {
            hintLabel.setText(hintText);
        }
    }

    public String getHintText() {
        return hintText;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    // Color setters
    public void setNormalBorderColor(Color color) {
        this.normalBorderColor = color;
        repaint();
    }

    public void setFocusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        repaint();
    }

    public void setErrorBorderColor(Color color) {
        this.errorBorderColor = color;
        repaint();
    }

    // Factory methods for common field types
    public static AnimatedTextField createEmailField() {
        AnimatedTextField field = new AnimatedTextField("Email Address", "Enter your email");
        field.setRequired(true);
        return field;
    }

    public static AnimatedTextField createPasswordField() {
        AnimatedTextField field = new AnimatedTextField("Password", "Enter your password");
        // Replace the JTextField with JPasswordField
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(field.textField.getFont());
        passwordField.setBorder(field.textField.getBorder());
        passwordField.setOpaque(field.textField.isOpaque());
        passwordField.setEchoChar('•');

        // Replace the text field in the component
        field.replaceTextField(passwordField);
        field.setRequired(true);
        return field;
    }

    public static AnimatedTextField createNameField(String labelText) {
        AnimatedTextField field = new AnimatedTextField(labelText, "Enter your " + labelText.toLowerCase());
        field.setRequired(true);
        return field;
    }

    public static AnimatedTextField createSearchField() {
        AnimatedTextField field = new AnimatedTextField("Search", "Type to search...");
        return field;
    }
}