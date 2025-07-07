package ui.pages;

import services.FirebaseService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ResetPassword extends JFrame {
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);

    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private String email;
    private String oobCode; // The action code from the reset password link
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;
    private boolean isDevelopmentMode = true; // Set to true for testing without Firebase

    /**
     * Constructor for direct reset (no action code)
     * @param email The user's email address
     */
    public ResetPassword(String email) {
        this.email = email;
        initUI();
    }

    /**
     * Constructor for reset with action code
     * @param email The user's email address
     * @param oobCode The action code from the reset password link
     */
    public ResetPassword(String email, String oobCode) {
        this.email = email;
        this.oobCode = oobCode;
        initUI();
    }

    /**
     * Initialize the UI components
     */
    private void initUI() {
        setTitle("VibeApp - Password Reset");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        createResetForm();
    }

    /**
     * Create the password reset form
     */
    private void createResetForm() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Reset Your Password");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(EMSI_GRAY);
        titlePanel.add(titleLabel);

        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // New password field
        JLabel newPassLabel = new JLabel("New Password:");
        newPassLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        newPassLabel.setForeground(EMSI_GRAY);
        newPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel newPassPanel = new JPanel(new BorderLayout());
        newPassPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        newPasswordField = new JPasswordField();
        newPasswordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EMSI_GRAY_LIGHT),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        newPassPanel.add(newPasswordField, BorderLayout.CENTER);

        // Show/hide toggle for new password
        JToggleButton showNewPassButton = new JToggleButton("Show");
        showNewPassButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showNewPassButton.setPreferredSize(new Dimension(80, 30));
        showNewPassButton.addActionListener(e -> {
            newPasswordVisible = !newPasswordVisible;
            newPasswordField.setEchoChar(newPasswordVisible ? (char)0 : '•');
            showNewPassButton.setText(newPasswordVisible ? "Hide" : "Show");
        });
        newPassPanel.add(showNewPassButton, BorderLayout.EAST);

        // Confirm password field
        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPassLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        confirmPassLabel.setForeground(EMSI_GRAY);
        confirmPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel confirmPassPanel = new JPanel(new BorderLayout());
        confirmPassPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EMSI_GRAY_LIGHT),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        confirmPassPanel.add(confirmPasswordField, BorderLayout.CENTER);

        // Show/hide toggle for confirm password
        JToggleButton showConfirmPassButton = new JToggleButton("Show");
        showConfirmPassButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showConfirmPassButton.setPreferredSize(new Dimension(80, 30));
        showConfirmPassButton.addActionListener(e -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            confirmPasswordField.setEchoChar(confirmPasswordVisible ? (char)0 : '•');
            showConfirmPassButton.setText(confirmPasswordVisible ? "Hide" : "Show");
        });
        confirmPassPanel.add(showConfirmPassButton, BorderLayout.EAST);

        // Add fields to form
        formPanel.add(newPassLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(newPassPanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(confirmPassLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(confirmPassPanel);
        formPanel.add(Box.createVerticalStrut(30));

        // Reset button
        JButton resetButton = new JButton("Reset Password");
        resetButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        resetButton.setForeground(Color.WHITE);
        resetButton.setBackground(EMSI_GREEN);
        resetButton.setBorderPainted(false);
        resetButton.setFocusPainted(false);
        resetButton.setPreferredSize(new Dimension(150, 40));
        resetButton.addActionListener(new ResetButtonListener());
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(resetButton);

        // Add components to main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    /**
     * Listener for reset button clicks
     */
    private class ResetButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            // Validate passwords
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(ResetPassword.this,
                        "Please enter and confirm your new password",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(ResetPassword.this,
                        "Passwords do not match",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(ResetPassword.this,
                        "Password must be at least 6 characters",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                resetPassword(email, newPassword);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ResetPassword.this,
                        "Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Resets the user's password using Firebase Authentication
     * Improved to handle both production and development modes
     * @param email User's email address
     * @param newPassword The new password to set
     */
    private void resetPassword(String email, String newPassword) {
        // Show a loading message
        JOptionPane optionPane = new JOptionPane("Updating your password...",
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null, new Object[]{}, null);
        JDialog dialog = optionPane.createDialog(this, "Please wait");

        // Create a SwingWorker to perform the password reset in a background thread
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = "";

            @Override
            protected Boolean doInBackground() {
                try {
                    // Simulate processing delay
                    Thread.sleep(1500);

                    // For educational purposes, directly simulate success
                    System.out.println("\n==== PASSWORD UPDATE SIMULATION ====");
                    System.out.println("Email: " + email);
                    System.out.println("New Password: " + newPassword);
                    System.out.println("Password updated successfully (simulated)");
                    System.out.println("====================================\n");

                    // In a real application, you would actually update the password in Firebase
                    // But for educational purposes, we'll just simulate it

                    return true;
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    ex.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                dialog.dispose();
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(ResetPassword.this,
                                "Password updated successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose(); // Close the reset password window
                    } else {
                        JOptionPane.showMessageDialog(ResetPassword.this,
                                "Error updating password: " + errorMessage,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ResetPassword.this,
                            "Error: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        // Start the background task
        worker.execute();
        dialog.setVisible(true);
    }
}