package ui.pages;

import com.vibeapp.VibeApp;
import ui.components.ModernButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import services.FirebaseService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;


public class RegisterPage extends JPanel {
    // Couleurs EMSI avec des variations
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private final Color EMSI_RED = new Color(217, 83, 30);
    private final Color EMSI_RED_LIGHT = new Color(230, 110, 50);

    private JTextField nomField;
    private JTextField prenomField;
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JPanel mainPanel;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    // Animation des bulles
    private java.util.List<Bubble> bubbles = new java.util.ArrayList<>();
    private Timer bubbleTimer;

    public RegisterPage() {
        /*
        setTitle("VibeApp - Inscription");
        setSize(950, 650); // Reduced height slightly
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

         */

        // Changer l'icône de l'application


        // Configuration générale
        setLayout(new BorderLayout());

        // Initialiser les bulles animées
        initBubbles();

        // Créer le panneau principal avec un fond dégradé
        createMainPanel();

        // Ajouter un copyright dans le pied de page
        addFooter();

        // Test Firebase connection
        initializeFirebase();

        setSize(1000, 750);

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

    private void initializeFirebase() {
        try {
            // Get the singleton instance instead of creating a new one
            FirebaseService firebaseService = FirebaseService.getInstance();

            // Test connection
            boolean connected = firebaseService.testConnection();

            if (connected) {
                // Only proceed with other Firebase operations if connected

                // Example: Create a new chat between users
                String[] users = {"user1", "user2"};
                String chatId = firebaseService.createChat(users);

                // Example: Send a message
                firebaseService.sendMessage(chatId, "user1", "Hello, welcome to com.vibeapp.VibeApp!");

                // Example: Listen for messages
                firebaseService.listenForMessages(chatId);

                System.out.println("Firebase initialized successfully");
            } else {
                System.err.println("Could not connect to Firebase, initialization aborted");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
        }
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
        int width = Math.max(getWidth(), 1000);
        int height = Math.max(getHeight(), 750);

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
            // Repaint the entire ui.pages.RegisterPage instead of just mainPanel
            repaint();
        });

        // Start the animation
        bubbleTimer.start();
    }

    private Image ajouterImage() {
        try {
            File file = new File("pictures/logoVibeApp.png");
            if (file.exists()) {
                return new ImageIcon(file.getAbsolutePath()).getImage();
            } else {
                System.out.println("Logo file not found at: " + file.getAbsolutePath());
                return createFallbackImage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createFallbackImage();
        }
    }

    private Image createFallbackImage() {
        //Create a simple fallback image
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(EMSI_GRAY);
        g2d.fillRect(10, 10, 40, 20);

        g2d.setColor(EMSI_GREEN);
        g2d.fillOval(12, 15, 25, 25);

        g2d.setColor(EMSI_RED);
        g2d.fillRect(44, 17, 8, 8);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("EMSI", 14, 25);

        g2d.dispose();

        return image;
    }

    private void createMainPanel() {
        // Ajouter un bouton de retour en haut de la page
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(10, 15, 0, 0));
        
        ModernButton backButton = ModernButton.createGhost("← Retour à la connexion");
        backButton.setButtonSize(ModernButton.ButtonSize.SMALL);
        backButton.setShadowEnabled(false);
        backButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("login");
        });
        
        topPanel.add(backButton);
        
        add(topPanel, BorderLayout.NORTH);
        
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

        // Créer une section droite (formulaire d'inscription)
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
        
        // Responsive padding based on window size
        int width = getWidth() > 0 ? getWidth() : 1000;
        int leftPadding = Math.max(20, width / 25);
        int rightPadding = Math.max(10, width / 50);
        int topBottomPadding = Math.max(40, getHeight() / 15);
        
        leftPanel.setBorder(new EmptyBorder(topBottomPadding, leftPadding, topBottomPadding, rightPadding));
        
        // Responsive preferred size
        int preferredWidth = Math.max(300, width / 2 - 50);
        leftPanel.setPreferredSize(new Dimension(preferredWidth, 0));


        // Logo com.vibeapp.VibeApp - taille réduite pour faire place au bouton de retour
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(0, 160));
        logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        try {
            // Utiliser le même chemin que dans ajouterImage()
            File logoFile = new File("pictures/logoVibeApp.png");
            if (logoFile.exists()) {
                // Charger le logo original avec haute qualité
                BufferedImage originalImage = ImageIO.read(logoFile);

                // Redimensionnement de haute qualité avec préservation de la netteté - taille réduite
                int targetWidth = 160; // Taille réduite pour faire place au bouton de retour
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

                        // Dessiner le logo - position légèrement plus haute pour compenser le bouton de retour
                        g2d.drawImage(resizedImage,
                                (getWidth() - targetWidth) / 2,
                                -10, // Position légèrement plus haute
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
                logoContentPanel.setPreferredSize(new Dimension(200, 200));

                // Créer et positionner le nom de l'application
                JLabel appNameLabel = new JLabel("VibeApp");
                appNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
                appNameLabel.setForeground(EMSI_GREEN);
                appNameLabel.setBounds(0, 160, 200, 30); // Position ajustée
                appNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // Créer et positionner le sous-titre
                JLabel subtitleLabel = new JLabel("Communication Platform");
                subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                subtitleLabel.setForeground(EMSI_GRAY_LIGHT);
                subtitleLabel.setBounds(0, 185, 200, 20); // Position ajustée
                subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // Ajouter les composants au panneau
                logoContentPanel.add(appNameLabel);
                logoContentPanel.add(subtitleLabel);

                logoPanel.add(logoContentPanel);

                System.out.println("Logo chargé avec succès pour le panneau gauche");
            } else {
                System.out.println("Logo introuvable pour le panneau gauche: " + logoFile.getAbsolutePath());
                // Fallback au logo dessiné manuellement
                JLabel fallbackLabel = new JLabel("VibeApp");
                fallbackLabel.setFont(new Font("Arial", Font.BOLD, 24));
                fallbackLabel.setForeground(EMSI_GREEN);
                logoPanel.add(fallbackLabel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback au logo dessiné manuellement
            JLabel fallbackLabel = new JLabel("VibeApp");
            fallbackLabel.setFont(new Font("Arial", Font.BOLD, 24));
            fallbackLabel.setForeground(EMSI_GREEN);
            logoPanel.add(fallbackLabel);
        }

        // Message de bienvenue
        JLabel welcomeTitle = new JLabel("Rejoignez VibeApp");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcomeTitle.setForeground(EMSI_GRAY);
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea welcomeText = new JTextArea(
                "Créez votre compte pour rejoindre la communauté VibeApp. " +
                        "Vous pourrez ainsi participer aux discussions, partager vos connaissances " +
                        "et collaborer avec vos camarades et professeurs."
        );
        welcomeText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeText.setLineWrap(true);
        welcomeText.setWrapStyleWord(true);
        welcomeText.setEditable(false);
        welcomeText.setOpaque(false);
        welcomeText.setForeground(EMSI_GRAY);
        welcomeText.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeText.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Avantages de l'inscription
        JPanel benefitsPanel = new JPanel();
        benefitsPanel.setLayout(new BoxLayout(benefitsPanel, BoxLayout.Y_AXIS));
        benefitsPanel.setOpaque(false);
        benefitsPanel.setBorder(new EmptyBorder(30, 10, 0, 0));

        String[] benefits = {
                "Accès illimité aux salles de discussion",
                "Création de groupes personnalisés",
                "Partage de documents et ressources",
                "Notifications personnalisées",
                "Support technique prioritaire"
        };

        for (String benefit : benefits) {
            JPanel benefitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            benefitRow.setOpaque(false);
            benefitRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            // Icône de validation animée
            JLabel checkIcon = new AnimatedCheckIcon(16, 16, EMSI_GREEN);

            JLabel benefitLabel = new JLabel(benefit);
            benefitLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            benefitLabel.setForeground(EMSI_GRAY);

            benefitRow.add(checkIcon);
            benefitRow.add(benefitLabel);

            benefitsPanel.add(benefitRow);
        }

        leftPanel.add(logoPanel);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(welcomeTitle);
        leftPanel.add(welcomeText);
        leftPanel.add(benefitsPanel);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        
        // Responsive padding
        int width = getWidth() > 0 ? getWidth() : 1000;
        int height = getHeight() > 0 ? getHeight() : 750;
        int sidePadding = Math.max(20, width / 30);
        int topBottomPadding = Math.max(30, height / 20);
        
        rightPanel.setBorder(new EmptyBorder(topBottomPadding, sidePadding, topBottomPadding, sidePadding));
        rightPanel.setOpaque(false);

        // Panneau de formulaire avec fond blanc et ombre améliorée
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(30, 35, 35, 35));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Responsive form panel size - Agrandir pour afficher le bouton
        int formWidth = Math.min(450, Math.max(350, width - 100));
        int formHeight = Math.min(650, Math.max(580, height - 50)); // Hauteur augmentée
        formPanel.setMaximumSize(new Dimension(formWidth, formHeight));
        formPanel.setPreferredSize(new Dimension(formWidth, formHeight));

        // Titre du formulaire
        JLabel formTitle = new JLabel("Créer un compte");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        formTitle.setForeground(EMSI_GRAY);
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Sous-titre
        JLabel formSubtitle = new JLabel("Remplissez le formulaire pour vous inscrire");
        formSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formSubtitle.setForeground(EMSI_GRAY_LIGHT);
        formSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Panel pour les champs Nom et Prénom (côte à côte)
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.setOpaque(false);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        // Panel Nom
        JPanel nomPanelContainer = new JPanel();
        nomPanelContainer.setLayout(new BoxLayout(nomPanelContainer, BoxLayout.Y_AXIS));
        nomPanelContainer.setOpaque(false);

        JLabel nomLabel = new JLabel("Nom");
        nomLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nomLabel.setForeground(EMSI_GRAY);
        nomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField nomContainer = new AnimatedTextField();
        nomField = nomContainer.getTextField();
        nomField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        nomPanelContainer.add(nomLabel);
        nomPanelContainer.add(Box.createVerticalStrut(8));
        nomPanelContainer.add(nomContainer);

        // Panel Prénom
        JPanel prenomPanelContainer = new JPanel();
        prenomPanelContainer.setLayout(new BoxLayout(prenomPanelContainer, BoxLayout.Y_AXIS));
        prenomPanelContainer.setOpaque(false);

        JLabel prenomLabel = new JLabel("Prénom");
        prenomLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        prenomLabel.setForeground(EMSI_GRAY);
        prenomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField prenomContainer = new AnimatedTextField();
        prenomField = prenomContainer.getTextField();
        prenomField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        prenomPanelContainer.add(prenomLabel);
        prenomPanelContainer.add(Box.createVerticalStrut(8));
        prenomPanelContainer.add(prenomContainer);

        namePanel.add(nomPanelContainer);
        namePanel.add(Box.createHorizontalStrut(15));
        namePanel.add(prenomPanelContainer);

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
                g2d.drawRect(2, 4, 14, 10);

                // Lignes diagonales
                g2d.drawLine(2, 4, 9, 9);
                g2d.drawLine(16, 4, 9, 9);

                g2d.dispose();
            }
        };
        emailIcon.setPreferredSize(new Dimension(20, 20));
        emailContainer.addIcon(emailIcon);

        // Champ username
        JLabel usernameLabel = new JLabel("Nom d'utilisateur");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usernameLabel.setForeground(EMSI_GRAY);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField usernameContainer = new AnimatedTextField();
        usernameField = usernameContainer.getTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Icône username
        JLabel usernameIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dessiner un simple icône utilisateur
                g2d.setColor(EMSI_GRAY_LIGHT);
                g2d.setStroke(new BasicStroke(1.5f));

                // Tête
                g2d.drawOval(5, 2, 8, 8);

                // Corps
                g2d.drawArc(2, 8, 14, 10, 0, 180);

                g2d.dispose();
            }
        };
        usernameIcon.setPreferredSize(new Dimension(20, 20));
        usernameContainer.addIcon(usernameIcon);

        // Champ mot de passe
        JLabel passwordLabel = new JLabel("Mot de passe");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordLabel.setForeground(EMSI_GRAY);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField passwordContainer = new AnimatedTextField();
        passwordField = new JPasswordField();
        passwordContainer.setTextField(passwordField);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setEchoChar('•');

        // Icône œil pour afficher/masquer le mot de passe
        JLabel eyeIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(EMSI_GRAY_LIGHT);
                g2d.setStroke(new BasicStroke(1.5f));

                // Œil
                g2d.drawOval(2, 5, 14, 10);
                g2d.fillOval(7, 7, 4, 6);

                // Ligne barrant l'œil si le mot de passe est masqué
                if (!passwordVisible) {
                    g2d.setColor(EMSI_RED_LIGHT);
                    g2d.drawLine(2, 18, 16, 2);
                }

                g2d.dispose();
            }
        };
        eyeIcon.setPreferredSize(new Dimension(20, 20));
        eyeIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                passwordVisible = !passwordVisible;
                passwordField.setEchoChar(passwordVisible ? (char) 0 : '•');
                eyeIcon.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                eyeIcon.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                eyeIcon.setBorder(null);
            }
        });

        passwordContainer.addIcon(eyeIcon);

        // Champ confirmation de mot de passe
        JLabel confirmPasswordLabel = new JLabel("Confirmer le mot de passe");
        confirmPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confirmPasswordLabel.setForeground(EMSI_GRAY);
        confirmPasswordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField confirmPasswordContainer = new AnimatedTextField();
        confirmPasswordField = new JPasswordField();
        confirmPasswordContainer.setTextField(confirmPasswordField);
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        confirmPasswordField.setEchoChar('•');

        // Icône œil pour confirmer le mot de passe
        JLabel confirmEyeIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(EMSI_GRAY_LIGHT);
                g2d.setStroke(new BasicStroke(1.5f));

                // Œil
                g2d.drawOval(2, 5, 14, 10);
                g2d.fillOval(7, 7, 4, 6);

                // Ligne barrant l'œil si le mot de passe est masqué
                if (!confirmPasswordVisible) {
                    g2d.setColor(EMSI_RED_LIGHT);
                    g2d.drawLine(2, 18, 16, 2);
                }

                g2d.dispose();
            }
        };
        confirmEyeIcon.setPreferredSize(new Dimension(20, 20));
        confirmEyeIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmEyeIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                confirmPasswordVisible = !confirmPasswordVisible;
                confirmPasswordField.setEchoChar(confirmPasswordVisible ? (char) 0 : '•');
                confirmEyeIcon.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                confirmEyeIcon.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                confirmEyeIcon.setBorder(null);
            }
        });

        confirmPasswordContainer.addIcon(confirmEyeIcon);

        // Case à cocher "J'accepte les conditions" avec design amélioré
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        termsPanel.setOpaque(false);
        termsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JCheckBox termsCheckBox = new JCheckBox("J'accepte les conditions d'utilisation");
        termsCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        termsCheckBox.setForeground(EMSI_GRAY);
        termsCheckBox.setOpaque(false);
        termsCheckBox.setFocusPainted(false);
        termsCheckBox.setIcon(new CustomCheckBoxIcon(false, EMSI_GREEN));
        termsCheckBox.setSelectedIcon(new CustomCheckBoxIcon(true, EMSI_GREEN));

        termsPanel.add(termsCheckBox);

        // Bouton "S'inscrire" avec design moderne
        ModernButton registerButton = ModernButton.createPrimary("S'inscrire");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        registerButton.addActionListener(e -> {
            if (validateForm()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Inscription réussie ! Bienvenue dans VibeApp.",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE
                );
                // Ici vous pourriez ajouter le code pour ouvrir la page principale de chat
                VibeApp.getInstance().showPage("login"); // Fermer la page d'inscription
            }
        });

        // Séparateur visuel
        JPanel separatorPanel = new JPanel();
        separatorPanel.setOpaque(false);
        separatorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        separatorPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        JLabel orLabel = new JLabel("ou");
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        orLabel.setForeground(EMSI_GRAY_LIGHT);
        separatorPanel.add(orLabel);

        // Lien "Retour à la connexion" sans cadre - plus visible
        JPanel loginLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginLinkPanel.setOpaque(false);
        loginLinkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        ModernButton loginButton = ModernButton.createGhost("← Retour à la connexion");
        loginButton.setButtonSize(ModernButton.ButtonSize.MEDIUM);
        loginButton.setShadowEnabled(false);
        loginButton.setPreferredSize(new Dimension(200, 40));
        
        loginButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("login");
        });
        
        loginLinkPanel.add(loginButton);

        // Assembler le formulaire avec espacement réduit
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(3));
        formPanel.add(formSubtitle);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(namePanel);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(emailLabel);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(emailContainer);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(usernameLabel);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(usernameContainer);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(passwordContainer);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(confirmPasswordLabel);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(confirmPasswordContainer);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(registerButton);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(separatorPanel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(loginLinkPanel);

        rightPanel.add(formPanel);

        return rightPanel;
    }

    private JPanel createCompactLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        leftPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        // Compact logo section
        JLabel titleLabel = new JLabel("VibeApp", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(EMSI_GREEN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Créez votre compte", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
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
        int formWidth = Math.min(400, width - 60);
        formPanel.setMaximumSize(new Dimension(formWidth, Integer.MAX_VALUE));
        
        // Compact form fields - reuse the same field creation logic but with smaller spacing
        createCompactFormFields(formPanel);
        
        rightPanel.add(formPanel);
        return rightPanel;
    }

    private void createCompactFormFields(JPanel formPanel) {
        // Form title
        JLabel formTitle = new JLabel("Inscription");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        formTitle.setForeground(EMSI_GRAY);
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create all form fields with reduced spacing - reuse existing field creation logic
        // Panel pour les champs Nom et Prénom (côte à côte)
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.setOpaque(false);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        // Panel Nom
        JPanel nomPanelContainer = new JPanel();
        nomPanelContainer.setLayout(new BoxLayout(nomPanelContainer, BoxLayout.Y_AXIS));
        nomPanelContainer.setOpaque(false);

        JLabel nomLabel = new JLabel("Nom");
        nomLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nomLabel.setForeground(EMSI_GRAY);
        nomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField nomContainer = new AnimatedTextField();
        nomField = nomContainer.getTextField();
        nomField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        nomPanelContainer.add(nomLabel);
        nomPanelContainer.add(Box.createVerticalStrut(5));
        nomPanelContainer.add(nomContainer);

        // Panel Prénom
        JPanel prenomPanelContainer = new JPanel();
        prenomPanelContainer.setLayout(new BoxLayout(prenomPanelContainer, BoxLayout.Y_AXIS));
        prenomPanelContainer.setOpaque(false);

        JLabel prenomLabel = new JLabel("Prénom");
        prenomLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prenomLabel.setForeground(EMSI_GRAY);
        prenomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField prenomContainer = new AnimatedTextField();
        prenomField = prenomContainer.getTextField();
        prenomField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        prenomPanelContainer.add(prenomLabel);
        prenomPanelContainer.add(Box.createVerticalStrut(5));
        prenomPanelContainer.add(prenomContainer);

        namePanel.add(nomPanelContainer);
        namePanel.add(Box.createHorizontalStrut(10));
        namePanel.add(prenomPanelContainer);

        // Email field
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailLabel.setForeground(EMSI_GRAY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField emailContainer = new AnimatedTextField();
        emailField = emailContainer.getTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Username field
        JLabel usernameLabel = new JLabel("Nom d'utilisateur");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usernameLabel.setForeground(EMSI_GRAY);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField usernameContainer = new AnimatedTextField();
        usernameField = usernameContainer.getTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Password field
        JLabel passwordLabel = new JLabel("Mot de passe");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordLabel.setForeground(EMSI_GRAY);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField passwordContainer = new AnimatedTextField();
        passwordField = new JPasswordField();
        passwordContainer.setTextField(passwordField);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setEchoChar('•');

        // Confirm password field
        JLabel confirmPasswordLabel = new JLabel("Confirmer mot de passe");
        confirmPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        confirmPasswordLabel.setForeground(EMSI_GRAY);
        confirmPasswordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AnimatedTextField confirmPasswordContainer = new AnimatedTextField();
        confirmPasswordField = new JPasswordField();
        confirmPasswordContainer.setTextField(confirmPasswordField);
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        confirmPasswordField.setEchoChar('•');

        // Register button
        AnimatedButton registerButton = new AnimatedButton("S'inscrire", EMSI_GREEN, EMSI_GREEN_LIGHT, EMSI_GREEN_DARK);
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerButton.addActionListener(e -> {
            if (validateForm()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Inscription réussie ! Bienvenue dans VibeApp.",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE
                );
                VibeApp.getInstance().showPage("login");
            }
        });

        // Séparateur compact
        JPanel separatorPanel = new JPanel();
        separatorPanel.setOpaque(false);
        separatorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        separatorPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        JLabel orLabel = new JLabel("ou");
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        orLabel.setForeground(EMSI_GRAY_LIGHT);
        separatorPanel.add(orLabel);

        // Login button compact mais très visible
        JPanel loginLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginLinkPanel.setOpaque(false);
        loginLinkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

        JButton loginButton = new JButton("← Retour à la connexion");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(EMSI_GREEN);
        loginButton.setBackground(Color.WHITE);
        loginButton.setOpaque(true);
        loginButton.setContentAreaFilled(true);
        loginButton.setBorderPainted(true);
        loginButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(EMSI_GREEN, 2, true),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(220, 40));
        
        // Effets hover pour la version compacte
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(EMSI_GREEN);
                loginButton.setForeground(Color.WHITE);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(Color.WHITE);
                loginButton.setForeground(EMSI_GREEN);
            }
        });
        
        loginButton.addActionListener(e -> {
            VibeApp.getInstance().showPage("login");
        });
        loginLinkPanel.add(loginButton);

        // Assemble compact form
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(namePanel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(emailLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(emailContainer);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(usernameLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(usernameContainer);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(passwordContainer);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(confirmPasswordLabel);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(confirmPasswordContainer);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(registerButton);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(separatorPanel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(loginLinkPanel);
    }

    private boolean validateForm() {
        // Récupérer les valeurs
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Vérifier que tous les champs sont remplis
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Veuillez remplir tous les champs obligatoires",
                    "Erreur d'inscription",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        // Vérifier que le nom d'utilisateur est valide
        if (username.length() < 3) {
            JOptionPane.showMessageDialog(
                    this,
                    "Le nom d'utilisateur doit contenir au moins 3 caractères",
                    "Erreur d'inscription",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        // Vérifier que l'email est valide
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        if (!pattern.matcher(email).matches()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Veuillez entrer une adresse email valide",
                    "Erreur d'inscription",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        // Vérifier que le mot de passe et sa confirmation correspondent
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Les mots de passe ne correspondent pas",
                    "Erreur d'inscription",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        // Vérifier la complexité du mot de passe (au moins 8 caractères)
        if (password.length() < 8) {
            JOptionPane.showMessageDialog(
                    this,
                    "Le mot de passe doit contenir au moins 8 caractères",
                    "Erreur d'inscription",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        // Si toutes les validations sont passées, enregistrer l'utilisateur dans Firebase
        try {
            // Get the singleton instance instead of creating a new one
            FirebaseService firebaseService = FirebaseService.getInstance();

            // Generate a unique ID for the user
            String userId = "user_" + System.currentTimeMillis();

            // Create user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("nom", nom);
            userData.put("prenom", prenom);
            userData.put("email", email);
            userData.put("username", username);
            userData.put("password", hashPassword(password));
            userData.put("createdAt", ServerValue.TIMESTAMP);

            // Save to Firebase
            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            usersRef.child(userId).setValueAsync(userData);

            System.out.println("User registered successfully: " + userId);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Error registering user: " + e.getMessage(),
                    "Firebase Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Fallback to a simple hash if SHA-256 is not available
            return Integer.toString(password.hashCode());
        }
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

    // Classe pour la case à cocher personnalisée
    class CustomCheckBoxIcon implements Icon {
        private final boolean selected;
        private final Color color;

        public CustomCheckBoxIcon(boolean selected, Color color) {
            this.selected = selected;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dessiner la case à cocher
            g2d.setColor(selected ? color : Color.WHITE);
            g2d.fillRoundRect(x, y, 16, 16, 3, 3);
            g2d.setColor(selected ? color.darker() : new Color(200, 200, 200));
            g2d.drawRoundRect(x, y, 16, 16, 3, 3);

            // Dessiner la coche si sélectionné
            if (selected) {
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(x + 4, y + 8, x + 7, y + 11);
                g2d.drawLine(x + 7, y + 11, x + 12, y + 5);
            }

            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    // Classe pour animer le bouton d'inscription
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

        public void setTextField(JTextField field) {
            remove(textField);
            textField = field;
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

    // Classe pour les icônes de validation animées
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

            // Cercle avec opacité variable
            float alpha = 0.6f + 0.4f * (float) Math.sin(animationProgress * 2 * Math.PI);
            g2d.setColor(new Color(
                    iconColor.getRed() / 255f,
                    iconColor.getGreen() / 255f,
                    iconColor.getBlue() / 255f,
                    alpha
            ));

            g2d.fillOval(0, 0, 16, 16);

            // Coche blanche
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            int offset = (int) (animationProgress * 2) % 2;
            g2d.drawLine(4, 8 + offset, 7, 11 + offset);
            g2d.drawLine(7, 11 + offset, 12, 5 + offset);

            g2d.dispose();
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
            int panelWidth = RegisterPage.this.getWidth();
            int panelHeight = RegisterPage.this.getHeight();

            // Use fallback dimensions if panel not yet sized
            if (panelWidth <= 0) panelWidth = 1000;
            if (panelHeight <= 0) panelHeight = 750;

            // Bounce off edges
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Utiliser le constructeur Color pour RGBA puis créer ColorUIResource
            UIManager.put("Button.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("CheckBox.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("TabbedPane.focus", new javax.swing.plaf.ColorUIResource(new Color(0, 0, 0, 0)));
            UIManager.put("ComboBox.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));
            UIManager.put("ComboBox.selectionForeground", new javax.swing.plaf.ColorUIResource(Color.WHITE));
            UIManager.put("TextField.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(0, 150, 70)));

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            // Ajouter une petite animation au démarrage
            JFrame splashFrame = new JFrame();
            splashFrame.setUndecorated(true);
            splashFrame.setBackground(new Color(0, 0, 0, 0));

            JPanel splashPanel = new JPanel() {
                private float progress = 0f;
                private Timer timer;

                {
                    timer = new Timer(20, e -> {
                        progress += 0.05f;
                        if (progress > 1f) {
                            ((Timer) e.getSource()).stop();
                            splashFrame.dispose();

                            // Afficher l'application principale
                            RegisterPage app = new RegisterPage();
                            app.setVisible(true);
                        }
                        repaint();
                    });
                    timer.start();
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Dessiner le logo EMSI avec une animation
                    int alpha = (int) (progress * 255);

                    // Rectangle gris avec coins arrondis
                    g2d.setColor(new Color(90, 90, 90, alpha));
                    g2d.fillRoundRect(50, 50, 100, 40, 8, 8);

                    // Silhouettes vertes
                    g2d.setColor(new Color(0, 150, 70, alpha));
                    int[] xPoints = {55, 70, 85, 100, 120, 140};
                    int[] yPoints = {90, 40, 70, 30, 50, 90};
                    g2d.fillPolygon(xPoints, yPoints, 6);

                    // Carré rouge avec coins arrondis
                    g2d.setColor(new Color(217, 83, 30, alpha));
                    g2d.fillRoundRect(145, 65, 10, 10, 3, 3);

                    // Texte
                    g2d.setColor(new Color(255, 255, 255, alpha));
                    g2d.setFont(new Font("Arial", Font.BOLD, 24));
                    g2d.drawString("EMSI", 60, 77);

                    g2d.setColor(new Color(0, 150, 70, alpha));
                    g2d.setFont(new Font("Arial", Font.BOLD, 18));
                    g2d.drawString("CHAT", 95, 110);

                    // Barre de chargement avec animation plus fluide
                    g2d.setColor(new Color(220, 220, 220, alpha));
                    g2d.fillRoundRect(50, 130, 150, 10, 5, 5);

                    // Animation de remplissage avec effet de pulsation
                    float pulseFactor = 1.0f + 0.05f * (float)Math.sin(progress * 10);
                    int width = (int)(150 * progress * pulseFactor);
                    if (width > 150) width = 150;

                    g2d.setColor(new Color(0, 150, 70, alpha));
                    g2d.fillRoundRect(50, 130, width, 10, 5, 5);

                    g2d.dispose();
                }
            };

            splashPanel.setPreferredSize(new Dimension(250, 180));
            splashPanel.setOpaque(false);

            splashFrame.setContentPane(splashPanel);
            splashFrame.pack();
            splashFrame.setLocationRelativeTo(null);
            splashFrame.setVisible(true);
        });
    }
}