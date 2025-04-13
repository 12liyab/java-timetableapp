package com.ktu.timetable.models;

import java.io.Serializable;

/**
 * Represents a physical classroom or lab where classes are held
 */
public class Classroom implements Serializable {
    
    public static final String TYPE_LECTURE_HALL = "Lecture Hall";
    public static final String TYPE_LAB = "Laboratory";
    public static final String TYPE_WORKSHOP = "Workshop";
    public static final String TYPE_SEMINAR_ROOM = "Seminar Room";
    
    private String id;
    private String name;
    private String buildingName;
    private String floor;
    private String roomNumber;
    private int capacity;
    private String type;
    private boolean hasProjector;
    private boolean hasAirCondition;
    private boolean hasComputers;
    private String notes;
    
    // Default constructor required for Firestore
    public Classroom() {
    }
    
    public Classroom(String id, String name, String buildingName, int capacity) {
        this.id = id;
        this.name = name;
        this.buildingName = buildingName;
        this.capacity = capacity;
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
    
    public String getBuildingName() {
        return buildingName;
    }
    
    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }
    
    public String getFloor() {
        return floor;
    }
    
    public void setFloor(String floor) {
        this.floor = floor;
    }
    
    public String getRoomNumber() {
        return roomNumber;
    }
    
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isHasProjector() {
        return hasProjector;
    }
    
    public void setHasProjector(boolean hasProjector) {
        this.hasProjector = hasProjector;
    }
    
    public boolean isHasAirCondition() {
        return hasAirCondition;
    }
    
    public void setHasAirCondition(boolean hasAirCondition) {
        this.hasAirCondition = hasAirCondition;
    }
    
    public boolean isHasComputers() {
        return hasComputers;
    }
    
    public void setHasComputers(boolean hasComputers) {
        this.hasComputers = hasComputers;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        
        if (buildingName != null && !buildingName.isEmpty()) {
            fullName.append(buildingName);
            
            if (roomNumber != null && !roomNumber.isEmpty()) {
                fullName.append(" - ").append(roomNumber);
            }
        } else {
            fullName.append(name);
        }
        
        return fullName.toString();
    }
    
    @Override
    public String toString() {
        return getFullName();
    }
}
