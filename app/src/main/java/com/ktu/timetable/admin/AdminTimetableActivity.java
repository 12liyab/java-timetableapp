package com.ktu.timetable.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ktu.timetable.R;
import com.ktu.timetable.adapters.TimetableAdapter;
import com.ktu.timetable.models.Classroom;
import com.ktu.timetable.models.Department;
import com.ktu.timetable.models.Lecturer;
import com.ktu.timetable.models.TimetableEntry;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for admins to view timetables with filtering options
 */
public class AdminTimetableActivity extends AppCompatActivity {

    private RecyclerView timetableRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FloatingActionButton filterFab;
    private TabLayout tabLayout;

    private TimetableAdapter adapter;
    private List<TimetableEntry> allTimetableEntries;
    private List<TimetableEntry> filteredTimetableEntries;
    private List<Department> departments;
    private List<Lecturer> lecturers;
    private List<Classroom> classrooms;
    private Map<String, String> levels;

    private DatabaseHelper databaseHelper;
    private String currentFilterDepartmentId = null;
    private String currentFilterLevel = null;
    private String currentFilterLecturerId = null;
    private String currentFilterClassroomId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Timetable Browser");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize UI elements
        timetableRecyclerView = findViewById(R.id.timetableRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        filterFab = findViewById(R.id.filterFab);
        tabLayout = findViewById(R.id.tabLayout);

        // Set up RecyclerView
        timetableRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allTimetableEntries = new ArrayList<>();
        filteredTimetableEntries = new ArrayList<>();
        adapter = new TimetableAdapter(filteredTimetableEntries);
        timetableRecyclerView.setAdapter(adapter);

        // Initialize data collections
        departments = new ArrayList<>();
        lecturers = new ArrayList<>();
        classrooms = new ArrayList<>();
        levels = new HashMap<>();
        levels.put("100", "Level 100");
        levels.put("200", "Level 200");
        levels.put("300", "Level 300");
        levels.put("400", "Level 400");

        // Setup tabs for days of the week
        setupTabs();

        // Set listeners
        filterFab.setOnClickListener(v -> showFilterDialog());

        // Load data
        loadDepartments();
        loadLecturers();
        loadClassrooms();
        loadAllTimetableEntries();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Set up tabs for days of the week
     */
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.monday));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tuesday));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.wednesday));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.thursday));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.friday));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.saturday));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.sunday));

        // Set current day as default
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Calendar.SUNDAY = 1, MONDAY = 2, etc.
        // We need to convert to our representation: MONDAY = 1, TUESDAY = 2, etc.
        int tabIndex = (dayOfWeek + 5) % 7;
        
        tabLayout.selectTab(tabLayout.getTabAt(tabIndex));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterTimetableByDay(tab.getPosition() + 1); // +1 because our data model uses 1-based indexing for days
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    /**
     * Load all departments from Firestore
     */
    private void loadDepartments() {
        FirebaseUtil.getDepartmentsCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    departments.clear();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Department department = documentSnapshot.toObject(Department.class);
                        if (department != null) {
                            departments.add(department);
                            databaseHelper.saveDepartment(department);
                        }
                    }
                    databaseHelper.updateSyncTime(FirebaseUtil.DEPARTMENTS_COLLECTION);
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    departments.addAll(databaseHelper.getAllDepartments());
                });
    }

    /**
     * Load all lecturers from Firestore
     */
    private void loadLecturers() {
        FirebaseUtil.getLecturersCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    lecturers.clear();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Lecturer lecturer = documentSnapshot.toObject(Lecturer.class);
                        if (lecturer != null) {
                            lecturers.add(lecturer);
                            databaseHelper.saveLecturer(lecturer);
                        }
                    }
                    databaseHelper.updateSyncTime(FirebaseUtil.LECTURERS_COLLECTION);
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    lecturers.addAll(databaseHelper.getAllLecturers());
                });
    }

    /**
     * Load all classrooms from Firestore
     */
    private void loadClassrooms() {
        FirebaseUtil.getClassroomsCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classrooms.clear();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Classroom classroom = documentSnapshot.toObject(Classroom.class);
                        if (classroom != null) {
                            classrooms.add(classroom);
                            databaseHelper.saveClassroom(classroom);
                        }
                    }
                    databaseHelper.updateSyncTime(FirebaseUtil.CLASSROOMS_COLLECTION);
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    classrooms.addAll(databaseHelper.getAllClassrooms());
                });
    }

    /**
     * Load all timetable entries from Firestore
     */
    private void loadAllTimetableEntries() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        FirebaseUtil.getTimetableCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTimetableEntries.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        TimetableEntry entry = documentSnapshot.toObject(TimetableEntry.class);
                        allTimetableEntries.add(entry);
                        databaseHelper.saveTimetableEntry(entry);
                    }
                    
                    databaseHelper.updateSyncTime(FirebaseUtil.TIMETABLE_COLLECTION);
                    
                    // Filter timetable by the current day tab
                    TabLayout.Tab selectedTab = tabLayout.getSelectedTab();
                    if (selectedTab != null) {
                        filterTimetableByDay(selectedTab.getPosition() + 1);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    allTimetableEntries.addAll(databaseHelper.getAllTimetableEntries());
                    
                    // Filter timetable by the current day tab
                    TabLayout.Tab selectedTab = tabLayout.getSelectedTab();
                    if (selectedTab != null) {
                        filterTimetableByDay(selectedTab.getPosition() + 1);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                });
    }

    /**
     * Filter timetable by day of week
     * @param dayOfWeek Day of week (1 = Monday, 2 = Tuesday, etc.)
     */
    private void filterTimetableByDay(int dayOfWeek) {
        filteredTimetableEntries.clear();
        
        for (TimetableEntry entry : allTimetableEntries) {
            if (entry.getDayOfWeek() == dayOfWeek) {
                // Apply other filters if set
                boolean matchesDepartment = currentFilterDepartmentId == null || 
                        currentFilterDepartmentId.equals(entry.getDepartmentId());
                boolean matchesLevel = currentFilterLevel == null || 
                        currentFilterLevel.equals(entry.getLevel());
                boolean matchesLecturer = currentFilterLecturerId == null || 
                        currentFilterLecturerId.equals(entry.getLecturerId());
                boolean matchesClassroom = currentFilterClassroomId == null || 
                        currentFilterClassroomId.equals(entry.getClassroomId());
                
                if (matchesDepartment && matchesLevel && matchesLecturer && matchesClassroom) {
                    filteredTimetableEntries.add(entry);
                }
            }
        }

        // Sort entries by start time
        filteredTimetableEntries.sort((o1, o2) -> {
            if (o1.getStartTime() == null || o2.getStartTime() == null) return 0;
            return o1.getStartTime().compareTo(o2.getStartTime());
        });
        
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    /**
     * Show filter dialog for timetable
     */
    private void showFilterDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.filter);
        
        View view = getLayoutInflater().inflate(R.layout.dialog_filter_timetable, null);
        
        // Department spinner
        androidx.appcompat.widget.AppCompatSpinner departmentSpinner = view.findViewById(R.id.departmentSpinner);
        List<String> departmentNames = new ArrayList<>();
        departmentNames.add("All Departments");
        Map<String, String> departmentMap = new HashMap<>();
        
        for (Department department : departments) {
            departmentNames.add(department.getName());
            departmentMap.put(department.getName(), department.getId());
        }
        
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, departmentNames);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departmentSpinner.setAdapter(departmentAdapter);
        
        // Set current selection if filter is applied
        if (currentFilterDepartmentId != null) {
            for (Department department : departments) {
                if (department.getId().equals(currentFilterDepartmentId)) {
                    int position = departmentNames.indexOf(department.getName());
                    if (position != -1) {
                        departmentSpinner.setSelection(position);
                    }
                    break;
                }
            }
        }
        
        // Level spinner
        androidx.appcompat.widget.AppCompatSpinner levelSpinner = view.findViewById(R.id.levelSpinner);
        List<String> levelNames = new ArrayList<>();
        levelNames.add("All Levels");
        levelNames.addAll(levels.values());
        
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelNames);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(levelAdapter);
        
        // Set current selection if filter is applied
        if (currentFilterLevel != null) {
            String levelName = levels.get(currentFilterLevel);
            if (levelName != null) {
                int position = levelNames.indexOf(levelName);
                if (position != -1) {
                    levelSpinner.setSelection(position);
                }
            }
        }
        
        // Lecturer spinner
        androidx.appcompat.widget.AppCompatSpinner lecturerSpinner = view.findViewById(R.id.lecturerSpinner);
        List<String> lecturerNames = new ArrayList<>();
        lecturerNames.add("All Lecturers");
        Map<String, String> lecturerMap = new HashMap<>();
        
        for (Lecturer lecturer : lecturers) {
            lecturerNames.add(lecturer.getFullName());
            lecturerMap.put(lecturer.getFullName(), lecturer.getId());
        }
        
        ArrayAdapter<String> lecturerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, lecturerNames);
        lecturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lecturerSpinner.setAdapter(lecturerAdapter);
        
        // Set current selection if filter is applied
        if (currentFilterLecturerId != null) {
            for (Lecturer lecturer : lecturers) {
                if (lecturer.getId().equals(currentFilterLecturerId)) {
                    int position = lecturerNames.indexOf(lecturer.getFullName());
                    if (position != -1) {
                        lecturerSpinner.setSelection(position);
                    }
                    break;
                }
            }
        }
        
        // Classroom spinner
        androidx.appcompat.widget.AppCompatSpinner classroomSpinner = view.findViewById(R.id.classroomSpinner);
        List<String> classroomNames = new ArrayList<>();
        classroomNames.add("All Classrooms");
        Map<String, String> classroomMap = new HashMap<>();
        
        for (Classroom classroom : classrooms) {
            classroomNames.add(classroom.getFullName());
            classroomMap.put(classroom.getFullName(), classroom.getId());
        }
        
        ArrayAdapter<String> classroomAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, classroomNames);
        classroomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classroomSpinner.setAdapter(classroomAdapter);
        
        // Set current selection if filter is applied
        if (currentFilterClassroomId != null) {
            for (Classroom classroom : classrooms) {
                if (classroom.getId().equals(currentFilterClassroomId)) {
                    int position = classroomNames.indexOf(classroom.getFullName());
                    if (position != -1) {
                        classroomSpinner.setSelection(position);
                    }
                    break;
                }
            }
        }
        
        builder.setView(view);
        
        builder.setPositiveButton(R.string.apply, (dialog, which) -> {
            // Apply filters
            String selectedDepartmentName = (String) departmentSpinner.getSelectedItem();
            String selectedLevelName = (String) levelSpinner.getSelectedItem();
            String selectedLecturerName = (String) lecturerSpinner.getSelectedItem();
            String selectedClassroomName = (String) classroomSpinner.getSelectedItem();
            
            // Reset filters
            currentFilterDepartmentId = null;
            currentFilterLevel = null;
            currentFilterLecturerId = null;
            currentFilterClassroomId = null;
            
            // Apply department filter if not "All Departments"
            if (!selectedDepartmentName.equals("All Departments")) {
                currentFilterDepartmentId = departmentMap.get(selectedDepartmentName);
            }
            
            // Apply level filter if not "All Levels"
            if (!selectedLevelName.equals("All Levels")) {
                for (Map.Entry<String, String> entry : levels.entrySet()) {
                    if (entry.getValue().equals(selectedLevelName)) {
                        currentFilterLevel = entry.getKey();
                        break;
                    }
                }
            }
            
            // Apply lecturer filter if not "All Lecturers"
            if (!selectedLecturerName.equals("All Lecturers")) {
                currentFilterLecturerId = lecturerMap.get(selectedLecturerName);
            }
            
            // Apply classroom filter if not "All Classrooms"
            if (!selectedClassroomName.equals("All Classrooms")) {
                currentFilterClassroomId = classroomMap.get(selectedClassroomName);
            }
            
            // Apply filters to current day view
            TabLayout.Tab selectedTab = tabLayout.getSelectedTab();
            if (selectedTab != null) {
                filterTimetableByDay(selectedTab.getPosition() + 1);
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        
        builder.setNeutralButton("Clear Filters", (dialog, which) -> {
            // Clear all filters
            currentFilterDepartmentId = null;
            currentFilterLevel = null;
            currentFilterLecturerId = null;
            currentFilterClassroomId = null;
            
            // Refresh the view
            TabLayout.Tab selectedTab = tabLayout.getSelectedTab();
            if (selectedTab != null) {
                filterTimetableByDay(selectedTab.getPosition() + 1);
            }
        });
        
        builder.show();
    }

    /**
     * Update empty view visibility based on timetable entries
     */
    private void updateEmptyView() {
        if (filteredTimetableEntries.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            timetableRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            timetableRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
