package com.ktu.timetable;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ktu.timetable.admin.AdminDashboardActivity;
import com.ktu.timetable.lecturer.LecturerDashboardActivity;
import com.ktu.timetable.models.User;
import com.ktu.timetable.student.StudentDashboardActivity;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;
import com.ktu.timetable.utils.NotificationHelper;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIMEOUT = 2000; // 2 seconds
    
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Create notification channels
        NotificationHelper.createNotificationChannels(this);
        
        // Get UI elements
        ImageView logoImageView = findViewById(R.id.logoImageView);
        TextView universityNameTextView = findViewById(R.id.universityNameTextView);
        TextView mottoTextView = findViewById(R.id.mottoTextView);
        
        // Load and start animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        logoImageView.startAnimation(fadeIn);
        universityNameTextView.startAnimation(fadeIn);
        mottoTextView.startAnimation(fadeIn);
        
        // Delay and redirect
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserAndRedirect, SPLASH_TIMEOUT);
    }
    
    /**
     * Check if user is already logged in and redirect accordingly
     */
    private void checkUserAndRedirect() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser != null) {
            // User is logged in, check role and redirect
            FirebaseUtil.getUsersCollection().document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            // Save user to local database for offline access
                            if (user != null) {
                                databaseHelper.saveUser(user);
                                redirectBasedOnUserRole(user);
                            } else {
                                // User data error, redirect to login
                                goToLoginScreen();
                            }
                        } else {
                            // User document doesn't exist, redirect to login
                            goToLoginScreen();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Network error, try to get user from local database
                        User offlineUser = databaseHelper.getUser(currentUser.getUid());
                        if (offlineUser != null) {
                            redirectBasedOnUserRole(offlineUser);
                        } else {
                            goToLoginScreen();
                        }
                    });
        } else {
            // User is not logged in
            goToLoginScreen();
        }
    }
    
    /**
     * Redirect to appropriate activity based on user role
     * @param user User object
     */
    private void redirectBasedOnUserRole(User user) {
        Intent intent;
        
        if (user.isAdmin()) {
            intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
        } else if (user.isLecturer()) {
            intent = new Intent(SplashActivity.this, LecturerDashboardActivity.class);
        } else if (user.isStudent()) {
            intent = new Intent(SplashActivity.this, StudentDashboardActivity.class);
        } else {
            // Default or unknown role
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
    
    /**
     * Go to login screen
     */
    private void goToLoginScreen() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
