import javax.swing.*;
import java.awt.*;
import ui.pages.UserSettingsPage;
import services.FirebaseService;

/**
 * Exemple d'utilisation de UserSettingsPage dans un onglet
 */
public class UserSettingsExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Test frame
                JFrame frame = new JFrame("Test UserSettingsPage");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(900, 700);
                frame.setLocationRelativeTo(null);
                
                // Create tabbed pane
                JTabbedPane tabbedPane = new JTabbedPane();
                
                // Add some example tabs
                tabbedPane.addTab("Accueil", new JLabel("Contenu de l'accueil", SwingConstants.CENTER));
                tabbedPane.addTab("Messages", new JLabel("Contenu des messages", SwingConstants.CENTER));
                
                // Create UserSettingsPage
                String testUserId = "test_user_123";
                FirebaseService firebaseService = FirebaseService.getInstance();
                UserSettingsPage settingsPage = new UserSettingsPage(testUserId, firebaseService);
                
                // Add settings page to tabbed pane
                settingsPage.addToTabbedPane(tabbedPane, "Paramètres");
                
                frame.add(tabbedPane);
                frame.setVisible(true);
                
                System.out.println("✅ UserSettingsPage configurée avec succès !");
                System.out.println("📁 Icône 'À propos' : pictures/about.png");
                System.out.println("❌ Bouton 'Fermer' : Ferme l'onglet ou la fenêtre avec animation");
                
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du test : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}