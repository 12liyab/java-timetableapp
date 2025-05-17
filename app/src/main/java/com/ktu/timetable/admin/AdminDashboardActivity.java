package com.ktu.timetable.admin;

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
import com.ktu.timetable.models.User;
import com.ktu.timetable.utils.FirebaseUtil;

/**
 * Dashboard for admin users with options to manage timetable system
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private Button logoutButton;
    private FirebaseAuth firebaseAuth;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.admin_dashboard);
        }

        // Initialize UI elements
        welcomeTextView = findViewById(R.id.welcomeTextView);
        logoutButton = findViewById(R.id.logoutButton);

        // Set listeners
        logoutButton.setOnClickListener(v -> logout());

        // Load user data
        loadUserData();
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

    /**
     * Load current user data from Firestore
     */
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
                        updateUI(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    /**
     * Update UI with user data
     * @param user User object
     */
    private void updateUI(User user) {
        if (user != null) {
            welcomeTextView.setText("Welcome, " + user.getDisplayName());
        }
    }

    /**
     * Handle view timetable click
     * @param view View that was clicked
     */
    public void onViewTimetableClick(View view) {
        // In an admin view, we could show all timetables or a selection interface
        // For now, let's redirect to the student timetable view for simplicity
        Intent intent = new Intent(this, AdminTimetableActivity.class);
        startActivity(intent);
    }

    /**
     * Handle manage courses click
     * @param view View that was clicked
     */
    public void onManageCoursesClick(View view) {
        Intent intent = new Intent(this, ManageCoursesActivity.class);
        startActivity(intent);
    }

    /**
     * Handle manage lecturers click
     * @param view View that was clicked
     */
    public void onManageLecturersClick(View view) {
        Intent intent = new Intent(this, ManageLecturersActivity.class);
        startActivity(intent);
    }

    /**
     * Handle manage classrooms click
     * @param view View that was clicked
     */
    public void onManageClassroomsClick(View view) {
        Intent intent = new Intent(this, ManageClassroomsActivity.class);
        startActivity(intent);
    }

    /**
     * Handle schedule class click
     * @param view View that was clicked
     */
    public void onScheduleClassClick(View view) {
        Intent intent = new Intent(this, ScheduleClassActivity.class);
        startActivity(intent);
    }

    /**
     * Logout the current user
     */
    private void logout() {
        firebaseAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
