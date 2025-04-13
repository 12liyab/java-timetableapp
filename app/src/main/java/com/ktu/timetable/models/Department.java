package com.ktu.timetable.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an academic department within the university
 */
public class Department implements Serializable {
    
    private String id;
    private String name;
    private String code;
    private String facultyId;
    private String facultyName;
    private List<String> programs;
    private String hodId; // Head of Department's user ID
    
    // Default constructor required for Firestore
    public Department() {
        programs = new ArrayList<>();
    }
    
    public Department(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.programs = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getFacultyId() {
        return facultyId;
    }
    
    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }
    
    public String getFacultyName() {
        return facultyName;
    }
    
    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }
    
    public List<String> getPrograms() {
        return programs;
    }
    
    public void setPrograms(List<String> programs) {
        this.programs = programs;
    }
    
    public void addProgram(String program) {
        if (programs == null) {
            programs = new ArrayList<>();
        }
        programs.add(program);
    }
    
    public String getHodId() {
        return hodId;
    }
    
    public void setHodId(String hodId) {
        this.hodId = hodId;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
