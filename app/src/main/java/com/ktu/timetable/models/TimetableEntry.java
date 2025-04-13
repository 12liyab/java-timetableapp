package com.ktu.timetable.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a single entry in the timetable (a scheduled class)
 */
public class TimetableEntry implements Serializable {
    
    public static final String TYPE_LECTURE = "Lecture";
    public static final String TYPE_PRACTICAL = "Practical";
    public static final String TYPE_TUTORIAL = "Tutorial";
    
    private String id;
    private String courseId;
    private String courseName;
    private String courseCode;
    private String lecturerId;
    private String lecturerName;
    private String classroomId;
    private String classroomName;
    private String departmentId;
    private String departmentName;
    private String level; // e.g., "100", "200", "300"
    private String semester; // e.g., "1", "2"
    private int dayOfWeek; // 1 = Monday, 2 = Tuesday, etc.
    private Date startTime;
    private Date endTime;
    private String type; // Lecture, Practical, Tutorial
    private Date lastModified;
    private String lastModifiedBy; // User ID of last person to modify
    
    // Default constructor required for Firestore
    public TimetableEntry() {
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
    
    public String getLecturerId() {
        return lecturerId;
    }
    
    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId;
    }
    
    public String getLecturerName() {
        return lecturerName;
    }
    
    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }
    
    public String getClassroomId() {
        return classroomId;
    }
    
    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }
    
    public String getClassroomName() {
        return classroomName;
    }
    
    public void setClassroomName(String classroomName) {
        this.classroomName = classroomName;
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
    
    public int getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Date getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
    
    /**
     * Check if this timetable entry conflicts with another one
     * @param other The other timetable entry to check against
     * @return true if there is a conflict, false otherwise
     */
    public boolean conflictsWith(TimetableEntry other) {
        // Different days, no conflict
        if (this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        
        // Check for time overlap
        return !(this.endTime.before(other.startTime) || this.startTime.after(other.endTime));
    }
    
    /**
     * Check if the lecturer has a conflict with this timetable entry
     * @param other The other timetable entry to check against
     * @return true if there is a lecturer conflict, false otherwise
     */
    public boolean hasLecturerConflict(TimetableEntry other) {
        // Different lecturers, no conflict
        if (!this.lecturerId.equals(other.lecturerId)) {
            return false;
        }
        
        // Same lecturer, check for time conflict
        return conflictsWith(other);
    }
    
    /**
     * Check if the classroom has a conflict with this timetable entry
     * @param other The other timetable entry to check against
     * @return true if there is a classroom conflict, false otherwise
     */
    public boolean hasClassroomConflict(TimetableEntry other) {
        // Different classrooms, no conflict
        if (!this.classroomId.equals(other.classroomId)) {
            return false;
        }
        
        // Same classroom, check for time conflict
        return conflictsWith(other);
    }
    
    @Override
    public String toString() {
        return courseCode + " - " + type;
    }
}
