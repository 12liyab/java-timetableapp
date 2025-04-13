package com.ktu.timetable.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.ktu.timetable.R;
import com.ktu.timetable.adapters.LecturerAdapter;
import com.ktu.timetable.models.Department;
import com.ktu.timetable.models.Lecturer;
import com.ktu.timetable.models.User;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ManageLecturersActivity extends AppCompatActivity implements LecturerAdapter.LecturerClickListener {

    private RecyclerView lecturersRecyclerView;
    private LecturerAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private FloatingActionButton addLecturerFab;
    
    private List<Lecturer> allLecturers;
    private List<Lecturer> filteredLecturers;
    private List<Department> departments;
    private Map<String, Department> departmentMap;
    
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_lecturers);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.manage_lecturers);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize UI components
        lecturersRecyclerView = findViewById(R.id.lecturersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchEditText = findViewById(R.id.searchEditText);
        addLecturerFab = findViewById(R.id.addLecturerFab);

        // Initialize data lists
        allLecturers = new ArrayList<>();
        filteredLecturers = new ArrayList<>();
        departments = new ArrayList<>();
        departmentMap = new HashMap<>();

        // Setup RecyclerView
        lecturersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LecturerAdapter(filteredLecturers, this);
        lecturersRecyclerView.setAdapter(adapter);

        // Set listeners
        addLecturerFab.setOnClickListener(v -> showAddEditLecturerDialog(null));
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLecturers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Load data
        loadDepartments();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDepartments() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getDepartmentsCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    departments.clear();
                    departmentMap.clear();

                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Department department = documentSnapshot.toObject(Department.class);
                        if (department != null) {
                            departments.add(department);
                            departmentMap.put(department.getId(), department);
                            
                            // Save to local database
                            databaseHelper.saveDepartment(department);
                        }
                    }

                    // Sync timestamp update
                    databaseHelper.updateSyncTime(FirebaseUtil.DEPARTMENTS_COLLECTION);
                    
                    // Load lecturers after departments
                    loadLecturers();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    departments.addAll(databaseHelper.getAllDepartments());
                    for (Department department : departments) {
                        departmentMap.put(department.getId(), department);
                    }
                    
                    // Load lecturers after departments
                    loadLecturers();
                    
                    Snackbar.make(lecturersRecyclerView, R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }

    private void loadLecturers() {
        FirebaseUtil.getLecturersCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allLecturers.clear();
                    filteredLecturers.clear();

                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Lecturer lecturer = documentSnapshot.toObject(Lecturer.class);
                        if (lecturer != null) {
                            // Set department name from departmentMap
                            Department department = departmentMap.get(lecturer.getDepartmentId());
                            if (department != null) {
                                lecturer.setDepartmentName(department.getName());
                            }
                            
                            allLecturers.add(lecturer);
                            filteredLecturers.add(lecturer);
                            
                            // Save to local database
                            databaseHelper.saveLecturer(lecturer);
                        }
                    }

                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.LECTURERS_COLLECTION);
                    
                    // Update UI
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    allLecturers.addAll(databaseHelper.getAllLecturers());
                    filteredLecturers.addAll(allLecturers);
                    
                    // Update UI
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                    
                    Snackbar.make(lecturersRecyclerView, R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }

    private void filterLecturers(String query) {
        filteredLecturers.clear();
        
        if (query.isEmpty()) {
            filteredLecturers.addAll(allLecturers);
        } else {
            query = query.toLowerCase();
            for (Lecturer lecturer : allLecturers) {
                if (lecturer.getFirstName().toLowerCase().contains(query) ||
                    lecturer.getLastName().toLowerCase().contains(query) ||
                    lecturer.getStaffId().toLowerCase().contains(query) ||
                    (lecturer.getDepartmentName() != null && lecturer.getDepartmentName().toLowerCase().contains(query))) {
                    filteredLecturers.add(lecturer);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredLecturers.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            lecturersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            lecturersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddEditLecturerDialog(Lecturer lecturer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_lecturer, null);
        builder.setView(dialogView);

        // Dialog title
        TextView dialogTitleTextView = dialogView.findViewById(R.id.dialogTitleTextView);
        dialogTitleTextView.setText(lecturer == null ? "Add Lecturer" : "Edit Lecturer");

        // Initialize form fields
        TextInputEditText staffIdEditText = dialogView.findViewById(R.id.staffIdEditText);
        AutoCompleteTextView titleSpinner = dialogView.findViewById(R.id.titleSpinner);
        TextInputEditText firstNameEditText = dialogView.findViewById(R.id.firstNameEditText);
        TextInputEditText lastNameEditText = dialogView.findViewById(R.id.lastNameEditText);
        AutoCompleteTextView departmentSpinner = dialogView.findViewById(R.id.departmentSpinner);
        TextInputEditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        TextInputEditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Setup title spinner
        String[] titles = {"Prof.", "Dr.", "Mr.", "Mrs.", "Ms."};
        ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, titles);
        titleSpinner.setAdapter(titleAdapter);

        // Setup department spinner
        List<String> departmentNames = new ArrayList<>();
        Map<String, String> departmentLookup = new HashMap<>();
        
        for (Department department : departments) {
            departmentNames.add(department.getName());
            departmentLookup.put(department.getName(), department.getId());
        }
        
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, departmentNames);
        departmentSpinner.setAdapter(departmentAdapter);

        // Populate dialog with lecturer data if editing
        if (lecturer != null) {
            staffIdEditText.setText(lecturer.getStaffId());
            titleSpinner.setText(lecturer.getTitle(), false);
            firstNameEditText.setText(lecturer.getFirstName());
            lastNameEditText.setText(lecturer.getLastName());
            emailEditText.setText(lecturer.getEmail());
            phoneEditText.setText(lecturer.getPhoneNumber());
            
            // Set department spinner
            Department department = departmentMap.get(lecturer.getDepartmentId());
            if (department != null) {
                departmentSpinner.setText(department.getName(), false);
            }
        }

        AlertDialog dialog = builder.create();

        // Set button listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        saveButton.setOnClickListener(v -> {
            // Validate input
            String staffId = staffIdEditText.getText().toString().trim();
            String title = titleSpinner.getText().toString().trim();
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName = lastNameEditText.getText().toString().trim();
            String departmentName = departmentSpinner.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            
            if (staffId.isEmpty()) {
                staffIdEditText.setError("Staff ID is required");
                return;
            }
            
            if (firstName.isEmpty()) {
                firstNameEditText.setError("First name is required");
                return;
            }
            
            if (lastName.isEmpty()) {
                lastNameEditText.setError("Last name is required");
                return;
            }
            
            if (departmentName.isEmpty()) {
                departmentSpinner.setError("Department is required");
                return;
            }
            
            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                return;
            }
            
            String departmentId = departmentLookup.get(departmentName);
            if (departmentId == null) {
                departmentSpinner.setError("Invalid department");
                return;
            }
            
            // Prepare lecturer object
            final Lecturer lecturerToSave;
            if (lecturer == null) {
                // New lecturer
                lecturerToSave = new Lecturer();
                lecturerToSave.setId(UUID.randomUUID().toString());
            } else {
                // Editing existing lecturer
                lecturerToSave = lecturer;
            }
            
            lecturerToSave.setStaffId(staffId);
            lecturerToSave.setTitle(title);
            lecturerToSave.setFirstName(firstName);
            lecturerToSave.setLastName(lastName);
            lecturerToSave.setDepartmentId(departmentId);
            lecturerToSave.setDepartmentName(departmentName);
            lecturerToSave.setEmail(email);
            lecturerToSave.setPhoneNumber(phone);
            
            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);
            
            // Check if user account exists
            if (lecturerToSave.getUserId() == null && lecturer == null) {
                // Create user account for new lecturer
                createUserAccount(lecturerToSave, dialog);
            } else {
                // Save lecturer directly
                saveLecturer(lecturerToSave, dialog);
            }
        });

        dialog.show();
    }
    
    private void createUserAccount(Lecturer lecturer, AlertDialog dialog) {
        // For new lecturers, we might create a user account in Firebase Auth
        // However, this would typically be done through a separate admin interface
        // or Firebase Cloud Functions for security reasons.
        // For this app, we'll just save the lecturer data in Firestore.
        
        // Normally, you would do something like:
        // FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
        // But we'll skip that step here and just attach a placeholder userId
        
        lecturer.setUserId("lecturer_" + lecturer.getId()); // Placeholder
        saveLecturer(lecturer, dialog);
    }
    
    private void saveLecturer(Lecturer lecturer, AlertDialog dialog) {
        // Save to Firestore
        FirebaseUtil.getLecturersCollection().document(lecturer.getId())
                .set(lecturer)
                .addOnSuccessListener(aVoid -> {
                    // Save to local database
                    databaseHelper.saveLecturer(lecturer);
                    
                    boolean isNewLecturer = !allLecturers.contains(lecturer);
                    
                    if (isNewLecturer) {
                        // New lecturer
                        allLecturers.add(lecturer);
                        filteredLecturers.add(lecturer);
                        adapter.notifyItemInserted(filteredLecturers.indexOf(lecturer));
                        Snackbar.make(lecturersRecyclerView, R.string.add_success, Snackbar.LENGTH_SHORT).show();
                    } else {
                        // Update existing lecturer
                        int position = filteredLecturers.indexOf(lecturer);
                        if (position != -1) {
                            adapter.notifyItemChanged(position);
                        }
                        Snackbar.make(lecturersRecyclerView, R.string.update_success, Snackbar.LENGTH_SHORT).show();
                    }
                    
                    updateEmptyView();
                    progressBar.setVisibility(View.GONE);
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(lecturersRecyclerView, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void deleteLecturer(Lecturer lecturer) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Lecturer")
                .setMessage("Are you sure you want to delete this lecturer? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    
                    FirebaseUtil.getLecturersCollection().document(lecturer.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from local database and lists
                                allLecturers.remove(lecturer);
                                int position = filteredLecturers.indexOf(lecturer);
                                if (position != -1) {
                                    filteredLecturers.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }
                                
                                updateEmptyView();
                                progressBar.setVisibility(View.GONE);
                                Snackbar.make(lecturersRecyclerView, R.string.delete_success, Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Snackbar.make(lecturersRecyclerView, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Lecturer lecturer) {
        showAddEditLecturerDialog(lecturer);
    }

    @Override
    public void onDeleteClick(Lecturer lecturer) {
        deleteLecturer(lecturer);
    }
}
