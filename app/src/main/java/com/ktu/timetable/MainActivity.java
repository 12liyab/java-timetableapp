package com.ktu.timetable;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.ktu.timetable.admin.AdminDashboardActivity;
import com.ktu.timetable.lecturer.LecturerDashboardActivity;
import com.ktu.timetable.models.User;
import com.ktu.timetable.student.StudentDashboardActivity;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

/**
 * Main activity that redirects to the appropriate dashboard based on user role
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Check if user is logged in and redirect if needed
        if (firebaseAuth.getCurrentUser() != null) {
            checkUserRoleAndRedirect();
        } else {
            // User not logged in, go to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
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
     * Check user role and redirect to appropriate dashboard
     */
    private void checkUserRoleAndRedirect() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, go to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        String userId = currentUser.getUid();
        
        // Try to get user from Firestore
        FirebaseUtil.getUsersCollection().document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                // Save user to local database
                                databaseHelper.saveUser(user);
                                redirectToUserDashboard(user);
                            } else {
                                // Error getting user data
                                goToLoginScreen();
                            }
                        } else {
                            // User document doesn't exist
                            goToLoginScreen();
                        }
                    } else {
                        // Network error, try to get user from local database
                        User offlineUser = databaseHelper.getUser(userId);
                        if (offlineUser != null) {
                            redirectToUserDashboard(offlineUser);
                        } else {
                            goToLoginScreen();
                        }
                    }
                });
    }
    
    /**
     * Redirect to user dashboard based on role
     * @param user User object
     */
    private void redirectToUserDashboard(User user) {
        Intent intent;
        
        if (user.isAdmin()) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if (user.isLecturer()) {
            intent = new Intent(this, LecturerDashboardActivity.class);
        } else if (user.isStudent()) {
            intent = new Intent(this, StudentDashboardActivity.class);
        } else {
            // Unknown role, go to login
            goToLoginScreen();
            return;
        }
        
        startActivity(intent);
        finish();
    }
    
    /**
     * Go to login screen
     */
    private void goToLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    /**
     * Logout user
     */
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
