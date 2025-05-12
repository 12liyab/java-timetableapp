package com.ktu.timetable.lecturer;

import android.content.Intent;  
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
 
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; 
import com.ktu.timetable.LoginActivity;
import com.ktu.timetable.R;
import com.ktu.timetable.models.Lecturer;
import com.ktu.timetable.models.User;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LecturerDashboardActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private TextView syncStatusTextView;
    private Button logoutButton;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;
    private User currentUser;
    private Lecturer currentLecturer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.lecturer_dashboard);
        } 
        
        // Initialize UI elements
        welcomeTextView = findViewById(R.id.welcomeTextView);
        syncStatusTextView = findViewById(R.id.syncStatusTextView);
        logoutButton = findViewById(R.id.logoutButton);
        
        // Set listeners
        logoutButton.setOnClickListener(v -> logout());
        
        // Load user data
        loadUserData();
        
        // Update sync status
        updateSyncStatus();
    } 
    
    @Override
    protected void onResume() {
        super.onResume();
        updateSyncStatus();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadUserData() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            // User not logged in, redirect to login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        FirebaseUtil.getUsersCollection().document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            // Save user to local database
                            databaseHelper.saveUser(currentUser);
                            
                            // Update UI with user data
                            updateUI();
                            
                            // Load lecturer profile
                            loadLecturerProfile();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Try to get user from local database
                    currentUser = databaseHelper.getUser(user.getUid());
                    if (currentUser != null) {
                        updateUI();
                        loadLecturerProfile();
                    } else {
                        // User data not available locally, redirect to login
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                });
    }
    
    private void loadLecturerProfile() {
        if (currentUser == null) return;
        
        FirebaseUtil.getLecturersCollection()
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        currentLecturer = documentSnapshot.toObject(Lecturer.class);
                        if (currentLecturer != null) {
                            // Save lecturer to local database
                            databaseHelper.saveLecturer(currentLecturer);
                            
                            // Subscribe to lecturer topic for notifications
                            FirebaseUtil.subscribeToLecturer(currentLecturer.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Lecturer profile loading failed, but we can continue
                    // since the user is authenticated
                });
    }
    
    private void updateUI() { 
        if (currentUser != null) {
            welcomeTextView.setText("Welcome, " + currentUser.getDisplayName());
        }
    }
    
    private void updateSyncStatus() {
        String lastSyncTime = databaseHelper.getLastSyncTime(FirebaseUtil.TIMETABLE_COLLECTION);
        if (lastSyncTime != null) {
            syncStatusTextView.setText("Last synced: " + lastSyncTime);
        } else {
            syncStatusTextView.setText("Last synced: Never");
        }
    }
    
    /**
     * Handle view timetable click
     * @param view View that was clicked
     */
    public void onViewTimetableClick(View view) {
        Intent intent = new Intent(this, LecturerTimetableActivity.class);
        if (currentLecturer != null) {
            intent.putExtra("LECTURER_ID", currentLecturer.getId());
        }
        startActivity(intent);
    }
    
    /**
     * Handle view courses click
     * @param view View that was clicked
     */
    public void onViewCoursesClick(View view) {
        // Implementation for viewing lecturer's courses
        // This could open another activity that shows all courses assigned to the lecturer
        // For now, we'll just open the timetable which indirectly shows the courses
        onViewTimetableClick(view);
    }
    
    private void logout() {
        firebaseAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
