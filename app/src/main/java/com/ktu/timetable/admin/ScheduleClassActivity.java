package com.ktu.timetable.admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ktu.timetable.R;
import com.ktu.timetable.models.Classroom;
import com.ktu.timetable.models.Course;
import com.ktu.timetable.models.Department;
import com.ktu.timetable.models.Lecturer;
import com.ktu.timetable.models.TimetableEntry;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ScheduleClassActivity extends AppCompatActivity {

    private AutoCompleteTextView departmentSpinner;
    private AutoCompleteTextView levelSpinner;
    private AutoCompleteTextView courseSpinner;
    private AutoCompleteTextView lecturerSpinner;
    private AutoCompleteTextView classroomSpinner;
    private AutoCompleteTextView dayOfWeekSpinner;
    private AutoCompleteTextView classTypeSpinner;
    private TextInputEditText startTimeEditText;
    private TextInputEditText endTimeEditText;
    private Button scheduleButton;
    private Button cancelButton;
    private ProgressBar progressBar;
    private TextView conflictWarningTextView;
    
    private List<Department> departments;
    private List<Course> courses;
    private List<Lecturer> lecturers;
    private List<Classroom> classrooms;
    private List<TimetableEntry> timetableEntries;
    
    private Map<String, Department> departmentMap;
    private Map<String, Course> courseMap;
    private Map<String, Lecturer> lecturerMap;
    private Map<String, Classroom> classroomMap;
    
    private String selectedDepartmentId;
    private String selectedLevel;
    private String selectedCourseId;
    private String selectedLecturerId;
    private String selectedClassroomId;
    private int selectedDayOfWeek;
    private String selectedClassType;
    private Date selectedStartTime;
    private Date selectedEndTime;
    
    private DatabaseHelper databaseHelper;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat timeFormatWithDate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_class);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.schedule_class);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Initialize date formatters
        timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
        timeFormatWithDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        
        // Initialize UI components
        departmentSpinner = findViewById(R.id.departmentSpinner);
        levelSpinner = findViewById(R.id.levelSpinner);
        courseSpinner = findViewById(R.id.courseSpinner);
        lecturerSpinner = findViewById(R.id.lecturerSpinner);
        classroomSpinner = findViewById(R.id.classroomSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        classTypeSpinner = findViewById(R.id.classTypeSpinner);
        startTimeEditText = findViewById(R.id.startTimeEditText);
        endTimeEditText = findViewById(R.id.endTimeEditText);
        scheduleButton = findViewById(R.id.scheduleButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);
        conflictWarningTextView = findViewById(R.id.conflictWarningTextView);
        
        // Initialize data lists
        departments = new ArrayList<>();
        courses = new ArrayList<>();
        lecturers = new ArrayList<>();
        classrooms = new ArrayList<>();
        timetableEntries = new ArrayList<>();
        
        departmentMap = new HashMap<>();
        courseMap = new HashMap<>();
        lecturerMap = new HashMap<>();
        classroomMap = new HashMap<>();
        
        // Setup spinners
        setupLevelSpinner();
        setupDayOfWeekSpinner();
        setupClassTypeSpinner();
        
        // Setup time pickers
        setupTimePickers();
        
        // Set listeners
        scheduleButton.setOnClickListener(v -> validateAndScheduleClass());
        cancelButton.setOnClickListener(v -> finish());
        
        departmentSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String departmentName = (String) parent.getItemAtPosition(position);
            for (Department department : departments) {
                if (department.getName().equals(departmentName)) {
                    selectedDepartmentId = department.getId();
                    
                    // Load courses for selected department
                    loadCoursesForDepartment(selectedDepartmentId);
                    break;
                }
            }
        });
        
        levelSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedLevel = (String) parent.getItemAtPosition(position);
            
            if (selectedDepartmentId != null) {
                loadCoursesForDepartment(selectedDepartmentId);
            }
        });
        
        courseSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String courseName = (String) parent.getItemAtPosition(position);
            for (Course course : courses) {
                if (courseName.equals(course.getCode() + " - " + course.getName())) {
                    selectedCourseId = course.getId();
                    break;
                }
            }
        });
        
        lecturerSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String lecturerName = (String) parent.getItemAtPosition(position);
            for (Lecturer lecturer : lecturers) {
                if (lecturer.getFullName().equals(lecturerName)) {
                    selectedLecturerId = lecturer.getId();
                    break;
                }
            }
        });
        
        classroomSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String classroomName = (String) parent.getItemAtPosition(position);
            for (Classroom classroom : classrooms) {
                if (classroom.getFullName().equals(classroomName)) {
                    selectedClassroomId = classroom.getId();
                    break;
                }
            }
        });
        
        dayOfWeekSpinner.setOnItemClickListener((parent, view, position, id) -> {
            // position is 0-based, but we need 1-based (1=Monday, 2=Tuesday, etc.)
            selectedDayOfWeek = position + 1;
        });
        
        classTypeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedClassType = (String) parent.getItemAtPosition(position);
        });
        
        // Load initial data
        loadData();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupLevelSpinner() {
        String[] levels = {"100", "200", "300", "400"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, levels);
        levelSpinner.setAdapter(adapter);
    }
    
    private void setupDayOfWeekSpinner() {
        String[] daysOfWeek = {
                getString(R.string.monday),
                getString(R.string.tuesday),
                getString(R.string.wednesday),
                getString(R.string.thursday),
                getString(R.string.friday),
                getString(R.string.saturday),
                getString(R.string.sunday)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, daysOfWeek);
        dayOfWeekSpinner.setAdapter(adapter);
    }
    
    private void setupClassTypeSpinner() {
        String[] classTypes = {
                TimetableEntry.TYPE_LECTURE,
                TimetableEntry.TYPE_PRACTICAL,
                TimetableEntry.TYPE_TUTORIAL
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, classTypes);
        classTypeSpinner.setAdapter(adapter);
    }
    
    private void setupTimePickers() {
        startTimeEditText.setOnClickListener(v -> showTimePickerDialog(true));
        endTimeEditText.setOnClickListener(v -> showTimePickerDialog(false));
    }
    
    private void showTimePickerDialog(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, selectedMinute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    
                    if (isStartTime) {
                        selectedStartTime = calendar.getTime();
                        startTimeEditText.setText(timeFormat.format(selectedStartTime));
                    } else {
                        selectedEndTime = calendar.getTime();
                        endTimeEditText.setText(timeFormat.format(selectedEndTime));
                    }
                    
                    checkForConflicts();
                }, hour, minute, false);
        
        timePickerDialog.show();
    }
    
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Load all departments
        loadDepartments();
        
        // Load all lecturers
        loadLecturers();
        
        // Load all classrooms
        loadClassrooms();
        
        // Load all timetable entries (for conflict checking)
        loadTimetableEntries();
    }
    
    private void loadDepartments() {
        FirebaseUtil.getDepartmentsCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    departments.clear();
                    departmentMap.clear();
                    List<String> departmentNames = new ArrayList<>();
                    
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Department department = documentSnapshot.toObject(Department.class);
                        if (department != null) {
                            departments.add(department);
                            departmentMap.put(department.getId(), department);
                            departmentNames.add(department.getName());
                            
                            // Save to local database
                            databaseHelper.saveDepartment(department);
                        }
                    }
                    
                    // Set departments adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, departmentNames);
                    departmentSpinner.setAdapter(adapter);
                    
                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.DEPARTMENTS_COLLECTION);
                    
                    checkIfAllDataLoaded();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    departments.addAll(databaseHelper.getAllDepartments());
                    List<String> departmentNames = new ArrayList<>();
                    
                    for (Department department : departments) {
                        departmentMap.put(department.getId(), department);
                        departmentNames.add(department.getName());
                    }
                    
                    // Set departments adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, departmentNames);
                    departmentSpinner.setAdapter(adapter);
                    
                    checkIfAllDataLoaded();
                    
                    Snackbar.make(findViewById(android.R.id.content), R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void loadLecturers() {
        FirebaseUtil.getLecturersCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    lecturers.clear();
                    lecturerMap.clear();
                    List<String> lecturerNames = new ArrayList<>();
                    
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Lecturer lecturer = documentSnapshot.toObject(Lecturer.class);
                        if (lecturer != null) {
                            lecturers.add(lecturer);
                            lecturerMap.put(lecturer.getId(), lecturer);
                            lecturerNames.add(lecturer.getFullName());
                            
                            // Save to local database
                            databaseHelper.saveLecturer(lecturer);
                        }
                    }
                    
                    // Set lecturers adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, lecturerNames);
                    lecturerSpinner.setAdapter(adapter);
                    
                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.LECTURERS_COLLECTION);
                    
                    checkIfAllDataLoaded();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    lecturers.addAll(databaseHelper.getAllLecturers());
                    List<String> lecturerNames = new ArrayList<>();
                    
                    for (Lecturer lecturer : lecturers) {
                        lecturerMap.put(lecturer.getId(), lecturer);
                        lecturerNames.add(lecturer.getFullName());
                    }
                    
                    // Set lecturers adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, lecturerNames);
                    lecturerSpinner.setAdapter(adapter);
                    
                    checkIfAllDataLoaded();
                    
                    Snackbar.make(findViewById(android.R.id.content), R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void loadClassrooms() {
        FirebaseUtil.getClassroomsCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classrooms.clear();
                    classroomMap.clear();
                    List<String> classroomNames = new ArrayList<>();
                    
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Classroom classroom = documentSnapshot.toObject(Classroom.class);
                        if (classroom != null) {
                            classrooms.add(classroom);
                            classroomMap.put(classroom.getId(), classroom);
                            classroomNames.add(classroom.getFullName());
                            
                            // Save to local database
                            databaseHelper.saveClassroom(classroom);
                        }
                    }
                    
                    // Set classrooms adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, classroomNames);
                    classroomSpinner.setAdapter(adapter);
                    
                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.CLASSROOMS_COLLECTION);
                    
                    checkIfAllDataLoaded();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    classrooms.addAll(databaseHelper.getAllClassrooms());
                    List<String> classroomNames = new ArrayList<>();
                    
                    for (Classroom classroom : classrooms) {
                        classroomMap.put(classroom.getId(), classroom);
                        classroomNames.add(classroom.getFullName());
                    }
                    
                    // Set classrooms adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, classroomNames);
                    classroomSpinner.setAdapter(adapter);
                    
                    checkIfAllDataLoaded();
                    
                    Snackbar.make(findViewById(android.R.id.content), R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void loadTimetableEntries() {
        FirebaseUtil.getTimetableCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    timetableEntries.clear();
                    
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        TimetableEntry entry = documentSnapshot.toObject(TimetableEntry.class);
                        if (entry != null) {
                            timetableEntries.add(entry);
                            
                            // Save to local database
                            databaseHelper.saveTimetableEntry(entry);
                        }
                    }
                    
                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.TIMETABLE_COLLECTION);
                    
                    checkIfAllDataLoaded();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    timetableEntries.addAll(databaseHelper.getAllTimetableEntries());
                    
                    checkIfAllDataLoaded();
                    
                    Snackbar.make(findViewById(android.R.id.content), R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void loadCoursesForDepartment(String departmentId) {
        FirebaseUtil.getCoursesCollection()
                .whereEqualTo("departmentId", departmentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courses.clear();
                    courseMap.clear();
                    List<String> courseNames = new ArrayList<>();
                    
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Course course = documentSnapshot.toObject(Course.class);
                        if (course != null) {
                            // If level is selected, filter by level
                            if (selectedLevel == null || selectedLevel.isEmpty() || 
                                    selectedLevel.equals(course.getLevel())) {
                                courses.add(course);
                                courseMap.put(course.getId(), course);
                                courseNames.add(course.getCode() + " - " + course.getName());
                            }
                            
                            // Save to local database
                            databaseHelper.saveCourse(course);
                        }
                    }
                    
                    // Set courses adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, courseNames);
                    courseSpinner.setAdapter(adapter);
                    
                    // Clear selected course
                    courseSpinner.setText("", false);
                    selectedCourseId = null;
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    List<Course> allCourses = databaseHelper.getCoursesByDepartment(departmentId);
                    courses.clear();
                    courseMap.clear();
                    List<String> courseNames = new ArrayList<>();
                    
                    for (Course course : allCourses) {
                        // If level is selected, filter by level
                        if (selectedLevel == null || selectedLevel.isEmpty() || 
                                selectedLevel.equals(course.getLevel())) {
                            courses.add(course);
                            courseMap.put(course.getId(), course);
                            courseNames.add(course.getCode() + " - " + course.getName());
                        }
                    }
                    
                    // Set courses adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, courseNames);
                    courseSpinner.setAdapter(adapter);
                    
                    // Clear selected course
                    courseSpinner.setText("", false);
                    selectedCourseId = null;
                    
                    Snackbar.make(findViewById(android.R.id.content), R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void checkIfAllDataLoaded() {
        if (!departments.isEmpty() && !lecturers.isEmpty() && !classrooms.isEmpty()) {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void checkForConflicts() {
        if (selectedDayOfWeek == 0 || selectedStartTime == null || selectedEndTime == null ||
                selectedLecturerId == null || selectedClassroomId == null) {
            return;
        }
        
        // Create a temporary timetable entry for conflict checking
        TimetableEntry newEntry = new TimetableEntry();
        newEntry.setDayOfWeek(selectedDayOfWeek);
        newEntry.setStartTime(selectedStartTime);
        newEntry.setEndTime(selectedEndTime);
        newEntry.setLecturerId(selectedLecturerId);
        newEntry.setClassroomId(selectedClassroomId);
        
        // Check for conflicts
        boolean hasConflict = false;
        for (TimetableEntry entry : timetableEntries) {
            if (entry.getDayOfWeek() == selectedDayOfWeek) {
                if (entry.hasLecturerConflict(newEntry) || entry.hasClassroomConflict(newEntry)) {
                    hasConflict = true;
                    break;
                }
            }
        }
        
        if (hasConflict) {
            conflictWarningTextView.setVisibility(View.VISIBLE);
        } else {
            conflictWarningTextView.setVisibility(View.GONE);
        }
    }
    
    private void validateAndScheduleClass() {
        // Validate all inputs
        if (selectedDepartmentId == null || selectedDepartmentId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a department", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedLevel == null || selectedLevel.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a level", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedCourseId == null || selectedCourseId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a course", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedLecturerId == null || selectedLecturerId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a lecturer", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedClassroomId == null || selectedClassroomId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a classroom", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDayOfWeek == 0) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a day of week", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedClassType == null || selectedClassType.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a class type", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedStartTime == null) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a start time", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedEndTime == null) {
            Snackbar.make(findViewById(android.R.id.content), "Please select an end time", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        // Check if end time is after start time
        if (selectedEndTime.before(selectedStartTime)) {
            Snackbar.make(findViewById(android.R.id.content), "End time must be after start time", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        // Check for conflicts
        checkForConflicts();
        if (conflictWarningTextView.getVisibility() == View.VISIBLE) {
            Snackbar.make(findViewById(android.R.id.content), R.string.conflict_error, Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        // Create and save the timetable entry
        scheduleClass();
    }
    
    private void scheduleClass() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Get objects from maps
        Course course = courseMap.get(selectedCourseId);
        Lecturer lecturer = lecturerMap.get(selectedLecturerId);
        Classroom classroom = classroomMap.get(selectedClassroomId);
        Department department = departmentMap.get(selectedDepartmentId);
        
        // Create timetable entry
        TimetableEntry entry = new TimetableEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setCourseId(course.getId());
        entry.setCourseName(course.getName());
        entry.setCourseCode(course.getCode());
        entry.setLecturerId(lecturer.getId());
        entry.setLecturerName(lecturer.getFullName());
        entry.setClassroomId(classroom.getId());
        entry.setClassroomName(classroom.getFullName());
        entry.setDepartmentId(department.getId());
        entry.setDepartmentName(department.getName());
        entry.setLevel(selectedLevel);
        entry.setSemester(course.getSemester());
        entry.setDayOfWeek(selectedDayOfWeek);
        entry.setStartTime(selectedStartTime);
        entry.setEndTime(selectedEndTime);
        entry.setType(selectedClassType);
        entry.setLastModified(new Date());
        entry.setLastModifiedBy(FirebaseUtil.getCurrentUserId());
        
        // Save to Firestore
        FirebaseUtil.getTimetableCollection().document(entry.getId())
                .set(entry)
                .addOnSuccessListener(aVoid -> {
                    // Save to local database
                    databaseHelper.saveTimetableEntry(entry);
                    
                    // Add to timetable entries list
                    timetableEntries.add(entry);
                    
                    // Show success message
                    Snackbar.make(findViewById(android.R.id.content), "Class scheduled successfully", Snackbar.LENGTH_SHORT).show();
                    
                    // Reset form
                    resetForm();
                    
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(findViewById(android.R.id.content), "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void resetForm() {
        // Clear selections
        departmentSpinner.setText("", false);
        levelSpinner.setText("", false);
        courseSpinner.setText("", false);
        lecturerSpinner.setText("", false);
        classroomSpinner.setText("", false);
        dayOfWeekSpinner.setText("", false);
        classTypeSpinner.setText("", false);
        startTimeEditText.setText("");
        endTimeEditText.setText("");
        
        // Reset selected values
        selectedDepartmentId = null;
        selectedLevel = null;
        selectedCourseId = null;
        selectedLecturerId = null;
        selectedClassroomId = null;
        selectedDayOfWeek = 0;
        selectedClassType = null;
        selectedStartTime = null;
        selectedEndTime = null;
        
        // Hide conflict warning
        conflictWarningTextView.setVisibility(View.GONE);
    }
}
