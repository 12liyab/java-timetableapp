package com.ktu.timetable.lecturer;

import android.os.Bundle;
import android.view.MenuItem; 
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.ktu.timetable.R;
import com.ktu.timetable.adapters.TimetableAdapter;
import com.ktu.timetable.models.TimetableEntry;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LecturerTimetableActivity extends AppCompatActivity {

    private RecyclerView timetableRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TabLayout tabLayout;
    
    private TimetableAdapter adapter;
    private List<TimetableEntry> allTimetableEntries;
    private List<TimetableEntry> filteredTimetableEntries;
    
    private DatabaseHelper databaseHelper;
    private String lecturerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_timetable);
        
        // Get lecturer ID from intent
        lecturerId = getIntent().getStringExtra("LECTURER_ID");
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Timetable");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Initialize UI elements
        timetableRecyclerView = findViewById(R.id.timetableRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        tabLayout = findViewById(R.id.tabLayout);
        
        // Setup RecyclerView
        timetableRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allTimetableEntries = new ArrayList<>();
        filteredTimetableEntries = new ArrayList<>();
        adapter = new TimetableAdapter(filteredTimetableEntries);
        timetableRecyclerView.setAdapter(adapter);
        
        // Setup tabs for days of the week
        setupTabs();
        
        // Load timetable data
        loadTimetableData();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
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
    
    private void loadTimetableData() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        if (lecturerId == null || lecturerId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Snackbar.make(findViewById(android.R.id.content), "Lecturer ID not found", Snackbar.LENGTH_LONG).show();
            return;
        }
        
        FirebaseUtil.getTimetableByLecturer(lecturerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTimetableEntries.clear();
                    
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        TimetableEntry entry = documentSnapshot.toObject(TimetableEntry.class);
                        if (entry != null) {
                            allTimetableEntries.add(entry);
                            
                            // Save to local database
                            databaseHelper.saveTimetableEntry(entry);
                        }
                    }
                    
                    // Update sync timestamp
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
                    allTimetableEntries.addAll(databaseHelper.getTimetableByLecturer(lecturerId));
                    
                    // Filter timetable by the current day tab
                    TabLayout.Tab selectedTab = tabLayout.getSelectedTab();
                    if (selectedTab != null) {
                        filterTimetableByDay(selectedTab.getPosition() + 1);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                    
                    Snackbar.make(findViewById(android.R.id.content), R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }
    
    private void filterTimetableByDay(int dayOfWeek) {
        filteredTimetableEntries.clear();
        
        for (TimetableEntry entry : allTimetableEntries) {
            if (entry.getDayOfWeek() == dayOfWeek) {
                filteredTimetableEntries.add(entry);
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
