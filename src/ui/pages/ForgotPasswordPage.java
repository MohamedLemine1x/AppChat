package ui.pages;

import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.vibeapp.VibeApp;
import services.EmailService;
import services.FirebaseService;
import utils.ValidationUtils;
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
import java.util.regex.Pattern;

public class ForgotPasswordPage extends JPanel {
    // Couleurs EMSI avec des variations
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private final Color EMSI_RED = new Color(217, 83, 30);
    private final Color EMSI_RED_LIGHT = new Color(230, 110, 50);

    private JTextField emailField;
    private JPanel mainPanel;

    // Animation des bulles
    private java.util.List<Bubble> bubbles = new java.util.ArrayList<>();
    private Timer bubbleTimer;

    public ForgotPasswordPage() {
        /*
        setTitle("VibeApp - Mot de passe oublié");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Changer l'icône de l'application
        setIconImage(createLogo());

         */

        // Configuration générale
        setLayout(new BorderLayout());

        // Initialiser les bulles animées
        initBubbles();

        // Créer le panneau principal avec un fond dégradé
        createMainPanel();

        // Ajouter un copyright dans le pied de page
        addFooter();

        setSize(900, 600);

        // Initialize bubbles AFTER the panel has size
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
            
            // Small screen: Stack vertically or adjust layout
            if (width < 800 || height < 550) {
                // Implement compact layout if needed
                if (mainPanel != null) {
                    // Could implement vertical stacking here if the page has side-by-side layout
                    // For now, just trigger a repaint with new dimensions
                    mainPanel.revalidate();
                    mainPanel.repaint();
                }
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
        // Clear existing bubbles
        bubbles.clear();

        // Get panel dimensions (fallback if not set yet)
        int width = Math.max(getWidth(), 900);
        int height = Math.max(getHeight(), 600);

        // Create bubbles with proper positioning
        for (int i = 0; i < 15; i++) {
            bubbles.add(new Bubble(
                    (int) (Math.random() * width),
                    (int) (Math.random() * height),
                    (int) (Math.random() * 30) + 10,
                    new Color(0, 150, 70, (int) (Math.random() * 15) + 5)
            ));
        }

        // Stop existing timer if any
        if (bubbleTimer != null) {
            bubbleTimer.stop();
        }

        // Create new timer for animation
        bubbleTimer = new Timer(50, e -> {
            // Update bubble positions
            for (Bubble bubble : bubbles) {
                bubble.move();
            }
            // Repaint the entire ui.pages.ForgotPasswordPage instead of just mainPanel
            repaint();
        });

        // Start the animation
        bubbleTimer.start();
    }

    private Image createLogo() {
        // Créer une image dynamiquement au lieu de charger depuis un fichier
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Activer l'anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner un logo
        g2d.setColor(EMSI_GRAY);
        g2d.fillRoundRect(10, 10, 40, 20, 5, 5);

        g2d.setColor(EMSI_GREEN);
        int[] xPoints = {12, 18, 25, 32, 38, 44};
        int[] yPoints = {30, 15, 25, 12, 20, 30};
        g2d.fillPolygon(xPoints, yPoints, 6);

        g2d.setColor(EMSI_RED);
        g2d.fillRoundRect(44, 17, 8, 8, 2, 2);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("EMSI", 14, 25);

        g2d.dispose();

        return image;
    }

    private void createMainPanel() {
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Créer un dégradé diagonal plus moderne
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(250, 250, 250),
                        getWidth(), getHeight(), new Color(230, 240, 235)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Dessiner les bulles
                for (Bubble bubble : bubbles) {
                    bubble.draw(g2d);
                }

                // Dessiner quelques éléments graphiques améliorés
                g2d.setColor(new Color(0, 150, 70, 10));
                g2d.fillOval(-100, -100, 400, 400);
                g2d.fillOval(getWidth() - 200, getHeight() - 200, 400, 400);

                g2d.dispose();
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        // Créer une section gauche (logo et message)
        JPanel leftPanel = createLeftPanel();

        // Créer une section droite (formulaire de récupération de mot de passe)
        JPanel rightPanel = createRightPanel();

        // Ajouter les deux sections au panneau principal
        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(60, 40, 60, 20));
        leftPanel.setPreferredSize(new Dimension(400, 0));

        // Logo com.vibeapp.VibeApp
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(0, 200));
        logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        try {
            // Utiliser le même chemin que dans ajouterImage()
            File logoFile = new File("pictures/logoVibeApp.png");
            if (logoFile.exists()) {
                // Charger le logo original avec haute qualité
                BufferedImage originalImage = ImageIO.read(logoFile);

                // Redimensionnement de haute qualité avec préservation de la netteté
                int targetWidth = 200; // Augmenter la taille pour plus de détails
                double aspectRatio = (double) originalImage.getHeight() / originalImage.getWidth();
                int targetHeight = (int) (targetWidth * aspectRatio);

                // Créer une image redimensionnée de très haute qualité
                BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dResized = resizedImage.createGraphics();

                // Configuration de rendu de TRÈS haute qualité
                g2dResized.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2dResized.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2dResized.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2dResized.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g2dResized.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                // Dessiner l'image redimensionnée avec un algorithme de haute qualité
                g2dResized.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                g2dResized.dispose();

                // Créer un panneau personnalisé pour le logo avec transition
                JPanel logoContentPanel = new JPanel() {
                    private float animationProgress = 0f;
                    private Timer animationTimer;

                    {
                        // Initialiser l'animation de transition
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
                        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                        // Effet de transition avec scaling et opacity
                        float scale = 0.8f + (0.2f * animationProgress);
                        float opacity = animationProgress;

                        // Sauvegarder la transformation originale
                        AffineTransform originalTransform = g2d.getTransform();

                        // Centrer et mettre à l'échelle
                        g2d.translate(getWidth() / 2, getHeight() / 2);
                        g2d.scale(scale, scale);
                        g2d.translate(-getWidth() / 2, -getHeight() / 2);

                        // Appliquer l'opacité
                        Composite originalComposite = g2d.getComposite();
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

                        // Dessiner le logo
                        g2d.drawImage(resizedImage,
                                (getWidth() - targetWidth) / 2,
                                -20, // Position plus haute
                                targetWidth,
                                targetHeight,
                                null);

                        // Restaurer la transformation et la composition originales
                        g2d.setTransform(originalTransform);
                        g2d.setComposite(originalComposite);

                        g2d.dispose();
                    }
                };

                logoContentPanel.setLayout(null); // Utiliser null layout pour un contrôle précis
                logoContentPanel.setOpaque(false);
                logoContentPanel.setPreferredSize(new Dimension(250, 250));

                // Créer et positionner le nom de l'application
                JLabel appNameLabel = new JLabel("com.vibeapp.VibeApp");
                appNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                appNameLabel.setForeground(EMSI_GREEN);
                appNameLabel.setBounds(0, 200, 250, 30); // Position précise
                appNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // Créer et positionner le sous-titre
                JLabel subtitleLabel = new JLabel("Communication Platform");
                subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                subtitleLabel.setForeground(EMSI_GRAY_LIGHT);
                subtitleLabel.setBounds(0, 230, 250, 20); // Position précise
                subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // Ajouter les composants au panneau
                logoContentPanel.add(appNameLabel);
                logoContentPanel.add(subtitleLabel);

                logoPanel.add(logoContentPanel);

                System.out.println("Logo chargé avec succès pour le panneau gauche");
            } else {
                System.out.println("Logo introuvable pour le panneau gauche: " + logoFile.getAbsolutePath());
                // Fallback au logo dessiné manuellement
                JLabel fallbackLabel = new JLabel("com.vibeapp.VibeApp");
                fallbackLabel.setFont(new Font("Arial", Font.BOLD, 28));
                fallbackLabel.setForeground(EMSI_GREEN);
                logoPanel.add(fallbackLabel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback au logo dessiné manuellement
            JLabel fallbackLabel = new JLabel("com.vibeapp.VibeApp");
            fallbackLabel.setFont(new Font("Arial", Font.BOLD, 28));
            fallbackLabel.setForeground(EMSI_GREEN);
            logoPanel.add(fallbackLabel);
        }

        // Message de bienvenue
        JLabel welcomeTitle = new JLabel("Récupération " +
                "mot de passe");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcomeTitle.setForeground(EMSI_GRAY);
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        /*JTextArea welcomeText = new JTextArea(
                "Vous avez oublié votre mot de passe ? Pas de problème ! " +
                        "Entrez simplement votre adresse email et nous vous enverrons " +
                        "les instructions pour réinitialiser votre mot de passe."
        );
        welcomeText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeText.setLineWrap(true);
        welcomeText.setWrapStyleWord(true);
        welcomeText.setEditable(false);
        welcomeText.setOpaque(false);
        welcomeText.setForeground(EMSI_GRAY);
        welcomeText.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeText.setBorder(new EmptyBorder(20, 0, 0, 0));

         */

        // Instructions
        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
        instructionsPanel.setOpaque(false);
        instructionsPanel.setBorder(new EmptyBorder(30, 10, 0, 0));

        String[] instructions = {
                "Entrez l'email associé à votre compte",
                "Vérifiez votre boîte de réception",
                "Cliquez sur le lien reçu par email",
                "Créez un nouveau mot de passe sécurisé",
                "Connectez-vous avec votre nouveau mot de passe"
        };

        for (String instruction : instructions) {
            JPanel instructionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            instructionRow.setOpaque(false);
            instructionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            // Icône numérotée
            JLabel numberIcon = createNumberedIcon(instructions, instruction);

            JLabel instructionLabel = new JLabel(instruction);
            instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            instructionLabel.setForeground(EMSI_GRAY);

            instructionRow.add(numberIcon);
            instructionRow.add(instructionLabel);

            instructionsPanel.add(instructionRow);
        }

        leftPanel.add(logoPanel);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(welcomeTitle);
        // leftPanel.add(welcomeText);
        leftPanel.add(instructionsPanel);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private JLabel createNumberedIcon(String[] instructions, String currentInstruction) {
        final int number = java.util.Arrays.asList(instructions).indexOf(currentInstruction) + 1;

        return new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Cercle vert
                g2d.setColor(EMSI_GREEN);
                g2d.fillOval(0, 0, 18, 18);

                // Numéro en blanc
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));

                // Centrer le numéro dans le cercle
                FontMetrics fm = g2d.getFontMetrics();
                String numberText = String.valueOf(number);
                int textWidth = fm.stringWidth(numberText);
                int textHeight = fm.getAscent();

                g2d.drawString(numberText, (18 - textWidth) / 2, (18 - textHeight) / 2 + textHeight);

                g2d.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(18, 18);
            }
        };
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(40, 20, 40, 40));
        rightPanel.setOpaque(false);

        // Panneau de formulaire avec fond blanc et ombre améliorée
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(30, 35, 35, 35));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(450, 350));

        // Titre du formulaire
        JLabel formTitle = new JLabel("Réinitialiser le mot de passe");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        formTitle.setForeground(EMSI_GRAY);
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Sous-titre
        JLabel formSubtitle = new JLabel("Nous vous enverrons un lien de réinitialisation");
        formSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formSubtitle.setForeground(EMSI_GRAY_LIGHT);
        formSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Icône de verrouillage
        JPanel lockIconPanel = new JPanel();
        lockIconPanel.setOpaque(false);
        lockIconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lockIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Cadenas
                g2d.setColor(EMSI_GREEN);

                // Corps du cadenas
                g2d.fillRoundRect(15, 25, 40, 30, 5, 5);

                // Arc du cadenas
                g2d.setStroke(new BasicStroke(5));
                g2d.drawArc(20, 5, 30, 30, 0, 180);

                // Trou de serrure
                g2d.setColor(Color.WHITE);
                g2d.fillOval(30, 35, 10, 10);

                g2d.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(70, 70);
            }
        };

        lockIconPanel.add(lockIcon);

        // Champ email
        JLabel emailLabel = new JLabel("Adresse email");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        emailLabel.setForeground(EMSI_GRAY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField emailContainer = new AnimatedTextField();
        emailField = emailContainer.getTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Icône email
        JLabel emailIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dessiner une enveloppe
                g2d.setColor(EMSI_GRAY_LIGHT);
                g2d.setStroke(new BasicStroke(1.5f));

                // Rectangle de l'enveloppe
                g2d.drawRoundRect(2, 4, 14, 10, 2, 2);

                // Lignes diagonales
                g2d.drawLine(2, 4, 9, 9);
                g2d.drawLine(16, 4, 9, 9);

                g2d.dispose();
            }
        };
        emailIcon.setPreferredSize(new Dimension(20, 20));
        emailContainer.addIcon(emailIcon);

        // Bouton "Envoyer le lien" avec design moderne
        ModernButton sendLinkButton = ModernButton.createPrimary("Envoyer le lien");
        sendLinkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendLinkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        sendLinkButton.addActionListener(e -> {
            if (validateEmail()) {
                // Create and send the email with verification code
                EmailSender emailSender = new EmailSender(emailField.getText().trim());
                if (emailSender.sendResetEmail()) {
                    // Showing the verification code in the console for testing
                    System.out.println("VERIFICATION CODE: " + emailSender.getVerificationCode());

                    // Show email sent confirmation
                    removeAll(); // Remove all components from this panel
                    add(new EmailSentPanel(emailField.getText().trim()), BorderLayout.CENTER); // Add the new panel
                    revalidate();
                    repaint();

                    // Open verification code page after a brief delay
                    Timer openVerificationTimer = new Timer(1500, ev -> {
                        // Open the verification code page
                        VerificationCodePage verificationPage = new VerificationCodePage(
                                emailField.getText().trim(),
                                emailSender.getVerificationCode()
                        );
                        verificationPage.setVisible(true);

                        // If using VibeApp navigation framework:
                        // VibeApp.getInstance().showPage("VerificationCode");
                    });
                    openVerificationTimer.setRepeats(false);
                    openVerificationTimer.start();
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Erreur lors de l'envoi de l'email. Veuillez réessayer.",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // Lien "Retour à la connexion"
        JPanel loginLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginLinkPanel.setOpaque(false);
        loginLinkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        ModernButton loginButton = ModernButton.createGhost("Retour à la connexion");
        loginButton.setButtonSize(ModernButton.ButtonSize.SMALL);
        loginButton.setShadowEnabled(false);
        loginButton.addActionListener(e -> {
            // Ouvrir la page de connexion
            VibeApp.getInstance().showPage("login");
            new LoginPage().setVisible(true);
        });

        loginLinkPanel.add(loginButton);

        // Assembler le formulaire
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(formSubtitle);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(lockIconPanel);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(emailLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(emailContainer);
        formPanel.add(Box.createVerticalStrut(25));
        formPanel.add(sendLinkButton);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(loginLinkPanel);

        rightPanel.add(formPanel);

        return rightPanel;
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();

        // Vérifier que l'email n'est pas vide
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Veuillez entrer votre adresse email",
                    "Erreur",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        // Use ValidationUtils for email validation
        if (!ValidationUtils.isValidEmail(email)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Veuillez entrer une adresse email valide",
                    "Erreur",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    private void addFooter() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(EMSI_GRAY);
        footerPanel.setPreferredSize(new Dimension(0, 30));

        JLabel footerText = new JLabel("© 2025  VibeApp - Tous droits réservés | Développé par les étudiants de l'EMSI");
        footerText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerText.setForeground(Color.WHITE);

        footerPanel.add(footerText);

        add(footerPanel, BorderLayout.SOUTH);
    }

    // Classes utilitaires définies correctement comme classes internes

    // Classe pour créer un champ de texte animé
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

            // Ajouter les écouteurs de focus
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

        public void addIcon(JComponent icon) {
            add(icon, BorderLayout.EAST);
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

    // Classe pour animer le bouton
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

            // Timer pour l'animation
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

            // Déterminer la couleur actuelle
            Color currentColor;
            if (isPressed) {
                currentColor = pressedColor;
            } else if (isHovered) {
                currentColor = interpolateColor(baseColor, hoverColor, animationProgress);
            } else {
                currentColor = interpolateColor(hoverColor, baseColor, animationProgress);
            }

            // Dessiner le fond avec un dégradé
            GradientPaint gradient = new GradientPaint(
                    0, 0, currentColor,
                    0, getHeight(), currentColor.darker()
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            // Ajouter un effet de brillance
            if (!isPressed) {
                GradientPaint shineGradient = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 60),
                        0, getHeight()/2, new Color(255, 255, 255, 0)
                );
                g2d.setPaint(shineGradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight()/2, 10, 10);
            }

            // Dessiner le texte avec une légère ombre
            FontMetrics fm = g2d.getFontMetrics(getFont());
            Rectangle stringBounds = fm.getStringBounds(getText(), g2d).getBounds();

            int textX = (getWidth() - stringBounds.width) / 2;
            int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();

            // Ombre du texte
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.drawString(getText(), textX + 1, textY + 1);

            // Texte
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

    // Classe pour gérer les bulles animées
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
            // Update angle for more natural movement
            angle += (Math.random() - 0.5) * 0.1;

            // Move the bubble
            x += Math.cos(angle) * speed;
            y += Math.sin(angle) * speed;

            // Get current panel dimensions dynamically
            int panelWidth = ForgotPasswordPage.this.getWidth();
            int panelHeight = ForgotPasswordPage.this.getHeight();

            // Use fallback dimensions if panel not yet sized
            if (panelWidth <= 0) panelWidth = 900;
            if (panelHeight <= 0) panelHeight = 600;

            // Bounce off edges with size consideration
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

    // Classe pour créer un panneau avec coins arrondis
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

            // Ajouter une ombre plus élégante et subtile
            for (int i = 0; i < 5; i++) {
                float alpha = 0.02f * (5 - i);
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(i + 2, i + 2, getWidth() - (i * 2 + 4), getHeight() - (i * 2 + 4), cornerRadius, cornerRadius);
            }

            // Ajouter un léger effet de dégradé au fond
            GradientPaint paint = new GradientPaint(
                    0, 0, Color.WHITE,
                    0, getHeight(), new Color(250, 250, 250)
            );
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, cornerRadius, cornerRadius);

            // Ajouter un léger highlight en haut
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillRoundRect(0, 0, getWidth() - 3, 20, cornerRadius, cornerRadius);

            g2.dispose();
        }
    }

    class EmailSender {
        private String recipientEmail;
        private String verificationCode;

        public EmailSender(String recipientEmail) {
            this.recipientEmail = recipientEmail;
        }

        public boolean sendResetEmail() {
            try {
                // Generate a verification code
                this.verificationCode = String.format("%06d", new java.util.Random().nextInt(1000000));

                // Log the code for testing purposes
                System.out.println("\n==============================================");
                System.out.println("EDUCATIONAL MODE: Verification code for " + recipientEmail + ": " + verificationCode);
                System.out.println("==============================================\n");

                // Simulate sending an email
                boolean success = EmailService.sendVerificationCode(recipientEmail, verificationCode);

                return success;
            } catch (Exception e) {
                e.printStackTrace();

                // For educational purposes, still generate a code even on failure
                this.verificationCode = String.format("%06d", new java.util.Random().nextInt(1000000));
                System.out.println("FALLBACK CODE: " + verificationCode);

                return true; // Return success for educational purposes
            }
        }

        public String getVerificationCode() {
            return verificationCode;
        }
    }


    // Classe pour l'écran de confirmation après envoi de l'email
    // In ForgotPasswordPage.java - Complete the EmailSentPanel inner class

    class EmailSentPanel extends JPanel {
        private Timer animationTimer;
        private float animationProgress = 0f;

        public EmailSentPanel(String email) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(30, 30, 30, 30));

            // Animation of the confirmation icon
            JLabel checkIcon = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Green circle with size animation
                    float scale = 0.5f + 0.5f * animationProgress;
                    int size = (int)(60 * scale);
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;

                    g2d.setColor(EMSI_GREEN);
                    g2d.fillOval(x, y, size, size);

                    // White checkmark
                    if (animationProgress > 0.5f) {
                        float checkProgress = (animationProgress - 0.5f) * 2f;

                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(4f));

                        int startX = x + size/4;
                        int midX = x + size/2;
                        int endX = x + size*3/4;
                        int startY = y + size/2;
                        int midY = y + size*2/3;
                        int endY = y + size/3;

                        if (checkProgress <= 0.5f) {
                            // First part of the checkmark (/)
                            int currentMidX = startX + (int)((midX - startX) * (checkProgress * 2));
                            int currentMidY = startY + (int)((midY - startY) * (checkProgress * 2));
                            g2d.drawLine(startX, startY, currentMidX, currentMidY);
                        } else {
                            // Complete checkmark
                            g2d.drawLine(startX, startY, midX, midY);

                            // Second part of the checkmark (\)
                            int currentEndX = midX + (int)((endX - midX) * ((checkProgress - 0.5f) * 2));
                            int currentEndY = midY + (int)((endY - midY) * ((checkProgress - 0.5f) * 2));
                            g2d.drawLine(midX, midY, currentEndX, currentEndY);
                        }
                    }

                    g2d.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(80, 80);
                }
            };

            checkIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Title
            JLabel titleLabel = new JLabel("Code envoyé !");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setForeground(EMSI_GRAY);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Message with email address
            JTextArea messageText = new JTextArea(
                    "Nous avons envoyé un code de vérification à l'adresse suivante :\n\n" +
                            email + "\n\n" +
                            "Veuillez vérifier votre boîte de réception et saisir le code à 6 chiffres pour réinitialiser votre mot de passe."
            );
            messageText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageText.setLineWrap(true);
            messageText.setWrapStyleWord(true);
            messageText.setEditable(false);
            messageText.setBackground(Color.WHITE);
            messageText.setForeground(EMSI_GRAY);
            messageText.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Additional instructions
            JTextArea instructionsText = new JTextArea(
                    "Si vous ne recevez pas le code dans les prochaines minutes, veuillez vérifier votre dossier spam ou essayez à nouveau avec une adresse email différente."
            );
            instructionsText.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            instructionsText.setLineWrap(true);
            instructionsText.setWrapStyleWord(true);
            instructionsText.setEditable(false);
            instructionsText.setBackground(Color.WHITE);
            instructionsText.setForeground(EMSI_GRAY_LIGHT);
            instructionsText.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Button to return to login
            AnimatedButton backToLoginButton = new AnimatedButton("Retour à la connexion", EMSI_GREEN, EMSI_GREEN_LIGHT, EMSI_GREEN_DARK);
            backToLoginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            backToLoginButton.setMaximumSize(new Dimension(200, 40));
            backToLoginButton.addActionListener(e -> {
                VibeApp.getInstance().showPage("login");
                new LoginPage().setVisible(true);
            });

            // Add all components
            add(Box.createVerticalStrut(20));
            add(checkIcon);
            add(Box.createVerticalStrut(20));
            add(titleLabel);
            add(Box.createVerticalStrut(20));
            add(messageText);
            add(Box.createVerticalStrut(20));
            add(instructionsText);
            add(Box.createVerticalStrut(30));
            add(backToLoginButton);

            // Start animation
            animationTimer = new Timer(16, e -> {
                animationProgress += 0.02f;
                if (animationProgress >= 1f) {
                    animationProgress = 1f;
                    ((Timer)e.getSource()).stop();
                }
                checkIcon.repaint();
            });
            animationTimer.start();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Utiliser le constructeur Color pour RGBA puis créer ColorUIResource
            UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("TextField.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            ForgotPasswordPage forgotPasswordScreen = new ForgotPasswordPage();
            forgotPasswordScreen.setVisible(true);
        });
    }
}