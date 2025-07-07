package ui.components;

import services.FirebaseService;
import services.GroupService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.*;

/**
 * A simple group creator dialog with user selection
 */
public class SimplifiedGroupCreator extends JDialog {
    private final String currentUserId;
    private GroupService groupService;
    private final List<String> selectedUserIds = new ArrayList<>();
    private JTextField groupNameField;
    private JTextArea descriptionField;
    private JPanel userListPanel;
    private JButton createButton;

    /**
     * Constructor
     * @param parent Parent frame
     * @param currentUserId Current user ID
     */
    public SimplifiedGroupCreator(Frame parent, String currentUserId) {
        super(parent, "Create a New Group", true);
        this.currentUserId = currentUserId;

        this.groupService = GroupService.getInstance();

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
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Group info panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 10));

        // Group name
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(new JLabel("Group Name:"), BorderLayout.WEST);
        groupNameField = new JTextField(20);
        namePanel.add(groupNameField, BorderLayout.CENTER);

        // Description
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(new JLabel("Description:"), BorderLayout.WEST);
        descriptionField = new JTextArea(3, 20);
        descriptionField.setLineWrap(true);
        descPanel.add(new JScrollPane(descriptionField), BorderLayout.CENTER);

        infoPanel.add(namePanel);
        infoPanel.add(descPanel);

        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // User selection panel
        JPanel selectionPanel = new JPanel(new BorderLayout());
        selectionPanel.add(new JLabel("Select Users:"), BorderLayout.NORTH);

        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setPreferredSize(new Dimension(450, 300));
        selectionPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(selectionPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        createButton = new JButton("Create Group");
        createButton.setEnabled(false);
        createButton.addActionListener(e -> createGroup());

        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add listener for validation
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

        add(mainPanel);
    }

    /**
     * Load users from Firebase
     */
    private void loadUsers() {
        // Show loading message
        userListPanel.removeAll();
        userListPanel.add(new JLabel("Loading users..."));
        userListPanel.revalidate();
        userListPanel.repaint();

        // Get Firebase instance
        FirebaseService firebaseService;
        try {
            firebaseService = FirebaseService.getInstance();
        } catch (IOException e) {
            userListPanel.removeAll();
            userListPanel.add(new JLabel("Error connecting to Firebase: " + e.getMessage()));
            userListPanel.revalidate();
            userListPanel.repaint();
            return;
        }

        DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userListPanel.removeAll();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();

                    // Skip current user
                    if (userId != null && !userId.equals(currentUserId)) {
                        // Get user info
                        String username = userSnapshot.child("username").getValue(String.class);
                        if (username == null || username.isEmpty()) {
                            String nom = userSnapshot.child("nom").getValue(String.class);
                            String prenom = userSnapshot.child("prenom").getValue(String.class);

                            if (nom != null && prenom != null) {
                                username = prenom + " " + nom;
                            } else {
                                username = "User " + userId.substring(0, 5);
                            }
                        }

                        // Add user to list
                        JPanel userPanel = new JPanel(new BorderLayout());
                        userPanel.setBorder(BorderFactory.createEtchedBorder());

                        JCheckBox checkBox = new JCheckBox(username);
                        checkBox.addActionListener(e -> {
                            if (checkBox.isSelected()) {
                                selectedUserIds.add(userId);
                            } else {
                                selectedUserIds.remove(userId);
                            }
                            validateForm();
                        });

                        userPanel.add(checkBox, BorderLayout.CENTER);
                        userListPanel.add(userPanel);
                    }
                }

                if (userListPanel.getComponentCount() == 0) {
                    userListPanel.add(new JLabel("No users found"));
                }

                userListPanel.revalidate();
                userListPanel.repaint();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                userListPanel.removeAll();
                userListPanel.add(new JLabel("Error loading users: " + databaseError.getMessage()));
                userListPanel.revalidate();
                userListPanel.repaint();
            }
        });
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

        // Disable button while creating
        createButton.setEnabled(false);
        createButton.setText("Creating...");

        // Add current user to list
        List<String> allUsers = new ArrayList<>(selectedUserIds);
        if (!allUsers.contains(currentUserId)) {
            allUsers.add(currentUserId);
        }

        // Create group with the new signature (5 parameters)
        String groupId = groupService.createGroup(groupName, description, currentUserId, allUsers, false);

        if (groupId != null) {
            JOptionPane.showMessageDialog(this, "Group created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create group", "Error", JOptionPane.ERROR_MESSAGE);
            createButton.setEnabled(true);
            createButton.setText("Create Group");
        }
    }
}