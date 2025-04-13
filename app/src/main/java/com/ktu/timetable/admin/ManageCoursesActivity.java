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
import android.widget.Switch;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ktu.timetable.R;
import com.ktu.timetable.adapters.CourseAdapter;
import com.ktu.timetable.models.Course;
import com.ktu.timetable.models.Department;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ManageCoursesActivity extends AppCompatActivity implements CourseAdapter.CourseClickListener {

    private RecyclerView coursesRecyclerView;
    private CourseAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private FloatingActionButton addCourseFab;

    private List<Course> allCourses;
    private List<Course> filteredCourses;
    private List<Department> departments;
    private Map<String, Department> departmentMap;
    
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_courses);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.manage_courses);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize UI components
        coursesRecyclerView = findViewById(R.id.coursesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchEditText = findViewById(R.id.searchEditText);
        addCourseFab = findViewById(R.id.addCourseFab);

        // Initialize data lists
        allCourses = new ArrayList<>();
        filteredCourses = new ArrayList<>();
        departments = new ArrayList<>();
        departmentMap = new HashMap<>();

        // Setup RecyclerView
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(filteredCourses, this);
        coursesRecyclerView.setAdapter(adapter);

        // Set listeners
        addCourseFab.setOnClickListener(v -> showAddEditCourseDialog(null));
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCourses(s.toString());
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
                    
                    // Load courses after departments
                    loadCourses();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    departments.addAll(databaseHelper.getAllDepartments());
                    for (Department department : departments) {
                        departmentMap.put(department.getId(), department);
                    }
                    
                    // Load courses after departments
                    loadCourses();
                    
                    Snackbar.make(coursesRecyclerView, R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }

    private void loadCourses() {
        FirebaseUtil.getCoursesCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allCourses.clear();
                    filteredCourses.clear();

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Course course = documentSnapshot.toObject(Course.class);
                        if (course != null) {
                            // Set department name from departmentMap
                            Department department = departmentMap.get(course.getDepartmentId());
                            if (department != null) {
                                course.setDepartmentName(department.getName());
                            }
                            
                            allCourses.add(course);
                            filteredCourses.add(course);
                            
                            // Save to local database
                            databaseHelper.saveCourse(course);
                        }
                    }

                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.COURSES_COLLECTION);
                    
                    // Update UI
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    allCourses.addAll(databaseHelper.getAllCourses());
                    filteredCourses.addAll(allCourses);
                    
                    // Update UI
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                    
                    Snackbar.make(coursesRecyclerView, R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }

    private void filterCourses(String query) {
        filteredCourses.clear();
        
        if (query.isEmpty()) {
            filteredCourses.addAll(allCourses);
        } else {
            query = query.toLowerCase();
            for (Course course : allCourses) {
                if (course.getCode().toLowerCase().contains(query) ||
                    course.getName().toLowerCase().contains(query) ||
                    (course.getDepartmentName() != null && course.getDepartmentName().toLowerCase().contains(query))) {
                    filteredCourses.add(course);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredCourses.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            coursesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            coursesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddEditCourseDialog(Course course) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_course, null);
        builder.setView(dialogView);

        // Dialog title
        TextView dialogTitleTextView = dialogView.findViewById(R.id.dialogTitleTextView);
        dialogTitleTextView.setText(course == null ? "Add Course" : "Edit Course");

        // Initialize form fields
        TextInputEditText courseCodeEditText = dialogView.findViewById(R.id.courseCodeEditText);
        TextInputEditText courseNameEditText = dialogView.findViewById(R.id.courseNameEditText);
        AutoCompleteTextView departmentSpinner = dialogView.findViewById(R.id.departmentSpinner);
        TextInputEditText creditHoursEditText = dialogView.findViewById(R.id.creditHoursEditText);
        AutoCompleteTextView levelSpinner = dialogView.findViewById(R.id.levelSpinner);
        AutoCompleteTextView semesterSpinner = dialogView.findViewById(R.id.semesterSpinner);
        Switch electiveSwitch = dialogView.findViewById(R.id.electiveSwitch);
        TextInputEditText descriptionEditText = dialogView.findViewById(R.id.descriptionEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

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

        // Setup level spinner
        String[] levels = {"100", "200", "300", "400"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, levels);
        levelSpinner.setAdapter(levelAdapter);

        // Setup semester spinner
        String[] semesters = {"1", "2"};
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, semesters);
        semesterSpinner.setAdapter(semesterAdapter);

        // Populate dialog with course data if editing
        if (course != null) {
            courseCodeEditText.setText(course.getCode());
            courseNameEditText.setText(course.getName());
            creditHoursEditText.setText(String.valueOf(course.getCreditHours()));
            levelSpinner.setText(course.getLevel(), false);
            semesterSpinner.setText(course.getSemester(), false);
            electiveSwitch.setChecked(course.isElective());
            descriptionEditText.setText(course.getDescription());
            
            // Set department spinner
            Department department = departmentMap.get(course.getDepartmentId());
            if (department != null) {
                departmentSpinner.setText(department.getName(), false);
            }
        }

        AlertDialog dialog = builder.create();

        // Set button listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        saveButton.setOnClickListener(v -> {
            // Validate input
            String code = courseCodeEditText.getText().toString().trim();
            String name = courseNameEditText.getText().toString().trim();
            String departmentName = departmentSpinner.getText().toString().trim();
            String creditHoursStr = creditHoursEditText.getText().toString().trim();
            String level = levelSpinner.getText().toString().trim();
            String semester = semesterSpinner.getText().toString().trim();
            boolean isElective = electiveSwitch.isChecked();
            String description = descriptionEditText.getText().toString().trim();
            
            if (code.isEmpty()) {
                courseCodeEditText.setError("Course code is required");
                return;
            }
            
            if (name.isEmpty()) {
                courseNameEditText.setError("Course name is required");
                return;
            }
            
            if (departmentName.isEmpty()) {
                departmentSpinner.setError("Department is required");
                return;
            }
            
            if (creditHoursStr.isEmpty()) {
                creditHoursEditText.setError("Credit hours is required");
                return;
            }
            
            if (level.isEmpty()) {
                levelSpinner.setError("Level is required");
                return;
            }
            
            if (semester.isEmpty()) {
                semesterSpinner.setError("Semester is required");
                return;
            }
            
            int creditHours;
            try {
                creditHours = Integer.parseInt(creditHoursStr);
            } catch (NumberFormatException e) {
                creditHoursEditText.setError("Invalid credit hours");
                return;
            }
            
            String departmentId = departmentLookup.get(departmentName);
            if (departmentId == null) {
                departmentSpinner.setError("Invalid department");
                return;
            }
            
            // Prepare course object
            final Course courseToSave;
            if (course == null) {
                // New course
                courseToSave = new Course();
                courseToSave.setId(UUID.randomUUID().toString());
            } else {
                // Editing existing course
                courseToSave = course;
            }
            
            courseToSave.setCode(code);
            courseToSave.setName(name);
            courseToSave.setDepartmentId(departmentId);
            courseToSave.setDepartmentName(departmentName);
            courseToSave.setCreditHours(creditHours);
            courseToSave.setLevel(level);
            courseToSave.setSemester(semester);
            courseToSave.setElective(isElective);
            courseToSave.setDescription(description);
            
            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);
            
            // Save to Firestore
            FirebaseUtil.getCoursesCollection().document(courseToSave.getId())
                    .set(courseToSave)
                    .addOnSuccessListener(aVoid -> {
                        // Save to local database
                        databaseHelper.saveCourse(courseToSave);
                        
                        if (course == null) {
                            // New course
                            allCourses.add(courseToSave);
                            filteredCourses.add(courseToSave);
                            adapter.notifyItemInserted(filteredCourses.indexOf(courseToSave));
                            Snackbar.make(coursesRecyclerView, R.string.add_success, Snackbar.LENGTH_SHORT).show();
                        } else {
                            // Update existing course
                            int position = filteredCourses.indexOf(course);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }
                            Snackbar.make(coursesRecyclerView, R.string.update_success, Snackbar.LENGTH_SHORT).show();
                        }
                        
                        updateEmptyView();
                        progressBar.setVisibility(View.GONE);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Snackbar.make(coursesRecyclerView, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void deleteCourse(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    
                    FirebaseUtil.getCoursesCollection().document(course.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from local database and lists
                                allCourses.remove(course);
                                int position = filteredCourses.indexOf(course);
                                if (position != -1) {
                                    filteredCourses.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }
                                
                                updateEmptyView();
                                progressBar.setVisibility(View.GONE);
                                Snackbar.make(coursesRecyclerView, R.string.delete_success, Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Snackbar.make(coursesRecyclerView, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Course course) {
        showAddEditCourseDialog(course);
    }

    @Override
    public void onDeleteClick(Course course) {
        deleteCourse(course);
    }
}
