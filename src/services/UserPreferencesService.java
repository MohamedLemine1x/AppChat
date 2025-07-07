package services;

import models.UserPreferences;
import com.google.firebase.database.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * UserPreferencesService - Service for managing user preferences in Firebase
 * Handles CRUD operations for user settings and profile data
 */
public class UserPreferencesService {

    // Firebase references
    private final FirebaseService firebaseService;
    private final DatabaseReference database;

    // Mock class for Firebase Storage (for now)
    private static class MockFirebaseStorage {
        private static MockFirebaseStorage instance = new MockFirebaseStorage();

        public static MockFirebaseStorage getInstance() {
            return instance;
        }

        public MockStorageReference getReference() {
            return new MockStorageReference();
        }
    }

    private static class MockStorageReference {
        public MockStorageReference child(String path) {
            return new MockStorageReference();
        }

        public MockUploadTask putStream(FileInputStream stream) {
            return new MockUploadTask();
        }

        public MockDownloadUrlTask getDownloadUrl() {
            return new MockDownloadUrlTask();
        }
    }

    private String displayName;
    private String bio;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    private static class MockUploadTask {
        public MockUploadTask addOnProgressListener(OnProgressListener listener) {
            // Mock progress updates
            new Thread(() -> {
                try {
                    for (int i = 0; i <= 100; i += 20) {
                        final int progress = i;
                        Thread.sleep(500);
                        listener.onProgress(new MockTaskSnapshot(progress));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            return this;
        }

        public MockUploadTask addOnSuccessListener(OnSuccessListener<MockTaskSnapshot> listener) {
            // Simulate success after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    listener.onSuccess(new MockTaskSnapshot(100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            return this;
        }

        public MockUploadTask addOnFailureListener(OnFailureListener listener) {
            // We won't trigger failure in this mock
            return this;
        }
    }

    private static class MockTaskSnapshot {
        private final int progress;

        public MockTaskSnapshot(int progress) {
            this.progress = progress;
        }

        public long getBytesTransferred() {
            return progress;
        }

        public long getTotalByteCount() {
            return 100;
        }
    }

    private static class MockDownloadUrlTask {
        public MockDownloadUrlTask addOnSuccessListener(OnSuccessListener<MockUri> listener) {
            // Simulate success with a fake URL
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    listener.onSuccess(new MockUri("https://example.com/fake-image-url"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            return this;
        }

        public MockDownloadUrlTask addOnFailureListener(OnFailureListener listener) {
            // We won't trigger failure in this mock
            return this;
        }
    }

    private static class MockUri {
        private final String url;

        public MockUri(String url) {
            this.url = url;
        }

        public String toString() {
            return url;
        }
    }

    // Callback interfaces for the mock implementation
    private interface OnProgressListener {
        void onProgress(MockTaskSnapshot snapshot);
    }

    private interface OnSuccessListener<T> {
        void onSuccess(T result);
    }

    private interface OnFailureListener {
        void onFailure(Exception e);
    }

    // Get mock Firebase storage
    private final MockFirebaseStorage storage = MockFirebaseStorage.getInstance();

    // Database paths
    private static final String PREFERENCES_PATH = "userPreferences";
    private static final String PROFILE_IMAGES_PATH = "profileImages";
    private static final String STORAGE_PROFILE_IMAGES = "profile-images";

    // Cache for quick access
    private Map<String, UserPreferences> preferencesCache = new HashMap<>();
    private long cacheExpiry = 5 * 60 * 1000; // 5 minutes
    private Map<String, Long> cacheTimestamps = new HashMap<>();

    /**
     * Callback interface for async operations
     */
    public interface PreferencesCallback {
        void onSuccess(UserPreferences preferences);
        void onError(String error);
    }

    /**
     * Callback interface for upload operations
     */
    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
        void onProgress(double progress);
    }

    /**
     * Constructor
     * @param firebaseService The Firebase service instance
     */
    public UserPreferencesService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
        this.database = firebaseService.getDatabase().getReference();
    }

    // ===== LOAD OPERATIONS =====

    /**
     * Loads user preferences from Firebase
     * @param userId The user ID
     * @param callback The callback for the result
     */
    public void loadUserPreferences(String userId, PreferencesCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID utilisateur invalide");
            return;
        }

        // Check cache first
        UserPreferences cachedPrefs = getCachedPreferences(userId);
        if (cachedPrefs != null) {
            callback.onSuccess(cachedPrefs);
            return;
        }

        // Load from Firebase
        DatabaseReference prefsRef = database.child(PREFERENCES_PATH).child(userId);

        prefsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    UserPreferences preferences;

                    if (dataSnapshot.exists()) {
                        // Convert Firebase data to UserPreferences
                        Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                        preferences = UserPreferences.fromMap(data);
                    } else {
                        // Create default preferences if none exist
                        preferences = new UserPreferences();
                        preferences.setUserId(userId);

                        // Save default preferences to Firebase
                        saveUserPreferences(preferences, new PreferencesCallback() {
                            @Override
                            public void onSuccess(UserPreferences prefs) {
                                // Default preferences saved successfully
                            }

                            @Override
                            public void onError(String error) {
                                System.err.println("Error saving default preferences: " + error);
                            }
                        });
                    }

                    // Cache the preferences
                    cachePreferences(userId, preferences);

                    // Return on UI thread
                    SwingUtilities.invokeLater(() -> callback.onSuccess(preferences));

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            callback.onError("Erreur lors du chargement des préférences: " + e.getMessage())
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                SwingUtilities.invokeLater(() ->
                        callback.onError("Erreur Firebase: " + databaseError.getMessage())
                );
            }
        });
    }

    /**
     * Loads user preferences synchronously (blocking)
     * @param userId The user ID
     * @param timeoutSeconds Timeout in seconds
     * @return The user preferences or null if failed
     */
    /**
     * @deprecated Use loadUserPreferences(String, PreferencesCallback) instead
     * This method blocks the calling thread and should not be used on the UI thread
     */
    @Deprecated
    public UserPreferences loadUserPreferencesSync(String userId, int timeoutSeconds) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        // Check cache first
        UserPreferences cachedPrefs = getCachedPreferences(userId);
        if (cachedPrefs != null) {
            return cachedPrefs;
        }

        // ATTENTION: Cette méthode bloque le thread appelant
        // Utilisez loadUserPreferences(String, PreferencesCallback) à la place
        final UserPreferences[] result = new UserPreferences[1];
        final CountDownLatch latch = new CountDownLatch(1);

        loadUserPreferences(userId, new PreferencesCallback() {
            @Override
            public void onSuccess(UserPreferences preferences) {
                result[0] = preferences;
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                System.err.println("Error loading preferences: " + error);
                latch.countDown();
            }
        });

        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result[0];
    }

    /**
     * Charge les préférences utilisateur de manière asynchrone (recommandé)
     * @param userId ID de l'utilisateur
     * @param callback Callback pour recevoir le résultat
     */
    public void loadUserPreferencesAsync(String userId, PreferencesCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("User ID is null or empty");
            }
            return;
        }

        // Check cache first
        UserPreferences cachedPrefs = getCachedPreferences(userId);
        if (cachedPrefs != null && callback != null) {
            SwingUtilities.invokeLater(() -> callback.onSuccess(cachedPrefs));
            return;
        }

        // Load asynchronously
        loadUserPreferences(userId, callback);
    }

    // ===== SAVE OPERATIONS =====

    /**
     * Saves user preferences to Firebase
     * @param preferences The preferences to save
     * @param callback The callback for the result
     */
    public void saveUserPreferences(UserPreferences preferences, PreferencesCallback callback) {
        if (preferences == null || !preferences.isValid()) {
            callback.onError("Préférences invalides");
            return;
        }

        String userId = preferences.getUserId();
        DatabaseReference prefsRef = database.child(PREFERENCES_PATH).child(userId);

        // Convert to Firebase format
        Map<String, Object> data = preferences.toMap();

        prefsRef.updateChildren(data, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    SwingUtilities.invokeLater(() ->
                            callback.onError("Erreur lors de la sauvegarde: " + error.getMessage())
                    );
                } else {
                    // Update cache
                    cachePreferences(userId, preferences);

                    SwingUtilities.invokeLater(() -> callback.onSuccess(preferences));
                }
            }
        });
    }

    /**
     * Updates specific preference fields
     * @param userId The user ID
     * @param updates Map of field updates
     * @param callback The callback for the result
     */
    public void updatePreferences(String userId, Map<String, Object> updates, PreferencesCallback callback) {
        if (userId == null || userId.trim().isEmpty() || updates == null || updates.isEmpty()) {
            callback.onError("Paramètres de mise à jour invalides");
            return;
        }

        DatabaseReference prefsRef = database.child(PREFERENCES_PATH).child(userId);

        // Add timestamp
        updates.put("lastUpdated", ServerValue.TIMESTAMP);

        prefsRef.updateChildren(updates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    SwingUtilities.invokeLater(() ->
                            callback.onError("Erreur lors de la mise à jour: " + error.getMessage())
                    );
                } else {
                    // Invalidate cache to force reload
                    invalidateCache(userId);

                    // Reload and return updated preferences
                    loadUserPreferences(userId, callback);
                }
            }
        });
    }

    // ===== PROFILE IMAGE OPERATIONS =====

    /**
     * Uploads a profile image to Firebase Storage
     * @param userId The user ID
     * @param imageFile The image file to upload
     * @param callback The upload callback
     */
    public void uploadProfileImage(String userId, File imageFile, UploadCallback callback) {
        if (userId == null || userId.trim().isEmpty() || imageFile == null || !imageFile.exists()) {
            callback.onError("Paramètres d'upload invalides");
            return;
        }

        // Validate file type
        String fileName = imageFile.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") &&
                !fileName.endsWith(".png") && !fileName.endsWith(".gif")) {
            callback.onError("Format d'image non supporté. Utilisez JPG, PNG ou GIF.");
            return;
        }

        // Validate file size (max 5MB)
        long fileSize = imageFile.length();
        if (fileSize > 5 * 1024 * 1024) {
            callback.onError("L'image ne doit pas dépasser 5MB");
            return;
        }

        // Create storage reference
        String imagePath = STORAGE_PROFILE_IMAGES + "/" + userId + "/" + System.currentTimeMillis() + "_" + imageFile.getName();
        MockStorageReference imageRef = storage.getReference().child(imagePath);

        try {
            // Start upload
            FileInputStream fileStream = new FileInputStream(imageFile);
            MockUploadTask uploadTask = imageRef.putStream(fileStream);

            // Monitor progress
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                SwingUtilities.invokeLater(() -> callback.onProgress(progress));
            });

            // Handle completion
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    // Update user preferences with new image URL
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profileImageUrl", imageUrl);

                    updatePreferences(userId, updates, new PreferencesCallback() {
                        @Override
                        public void onSuccess(UserPreferences preferences) {
                            SwingUtilities.invokeLater(() -> callback.onSuccess(imageUrl));
                        }

                        @Override
                        public void onError(String error) {
                            SwingUtilities.invokeLater(() ->
                                    callback.onError("Image uploadée mais erreur de sauvegarde: " + error)
                            );
                        }
                    });

                }).addOnFailureListener(e -> {
                    SwingUtilities.invokeLater(() ->
                            callback.onError("Erreur lors de la récupération de l'URL: " + e.getMessage())
                    );
                });

            }).addOnFailureListener(e -> {
                SwingUtilities.invokeLater(() ->
                        callback.onError("Erreur lors de l'upload: " + e.getMessage())
                );
            });

        } catch (Exception e) {
            callback.onError("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    /**
     * Removes the profile image for a user
     * @param userId The user ID
     * @param callback The callback for the result
     */
    public void removeProfileImage(String userId, PreferencesCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID utilisateur invalide");
            return;
        }

        // Update preferences to remove image URL
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", "");

        updatePreferences(userId, updates, callback);

        // TODO: Also delete the actual image file from Storage
        // This requires storing the image path reference
    }

    // ===== BULK OPERATIONS =====

    /**
     * Loads preferences for multiple users
     * @param userIds List of user IDs
     * @param callback Callback with Map of userId -> UserPreferences
     */
    public void loadMultipleUserPreferences(String[] userIds, MultiplePreferencesCallback callback) {
        if (userIds == null || userIds.length == 0) {
            callback.onError("Liste d'utilisateurs vide");
            return;
        }

        Map<String, UserPreferences> results = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(userIds.length);

        for (String userId : userIds) {
            loadUserPreferences(userId, new PreferencesCallback() {
                @Override
                public void onSuccess(UserPreferences preferences) {
                    synchronized (results) {
                        results.put(userId, preferences);
                    }
                    latch.countDown();
                }

                @Override
                public void onError(String error) {
                    System.err.println("Error loading preferences for user " + userId + ": " + error);
                    latch.countDown();
                }
            });
        }

        // Wait for all loads to complete
        new Thread(() -> {
            try {
                latch.await(30, TimeUnit.SECONDS); // 30 second timeout
                SwingUtilities.invokeLater(() -> callback.onSuccess(results));
            } catch (InterruptedException e) {
                SwingUtilities.invokeLater(() -> callback.onError("Timeout lors du chargement multiple"));
            }
        }).start();
    }

    /**
     * Resets user preferences to default values
     * @param userId The user ID
     * @param callback The callback for the result
     */
    public void resetToDefaults(String userId, PreferencesCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("ID utilisateur invalide");
            return;
        }

        // Create default preferences
        UserPreferences defaultPrefs = new UserPreferences();
        defaultPrefs.setUserId(userId);

        // Load current preferences to preserve identity info
        loadUserPreferences(userId, new PreferencesCallback() {
            @Override
            public void onSuccess(UserPreferences currentPrefs) {
                // Preserve user identity
                defaultPrefs.setDisplayName(currentPrefs.getDisplayName());
                defaultPrefs.setProfileImageUrl(currentPrefs.getProfileImageUrl());
                defaultPrefs.setPhoneNumber(currentPrefs.getPhoneNumber());

                // Save the reset preferences
                saveUserPreferences(defaultPrefs, callback);
            }

            @Override
            public void onError(String error) {
                // If we can't load current preferences, just save defaults
                saveUserPreferences(defaultPrefs, callback);
            }
        });
    }

    // ===== CACHE MANAGEMENT =====

    /**
     * Gets cached preferences if available and not expired
     */
    private UserPreferences getCachedPreferences(String userId) {
        if (!preferencesCache.containsKey(userId)) {
            return null;
        }

        Long timestamp = cacheTimestamps.get(userId);
        if (timestamp == null || (System.currentTimeMillis() - timestamp) > cacheExpiry) {
            // Cache expired
            preferencesCache.remove(userId);
            cacheTimestamps.remove(userId);
            return null;
        }

        return preferencesCache.get(userId);
    }

    /**
     * Caches preferences for quick access
     */
    private void cachePreferences(String userId, UserPreferences preferences) {
        preferencesCache.put(userId, preferences.copy());
        cacheTimestamps.put(userId, System.currentTimeMillis());
    }

    /**
     * Invalidates cache for a specific user
     */
    private void invalidateCache(String userId) {
        preferencesCache.remove(userId);
        cacheTimestamps.remove(userId);
    }

    /**
     * Clears all cached preferences
     */
    public void clearCache() {
        preferencesCache.clear();
        cacheTimestamps.clear();
    }

    // ===== LISTENERS FOR REAL-TIME UPDATES =====

    /**
     * Listener interface for real-time preference changes
     */
    public interface PreferencesUpdateListener {
        void onPreferencesUpdated(UserPreferences preferences);
        void onError(String error);
    }

    /**
     * Sets up a real-time listener for preference changes
     * @param userId The user ID to monitor
     * @param listener The update listener
     * @return ValueEventListener that can be used to remove the listener
     */
    public ValueEventListener listenToPreferences(String userId, PreferencesUpdateListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onError("ID utilisateur invalide");
            return null;
        }

        DatabaseReference prefsRef = database.child(PREFERENCES_PATH).child(userId);

        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                        UserPreferences preferences = UserPreferences.fromMap(data);

                        // Update cache
                        cachePreferences(userId, preferences);

                        SwingUtilities.invokeLater(() -> listener.onPreferencesUpdated(preferences));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            listener.onError("Erreur lors de la mise à jour: " + e.getMessage())
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                SwingUtilities.invokeLater(() ->
                        listener.onError("Listener annulé: " + databaseError.getMessage())
                );
            }
        };

        prefsRef.addValueEventListener(valueListener);
        return valueListener;
    }

    /**
     * Removes a real-time listener
     * @param userId The user ID
     * @param listener The listener to remove
     */
    public void removePreferencesListener(String userId, ValueEventListener listener) {
        if (userId != null && listener != null) {
            DatabaseReference prefsRef = database.child(PREFERENCES_PATH).child(userId);
            prefsRef.removeEventListener(listener);
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Checks if the service is properly initialized
     * @return true if ready to use
     */
    public boolean isReady() {
        return firebaseService != null && database != null && storage != null;
    }

    /**
     * Gets cache statistics
     * @return Map with cache info
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedUsers", preferencesCache.size());
        stats.put("cacheExpiryMinutes", cacheExpiry / (60 * 1000));

        // Count expired entries
        long currentTime = System.currentTimeMillis();
        int expiredCount = 0;
        for (Long timestamp : cacheTimestamps.values()) {
            if ((currentTime - timestamp) > cacheExpiry) {
                expiredCount++;
            }
        }
        stats.put("expiredEntries", expiredCount);

        return stats;
    }

    // ===== ADDITIONAL CALLBACK INTERFACES =====

    /**
     * Callback interface for multiple user preferences
     */
    public interface MultiplePreferencesCallback {
        void onSuccess(Map<String, UserPreferences> preferences);
        void onError(String error);
    }
}