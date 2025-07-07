package services;

import com.google.firebase.database.*;
import models.User;
import utils.PasswordUtils;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AuthService - Handles user authentication operations
 * Manages login, registration, and password reset functionality
 */
public class AuthService {
    private FirebaseService firebaseService;
    private static AuthService instance;

    // Private constructor for singleton pattern
    private AuthService() {
        try {
            this.firebaseService = FirebaseService.getInstance();
        } catch (Exception e) {
            System.err.println("Error initializing AuthService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Singleton pattern to get the instance
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Login user with email and password
     * @param email User's email
     * @param password User's plain text password
     * @return User object if login successful, null otherwise
     */
    public User login(String email, String password) {
        try {
            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            CountDownLatch latch = new CountDownLatch(1);
            final User[] result = {null};

            // Query users by email
            usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                        String storedPasswordHash = userSnapshot.child("passwordHash").getValue(String.class);

                                        // Check both "passwordHash" and "password" fields for compatibility
                                        if (storedPasswordHash == null) {
                                            storedPasswordHash = userSnapshot.child("password").getValue(String.class);
                                        }

                                        // Verify password using multiple methods
                                        if (storedPasswordHash != null && verifyPassword(password, storedPasswordHash)) {
                                            // Create User object
                                            User user = new User();
                                            user.setUserId(userSnapshot.getKey());
                                            user.setEmail(userSnapshot.child("email").getValue(String.class));
                                            user.setUsername(userSnapshot.child("username").getValue(String.class));
                                            user.setFirstName(userSnapshot.child("firstName").getValue(String.class));
                                            user.setLastName(userSnapshot.child("lastName").getValue(String.class));

                                            // Update last login timestamp
                                            userSnapshot.getRef().child("lastLogin").setValueAsync(ServerValue.TIMESTAMP);

                                            result[0] = user;
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing login data: " + e.getMessage());
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.err.println("Login query cancelled: " + databaseError.getMessage());
                            latch.countDown();
                        }
                    });

            // Wait for the query to complete
            latch.await(10, TimeUnit.SECONDS);
            return result[0];

        } catch (Exception e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Register a new user
     * @param user User object with user details
     * @param password Plain text password
     * @return true if registration successful, false otherwise
     */
    public boolean register(User user, String password) {
        try {
            // Check if email already exists
            if (emailExists(user.getEmail())) {
                System.err.println("Email already exists: " + user.getEmail());
                return false;
            }

            // Hash the password
            String hashedPassword = hashPassword(password);

            // Create user data map
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("username", user.getUsername());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("passwordHash", hashedPassword);
            userData.put("createdAt", ServerValue.TIMESTAMP);
            userData.put("lastLogin", ServerValue.TIMESTAMP);

            // Initialize user status
            Map<String, Object> status = new HashMap<>();
            status.put("online", false);
            status.put("lastSeen", ServerValue.TIMESTAMP);
            userData.put("status", status);

            // Create empty chats map
            userData.put("chats", new HashMap<String, Object>());

            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            String userId = usersRef.push().getKey();

            if (userId != null) {
                CountDownLatch latch = new CountDownLatch(1);
                final boolean[] success = {false};

                usersRef.child(userId).setValue(userData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error == null) {
                            user.setUserId(userId);
                            success[0] = true;
                            System.out.println("User registered successfully: " + user.getEmail());
                        } else {
                            System.err.println("Error registering user: " + error.getMessage());
                        }
                        latch.countDown();
                    }
                });

                latch.await(10, TimeUnit.SECONDS);
                return success[0];
            }

        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Send password reset email (placeholder - in real app would send email)
     * @param email User's email
     * @return true if reset email sent successfully
     */
    public boolean resetPassword(String email) {
        try {
            // Check if email exists
            if (!emailExists(email)) {
                System.err.println("Email not found: " + email);
                return false;
            }

            // For now, we'll just log the action
            System.out.println("Password reset requested for: " + email);

            // You could store a reset token in Firebase here
            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                    // Generate reset token (in real app, use secure random)
                                    String resetToken = "reset_" + System.currentTimeMillis();
                                    long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours

                                    Map<String, Object> resetData = new HashMap<>();
                                    resetData.put("token", resetToken);
                                    resetData.put("expires", expirationTime);
                                    resetData.put("used", false);

                                    userSnapshot.getRef().child("passwordReset").setValueAsync(resetData);
                                    success[0] = true;
                                    break;
                                }
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.err.println("Password reset query cancelled: " + databaseError.getMessage());
                            latch.countDown();
                        }
                    });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("Error during password reset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if email already exists in database
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    private boolean emailExists(String email) {
        try {
            DatabaseReference usersRef = firebaseService.getDatabase().getReference("users");
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] exists = {false};

            usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            exists[0] = dataSnapshot.exists();
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.err.println("Email check query cancelled: " + databaseError.getMessage());
                            latch.countDown();
                        }
                    });

            latch.await(5, TimeUnit.SECONDS);
            return exists[0];

        } catch (Exception e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hash password using SHA-256
     * @param password Plain text password
     * @return Hashed password
     */
    private String hashPassword(String password) {
        try {
            return PasswordUtils.hashPassword(password);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing password: " + e.getMessage());
            // Fallback to simple hash for compatibility
            return PasswordUtils.simpleHash(password);
        }
    }

    /**
     * Verify password with multiple hashing methods for compatibility
     */
    private boolean verifyPassword(String plainPassword, String storedHash) {
        try {
            // Try modern hash with salt
            if (PasswordUtils.verifyPassword(plainPassword, storedHash)) {
                return true;
            }
        } catch (Exception e) {
            // Continue to fallback methods
        }

        try {
            // Try simple SHA-256 hash
            String simpleHash = PasswordUtils.simpleHash(plainPassword);
            if (simpleHash.equals(storedHash)) {
                return true;
            }
        } catch (Exception e) {
            // Continue to fallback methods
        }

        // Try basic hashCode (last resort)
        return String.valueOf(plainPassword.hashCode()).equals(storedHash);
    }

    /**
     * Update user's online status
     * @param userId User ID
     * @param isOnline Online status
     */
    public void updateUserStatus(String userId, boolean isOnline) {
        try {
            DatabaseReference statusRef = firebaseService.getDatabase()
                    .getReference("users/" + userId + "/status");

            Map<String, Object> status = new HashMap<>();
            status.put("online", isOnline);
            status.put("lastSeen", ServerValue.TIMESTAMP);

            statusRef.setValueAsync(status);

        } catch (Exception e) {
            System.err.println("Error updating user status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get user by ID
     * @param userId User ID
     * @return User object if found, null otherwise
     */
    public User getUserById(String userId) {
        try {
            DatabaseReference userRef = firebaseService.getDatabase().getReference("users/" + userId);
            CountDownLatch latch = new CountDownLatch(1);
            final User[] result = {null};

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User user = new User();
                        user.setUserId(dataSnapshot.getKey());
                        user.setEmail(dataSnapshot.child("email").getValue(String.class));
                        user.setUsername(dataSnapshot.child("username").getValue(String.class));
                        user.setFirstName(dataSnapshot.child("firstName").getValue(String.class));
                        user.setLastName(dataSnapshot.child("lastName").getValue(String.class));

                        result[0] = user;
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Get user query cancelled: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            return result[0];

        } catch (Exception e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            return null;
        }
    }
}