import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import services.FirebaseService;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {


        // Test de connectivité Firebase
        try {
            System.out.println("Testing Firebase connection...");
            FirebaseService firebase = FirebaseService.getInstance();
            DatabaseReference testRef = firebase.getDatabase().getReference(".info/connected");
            testRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Boolean connected = snapshot.getValue(Boolean.class);
                    System.out.println("Firebase connection status: " + (connected ? "CONNECTED" : "DISCONNECTED"));
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Firebase connection test failed: " + error.getMessage());
                }
            });

            // Test d'accès à la collection "users"
            DatabaseReference usersRef = firebase.getDatabase().getReference("users");
            System.out.println("Attempting to access users at: " + usersRef.toString());

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    System.out.println("Users data access successful. Found " + dataSnapshot.getChildrenCount() + " users");
                    // Afficher les clés pour débugger
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        System.out.println("User key: " + child.getKey());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Users data access failed: " + databaseError.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Firebase test error: " + e.getMessage());
            e.printStackTrace();
        }

    }
}