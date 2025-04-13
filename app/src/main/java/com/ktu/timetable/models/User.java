package com.ktu.timetable.models;

import java.io.Serializable;

/**
 * Represents a user of the timetable system with role-based access control
 */
public class User implements Serializable {
    
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_LECTURER = "lecturer";
    public static final String ROLE_STUDENT = "student";
    
    private String uid;
    private String email;
    private String displayName;
    private String role;
    private String departmentId;
    private String level; // For students only
    private String staffId; // For lecturers and admins
    
    // Default constructor required for Firestore
    public User() {
    }
    
    public User(String uid, String email, String displayName, String role) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }
    
    // Getters and Setters
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getStaffId() {
        return staffId;
    }
    
    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }
    
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
    
    public boolean isLecturer() {
        return ROLE_LECTURER.equals(role);
    }
    
    public boolean isStudent() {
        return ROLE_STUDENT.equals(role);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
