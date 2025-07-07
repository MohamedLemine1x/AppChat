package ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;

/**
 * Composant d'indicateur de chargement avec animation fluide
 * Améliore l'UX en montrant visuellement les opérations en cours
 */
public class LoadingIndicator extends JPanel {
    
    private Timer animationTimer;
    private int rotationAngle = 0;
    private boolean isLoading = false;
    private String loadingText = "Chargement...";
    private Color primaryColor = new Color(0, 150, 70);
    private Color backgroundColor = Color.WHITE;
    
    // Types d'indicateurs
    public enum IndicatorType {
        SPINNER,      // Cercle qui tourne
        DOTS,         // Points qui bougent
        PROGRESS_BAR, // Barre de progression
        PULSE         // Animation de pulsation
    }
    
    private IndicatorType type;
    private int dotIndex = 0;
    private float pulseAlpha = 0.3f;
    private boolean pulseIncreasing = true;
    
    public LoadingIndicator() {
        this(IndicatorType.SPINNER);
    }
    
    public LoadingIndicator(IndicatorType type) {
        this.type = type;
        setPreferredSize(new Dimension(100, 100));
        setOpaque(false);
        
        // Timer pour l'animation (60 FPS)
        animationTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAnimation();
                repaint();
            }
        });
    }
    
    private void updateAnimation() {
        switch (type) {
            case SPINNER:
                rotationAngle = (rotationAngle + 6) % 360;
                break;
            case DOTS:
                dotIndex = (dotIndex + 1) % 12;
                break;
            case PROGRESS_BAR:
                rotationAngle = (rotationAngle + 2) % 100;
                break;
            case PULSE:
                if (pulseIncreasing) {
                    pulseAlpha += 0.02f;
                    if (pulseAlpha >= 1.0f) {
                        pulseAlpha = 1.0f;
                        pulseIncreasing = false;
                    }
                } else {
                    pulseAlpha -= 0.02f;
                    if (pulseAlpha <= 0.3f) {
                        pulseAlpha = 0.3f;
                        pulseIncreasing = true;
                    }
                }
                break;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (!isLoading) return;
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        switch (type) {
            case SPINNER:
                drawSpinner(g2d, centerX, centerY);
                break;
            case DOTS:
                drawDots(g2d, centerX, centerY);
                break;
            case PROGRESS_BAR:
                drawProgressBar(g2d, centerX, centerY);
                break;
            case PULSE:
                drawPulse(g2d, centerX, centerY);
                break;
        }
        
        // Dessiner le texte si nécessaire
        if (loadingText != null && !loadingText.isEmpty()) {
            drawLoadingText(g2d, centerX, centerY);
        }
        
        g2d.dispose();
    }
    
    private void drawSpinner(Graphics2D g2d, int centerX, int centerY) {
        int radius = Math.min(getWidth(), getHeight()) / 4;
        
        for (int i = 0; i < 12; i++) {
            float alpha = 1.0f - (i * 0.08f);
            if (alpha < 0.1f) alpha = 0.1f;
            
            int angle = (rotationAngle + i * 30) % 360;
            double radians = Math.toRadians(angle);
            
            int x1 = centerX + (int) (Math.cos(radians) * radius * 0.6);
            int y1 = centerY + (int) (Math.sin(radians) * radius * 0.6);
            int x2 = centerX + (int) (Math.cos(radians) * radius);
            int y2 = centerY + (int) (Math.sin(radians) * radius);
            
            g2d.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), (int)(255 * alpha)));
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }
    
    private void drawDots(Graphics2D g2d, int centerX, int centerY) {
        int radius = Math.min(getWidth(), getHeight()) / 4;
        
        for (int i = 0; i < 8; i++) {
            float alpha = (i == dotIndex) ? 1.0f : 0.3f;
            
            double angle = i * Math.PI * 2 / 8;
            int x = centerX + (int) (Math.cos(angle) * radius);
            int y = centerY + (int) (Math.sin(angle) * radius);
            
            g2d.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), (int)(255 * alpha)));
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
    }
    
    private void drawProgressBar(Graphics2D g2d, int centerX, int centerY) {
        int width = getWidth() - 40;
        int height = 6;
        int x = 20;
        int y = centerY - height / 2;
        
        // Fond de la barre
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRoundRect(x, y, width, height, height, height);
        
        // Barre de progression
        int progressWidth = (int) (width * (rotationAngle / 100.0f));
        g2d.setColor(primaryColor);
        g2d.fillRoundRect(x, y, progressWidth, height, height, height);
    }
    
    private void drawPulse(Graphics2D g2d, int centerX, int centerY) {
        int radius = Math.min(getWidth(), getHeight()) / 4;
        
        g2d.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), (int)(255 * pulseAlpha)));
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // Cercle extérieur plus transparent
        int outerRadius = (int) (radius * (1 + pulseAlpha));
        g2d.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), (int)(100 * pulseAlpha)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(centerX - outerRadius, centerY - outerRadius, outerRadius * 2, outerRadius * 2);
    }
    
    private void drawLoadingText(Graphics2D g2d, int centerX, int centerY) {
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(loadingText);
        int textHeight = fm.getHeight();
        
        int textY = centerY + Math.min(getWidth(), getHeight()) / 3 + textHeight;
        
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString(loadingText, centerX - textWidth / 2, textY);
    }
    
    // Méthodes publiques
    public void startLoading() {
        startLoading(null);
    }
    
    public void startLoading(String text) {
        isLoading = true;
        if (text != null) {
            loadingText = text;
        }
        animationTimer.start();
        setVisible(true);
    }
    
    public void stopLoading() {
        isLoading = false;
        animationTimer.stop();
        setVisible(false);
    }
    
    public boolean isLoading() {
        return isLoading;
    }
    
    public void setLoadingText(String text) {
        this.loadingText = text;
        repaint();
    }
    
    public void setPrimaryColor(Color color) {
        this.primaryColor = color;
        repaint();
    }
    
    public void setIndicatorType(IndicatorType type) {
        this.type = type;
        repaint();
    }
    
    // Créer un overlay de chargement pour couvrir un composant
    public static JPanel createOverlay(JComponent parent, String text) {
        JPanel overlay = new JPanel(new BorderLayout());
        overlay.setOpaque(true);
        overlay.setBackground(new Color(255, 255, 255, 200));
        
        LoadingIndicator indicator = new LoadingIndicator(IndicatorType.SPINNER);
        indicator.setLoadingText(text);
        
        overlay.add(indicator, BorderLayout.CENTER);
        return overlay;
    }
    
    // Méthodes utilitaires pour afficher/masquer les overlays
    public static void showOverlay(JComponent parent, String text) {
        JPanel overlay = createOverlay(parent, text);
        
        if (parent.getLayout() instanceof BorderLayout) {
            parent.add(overlay, BorderLayout.CENTER);
        } else {
            parent.setLayout(new OverlayLayout(parent));
            parent.add(overlay);
        }
        
        LoadingIndicator indicator = findLoadingIndicator(overlay);
        if (indicator != null) {
            indicator.startLoading();
        }
        
        parent.revalidate();
        parent.repaint();
    }
    
    public static void hideOverlay(JComponent parent) {
        Component[] components = parent.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                LoadingIndicator indicator = findLoadingIndicator((JPanel) comp);
                if (indicator != null) {
                    indicator.stopLoading();
                    parent.remove(comp);
                    break;
                }
            }
        }
        parent.revalidate();
        parent.repaint();
    }
    
    private static LoadingIndicator findLoadingIndicator(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof LoadingIndicator) {
                return (LoadingIndicator) comp;
            } else if (comp instanceof Container) {
                LoadingIndicator found = findLoadingIndicator((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    @Override
    public void finalize() throws Throwable {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        super.finalize();
    }
}