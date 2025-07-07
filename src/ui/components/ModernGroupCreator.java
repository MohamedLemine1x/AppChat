package ui.components;

import services.FirebaseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import services.GroupService;

import com.google.firebase.database.*;

/**
 * A modern group creator dialog that allows selecting users from a list
 */
public class ModernGroupCreator extends JDialog {
    private final List<String> currentUserId;
    private final GroupService groupService;
    private final FirebaseService firebaseService;
    private final List<String> selectedUserIds = new ArrayList<>();
    private JTextField groupNameField;
    private JTextArea descriptionField;
    private JPanel userListPanel;
    private JButton createButton;

    /**
     * Constructor
     * @param parent Parent frame
     * @param currentUserId Current user ID
     * @param groupService Group service
     * @param firebaseService Firebase service
     */
    public ModernGroupCreator(Frame parent, String currentUserId, GroupService groupService, FirebaseService firebaseService) {
        super(parent, "Create a New Group", true);
        this.currentUserId = Collections.singletonList(currentUserId);
        this.groupService = groupService;
        this.firebaseService = firebaseService;

        setSize(500, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initUI();
        loadUsers();
    }

    /**
     * Initialize the UI
     */
    private void initUI() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Header
        JLabel headerLabel = new JLabel("Create a New Group");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerLabel.setForeground(new Color(60, 60, 60));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Form panel (center)
        JPanel formPanel = new JPanel(new BorderLayout(0, 15));
        formPanel.setOpaque(false);

        // Group info panel
        JPanel groupInfoPanel = new JPanel(new BorderLayout(0, 10));
        groupInfoPanel.setOpaque(false);

        // Group name
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setOpaque(false);
        JLabel nameLabel = new JLabel("Group Name");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        namePanel.add(nameLabel, BorderLayout.NORTH);

        groupNameField = new JTextField(20);
        groupNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        groupNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        namePanel.add(groupNameField, BorderLayout.CENTER);

        // Description
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setOpaque(false);
        JLabel descriptionLabel = new JLabel("Description (Optional)");
        descriptionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        descriptionPanel.add(descriptionLabel, BorderLayout.NORTH);

        descriptionField = new JTextArea(3, 20);
        descriptionField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        descriptionPanel.add(new JScrollPane(descriptionField), BorderLayout.CENTER);

        groupInfoPanel.add(namePanel, BorderLayout.NORTH);
        groupInfoPanel.add(descriptionPanel, BorderLayout.CENTER);

        // Members selection
        JPanel membersPanel = new JPanel(new BorderLayout());
        membersPanel.setOpaque(false);

        JLabel membersLabel = new JLabel("Select Group Members");
        membersLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        membersPanel.add(membersLabel, BorderLayout.NORTH);

        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        membersPanel.add(scrollPane, BorderLayout.CENTER);

        // Add all panels to form
        formPanel.add(groupInfoPanel, BorderLayout.NORTH);
        formPanel.add(membersPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.setOpaque(false);

        JButton cancelButton = createButton("Cancel", new Color(120, 120, 120), Color.WHITE);
        cancelButton.addActionListener(e -> dispose());

        createButton = createButton("Create Group", new Color(0, 150, 70), Color.WHITE);
        createButton.setEnabled(false); // Disabled until group name and members are selected
        createButton.addActionListener(e -> createGroup());

        buttonsPanel.add(cancelButton);
        buttonsPanel.add(createButton);

        // Add validation listeners
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

        // Add everything to main panel
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add to dialog
        add(mainPanel);
    }

    /**
     * Load users from Firebase
     */
    private void loadUsers() {
        try {
            // Show loading indicator
            userListPanel.removeAll();
            JLabel loadingLabel = new JLabel("Loading users...");
            loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            userListPanel.add(loadingLabel);
            userListPanel.revalidate();
            userListPanel.repaint();

            // Get users from Firebase
            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userListPanel.removeAll();

                    // Process users
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();

                        // Skip current user
                        if (userId != null && !userId.equals(currentUserId)) {
                            String username = userSnapshot.child("username").getValue(String.class);

                            // If username is not set, use nom + prenom
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
                                    username = "User " + userId.substring(0, 5);
                                }
                            }

                            String email = userSnapshot.child("email").getValue(String.class);
                            if (email == null || email.isEmpty()) {
                                email = "No email";
                            }

                            // Create user item
                            addUserItem(userId, username, email);
                        }
                    }

                    // If no users found
                    if (userListPanel.getComponentCount() == 0) {
                        JLabel noUsersLabel = new JLabel("No users found");
                        noUsersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        userListPanel.add(noUsersLabel);
                    }

                    userListPanel.revalidate();
                    userListPanel.repaint();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    userListPanel.removeAll();
                    JLabel errorLabel = new JLabel("Error loading users: " + databaseError.getMessage());
                    errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    userListPanel.add(errorLabel);
                    userListPanel.revalidate();
                    userListPanel.repaint();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();

            userListPanel.removeAll();
            JLabel errorLabel = new JLabel("Error loading users: " + e.getMessage());
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            userListPanel.add(errorLabel);
            userListPanel.revalidate();
            userListPanel.repaint();
        }
    }

    /**
     * Add a user item to the list
     * @param userId User ID
     * @param username Username
     * @param email Email
     */
    private void addUserItem(String userId, String username, String email) {
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(10, 5, 10, 5)
        ));
        userPanel.setBackground(Color.WHITE);
        userPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // User avatar
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Generate color based on user ID
                int hash = userId.hashCode();
                int red = (hash & 0xFF0000) >> 16;
                int green = (hash & 0x00FF00) >> 8;
                int blue = hash & 0x0000FF;

                // Ensure color is not too light or dark
                red = Math.max(Math.min(red, 200), 50);
                green = Math.max(Math.min(green, 200), 50);
                blue = Math.max(Math.min(blue, 200), 50);

                g2d.setColor(new Color(red, green, blue));
                g2d.fillOval(0, 0, getWidth(), getHeight());

                // Draw initial
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));

                String initial = username.substring(0, 1).toUpperCase();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

                g2d.drawString(initial, x, y);
                g2d.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 40);
            }
        };
        userPanel.add(avatarPanel, BorderLayout.WEST);

        // User info
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel emailLabel = new JLabel(email);
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        emailLabel.setForeground(new Color(100, 100, 100));

        infoPanel.add(nameLabel, BorderLayout.NORTH);
        infoPanel.add(emailLabel, BorderLayout.CENTER);
        userPanel.add(infoPanel, BorderLayout.CENTER);

        // Checkbox
        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        userPanel.add(checkBox, BorderLayout.EAST);

        // Click listener
        userPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                checkBox.setSelected(!checkBox.isSelected());
                if (checkBox.isSelected()) {
                    selectedUserIds.add(userId);
                    userPanel.setBackground(new Color(240, 255, 240));
                } else {
                    selectedUserIds.remove(userId);
                    userPanel.setBackground(Color.WHITE);
                }
                validateForm();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!checkBox.isSelected()) {
                    userPanel.setBackground(new Color(245, 245, 245));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!checkBox.isSelected()) {
                    userPanel.setBackground(Color.WHITE);
                }
            }
        });

        // Checkbox listener
        checkBox.addActionListener(e -> {
            if (checkBox.isSelected()) {
                selectedUserIds.add(userId);
                userPanel.setBackground(new Color(240, 255, 240));
            } else {
                selectedUserIds.remove(userId);
                userPanel.setBackground(Color.WHITE);
            }
            validateForm();
        });

        userListPanel.add(userPanel);
    }

    /**
     * Validate the form
     */
    private void validateForm() {
        boolean valid = !groupNameField.getText().trim().isEmpty() && !selectedUserIds.isEmpty();
        createButton.setEnabled(valid);
    }

    /**
     * Create a new group
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

        // Disable create button and show progress
        createButton.setEnabled(false);
        createButton.setText("Creating...");

        // Add current user to the list
        List<String> allUsers = new ArrayList<>(selectedUserIds);
        if (!allUsers.contains(currentUserId)) {
            allUsers.add(String.valueOf(currentUserId));
        }

        // Create group with 4 parameters (matches your actual method signature)
        String groupId = groupService.createGroup(groupName, description, currentUserId.get(0), allUsers, false);

        if (groupId != null) {
            JOptionPane.showMessageDialog(this, "Group created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create group. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            createButton.setEnabled(true);
            createButton.setText("Create Group");
        }
    }

    /**
     * Create a styled button
     * @param text Button text
     * @param bgColor Background color
     * @param fgColor Foreground color
     * @return Styled button
     */
    private JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(fgColor);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }
}