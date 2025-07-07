package ui.pages;

import com.vibeapp.VibeApp;
import services.FirebaseService;
import ui.components.ModernButton;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import com.google.firebase.database.*;

public class LoginPage extends JPanel {
    // EMSI Colors
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private final Color EMSI_GRAY_LIGHTER = new Color(240, 240, 240);
    private final Color EMSI_RED = new Color(217, 83, 30);
    private final Color EMSI_RED_LIGHT = new Color(230, 110, 50);

    // UI Components
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPanel mainPanel;
    private boolean passwordVisible = false;

    // Animation
    private java.util.List<Bubble> bubbles = new java.util.ArrayList<>();
    private Timer bubbleTimer;

    public LoginPage() {
        setLayout(new BorderLayout());

        // Initialize bubbles
        initBubbles();

        // Create main panel
        createMainPanel();

        // Add footer
        addFooter();

        setSize(950, 650);

        // Initialize bubbles after panel has size
        SwingUtilities.invokeLater(() -> {
            initBubbles();
        });

        // Add responsiveness on resize
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (mainPanel != null) {
                    // Reinitialize bubbles with new dimensions
                    initBubbles();
                    // Trigger responsive layout updates
                    handleResponsiveLayout();
                    mainPanel.revalidate();
                    mainPanel.repaint();
                }
            }
        });
    }

    private void handleResponsiveLayout() {
        SwingUtilities.invokeLater(() -> {
            int width = getWidth();
            int height = getHeight();
            
            // Small screen: Stack vertically
            if (width < 900 || height < 600) {
                if (mainPanel != null) {
                    mainPanel.removeAll();
                    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
                    
                    // Create compact panels for small screens
                    JPanel compactLeftPanel = createCompactLeftPanel();
                    JPanel compactRightPanel = createCompactRightPanel();
                    
                    mainPanel.add(compactLeftPanel);
                    mainPanel.add(compactRightPanel);
                }
            } 
            // Large screen: Side by side
            else {
                if (mainPanel != null) {
                    mainPanel.removeAll();
                    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
                    
                    // Create regular panels for large screens
                    JPanel leftPanel = createLeftPanel();
                    JPanel rightPanel = createRightPanel();
                    
                    mainPanel.add(leftPanel);
                    mainPanel.add(rightPanel);
                }
            }
            
            if (mainPanel != null) {
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width > 0 && height > 0) {
            SwingUtilities.invokeLater(() -> {
                initBubbles();
            });
        }
    }

    private void initBubbles() {
        bubbles.clear();

        int width = Math.max(getWidth(), 950);
        int height = Math.max(getHeight(), 650);

        for (int i = 0; i < 15; i++) {
            bubbles.add(new Bubble(
                    (int) (Math.random() * width),
                    (int) (Math.random() * height),
                    (int) (Math.random() * 30) + 10,
                    new Color(0, 150, 70, (int) (Math.random() * 15) + 5)
            ));
        }

        if (bubbleTimer != null) {
            bubbleTimer.stop();
        }

        bubbleTimer = new Timer(50, e -> {
            for (Bubble bubble : bubbles) {
                bubble.move();
            }
            repaint();
        });

        bubbleTimer.start();
    }

    private void createMainPanel() {
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(250, 250, 250),
                        getWidth(), getHeight(), new Color(230, 240, 235)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw bubbles
                for (Bubble bubble : bubbles) {
                    bubble.draw(g2d);
                }

                // Decorative elements
                g2d.setColor(new Color(0, 150, 70, 10));
                g2d.fillOval(-100, -100, 400, 400);
                g2d.fillOval(getWidth() - 200, getHeight() - 200, 400, 400);

                g2d.dispose();
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        // Create left and right panels
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        
        // Responsive padding based on window size
        int width = getWidth() > 0 ? getWidth() : 950;
        int leftPadding = Math.max(20, width / 25);
        int rightPadding = Math.max(10, width / 50);
        int topBottomPadding = Math.max(40, getHeight() / 15);
        
        leftPanel.setBorder(new EmptyBorder(topBottomPadding, leftPadding, topBottomPadding, rightPadding));
        
        // Responsive preferred size
        int preferredWidth = Math.max(350, width / 2 - 50);
        leftPanel.setPreferredSize(new Dimension(preferredWidth, 0));

        // Logo section
        JPanel logoPanel = createLogoPanel();

        // Welcome section
        JLabel welcomeTitle = new JLabel("Bienvenue sur VibeApp");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcomeTitle.setForeground(EMSI_GRAY);
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea welcomeText = new JTextArea(
                "VibeApp vous ouvre les portes d'une communication sans frontières.\n " +
                        "Sécurité, partage, spontanéité - Tout commence ici."
        );
        welcomeText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeText.setLineWrap(true);
        welcomeText.setWrapStyleWord(true);
        welcomeText.setEditable(false);
        welcomeText.setOpaque(false);
        welcomeText.setForeground(EMSI_GRAY);
        welcomeText.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeText.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Features section
        JPanel featuresPanel = createFeaturesPanel();

        leftPanel.add(logoPanel);
        leftPanel.add(Box.createVerticalStrut(30));
        leftPanel.add(welcomeTitle);
        leftPanel.add(welcomeText);
        leftPanel.add(featuresPanel);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        
        // Responsive padding
        int width = getWidth() > 0 ? getWidth() : 950;
        int height = getHeight() > 0 ? getHeight() : 650;
        int sidePadding = Math.max(20, width / 30);
        int topBottomPadding = Math.max(40, height / 15);
        
        rightPanel.setBorder(new EmptyBorder(topBottomPadding, sidePadding, topBottomPadding, sidePadding));
        rightPanel.setOpaque(false);

        // Form panel
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(35, 40, 40, 40));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Responsive form panel size
        int formWidth = Math.min(400, Math.max(300, width - 100));
        int formHeight = Math.min(500, Math.max(400, height - 150));
        formPanel.setMaximumSize(new Dimension(formWidth, formHeight));
        formPanel.setPreferredSize(new Dimension(formWidth, formHeight));

        // Form title
        JLabel formTitle = new JLabel("Connexion");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        formTitle.setForeground(EMSI_GRAY);
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel formSubtitle = new JLabel("Accédez à votre espace de discussion");
        formSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formSubtitle.setForeground(EMSI_GRAY_LIGHT);
        formSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Email field
        JLabel emailLabel = new JLabel("Adresse email");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        emailLabel.setForeground(EMSI_GRAY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField emailContainer = new AnimatedTextField();
        emailField = emailContainer.getTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Password field
        JLabel passwordLabel = new JLabel("Mot de passe");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordLabel.setForeground(EMSI_GRAY);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField passwordContainer = new AnimatedTextField();
        passwordField = new JPasswordField();
        passwordContainer.setTextField(passwordField);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setEchoChar('\u2022');

        // Login button
        ModernButton loginButton = ModernButton.createPrimary("Se connecter");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        loginButton.addActionListener(e -> handleLogin());

        // Forgot password button
        ModernButton forgotPasswordButton = ModernButton.createGhost("Mot de passe oublié ?");
        forgotPasswordButton.setButtonSize(ModernButton.ButtonSize.SMALL);
        forgotPasswordButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgotPasswordButton.setShadowEnabled(false);

        forgotPasswordButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("forgotPassword");
        });

        // Separator
        JPanel separatorPanel = createSeparatorPanel();

        // Register button
        ModernButton registerButton = ModernButton.createOutline("S'inscrire");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        registerButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("register");
        });

        // Assemble form
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(formSubtitle);
        formPanel.add(Box.createVerticalStrut(25));
        formPanel.add(emailLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(emailContainer);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(passwordContainer);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(loginButton);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(forgotPasswordButton);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(separatorPanel);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(registerButton);

        rightPanel.add(formPanel);
        return rightPanel;
    }

    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(0, 200));
        logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        try {
            File logoFile = new File("pictures/logoVibeApp.png");
            if (logoFile.exists()) {
                BufferedImage originalImage = ImageIO.read(logoFile);
                int targetWidth = 200;
                double aspectRatio = (double) originalImage.getHeight() / originalImage.getWidth();
                int targetHeight = (int) (targetWidth * aspectRatio);

                BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dResized = resizedImage.createGraphics();
                g2dResized.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2dResized.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2dResized.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2dResized.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                g2dResized.dispose();

                JPanel logoContentPanel = new AnimatedLogoPanel(resizedImage, targetWidth, targetHeight);
                logoPanel.add(logoContentPanel);
            } else {
                JLabel fallbackLabel = new JLabel("VibeApp");
                fallbackLabel.setFont(new Font("Arial", Font.BOLD, 28));
                fallbackLabel.setForeground(EMSI_GREEN);
                logoPanel.add(fallbackLabel);
            }
        } catch (Exception e) {
            JLabel fallbackLabel = new JLabel("VibeApp");
            fallbackLabel.setFont(new Font("Arial", Font.BOLD, 28));
            fallbackLabel.setForeground(EMSI_GREEN);
            logoPanel.add(fallbackLabel);
        }

        return logoPanel;
    }

    private JPanel createFeaturesPanel() {
        JPanel featuresPanel = new JPanel();
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setOpaque(false);
        featuresPanel.setBorder(new EmptyBorder(30, 10, 0, 0));

        String[] features = {
                "Discussions instantanées sécurisées",
                "Partage de fichiers et de ressources"
        };

        for (String feature : features) {
            JPanel featureRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            featureRow.setOpaque(false);
            featureRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JLabel checkIcon = new AnimatedCheckIcon(16, 16, EMSI_GREEN);
            JLabel featureLabel = new JLabel(feature);
            featureLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            featureLabel.setForeground(EMSI_GRAY);

            featureRow.add(checkIcon);
            featureRow.add(featureLabel);
            featuresPanel.add(featureRow);
        }

        return featuresPanel;
    }

    private JPanel createSeparatorPanel() {
        JPanel separatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        separatorPanel.setOpaque(false);
        separatorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel lineLeft = new JPanel();
        lineLeft.setBackground(new Color(220, 220, 220));
        lineLeft.setPreferredSize(new Dimension(70, 1));

        JLabel orLabel = new JLabel("OU");
        orLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        orLabel.setForeground(EMSI_GRAY_LIGHT);

        JPanel lineRight = new JPanel();
        lineRight.setBackground(new Color(220, 220, 220));
        lineRight.setPreferredSize(new Dimension(70, 1));

        separatorPanel.add(lineLeft);
        separatorPanel.add(orLabel);
        separatorPanel.add(lineRight);

        return separatorPanel;
    }

    private void addFooter() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(EMSI_GRAY);
        footerPanel.setPreferredSize(new Dimension(0, 30));

        JLabel footerText = new JLabel("© 2025 VibeApp - Tous droits réservés | Développé par les étudiants de l'EMSI");
        footerText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerText.setForeground(Color.WHITE);

        footerPanel.add(footerText);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Veuillez remplir tous les champs",
                    "Erreur de connexion",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Authenticate user using Firebase
        authenticateUser(email, password);
    }

    private void authenticateUser(String email, String password) {
        try {
            FirebaseService firebaseService = FirebaseService.getInstance();
            String hashedPassword = hashPassword(password);

            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            Query query = usersRef.orderByChild("email").equalTo(email);

            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] authSuccess = {false};
            final String[] userId = {null};

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String storedPasswordHash = userSnapshot.child("password").getValue(String.class);
                            if (storedPasswordHash != null && storedPasswordHash.equals(hashedPassword)) {
                                authSuccess[0] = true;
                                userId[0] = userSnapshot.getKey();
                                break;
                            }
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error authenticating user: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (authSuccess[0]) {
                VibeApp.getInstance().showMainChat(userId[0]);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Identifiants incorrects. Veuillez vérifier votre email et mot de passe.",
                        "Erreur de connexion",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erreur de connexion à Firebase: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Integer.toString(password.hashCode());
        }
    }

    // Inner classes - same as your existing code
    class AnimatedTextField extends JPanel {
        private JTextField textField;
        private Color borderColor = new Color(220, 220, 220);
        private Color focusBorderColor = EMSI_GREEN;
        private boolean isFocused = false;

        public AnimatedTextField() {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(borderColor, 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));

            textField = new JTextField();
            textField.setBorder(null);
            textField.setOpaque(false);

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    animateBorder();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    animateBorder();
                }
            });

            add(textField, BorderLayout.CENTER);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        }

        public JTextField getTextField() {
            return textField;
        }

        public void setTextField(JTextField field) {
            remove(textField);
            textField = field;
            textField.setBorder(null);
            textField.setOpaque(false);

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    animateBorder();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    animateBorder();
                }
            });

            add(textField, BorderLayout.CENTER);
        }

        private void animateBorder() {
            Color targetColor = isFocused ? focusBorderColor : borderColor;
            Timer timer = new Timer(20, new ActionListener() {
                private float progress = 0f;

                @Override
                public void actionPerformed(ActionEvent e) {
                    progress += 0.1f;
                    if (progress > 1.0f) {
                        progress = 1.0f;
                        ((Timer) e.getSource()).stop();
                    }

                    Color currentColor = interpolateColor(
                            isFocused ? borderColor : focusBorderColor,
                            targetColor,
                            progress
                    );

                    setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(currentColor, 1, true),
                            BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));
                    repaint();
                }
            });
            timer.start();
        }

        private Color interpolateColor(Color c1, Color c2, float fraction) {
            int red = (int) (c1.getRed() + fraction * (c2.getRed() - c1.getRed()));
            int green = (int) (c1.getGreen() + fraction * (c2.getGreen() - c1.getGreen()));
            int blue = (int) (c1.getBlue() + fraction * (c2.getBlue() - c1.getBlue()));
            return new Color(red, green, blue);
        }
    }

    class AnimatedButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private Color pressedColor;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private float animationProgress = 0f;
        private Timer animationTimer;

        public AnimatedButton(String text, Color baseColor, Color hoverColor, Color pressedColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            this.pressedColor = pressedColor;

            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

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

            Color currentColor;
            if (isPressed) {
                currentColor = pressedColor;
            } else if (isHovered) {
                currentColor = interpolateColor(baseColor, hoverColor, animationProgress);
            } else {
                currentColor = interpolateColor(hoverColor, baseColor, animationProgress);
            }

            g2d.setColor(currentColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            FontMetrics fm = g2d.getFontMetrics(getFont());
            Rectangle stringBounds = fm.getStringBounds(getText(), g2d).getBounds();

            int textX = (getWidth() - stringBounds.width) / 2;
            int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();

            g2d.setColor(getForeground());
            g2d.setFont(getFont());
            g2d.drawString(getText(), textX, textY);

            g2d.dispose();
        }

        private Color interpolateColor(Color c1, Color c2, float fraction) {
            int red = (int) (c1.getRed() + fraction * (c2.getRed() - c1.getRed()));
            int green = (int) (c1.getGreen() + fraction * (c2.getGreen() - c1.getGreen()));
            int blue = (int) (c1.getBlue() + fraction * (c2.getBlue() - c1.getBlue()));
            return new Color(red, green, blue);
        }
    }

    class AnimatedCheckIcon extends JLabel {
        private final Color iconColor;
        private float animationProgress = 0f;
        private Timer animationTimer;

        public AnimatedCheckIcon(int width, int height, Color color) {
            this.iconColor = color;
            setPreferredSize(new Dimension(width, height));

            animationTimer = new Timer(16, e -> {
                animationProgress += 0.05f;
                if (animationProgress > 1f) {
                    animationProgress = 0f;
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    animationTimer.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    animationTimer.stop();
                    animationProgress = 0f;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float alpha = 0.6f + 0.4f * (float) Math.sin(animationProgress * 2 * Math.PI);
            g2d.setColor(new Color(
                    iconColor.getRed() / 255f,
                    iconColor.getGreen() / 255f,
                    iconColor.getBlue() / 255f,
                    alpha
            ));

            g2d.fillOval(0, 0, 16, 16);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            int offset = (int) (animationProgress * 2) % 2;
            g2d.drawLine(4, 8 + offset, 7, 11 + offset);
            g2d.drawLine(7, 11 + offset, 12, 5 + offset);

            g2d.dispose();
        }
    }

    class Bubble {
        private int x, y;
        private int size;
        private Color color;
        private float speed;
        private float angle;

        public Bubble(int x, int y, int size, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.speed = 0.2f + (float) Math.random() * 0.5f;
            this.angle = (float) (Math.random() * 2 * Math.PI);
        }

        public void move() {
            angle += (Math.random() - 0.5) * 0.1;
            x += Math.cos(angle) * speed;
            y += Math.sin(angle) * speed;

            int panelWidth = LoginPage.this.getWidth();
            int panelHeight = LoginPage.this.getHeight();

            if (panelWidth <= 0) panelWidth = 950;
            if (panelHeight <= 0) panelHeight = 650;

            if (x < 0) {
                x = 0;
                angle = (float) Math.PI - angle;
            }
            if (x > panelWidth - size) {
                x = panelWidth - size;
                angle = (float) Math.PI - angle;
            }
            if (y < 0) {
                y = 0;
                angle = -angle;
            }
            if (y > panelHeight - size) {
                y = panelHeight - size;
                angle = -angle;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillOval(x, y, size, size);
        }
    }

    class RoundedPanel extends JPanel {
        private int cornerRadius;

        public RoundedPanel(int radius) {
            super();
            this.cornerRadius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Add shadow
            for (int i = 0; i < 5; i++) {
                float alpha = 0.03f * (5 - i);
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(i + 2, i + 2, getWidth() - (i * 2 + 4), getHeight() - (i * 2 + 4), cornerRadius, cornerRadius);
            }

            // Background gradient
            GradientPaint paint = new GradientPaint(
                    0, 0, Color.WHITE,
                    0, getHeight(), new Color(250, 250, 250)
            );
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, cornerRadius, cornerRadius);

            // Highlight
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillRoundRect(0, 0, getWidth() - 3, 20, cornerRadius, cornerRadius);

            g2.dispose();
        }
    }

    class AnimatedLogoPanel extends JPanel {
        private BufferedImage logoImage;
        private int targetWidth, targetHeight;
        private float animationProgress = 0f;
        private Timer animationTimer;

        public AnimatedLogoPanel(BufferedImage image, int width, int height) {
            this.logoImage = image;
            this.targetWidth = width;
            this.targetHeight = height;

            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(250, 250));

            JLabel appNameLabel = new JLabel("VibeApp");
            appNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            appNameLabel.setForeground(EMSI_GREEN);
            appNameLabel.setBounds(0, 200, 250, 30);
            appNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel subtitleLabel = new JLabel("Communication Platform");
            subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            subtitleLabel.setForeground(EMSI_GRAY_LIGHT);
            subtitleLabel.setBounds(0, 230, 250, 20);
            subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

            add(appNameLabel);
            add(subtitleLabel);

            animationTimer = new Timer(20, e -> {
                animationProgress += 0.05f;
                if (animationProgress > 1f) {
                    animationProgress = 1f;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            float scale = 0.8f + (0.2f * animationProgress);
            float opacity = animationProgress;

            AffineTransform originalTransform = g2d.getTransform();

            g2d.translate(getWidth() / 2, getHeight() / 2);
            g2d.scale(scale, scale);
            g2d.translate(-getWidth() / 2, -getHeight() / 2);

            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            g2d.drawImage(logoImage,
                    (getWidth() - targetWidth) / 2,
                    -20,
                    targetWidth,
                    targetHeight,
                    null);

            g2d.setTransform(originalTransform);
            g2d.setComposite(originalComposite);
            g2d.dispose();
        }
    }

    private JPanel createCompactLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        leftPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Compact logo section
        JLabel titleLabel = new JLabel("VibeApp", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(EMSI_GREEN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Bienvenue sur la plateforme", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(EMSI_GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(subtitleLabel);
        
        return leftPanel;
    }

    private JPanel createCompactRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        rightPanel.setOpaque(false);
        
        // Create a more compact form panel
        RoundedPanel formPanel = new RoundedPanel(15);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 25, 25, 25));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        int width = getWidth() > 0 ? getWidth() : 800;
        int formWidth = Math.min(380, width - 60);
        formPanel.setMaximumSize(new Dimension(formWidth, Integer.MAX_VALUE));
        
        // Compact form fields
        createCompactFormFields(formPanel);
        
        rightPanel.add(formPanel);
        return rightPanel;
    }

    private void createCompactFormFields(JPanel formPanel) {
        // Form title
        JLabel formTitle = new JLabel("Connexion");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        formTitle.setForeground(EMSI_GRAY);
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Email field
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailLabel.setForeground(EMSI_GRAY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField emailContainer = new AnimatedTextField();
        emailField = emailContainer.getTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Password field
        JLabel passwordLabel = new JLabel("Mot de passe");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordLabel.setForeground(EMSI_GRAY);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField passwordContainer = new AnimatedTextField();
        passwordField = new JPasswordField();
        passwordContainer.setTextField(passwordField);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setEchoChar('\u2022');

        // Login button
        ModernButton loginButton = ModernButton.createPrimary("Se connecter");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.addActionListener(e -> handleLogin());

        // Forgot password button
        ModernButton forgotPasswordButton = ModernButton.createGhost("Mot de passe oublié ?");
        forgotPasswordButton.setButtonSize(ModernButton.ButtonSize.SMALL);
        forgotPasswordButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgotPasswordButton.setShadowEnabled(false);
        forgotPasswordButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("forgotPassword");
        });

        // Separator
        JPanel separatorPanel = createSeparatorPanel();

        // Register button
        ModernButton registerButton = ModernButton.createOutline("S'inscrire");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("register");
        });

        // Assemble compact form
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(emailLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(emailContainer);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(passwordContainer);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(loginButton);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(forgotPasswordButton);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(separatorPanel);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(registerButton);
    }
}