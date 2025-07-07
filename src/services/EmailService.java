package services;

/**
 * Email service for sending verification codes and reset links
 * SIMPLIFIED VERSION for educational purposes - no actual emails sent
 */
public class EmailService {
    // Always in development mode for educational purpose
    private static final boolean DEVELOPMENT_MODE = true;

    /**
     * Simulate sending a verification code email (no actual email sent)
     * @param recipientEmail Recipient's email address
     * @param verificationCode The 6-digit verification code
     * @return true always (simulating success)
     */
    public static boolean sendVerificationCode(String recipientEmail, String verificationCode) {
        // Print verification details to console instead of sending an email
        System.out.println("\n========== SIMULATED EMAIL ==========");
        System.out.println("TO: " + recipientEmail);
        System.out.println("SUBJECT: VibeApp - Code de vérification");
        System.out.println("---------------------------------------");
        System.out.println("Bonjour,");
        System.out.println("Votre code de vérification est: " + verificationCode);
        System.out.println("Ce code expirera dans 15 minutes.");
        System.out.println("========== END OF EMAIL ==========\n");

        // Always return true to simulate successful email sending
        return true;
    }
}