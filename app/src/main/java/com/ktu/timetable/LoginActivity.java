package com.ktu.timetable;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ktu.timetable.admin.AdminDashboardActivity;
import com.ktu.timetable.lecturer.LecturerDashboardActivity;
import com.ktu.timetable.models.User;
import com.ktu.timetable.student.StudentDashboardActivity;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

public class LoginActivity extends AppCompatActivity { // AppCompatActivity is a subclass of Activity

    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView forgotPasswordTextView;
    private ProgressBar progressBar;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressBar = findViewById(R.id.progressBar);
        
        // Set listeners
        loginButton.setOnClickListener(v -> attemptLogin());
        
        forgotPasswordTextView.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Snackbar.make(v, "Please enter your email first", Snackbar.LENGTH_SHORT).show();
            } else {
                sendPasswordResetEmail(email);
            }
        });
    }

    /**
     * Attempt to login with provided credentials
     */
    private void attemptLogin() {
        // Get email and password
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Validate input
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }
        
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        
        // Attempt to sign in
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserDataAndRedirect(user.getUid());
                        }
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, getString(R.string.login_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    /**
     * Fetch user data from Firestore and redirect to appropriate activity
     * @param userId User ID
     */
    private void fetchUserDataAndRedirect(String userId) {
        FirebaseUtil.getUsersCollection().document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Save user to local database for offline access
                            databaseHelper.saveUser(user);
                            
                            // Subscribe to appropriate FCM topics based on user role
                            if (user.isStudent()) {
                                FirebaseUtil.subscribeToDepartmentAndLevel(
                                        user.getDepartmentId(), 
                                        user.getLevel()
                                );
                            } else if (user.isLecturer()) {
                                FirebaseUtil.subscribeToLecturer(userId);
                            }
                            
                            // Redirect to appropriate activity
                            redirectBasedOnUserRole(user);
                        }
                    } else {
                        // User document doesn't exist
                        Toast.makeText(LoginActivity.this, "User data not found",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Redirect to appropriate activity based on user role
     * @param user User object
     */
    private void redirectBasedOnUserRole(User user) {
        Intent intent;
        
        if (user.isAdmin()) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else if (user.isLecturer()) {
            intent = new Intent(LoginActivity.this, LecturerDashboardActivity.class);
        } else if (user.isStudent()) {
            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
        } else {
            // Default or unknown role
            Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show();
            return;
        }
        
        startActivity(intent);
        finish();
    }
    
    /**
     * Send password reset email
     * @param email User email
     */
    private void sendPasswordResetEmail(String email) {
        progressBar.setVisibility(View.VISIBLE);
        
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Password reset email sent to " + email,
                                Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Failed to send password reset email",
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }
}
