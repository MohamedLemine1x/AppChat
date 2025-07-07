package ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.SwingWorker;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.geom.Ellipse2D;

/**
 * ProfileImageEditor - Component for editing profile pictures
 * Allows uploading, cropping, and removing profile images
 */
public class ProfileImageEditor extends JPanel {

    // Colors
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_RED = new Color(217, 83, 30);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    // Components
    private AvatarPanel avatarDisplay;
    private ModernButton uploadButton;
    private ModernButton removeButton;
    private JLabel statusLabel;

    // State
    private BufferedImage currentImage;
    private String currentImagePath;
    private ProfileImageChangeListener changeListener;

    // Default avatar settings
    private String userInitial = "U";
    private Color avatarColor = EMSI_GREEN;

    /**
     * Interface for profile image change callbacks
     */
    public interface ProfileImageChangeListener {
        void onImageChanged(String imagePath, BufferedImage image);
        void onImageRemoved();
    }

    /**
     * Creates a new profile image editor
     */
    public ProfileImageEditor() {
        this(80);
    }

    /**
     * Creates a new profile image editor with specified avatar size
     * @param avatarSize The size of the avatar display
     */
    public ProfileImageEditor(int avatarSize) {
        initializeComponent(avatarSize);
    }

    /**
     * Initializes the component
     */
    private void initializeComponent(int avatarSize) {
        setLayout(new BorderLayout(15, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 0, 10, 0));

        // Create avatar display
        avatarDisplay = new AvatarPanel(avatarSize, avatarColor);
        avatarDisplay.setInitial(userInitial);

        // Create control panel
        JPanel controlPanel = createControlPanel();

        // Status label
        statusLabel = new JLabel("Aucune image sélectionnée");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(EMSI_GRAY);

        // Layout
        add(avatarDisplay, BorderLayout.WEST);
        add(controlPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Creates the control panel with buttons
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Upload button
        uploadButton = createStyledButton("Changer la photo", EMSI_GREEN);
        uploadButton.addActionListener(e -> openImageChooser());

        // Remove button
        removeButton = createStyledButton("Supprimer", EMSI_RED);
        removeButton.addActionListener(e -> removeImage());
        removeButton.setEnabled(false);

        // Add buttons with spacing
        panel.add(uploadButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(removeButton);

        return panel;
    }

    /**
     * Creates a styled button
     */
    private ModernButton createStyledButton(String text, Color color) {
        ModernButton button;
        if (color.equals(EMSI_GREEN)) {
            button = ModernButton.createPrimary(text);
        } else if (color.equals(EMSI_RED)) {
            button = ModernButton.createDanger(text);
        } else {
            button = ModernButton.createSecondary(text);
        }
        
        button.setButtonSize(ModernButton.ButtonSize.SMALL);
        button.setMaximumSize(new Dimension(150, 35));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        return button;
    }

    /**
     * Opens the image file chooser
     */
    private void openImageChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sélectionner une image de profil");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Images (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImage(selectedFile);
        }
    }

    /**
     * Loads an image from file
     */
    private void loadImage(File file) {
        // Perform image loading in background thread to avoid UI freezing
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    throw new Exception("Format d'image non supporté");
                }

                // Validate image size
                if (image.getWidth() < 50 || image.getHeight() < 50) {
                    throw new Exception("L'image doit faire au moins 50x50 pixels");
                }

                // Check file size (limit to 5MB)
                long fileSize = file.length();
                if (fileSize > 5 * 1024 * 1024) {
                    throw new Exception("L'image ne doit pas dépasser 5MB");
                }

                return image;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    
                    // Show crop dialog if image is large (on UI thread)
                    if (image.getWidth() > 400 || image.getHeight() > 400) {
                        image = showCropDialog(image);
                        if (image == null) {
                            return; // User cancelled
                        }
                    }

                    // Set the new image
                    setImage(file.getAbsolutePath(), image);

                } catch (Exception e) {
                    showError("Erreur lors du chargement de l'image: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Shows a simple crop dialog (basic implementation)
     */
    private BufferedImage showCropDialog(BufferedImage originalImage) {
        // For now, just resize the image to a reasonable size
        // TODO: Implement a proper crop dialog with selection rectangle
        int size = Math.min(originalImage.getWidth(), originalImage.getHeight());
        int x = (originalImage.getWidth() - size) / 2;
        int y = (originalImage.getHeight() - size) / 2;

        BufferedImage croppedImage = originalImage.getSubimage(x, y, size, size);

        // Resize to 200x200 for profile picture
        BufferedImage resizedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(croppedImage, 0, 0, 200, 200, null);
        g2.dispose();

        return resizedImage;
    }

    /**
     * Sets the current image
     */
    private void setImage(String imagePath, BufferedImage image) {
        this.currentImage = image;
        this.currentImagePath = imagePath;

        // Update avatar display
        avatarDisplay.setAvatarImage(image);

        // Update UI state
        removeButton.setEnabled(true);
        statusLabel.setText("Image chargée: " + new File(imagePath).getName());
        statusLabel.setForeground(EMSI_GREEN);

        // Notify listener
        if (changeListener != null) {
            changeListener.onImageChanged(imagePath, image);
        }

        repaint();
    }

    /**
     * Removes the current image
     */
    private void removeImage() {
        int result = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir supprimer votre photo de profil?",
                "Confirmer la suppression",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            currentImage = null;
            currentImagePath = null;

            // Reset avatar display
            avatarDisplay.clearImage();
            avatarDisplay.setInitial(userInitial);

            // Update UI state
            removeButton.setEnabled(false);
            statusLabel.setText("Aucune image sélectionnée");
            statusLabel.setForeground(EMSI_GRAY);

            // Notify listener
            if (changeListener != null) {
                changeListener.onImageRemoved();
            }

            repaint();
        }
    }

    /**
     * Shows an error message
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Sets the user initial for the default avatar
     */
    public void setUserInitial(String initial) {
        this.userInitial = initial != null && !initial.isEmpty() ? initial.substring(0, 1).toUpperCase() : "U";
        if (currentImage == null) {
            avatarDisplay.setInitial(this.userInitial);
        }
    }

    /**
     * Sets the avatar color
     */
    public void setAvatarColor(Color color) {
        this.avatarColor = color != null ? color : EMSI_GREEN;
        avatarDisplay.setBackground(this.avatarColor);
    }

    /**
     * Sets the current image from URL or path
     */
    public void setImagePath(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        setImage(imagePath, image);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading image from path: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the current image path
     */
    public String getImagePath() {
        return currentImagePath;
    }

    /**
     * Gets the current image
     */
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * Sets the change listener
     */
    public void setChangeListener(ProfileImageChangeListener listener) {
        this.changeListener = listener;
    }

    /**
     * Checks if an image is currently loaded
     */
    public boolean hasImage() {
        return currentImage != null;
    }
}