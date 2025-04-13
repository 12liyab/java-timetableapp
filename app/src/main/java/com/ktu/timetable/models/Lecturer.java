package com.ktu.timetable.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a lecturer who teaches courses at the university
 */
public class Lecturer implements Serializable {
    
    private String id;
    private String userId;
    private String staffId;
    private String title;  // Prof., Dr., Mr., Mrs., etc.
    private String firstName;
    private String lastName;
    private String departmentId;
    private String departmentName;
    private String email;
    private String phoneNumber;
    private List<String> specializations;
    private List<String> courseIds;
    
    // Default constructor required for Firestore
    public Lecturer() {
        specializations = new ArrayList<>();
        courseIds = new ArrayList<>();
    }
    
    public Lecturer(String id, String userId, String staffId, String firstName, String lastName) {
        this.id = id;
        this.userId = userId;
        this.staffId = staffId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.specializations = new ArrayList<>();
        this.courseIds = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getStaffId() {
        return staffId;
    }
    
    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getFullName() {
        String fullName = "";
        if (title != null && !title.isEmpty()) {
            fullName += title + " ";
        }
        if (firstName != null) {
            fullName += firstName + " ";
        }
        if (lastName != null) {
            fullName += lastName;
        }
        return fullName.trim();
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public List<String> getSpecializations() {
        return specializations;
    }
    
    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations;
    }
    
    public void addSpecialization(String specialization) {
        if (specializations == null) {
            specializations = new ArrayList<>();
        }
        specializations.add(specialization);
    }
    
    public List<String> getCourseIds() {
        return courseIds;
    }
    
    public void setCourseIds(List<String> courseIds) {
        this.courseIds = courseIds;
    }
    
    public void addCourseId(String courseId) {
        if (courseIds == null) {
            courseIds = new ArrayList<>();
        }
        if (!courseIds.contains(courseId)) {
            courseIds.add(courseId);
        }
    }
    
    public void removeCourseId(String courseId) {
        if (courseIds != null) {
            courseIds.remove(courseId);
        }
    }
    
    @Override
    public String toString() {
        return getFullName();
    }
}
