package ui.pages;

import ui.components.ModernButton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Page for entering email verification code before password reset
 */
public class VerificationCodePage extends JFrame {
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);

    private final JTextField[] codeFields = new JTextField[6];
    private final String email;
    private final String verificationCode;

    /**
     * Constructor
     * @param email User's email address
     * @param verificationCode The verification code sent to email
     */
    public VerificationCodePage(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;

        initUI();
    }

    private void initUI() {
        setTitle("VibeApp - Email Verification");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Title
        JLabel titleLabel = new JLabel("Verify Your Email");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(EMSI_GRAY);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Subtitle with email
        JLabel subtitleLabel = new JLabel("<html>We sent a verification code to<br><b>" + email + "</b></html>");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(EMSI_GRAY);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(subtitleLabel);

        // Code fields panel
        JPanel codePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        codePanel.setBackground(Color.WHITE);

        // Create 6 code input fields
        for (int i = 0; i < 6; i++) {
            final int currentIndex = i;
            codeFields[i] = new JTextField(1);
            codeFields[i].setFont(new Font("Segoe UI", Font.BOLD, 20));
            codeFields[i].setHorizontalAlignment(SwingConstants.CENTER);
            codeFields[i].setPreferredSize(new Dimension(45, 45));
            codeFields[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(EMSI_GRAY_LIGHT, 1, true),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            // Add focus listener
            codeFields[i].addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    codeFields[currentIndex].setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(EMSI_GREEN, 2, true),
                            BorderFactory.createEmptyBorder(4, 4, 4, 4)
                    ));
                    codeFields[currentIndex].selectAll();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    codeFields[currentIndex].setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(EMSI_GRAY_LIGHT, 1, true),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
                }
            });

            // Add key listener to auto-advance to next field
            codeFields[i].addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyTyped(java.awt.event.KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!Character.isDigit(c)) {
                        e.consume(); // Only allow digits
                        return;
                    }

                    // If there's already a character, replace it
                    if (codeFields[currentIndex].getText().length() > 0) {
                        codeFields[currentIndex].setText("" + c);
                        e.consume();
                    }

                    // Auto-advance to next field
                    SwingUtilities.invokeLater(() -> {
                        if (currentIndex < 5 && codeFields[currentIndex].getText().length() == 1) {
                            codeFields[currentIndex + 1].requestFocus();
                        }
                    });
                }

                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE) {
                        // If field is empty and user presses backspace, go back to previous field
                        if (currentIndex > 0 && codeFields[currentIndex].getText().isEmpty()) {
                            codeFields[currentIndex - 1].requestFocus();
                            codeFields[currentIndex - 1].setText("");
                            e.consume();
                        }
                    }
                }
            });

            codePanel.add(codeFields[i]);
        }

        // Resend code link
        JPanel resendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        resendPanel.setBackground(Color.WHITE);

        JLabel resendLabel = new JLabel("Didn't receive the code?");
        resendLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resendLabel.setForeground(EMSI_GRAY);

        ModernButton resendButton = ModernButton.createGhost("Resend Code");
        resendButton.setButtonSize(ModernButton.ButtonSize.SMALL);
        resendButton.setShadowEnabled(false);
        resendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Generate a new verification code and send it
                    String newCode = generateVerificationCode();

                    // Send the code via email (in a real app)
                    // For now just show a message
                    JOptionPane.showMessageDialog(VerificationCodePage.this,
                            "A new verification code has been sent to your email.",
                            "Code Sent",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Print the new code to console for testing
                    System.out.println("New verification code: " + newCode);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VerificationCodePage.this,
                            "Error sending new code: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        resendPanel.add(resendLabel);
        resendPanel.add(resendButton);

        // Verify button
        ModernButton verifyButton = ModernButton.createPrimary("Verify Code");
        verifyButton.setPreferredSize(new Dimension(150, 40));
        // Verify button action listener
        verifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder enteredCode = new StringBuilder();
                for (JTextField field : codeFields) {
                    enteredCode.append(field.getText());
                }

                if (enteredCode.length() < 6) {
                    JOptionPane.showMessageDialog(VerificationCodePage.this,
                            "Please enter the complete 6-digit code",
                            "Incomplete Code",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // For educational purposes - accept either the actual code or "123456"
                if (enteredCode.toString().equals(verificationCode) || enteredCode.toString().equals("123456")) {
                    JOptionPane.showMessageDialog(VerificationCodePage.this,
                            "Code verified successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Make sure to use the right constructor
                    ResetPassword resetPasswordPage = new ResetPassword(email);
                    resetPasswordPage.setVisible(true);

                    // Close this window
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(VerificationCodePage.this,
                            "Invalid verification code. Please try again.\n" +
                                    "Hint: For testing, you can always use 123456",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(verifyButton);

        // Main content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(codePanel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(resendPanel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(buttonPanel);

        // Add everything to the main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    /**
     * Generate a random 6-digit verification code
     */
    private String generateVerificationCode() {
        java.util.Random random = new java.util.Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // For testing, generate a random code
            String testCode = String.valueOf(100000 + new java.util.Random().nextInt(900000));
            System.out.println("Test verification code: " + testCode);

            VerificationCodePage codePage = new VerificationCodePage("test@example.com", testCode);
            codePage.setVisible(true);
        });
    }
}