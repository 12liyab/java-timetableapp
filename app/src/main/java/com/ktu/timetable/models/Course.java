package com.ktu.timetable.models;

import java.io.Serializable;

/**
 * Represents an academic course offered by the university
 */
public class Course implements Serializable {
    
    private String id;
    private String code;
    private String name;
    private String departmentId;
    private String departmentName;
    private int creditHours;
    private String level; // e.g., "100", "200", "300", etc.
    private String semester; // e.g., "1", "2"
    private boolean isElective;
    private String description;
    
    // Default constructor required for Firestore
    public Course() {
    }
    
    public Course(String id, String code, String name, String departmentId, int creditHours) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.creditHours = creditHours;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    public String getDepartmentName() {
        return departmentName;
    }
    
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
    
    public int getCreditHours() {
        return creditHours;
    }
    
    public void setCreditHours(int creditHours) {
        this.creditHours = creditHours;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    public boolean isElective() {
        return isElective;
    }
    
    public void setElective(boolean elective) {
        isElective = elective;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return code + " - " + name;
    }
}
