package ui.components;

import services.FirebaseService;
import services.GroupService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.*;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * A simple group creator dialog that just lists users to select
 */
public class SimpleGroupCreator extends JDialog {
    // Services
    private final String currentUserId;
    private final GroupService groupService;
    private final FirebaseService firebaseService;

    // UI Components
    private JTextField groupNameField;
    private JTextArea descriptionField;
    private JPanel userListPanel;
    private JButton createButton;

    // Selected users
    private final List<String> selectedUserIds = new ArrayList<>();

    /**
     * Constructor
     */
    public SimpleGroupCreator(Frame parent, String currentUserId) throws IOException {
        super(parent, "Create a New Group", true);
        this.currentUserId = currentUserId;
        this.groupService = GroupService.getInstance();
        this.firebaseService = FirebaseService.getInstance();

        // Setup dialog with modern sizing
        setSize(580, 720);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initUI();
        loadUsers();
    }

    /**
     * Initialize the UI with modern design
     */
    private void initUI() {
        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Beautiful gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(248, 251, 255),
                    0, getHeight(), new Color(240, 248, 255)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Enhanced header section
        JPanel headerPanel = createModernHeader();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Enhanced form panel with modern card design
        JPanel formPanel = createModernFormPanel();
        
        // Enhanced user selection panel
        JPanel selectionPanel = createModernUserSelectionPanel();
        
        // Enhanced button panel
        JPanel buttonPanel = createModernButtonPanel();

        // Combine form and user selection into center panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(selectionPanel, BorderLayout.CENTER);

        // Add panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add validation listener
        groupNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                validateForm();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                validateForm();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                validateForm();
            }
        });

        // Add main panel to dialog
        setContentPane(mainPanel);
    }

    /**
     * Creates the modern header panel
     */
    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(30, 30, 20, 30));
        
        // Main title section
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        titleSection.setOpaque(false);
        
        // Enhanced icon with background circle
        JPanel iconContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient circle background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(0, 180, 85),
                    getWidth(), getHeight(), new Color(0, 150, 70)
                );
                g2d.setPaint(gradient);
                g2d.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
                
                // Subtle shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillOval(7, 7, getWidth() - 10, getHeight() - 10);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        iconContainer.setOpaque(false);
        iconContainer.setPreferredSize(new Dimension(70, 70));
        iconContainer.setLayout(new BorderLayout());
        
        JLabel iconLabel = new JLabel("👥", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Apple Color Emoji", Font.PLAIN, 32));
        iconLabel.setForeground(Color.WHITE);
        iconContainer.add(iconLabel, BorderLayout.CENTER);
        
        // Title and subtitle
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Créer un nouveau groupe");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(40, 40, 40));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Rassemblez vos contacts dans une conversation de groupe");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(120, 120, 120));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitleLabel);
        
        titleSection.add(iconContainer);
        titleSection.add(textPanel);
        
        headerPanel.add(titleSection, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    /**
     * Creates the modern form panel with enhanced styling
     */
    private JPanel createModernFormPanel() {
        JPanel formPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Card background with shadow
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // Subtle border
                g2d.setColor(new Color(230, 235, 245));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                
                // Light shadow
                g2d.setColor(new Color(0, 0, 0, 8));
                g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 16, 16);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        // Group name section
        JPanel nameSection = createFieldSection("Nom du groupe", "Entrez le nom de votre groupe");
        groupNameField = createModernTextField();
        nameSection.add(Box.createVerticalStrut(8));
        nameSection.add(groupNameField);
        
        // Description section  
        JPanel descSection = createFieldSection("Description", "Décrivez brièvement votre groupe (optionnel)");
        descriptionField = createModernTextArea();
        JScrollPane descScroll = new JScrollPane(descriptionField);
        descScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 1, true),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        descScroll.setPreferredSize(new Dimension(0, 80));
        descSection.add(Box.createVerticalStrut(8));
        descSection.add(descScroll);
        
        formPanel.add(nameSection);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(descSection);
        
        return formPanel;
    }
    
    /**
     * Creates a field section with label and description
     */
    private JPanel createFieldSection(String title, String description) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(new Color(120, 120, 120));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(3));
        section.add(descLabel);
        
        return section;
    }
    
    /**
     * Creates a modern styled text field
     */
    private JTextField createModernTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Border
                if (hasFocus()) {
                    g2d.setColor(new Color(0, 150, 70));
                    g2d.setStroke(new BasicStroke(2));
                } else {
                    g2d.setColor(new Color(220, 225, 235));
                    g2d.setStroke(new BasicStroke(1));
                }
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBackground(new Color(250, 252, 255));
        field.setForeground(new Color(40, 40, 40));
        field.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        field.setOpaque(false);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        return field;
    }
    
    /**
     * Creates a modern styled text area
     */
    private JTextArea createModernTextArea() {
        JTextArea area = new JTextArea(3, 20);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        area.setBackground(new Color(250, 252, 255));
        area.setForeground(new Color(40, 40, 40));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        
        return area;
    }
    
    /**
     * Creates the modern user selection panel
     */
    private JPanel createModernUserSelectionPanel() {
        JPanel selectionPanel = new JPanel(new BorderLayout(0, 15));
        selectionPanel.setOpaque(false);
        
        // Section header
        JPanel headerSection = new JPanel(new BorderLayout());
        headerSection.setOpaque(false);
        
        JLabel sectionTitle = new JLabel("Ajouter des membres");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(new Color(50, 50, 50));
        
        JLabel memberCount = new JLabel("0 membre(s) sélectionné(s)");
        memberCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        memberCount.setForeground(new Color(120, 120, 120));
        
        headerSection.add(sectionTitle, BorderLayout.WEST);
        headerSection.add(memberCount, BorderLayout.EAST);
        
        // User list with modern styling
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);
        userListPanel.setBorder(new EmptyBorder(15, 0, 15, 0));
        
        JScrollPane userScroll = new JScrollPane(userListPanel) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Modern scrollpane background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Border
                g2d.setColor(new Color(230, 235, 245));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        userScroll.setOpaque(false);
        userScroll.getViewport().setOpaque(false);
        userScroll.setPreferredSize(new Dimension(0, 200));
        
        // Custom scrollbar
        userScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(200, 200, 200);
                this.trackColor = new Color(245, 245, 245);
            }
        });
        
        selectionPanel.add(headerSection, BorderLayout.NORTH);
        selectionPanel.add(userScroll, BorderLayout.CENTER);
        
        return selectionPanel;
    }
    
    /**
     * Creates the modern button panel
     */
    private JPanel createModernButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(20, 30, 30, 30));

        JButton cancelButton = createModernButton("Annuler", new Color(120, 120, 120), Color.WHITE, false);
        cancelButton.addActionListener(e -> dispose());

        createButton = createModernButton("Créer le groupe", new Color(0, 150, 70), Color.WHITE, true);
        createButton.setEnabled(false);
        createButton.addActionListener(e -> createGroup());

        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);
        
        return buttonPanel;
    }
    
    /**
     * Creates a modern styled button
     */
    private JButton createModernButton(String text, Color bgColor, Color fgColor, boolean isPrimary) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color currentBgColor = bgColor;
                if (getModel().isPressed()) {
                    currentBgColor = bgColor.darker();
                } else if (getModel().isRollover() && isEnabled()) {
                    currentBgColor = isPrimary ? bgColor.brighter() : bgColor.darker();
                }
                
                // Button background with gradient
                if (isPrimary && isEnabled()) {
                    GradientPaint gradient = new GradientPaint(
                        0, 0, currentBgColor,
                        0, getHeight(), currentBgColor.darker()
                    );
                    g2d.setPaint(gradient);
                } else {
                    g2d.setColor(currentBgColor);
                }
                
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Shadow effect for primary button
                if (isPrimary && isEnabled()) {
                    g2d.setColor(new Color(0, 0, 0, 20));
                    g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 12, 12);
                }
                
                // Border
                g2d.setColor(bgColor.darker());
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setForeground(fgColor);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 42));
        
        return button;
    }

    /**
     * Load users from Firebase with improved error handling
     */
    private void loadUsers() {
        // Show loading message
        userListPanel.removeAll();
        JLabel loadingLabel = new JLabel("Loading users...");
        userListPanel.add(loadingLabel);
        userListPanel.revalidate();
        userListPanel.repaint();

        try {
            if (firebaseService == null) {
                System.err.println("ERROR: firebaseService is null");
                loadMockUsers();
                return;
            }

            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    System.out.println("DEBUG: onDataChange called, found " + dataSnapshot.getChildrenCount() + " users");
                    userListPanel.removeAll();

                    boolean usersAdded = false;
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        String username = userSnapshot.child("username").getValue(String.class);
                        if (username == null || username.isEmpty()) {
                            String nom = userSnapshot.child("nom").getValue(String.class);
                            String prenom = userSnapshot.child("prenom").getValue(String.class);
                            if (nom != null && prenom != null) {
                                username = prenom + " " + nom;
                            } else if (prenom != null) {
                                username = prenom;
                            } else if (nom != null) {
                                username = nom;
                            } else {
                                username = "User " + userId.substring(0, Math.min(userId.length(), 5));
                            }
                        }
                        // Show all users, including current user (with note)
                        if (userId != null) {
                            if (userId.equals(currentUserId)) {
                                addUserItem(userId, username + " (vous)");
                            } else {
                                addUserItem(userId, username);
                            }
                            usersAdded = true;
                        }
                    }
                    if (!usersAdded) {
                        // No users at all, show a message and a mock user
                        JLabel noUsersLabel = new JLabel("Aucun utilisateur trouvé. Utilisateur de test ajouté.");
                        noUsersLabel.setForeground(Color.RED);
                        noUsersLabel.setFont(new Font("Arial", Font.BOLD, 12));
                        noUsersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        userListPanel.add(noUsersLabel);
                        addUserItem("mockuser", "Utilisateur Test");
                    }
                    userListPanel.revalidate();
                    userListPanel.repaint();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error loading users: " + databaseError.getMessage());
                    loadMockUsers();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading users: " + e.getMessage());
            loadMockUsers();
        }
    }

    /**
     * Load mock users as a fallback
     */
    private void loadMockUsers() {
        userListPanel.removeAll();

        // Ajouter un label indiquant que ce sont des données de test
        JLabel mockLabel = new JLabel("⚠️ Using test data (Firebase connection issue)");
        mockLabel.setForeground(Color.RED);
        mockLabel.setFont(new Font("Arial", Font.BOLD, 12));
        mockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userListPanel.add(mockLabel);

        // Ajouter quelques utilisateurs fictifs
        String[] mockUsers = {
                "user1|John Doe",
                "user2|Jane Smith",
                "user3|Robert Johnson",
                "user4|Emily Davis",
                "user5|Michael Wilson",
                "user6|Sarah Brown"
        };

        for (String mockUser : mockUsers) {
            String[] parts = mockUser.split("\\|");
            String userId = parts[0];
            String username = parts[1];

            addUserItem(userId, username);
        }

        userListPanel.revalidate();
        userListPanel.repaint();
    }

    /**
     * Add a modern user item to the list
     */
    private void addUserItem(String userId, String username) {
        ModernUserItem userItem = new ModernUserItem(userId, username);
        userListPanel.add(userItem);
        userListPanel.add(Box.createVerticalStrut(8));
    }
    
    /**
     * Modern user item component with enhanced design
     */
    private class ModernUserItem extends JPanel {
        private final String userId;
        private final String username;
        private boolean isSelected = false;
        private JPanel selectionIndicator;
        
        public ModernUserItem(String userId, String username) {
            this.userId = userId;
            this.username = username;
            
            setLayout(new BorderLayout(15, 0));
            setPreferredSize(new Dimension(0, 65));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 245, 250)),
                new EmptyBorder(12, 20, 12, 20)
            ));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            createComponents();
            addInteraction();
        }
        
        private void createComponents() {
            // Selection indicator (left accent)
            selectionIndicator = new JPanel();
            selectionIndicator.setPreferredSize(new Dimension(4, 45));
            selectionIndicator.setBackground(new Color(0, 0, 0, 0)); // Transparent initially
            selectionIndicator.setOpaque(true);
            
            // Avatar with user initial
            JPanel avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Generate unique color based on userId
                    int hash = userId.hashCode();
                    int red = Math.abs((hash & 0xFF0000) >> 16);
                    int green = Math.abs((hash & 0x00FF00) >> 8);
                    int blue = Math.abs(hash & 0x0000FF);
                    
                    // Ensure readable colors
                    red = Math.max(Math.min(red + 100, 200), 80);
                    green = Math.max(Math.min(green + 100, 200), 80);
                    blue = Math.max(Math.min(blue + 100, 200), 80);
                    
                    Color avatarColor = new Color(red, green, blue);
                    
                    // Draw avatar circle
                    g2d.setColor(avatarColor);
                    g2d.fillOval(0, 0, getWidth(), getHeight());
                    
                    // Draw initial
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    String initial = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "?";
                    
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(initial)) / 2;
                    int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2d.drawString(initial, x, y);
                    
                    g2d.dispose();
                    super.paintComponent(g);
                }
            };
            avatarPanel.setPreferredSize(new Dimension(42, 42));
            avatarPanel.setOpaque(false);
            
            // User info panel
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
            
            JLabel nameLabel = new JLabel(username);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            nameLabel.setForeground(new Color(45, 45, 45));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JLabel statusLabel = new JLabel("Disponible");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabel.setForeground(new Color(120, 120, 120));
            statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(2));
            infoPanel.add(statusLabel);
            
            // Modern checkbox indicator
            JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            checkboxPanel.setOpaque(false);
            checkboxPanel.setPreferredSize(new Dimension(24, 24));
            
            JLabel checkboxLabel = new JLabel("○");
            checkboxLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            checkboxLabel.setForeground(new Color(180, 180, 180));
            checkboxPanel.add(checkboxLabel);
            
            // Layout components
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setOpaque(false);
            leftPanel.add(selectionIndicator, BorderLayout.WEST);
            leftPanel.add(avatarPanel, BorderLayout.CENTER);
            
            add(leftPanel, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
            add(checkboxPanel, BorderLayout.EAST);
        }
        
        private void addInteraction() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleSelection();
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(new Color(248, 251, 255));
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(Color.WHITE);
                    }
                }
            });
        }
        
        private void toggleSelection() {
            isSelected = !isSelected;
            updateAppearance();
            
            if (isSelected) {
                selectedUserIds.add(userId);
            } else {
                selectedUserIds.remove(userId);
            }
            validateForm();
        }
        
        private void updateAppearance() {
            // Update background
            if (isSelected) {
                setBackground(new Color(236, 248, 244));
                selectionIndicator.setBackground(new Color(0, 150, 70));
            } else {
                setBackground(Color.WHITE);
                selectionIndicator.setBackground(new Color(0, 0, 0, 0));
            }
            
            // Update checkbox
            JPanel checkboxPanel = (JPanel) getComponent(2);
            JLabel checkboxLabel = (JLabel) checkboxPanel.getComponent(0);
            
            if (isSelected) {
                checkboxLabel.setText("●");
                checkboxLabel.setForeground(new Color(0, 150, 70));
            } else {
                checkboxLabel.setText("○");
                checkboxLabel.setForeground(new Color(180, 180, 180));
            }
            
            repaint();
        }
    }

    /**
     * Validate the form
     */
    private void validateForm() {
        boolean valid = !groupNameField.getText().trim().isEmpty() && !selectedUserIds.isEmpty();
        createButton.setEnabled(valid);
    }

    /**
     * Create a group
     */
    private void createGroup() {
        String groupName = groupNameField.getText().trim();
        String description = descriptionField.getText().trim();

        if (groupName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a group name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedUserIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one member", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable create button
        createButton.setEnabled(false);
        createButton.setText("Creating...");

        // Add current user to members
        List<String> allUsers = new ArrayList<>(selectedUserIds);
        if (!allUsers.contains(currentUserId)) {
            allUsers.add(currentUserId);
        }

        String groupId = groupService.createGroup(groupName, description, currentUserId, allUsers, false);
        if (groupId != null) {
            JOptionPane.showMessageDialog(this, "Groupe créé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Échec de création du groupe. Un groupe avec ce nom existe peut-être déjà.", 
                "Erreur", JOptionPane.ERROR_MESSAGE);
            createButton.setEnabled(true);
            createButton.setText("Create Group");
        }
    }
}