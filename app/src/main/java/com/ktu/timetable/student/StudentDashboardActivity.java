package com.ktu.timetable.student;

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
import com.ktu.timetable.models.Department;
import com.ktu.timetable.models.User;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private TextView departmentInfoTextView;
    private TextView levelInfoTextView;
    private TextView syncStatusTextView;
    private Button logoutButton;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;
    private User currentUser;
    private Department userDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.student_dashboard);
        }
        
        // Initialize UI elements
        welcomeTextView = findViewById(R.id.welcomeTextView);
        departmentInfoTextView = findViewById(R.id.departmentInfoTextView);
        levelInfoTextView = findViewById(R.id.levelInfoTextView);
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
                            
                            // Load department info
                            loadDepartmentInfo();
                            
                            // Subscribe to topics for notifications
                            FirebaseUtil.subscribeToDepartmentAndLevel(
                                    currentUser.getDepartmentId(), 
                                    currentUser.getLevel()
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Try to get user from local database
                    currentUser = databaseHelper.getUser(user.getUid());
                    if (currentUser != null) {
                        updateUI();
                        loadDepartmentInfo();
                    } else {
                        // User data not available locally, redirect to login
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                });
    }
    
    private void loadDepartmentInfo() {
        if (currentUser == null || currentUser.getDepartmentId() == null) return;
        
        FirebaseUtil.getDepartmentsCollection().document(currentUser.getDepartmentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userDepartment = documentSnapshot.toObject(Department.class);
                        if (userDepartment != null) {
                            // Save department to local database
                            databaseHelper.saveDepartment(userDepartment);
                            
                            // Update department info in UI
                            departmentInfoTextView.setText("Department: " + userDepartment.getName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Try to get department from local database
                    Department department = null;
                    for (Department dept : databaseHelper.getAllDepartments()) {
                        if (dept.getId().equals(currentUser.getDepartmentId())) {
                            department = dept;
                            break;
                        }
                    }
                    
                    if (department != null) {
                        userDepartment = department;
                        departmentInfoTextView.setText("Department: " + userDepartment.getName());
                    }
                });
    }
    
    private void updateUI() {
        if (currentUser != null) {
            welcomeTextView.setText("Welcome, " + currentUser.getDisplayName());
            levelInfoTextView.setText("Level: " + currentUser.getLevel());
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
        Intent intent = new Intent(this, StudentTimetableActivity.class);
        if (currentUser != null) {
            intent.putExtra("DEPARTMENT_ID", currentUser.getDepartmentId());
            intent.putExtra("LEVEL", currentUser.getLevel());
        }
        startActivity(intent);
    }
    
    private void logout() {
        firebaseAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
