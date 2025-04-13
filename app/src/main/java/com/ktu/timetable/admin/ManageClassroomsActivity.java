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
import com.google.firebase.firestore.DocumentSnapshot;
import com.ktu.timetable.R;
import com.ktu.timetable.adapters.ClassroomAdapter;
import com.ktu.timetable.models.Classroom;
import com.ktu.timetable.utils.DatabaseHelper;
import com.ktu.timetable.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManageClassroomsActivity extends AppCompatActivity implements ClassroomAdapter.ClassroomClickListener {

    private RecyclerView classroomsRecyclerView;
    private ClassroomAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private FloatingActionButton addClassroomFab;
    
    private List<Classroom> allClassrooms;
    private List<Classroom> filteredClassrooms;
    
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_classrooms);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.manage_classrooms);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize UI components
        classroomsRecyclerView = findViewById(R.id.classroomsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchEditText = findViewById(R.id.searchEditText);
        addClassroomFab = findViewById(R.id.addClassroomFab);

        // Initialize data lists
        allClassrooms = new ArrayList<>();
        filteredClassrooms = new ArrayList<>();

        // Setup RecyclerView
        classroomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassroomAdapter(filteredClassrooms, this);
        classroomsRecyclerView.setAdapter(adapter);

        // Set listeners
        addClassroomFab.setOnClickListener(v -> showAddEditClassroomDialog(null));
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClassrooms(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Load data
        loadClassrooms();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadClassrooms() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getClassroomsCollection()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allClassrooms.clear();
                    filteredClassrooms.clear();

                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Classroom classroom = documentSnapshot.toObject(Classroom.class);
                        if (classroom != null) {
                            allClassrooms.add(classroom);
                            filteredClassrooms.add(classroom);
                            
                            // Save to local database
                            databaseHelper.saveClassroom(classroom);
                        }
                    }

                    // Update sync timestamp
                    databaseHelper.updateSyncTime(FirebaseUtil.CLASSROOMS_COLLECTION);
                    
                    // Update UI
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    // Try to load from local database
                    allClassrooms.addAll(databaseHelper.getAllClassrooms());
                    filteredClassrooms.addAll(allClassrooms);
                    
                    // Update UI
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                    
                    Snackbar.make(classroomsRecyclerView, R.string.network_error, Snackbar.LENGTH_LONG).show();
                });
    }

    private void filterClassrooms(String query) {
        filteredClassrooms.clear();
        
        if (query.isEmpty()) {
            filteredClassrooms.addAll(allClassrooms);
        } else {
            query = query.toLowerCase();
            for (Classroom classroom : allClassrooms) {
                if (classroom.getName().toLowerCase().contains(query) ||
                    (classroom.getBuildingName() != null && classroom.getBuildingName().toLowerCase().contains(query)) ||
                    (classroom.getRoomNumber() != null && classroom.getRoomNumber().toLowerCase().contains(query))) {
                    filteredClassrooms.add(classroom);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredClassrooms.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            classroomsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            classroomsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddEditClassroomDialog(Classroom classroom) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_classroom, null);
        builder.setView(dialogView);

        // Dialog title
        TextView dialogTitleTextView = dialogView.findViewById(R.id.dialogTitleTextView);
        dialogTitleTextView.setText(classroom == null ? "Add Classroom" : "Edit Classroom");

        // Initialize form fields
        TextInputEditText classroomNameEditText = dialogView.findViewById(R.id.classroomNameEditText);
        TextInputEditText buildingNameEditText = dialogView.findViewById(R.id.buildingNameEditText);
        TextInputEditText floorEditText = dialogView.findViewById(R.id.floorEditText);
        TextInputEditText roomNumberEditText = dialogView.findViewById(R.id.roomNumberEditText);
        AutoCompleteTextView classroomTypeSpinner = dialogView.findViewById(R.id.classroomTypeSpinner);
        TextInputEditText capacityEditText = dialogView.findViewById(R.id.capacityEditText);
        Switch projectorSwitch = dialogView.findViewById(R.id.projectorSwitch);
        Switch acSwitch = dialogView.findViewById(R.id.acSwitch);
        Switch computersSwitch = dialogView.findViewById(R.id.computersSwitch);
        TextInputEditText notesEditText = dialogView.findViewById(R.id.notesEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Setup classroom type spinner
        String[] types = {
                Classroom.TYPE_LECTURE_HALL, 
                Classroom.TYPE_LAB, 
                Classroom.TYPE_WORKSHOP, 
                Classroom.TYPE_SEMINAR_ROOM
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, types);
        classroomTypeSpinner.setAdapter(typeAdapter);

        // Populate dialog with classroom data if editing
        if (classroom != null) {
            classroomNameEditText.setText(classroom.getName());
            buildingNameEditText.setText(classroom.getBuildingName());
            floorEditText.setText(classroom.getFloor());
            roomNumberEditText.setText(classroom.getRoomNumber());
            classroomTypeSpinner.setText(classroom.getType(), false);
            capacityEditText.setText(String.valueOf(classroom.getCapacity()));
            projectorSwitch.setChecked(classroom.isHasProjector());
            acSwitch.setChecked(classroom.isHasAirCondition());
            computersSwitch.setChecked(classroom.isHasComputers());
            notesEditText.setText(classroom.getNotes());
        }

        AlertDialog dialog = builder.create();

        // Set button listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        saveButton.setOnClickListener(v -> {
            // Validate input
            String name = classroomNameEditText.getText().toString().trim();
            String buildingName = buildingNameEditText.getText().toString().trim();
            String floor = floorEditText.getText().toString().trim();
            String roomNumber = roomNumberEditText.getText().toString().trim();
            String type = classroomTypeSpinner.getText().toString().trim();
            String capacityStr = capacityEditText.getText().toString().trim();
            boolean hasProjector = projectorSwitch.isChecked();
            boolean hasAC = acSwitch.isChecked();
            boolean hasComputers = computersSwitch.isChecked();
            String notes = notesEditText.getText().toString().trim();
            
            if (name.isEmpty()) {
                classroomNameEditText.setError("Classroom name is required");
                return;
            }
            
            if (buildingName.isEmpty()) {
                buildingNameEditText.setError("Building name is required");
                return;
            }
            
            if (roomNumber.isEmpty()) {
                roomNumberEditText.setError("Room number is required");
                return;
            }
            
            if (type.isEmpty()) {
                classroomTypeSpinner.setError("Classroom type is required");
                return;
            }
            
            if (capacityStr.isEmpty()) {
                capacityEditText.setError("Capacity is required");
                return;
            }
            
            int capacity;
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                capacityEditText.setError("Invalid capacity");
                return;
            }
            
            // Prepare classroom object
            final Classroom classroomToSave;
            if (classroom == null) {
                // New classroom
                classroomToSave = new Classroom();
                classroomToSave.setId(UUID.randomUUID().toString());
            } else {
                // Editing existing classroom
                classroomToSave = classroom;
            }
            
            classroomToSave.setName(name);
            classroomToSave.setBuildingName(buildingName);
            classroomToSave.setFloor(floor);
            classroomToSave.setRoomNumber(roomNumber);
            classroomToSave.setType(type);
            classroomToSave.setCapacity(capacity);
            classroomToSave.setHasProjector(hasProjector);
            classroomToSave.setHasAirCondition(hasAC);
            classroomToSave.setHasComputers(hasComputers);
            classroomToSave.setNotes(notes);
            
            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);
            
            // Save to Firestore
            FirebaseUtil.getClassroomsCollection().document(classroomToSave.getId())
                    .set(classroomToSave)
                    .addOnSuccessListener(aVoid -> {
                        // Save to local database
                        databaseHelper.saveClassroom(classroomToSave);
                        
                        if (classroom == null) {
                            // New classroom
                            allClassrooms.add(classroomToSave);
                            filteredClassrooms.add(classroomToSave);
                            adapter.notifyItemInserted(filteredClassrooms.indexOf(classroomToSave));
                            Snackbar.make(classroomsRecyclerView, R.string.add_success, Snackbar.LENGTH_SHORT).show();
                        } else {
                            // Update existing classroom
                            int position = filteredClassrooms.indexOf(classroom);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }
                            Snackbar.make(classroomsRecyclerView, R.string.update_success, Snackbar.LENGTH_SHORT).show();
                        }
                        
                        updateEmptyView();
                        progressBar.setVisibility(View.GONE);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Snackbar.make(classroomsRecyclerView, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void deleteClassroom(Classroom classroom) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Classroom")
                .setMessage("Are you sure you want to delete this classroom? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    
                    FirebaseUtil.getClassroomsCollection().document(classroom.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from local database and lists
                                allClassrooms.remove(classroom);
                                int position = filteredClassrooms.indexOf(classroom);
                                if (position != -1) {
                                    filteredClassrooms.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }
                                
                                updateEmptyView();
                                progressBar.setVisibility(View.GONE);
                                Snackbar.make(classroomsRecyclerView, R.string.delete_success, Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Snackbar.make(classroomsRecyclerView, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Classroom classroom) {
        showAddEditClassroomDialog(classroom);
    }

    @Override
    public void onDeleteClick(Classroom classroom) {
        deleteClassroom(classroom);
    }
}
