import com.vibeapp.VibeApp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * SplashScreen shown when the application starts
 */
public class SplashScreen extends JPanel {
    // Colors
    private final Color EMSI_GREEN = new Color(0, 150, 70);
    private final Color EMSI_GREEN_LIGHT = new Color(0, 180, 85);
    private final Color EMSI_GREEN_DARK = new Color(0, 120, 55);
    private final Color EMSI_GRAY = new Color(90, 90, 90);
    private final Color EMSI_GRAY_LIGHT = new Color(120, 120, 120);
    private final Color EMSI_RED = new Color(217, 83, 30);

    // Progress animation
    private float progress = 0.0f;
    private Timer progressTimer;
    private BufferedImage logoImage = null;

    public SplashScreen() {
        setLayout(new BorderLayout());

        // Load the logo
        loadLogo();

        // Start the progress animation
        startProgressAnimation();
    }

    private void loadLogo() {
        try {
            File logoFile = new File("pictures/logoVibeApp.png");
            if (logoFile.exists()) {
                // Load the original image with transparency preservation
                BufferedImage originalImage = ImageIO.read(logoFile);

                // Calculate size for high resolution
                int targetWidth = 250; // Increased for more details
                double scaleFactor = (double) targetWidth / originalImage.getWidth();
                int targetHeight = (int)(originalImage.getHeight() * scaleFactor);

                // Create image with alpha channel for perfect transparency
                logoImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dResized = logoImage.createGraphics();

                // Configure for VERY high quality rendering
                g2dResized.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2dResized.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2dResized.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2dResized.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

                // Make background completely transparent
                g2dResized.setComposite(AlphaComposite.Clear);
                g2dResized.fillRect(0, 0, targetWidth, targetHeight);

                // Restore composition mode for drawing
                g2dResized.setComposite(AlphaComposite.SrcOver);

                // Draw the resized image with transparency
                g2dResized.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                g2dResized.dispose();
            }
        } catch (Exception e) {
            System.err.println("Error loading and resizing logo: " + e.getMessage());
        }
    }

    private void startProgressAnimation() {
        progressTimer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progress += 0.05f;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                    progressTimer.stop();

                    // Transition to login page after splash completes
                    SwingUtilities.invokeLater(() -> {
                        VibeApp.getInstance().showPage("login");
                    });
                }
                repaint();
            }
        });
        progressTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Configure high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        // Create a gradient background
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(250, 250, 250),
                getWidth(), getHeight(), new Color(230, 240, 235)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw some decorative elements
        g2d.setColor(new Color(0, 150, 70, 10));
        g2d.fillOval(-100, -100, 400, 400);
        g2d.fillOval(getWidth() - 200, getHeight() - 200, 400, 400);

        // Draw the com.vibeapp.VibeApp logo with fade-in animation
        if (logoImage != null) {
            // Calculate position to center the logo
            int imgWidth = logoImage.getWidth();
            int imgHeight = logoImage.getHeight();
            int x = (getWidth() - imgWidth) / 2;
            int y = (getHeight() - imgHeight) / 2 - 50; // Position it a bit higher

            // Apply progressive opacity
            float alpha = Math.min(1.0f, progress * 2); // Appears faster
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Draw the image with transparency
            g2d.drawImage(logoImage, x, y, null);
        } else {
            // Fallback to text if logo is not found
            g2d.setColor(new Color(0, 150, 70, (int)(progress * 255)));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "com.vibeapp.VibeApp";
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text, (getWidth() - textWidth) / 2, getHeight() / 2 - 30);

            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            fm = g2d.getFontMetrics();
            String subtitle = "Communication Platform";
            textWidth = fm.stringWidth(subtitle);
            g2d.drawString(subtitle, (getWidth() - textWidth) / 2, getHeight() / 2 + 10);
        }

        // Draw application version
        g2d.setColor(new Color(EMSI_GRAY_LIGHT.getRed(), EMSI_GRAY_LIGHT.getGreen(),
                EMSI_GRAY_LIGHT.getBlue(), (int)(progress * 255)));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String version = "Version " + VibeApp.APP_VERSION;
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(version);
        g2d.drawString(version, (getWidth() - textWidth) / 2, getHeight() / 2 + 40);

        // Draw the progress bar
        int barWidth = 200;
        int barHeight = 6;
        int barX = (getWidth() - barWidth) / 2;
        int barY = getHeight() / 2 + 80;

        // Background
        g2d.setColor(new Color(220, 220, 220, (int)(progress * 255)));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, barHeight, barHeight);

        // Progress (green with transparency)
        g2d.setColor(new Color(0, 150, 70, (int)(progress * 255)));
        g2d.fillRoundRect(barX, barY, (int)(barWidth * progress), barHeight, barHeight, barHeight);

        // Loading text
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String loadingText = "Chargement...";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(loadingText);
        g2d.drawString(loadingText, (getWidth() - textWidth) / 2, barY + 25);

        g2d.dispose();
    }
}