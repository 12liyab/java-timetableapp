package com.ktu.timetable.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ktu.timetable.models.Classroom;
import com.ktu.timetable.models.Course;
import com.ktu.timetable.models.Department;
import com.ktu.timetable.models.Lecturer;
import com.ktu.timetable.models.TimetableEntry;
import com.ktu.timetable.models.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite database helper for offline storage
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "DatabaseHelper";
    
    private static final String DATABASE_NAME = "ktu_timetable.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_DEPARTMENTS = "departments";
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_LECTURERS = "lecturers";
    private static final String TABLE_CLASSROOMS = "classrooms";
    private static final String TABLE_TIMETABLE = "timetable";
    private static final String TABLE_SYNC_INFO = "sync_info";
    
    // Common columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    
    // Date format for storing dates
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    
    private static DatabaseHelper instance;
    
    /**
     * Get singleton instance of DatabaseHelper
     * @param context Application context
     * @return DatabaseHelper instance
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        db.execSQL(
                "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                "email TEXT, " +
                "display_name TEXT, " +
                "role TEXT, " +
                "department_id TEXT, " +
                "level TEXT, " +
                "staff_id TEXT, " +
                COLUMN_TIMESTAMP + " TEXT" +
                ")"
        );
        
        // Create departments table
        db.execSQL(
                "CREATE TABLE " + TABLE_DEPARTMENTS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                "code TEXT, " +
                "faculty_id TEXT, " +
                "faculty_name TEXT, " +
                "hod_id TEXT, " +
                COLUMN_TIMESTAMP + " TEXT" +
                ")"
        );
        
        // Create courses table
        db.execSQL(
                "CREATE TABLE " + TABLE_COURSES + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                "code TEXT, " +
                COLUMN_NAME + " TEXT, " +
                "department_id TEXT, " +
                "department_name TEXT, " +
                "credit_hours INTEGER, " +
                "level TEXT, " +
                "semester TEXT, " +
                "is_elective INTEGER, " +
                "description TEXT, " +
                COLUMN_TIMESTAMP + " TEXT" +
                ")"
        );
        
        // Create lecturers table
        db.execSQL(
                "CREATE TABLE " + TABLE_LECTURERS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "staff_id TEXT, " +
                "title TEXT, " +
                "first_name TEXT, " +
                "last_name TEXT, " +
                "department_id TEXT, " +
                "department_name TEXT, " +
                "email TEXT, " +
                "phone_number TEXT, " +
                COLUMN_TIMESTAMP + " TEXT" +
                ")"
        );
        
        // Create classrooms table
        db.execSQL(
                "CREATE TABLE " + TABLE_CLASSROOMS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                "building_name TEXT, " +
                "floor TEXT, " +
                "room_number TEXT, " +
                "capacity INTEGER, " +
                "type TEXT, " +
                "has_projector INTEGER, " +
                "has_air_condition INTEGER, " +
                "has_computers INTEGER, " +
                "notes TEXT, " +
                COLUMN_TIMESTAMP + " TEXT" +
                ")"
        );
        
        // Create timetable table
        db.execSQL(
                "CREATE TABLE " + TABLE_TIMETABLE + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                "course_id TEXT, " +
                "course_name TEXT, " +
                "course_code TEXT, " +
                "lecturer_id TEXT, " +
                "lecturer_name TEXT, " +
                "classroom_id TEXT, " +
                "classroom_name TEXT, " +
                "department_id TEXT, " +
                "department_name TEXT, " +
                "level TEXT, " +
                "semester TEXT, " +
                "day_of_week INTEGER, " +
                "start_time TEXT, " +
                "end_time TEXT, " +
                "type TEXT, " +
                "last_modified TEXT, " +
                "last_modified_by TEXT, " +
                COLUMN_TIMESTAMP + " TEXT" +
                ")"
        );
        
        // Create sync info table
        db.execSQL(
                "CREATE TABLE " + TABLE_SYNC_INFO + " (" +
                "collection_name TEXT PRIMARY KEY, " +
                "last_sync_time TEXT" +
                ")"
        );
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop and recreate all tables on upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEPARTMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LECTURERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSROOMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIMETABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SYNC_INFO);
        
        onCreate(db);
    }
    
    /**
     * Save user to database
     * @param user User to save
     * @return true if successful, false otherwise
     */
    public boolean saveUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ID, user.getUid());
        values.put("email", user.getEmail());
        values.put("display_name", user.getDisplayName());
        values.put("role", user.getRole());
        values.put("department_id", user.getDepartmentId());
        values.put("level", user.getLevel());
        values.put("staff_id", user.getStaffId());
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        
        long result = db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
    
    /**
     * Get user by ID
     * @param userId User ID
     * @return User object or null if not found
     */
    public User getUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                COLUMN_ID + " = ?",
                new String[]{userId},
                null,
                null,
                null
        );
        
        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setUid(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
            user.setEmail(cursor.getString(cursor.getColumnIndex("email")));
            user.setDisplayName(cursor.getString(cursor.getColumnIndex("display_name")));
            user.setRole(cursor.getString(cursor.getColumnIndex("role")));
            user.setDepartmentId(cursor.getString(cursor.getColumnIndex("department_id")));
            user.setLevel(cursor.getString(cursor.getColumnIndex("level")));
            user.setStaffId(cursor.getString(cursor.getColumnIndex("staff_id")));
        }
        
        cursor.close();
        return user;
    }
    
    /**
     * Save department to database
     * @param department Department to save
     * @return true if successful, false otherwise
     */
    public boolean saveDepartment(Department department) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ID, department.getId());
        values.put(COLUMN_NAME, department.getName());
        values.put("code", department.getCode());
        values.put("faculty_id", department.getFacultyId());
        values.put("faculty_name", department.getFacultyName());
        values.put("hod_id", department.getHodId());
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        
        long result = db.insertWithOnConflict(TABLE_DEPARTMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
    
    /**
     * Get all departments from database
     * @return List of departments
     */
    public List<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_DEPARTMENTS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_NAME + " ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                Department department = new Department();
                department.setId(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
                department.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
                department.setCode(cursor.getString(cursor.getColumnIndex("code")));
                department.setFacultyId(cursor.getString(cursor.getColumnIndex("faculty_id")));
                department.setFacultyName(cursor.getString(cursor.getColumnIndex("faculty_name")));
                department.setHodId(cursor.getString(cursor.getColumnIndex("hod_id")));
                
                departments.add(department);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return departments;
    }
    
    /**
     * Save course to database
     * @param course Course to save
     * @return true if successful, false otherwise
     */
    public boolean saveCourse(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ID, course.getId());
        values.put("code", course.getCode());
        values.put(COLUMN_NAME, course.getName());
        values.put("department_id", course.getDepartmentId());
        values.put("department_name", course.getDepartmentName());
        values.put("credit_hours", course.getCreditHours());
        values.put("level", course.getLevel());
        values.put("semester", course.getSemester());
        values.put("is_elective", course.isElective() ? 1 : 0);
        values.put("description", course.getDescription());
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        
        long result = db.insertWithOnConflict(TABLE_COURSES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
    
    /**
     * Get all courses from database
     * @return List of courses
     */
    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_COURSES,
                null,
                null,
                null,
                null,
                null,
                "code ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                Course course = new Course();
                course.setId(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
                course.setCode(cursor.getString(cursor.getColumnIndex("code")));
                course.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
                course.setDepartmentId(cursor.getString(cursor.getColumnIndex("department_id")));
                course.setDepartmentName(cursor.getString(cursor.getColumnIndex("department_name")));
                course.setCreditHours(cursor.getInt(cursor.getColumnIndex("credit_hours")));
                course.setLevel(cursor.getString(cursor.getColumnIndex("level")));
                course.setSemester(cursor.getString(cursor.getColumnIndex("semester")));
                course.setElective(cursor.getInt(cursor.getColumnIndex("is_elective")) == 1);
                course.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                
                courses.add(course);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return courses;
    }
    
    /**
     * Get courses by department ID
     * @param departmentId Department ID
     * @return List of courses
     */
    public List<Course> getCoursesByDepartment(String departmentId) {
        List<Course> courses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_COURSES,
                null,
                "department_id = ?",
                new String[]{departmentId},
                null,
                null,
                "code ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                Course course = new Course();
                course.setId(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
                course.setCode(cursor.getString(cursor.getColumnIndex("code")));
                course.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
                course.setDepartmentId(cursor.getString(cursor.getColumnIndex("department_id")));
                course.setDepartmentName(cursor.getString(cursor.getColumnIndex("department_name")));
                course.setCreditHours(cursor.getInt(cursor.getColumnIndex("credit_hours")));
                course.setLevel(cursor.getString(cursor.getColumnIndex("level")));
                course.setSemester(cursor.getString(cursor.getColumnIndex("semester")));
                course.setElective(cursor.getInt(cursor.getColumnIndex("is_elective")) == 1);
                course.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                
                courses.add(course);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return courses;
    }
    
    /**
     * Save lecturer to database
     * @param lecturer Lecturer to save
     * @return true if successful, false otherwise
     */
    public boolean saveLecturer(Lecturer lecturer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ID, lecturer.getId());
        values.put("user_id", lecturer.getUserId());
        values.put("staff_id", lecturer.getStaffId());
        values.put("title", lecturer.getTitle());
        values.put("first_name", lecturer.getFirstName());
        values.put("last_name", lecturer.getLastName());
        values.put("department_id", lecturer.getDepartmentId());
        values.put("department_name", lecturer.getDepartmentName());
        values.put("email", lecturer.getEmail());
        values.put("phone_number", lecturer.getPhoneNumber());
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        
        long result = db.insertWithOnConflict(TABLE_LECTURERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
    
    /**
     * Get all lecturers from database
     * @return List of lecturers
     */
    public List<Lecturer> getAllLecturers() {
        List<Lecturer> lecturers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_LECTURERS,
                null,
                null,
                null,
                null,
                null,
                "last_name ASC, first_name ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                Lecturer lecturer = new Lecturer();
                lecturer.setId(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
                lecturer.setUserId(cursor.getString(cursor.getColumnIndex("user_id")));
                lecturer.setStaffId(cursor.getString(cursor.getColumnIndex("staff_id")));
                lecturer.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                lecturer.setFirstName(cursor.getString(cursor.getColumnIndex("first_name")));
                lecturer.setLastName(cursor.getString(cursor.getColumnIndex("last_name")));
                lecturer.setDepartmentId(cursor.getString(cursor.getColumnIndex("department_id")));
                lecturer.setDepartmentName(cursor.getString(cursor.getColumnIndex("department_name")));
                lecturer.setEmail(cursor.getString(cursor.getColumnIndex("email")));
                lecturer.setPhoneNumber(cursor.getString(cursor.getColumnIndex("phone_number")));
                
                lecturers.add(lecturer);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return lecturers;
    }
    
    /**
     * Save classroom to database
     * @param classroom Classroom to save
     * @return true if successful, false otherwise
     */
    public boolean saveClassroom(Classroom classroom) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ID, classroom.getId());
        values.put(COLUMN_NAME, classroom.getName());
        values.put("building_name", classroom.getBuildingName());
        values.put("floor", classroom.getFloor());
        values.put("room_number", classroom.getRoomNumber());
        values.put("capacity", classroom.getCapacity());
        values.put("type", classroom.getType());
        values.put("has_projector", classroom.isHasProjector() ? 1 : 0);
        values.put("has_air_condition", classroom.isHasAirCondition() ? 1 : 0);
        values.put("has_computers", classroom.isHasComputers() ? 1 : 0);
        values.put("notes", classroom.getNotes());
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        
        long result = db.insertWithOnConflict(TABLE_CLASSROOMS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
    
    /**
     * Get all classrooms from database
     * @return List of classrooms
     */
    public List<Classroom> getAllClassrooms() {
        List<Classroom> classrooms = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_CLASSROOMS,
                null,
                null,
                null,
                null,
                null,
                "building_name ASC, room_number ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                Classroom classroom = new Classroom();
                classroom.setId(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
                classroom.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
                classroom.setBuildingName(cursor.getString(cursor.getColumnIndex("building_name")));
                classroom.setFloor(cursor.getString(cursor.getColumnIndex("floor")));
                classroom.setRoomNumber(cursor.getString(cursor.getColumnIndex("room_number")));
                classroom.setCapacity(cursor.getInt(cursor.getColumnIndex("capacity")));
                classroom.setType(cursor.getString(cursor.getColumnIndex("type")));
                classroom.setHasProjector(cursor.getInt(cursor.getColumnIndex("has_projector")) == 1);
                classroom.setHasAirCondition(cursor.getInt(cursor.getColumnIndex("has_air_condition")) == 1);
                classroom.setHasComputers(cursor.getInt(cursor.getColumnIndex("has_computers")) == 1);
                classroom.setNotes(cursor.getString(cursor.getColumnIndex("notes")));
                
                classrooms.add(classroom);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return classrooms;
    }
    
    /**
     * Save timetable entry to database
     * @param entry Timetable entry to save
     * @return true if successful, false otherwise
     */
    public boolean saveTimetableEntry(TimetableEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ID, entry.getId());
        values.put("course_id", entry.getCourseId());
        values.put("course_name", entry.getCourseName());
        values.put("course_code", entry.getCourseCode());
        values.put("lecturer_id", entry.getLecturerId());
        values.put("lecturer_name", entry.getLecturerName());
        values.put("classroom_id", entry.getClassroomId());
        values.put("classroom_name", entry.getClassroomName());
        values.put("department_id", entry.getDepartmentId());
        values.put("department_name", entry.getDepartmentName());
        values.put("level", entry.getLevel());
        values.put("semester", entry.getSemester());
        values.put("day_of_week", entry.getDayOfWeek());
        values.put("start_time", formatDate(entry.getStartTime()));
        values.put("end_time", formatDate(entry.getEndTime()));
        values.put("type", entry.getType());
        values.put("last_modified", formatDate(entry.getLastModified()));
        values.put("last_modified_by", entry.getLastModifiedBy());
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        
        long result = db.insertWithOnConflict(TABLE_TIMETABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
    
    /**
     * Get timetable entries by department and level
     * @param departmentId Department ID
     * @param level Student level
     * @return List of timetable entries
     */
    public List<TimetableEntry> getTimetableByDepartmentAndLevel(String departmentId, String level) {
        List<TimetableEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = "department_id = ? AND level = ?";
        String[] selectionArgs = {departmentId, level};
        
        Cursor cursor = db.query(
                TABLE_TIMETABLE,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "day_of_week ASC, start_time ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                TimetableEntry entry = cursorToTimetableEntry(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return entries;
    }
    
    /**
     * Get timetable entries by lecturer ID
     * @param lecturerId Lecturer ID
     * @return List of timetable entries
     */
    public List<TimetableEntry> getTimetableByLecturer(String lecturerId) {
        List<TimetableEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = "lecturer_id = ?";
        String[] selectionArgs = {lecturerId};
        
        Cursor cursor = db.query(
                TABLE_TIMETABLE,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "day_of_week ASC, start_time ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                TimetableEntry entry = cursorToTimetableEntry(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return entries;
    }
    
    /**
     * Get timetable entries by classroom ID
     * @param classroomId Classroom ID
     * @return List of timetable entries
     */
    public List<TimetableEntry> getTimetableByClassroom(String classroomId) {
        List<TimetableEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = "classroom_id = ?";
        String[] selectionArgs = {classroomId};
        
        Cursor cursor = db.query(
                TABLE_TIMETABLE,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "day_of_week ASC, start_time ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                TimetableEntry entry = cursorToTimetableEntry(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return entries;
    }
    
    /**
     * Get all timetable entries from database
     * @return List of timetable entries
     */
    public List<TimetableEntry> getAllTimetableEntries() {
        List<TimetableEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_TIMETABLE,
                null,
                null,
                null,
                null,
                null,
                "day_of_week ASC, start_time ASC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                TimetableEntry entry = cursorToTimetableEntry(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return entries;
    }
    
    /**
     * Update sync time for a collection
     * @param collectionName Collection name
     */
    public void updateSyncTime(String collectionName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put("collection_name", collectionName);
        values.put("last_sync_time", getCurrentTimestamp());
        
        db.insertWithOnConflict(TABLE_SYNC_INFO, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    /**
     * Get last sync time for a collection
     * @param collectionName Collection name
     * @return Last sync time or null if never synced
     */
    public String getLastSyncTime(String collectionName) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_SYNC_INFO,
                new String[]{"last_sync_time"},
                "collection_name = ?",
                new String[]{collectionName},
                null,
                null,
                null
        );
        
        String lastSyncTime = null;
        if (cursor.moveToFirst()) {
            lastSyncTime = cursor.getString(0);
        }
        
        cursor.close();
        return lastSyncTime;
    }
    
    /**
     * Delete all data from all tables
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_DEPARTMENTS, null, null);
        db.delete(TABLE_COURSES, null, null);
        db.delete(TABLE_LECTURERS, null, null);
        db.delete(TABLE_CLASSROOMS, null, null);
        db.delete(TABLE_TIMETABLE, null, null);
        db.delete(TABLE_SYNC_INFO, null, null);
    }
    
    /**
     * Convert cursor to TimetableEntry object
     * @param cursor Database cursor
     * @return TimetableEntry object
     */
    private TimetableEntry cursorToTimetableEntry(Cursor cursor) {
        TimetableEntry entry = new TimetableEntry();
        
        entry.setId(cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
        entry.setCourseId(cursor.getString(cursor.getColumnIndex("course_id")));
        entry.setCourseName(cursor.getString(cursor.getColumnIndex("course_name")));
        entry.setCourseCode(cursor.getString(cursor.getColumnIndex("course_code")));
        entry.setLecturerId(cursor.getString(cursor.getColumnIndex("lecturer_id")));
        entry.setLecturerName(cursor.getString(cursor.getColumnIndex("lecturer_name")));
        entry.setClassroomId(cursor.getString(cursor.getColumnIndex("classroom_id")));
        entry.setClassroomName(cursor.getString(cursor.getColumnIndex("classroom_name")));
        entry.setDepartmentId(cursor.getString(cursor.getColumnIndex("department_id")));
        entry.setDepartmentName(cursor.getString(cursor.getColumnIndex("department_name")));
        entry.setLevel(cursor.getString(cursor.getColumnIndex("level")));
        entry.setSemester(cursor.getString(cursor.getColumnIndex("semester")));
        entry.setDayOfWeek(cursor.getInt(cursor.getColumnIndex("day_of_week")));
        
        String startTimeStr = cursor.getString(cursor.getColumnIndex("start_time"));
        String endTimeStr = cursor.getString(cursor.getColumnIndex("end_time"));
        String lastModifiedStr = cursor.getString(cursor.getColumnIndex("last_modified"));
        
        try {
            if (startTimeStr != null) {
                entry.setStartTime(DATE_FORMAT.parse(startTimeStr));
            }
            if (endTimeStr != null) {
                entry.setEndTime(DATE_FORMAT.parse(endTimeStr));
            }
            if (lastModifiedStr != null) {
                entry.setLastModified(DATE_FORMAT.parse(lastModifiedStr));
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
        }
        
        entry.setType(cursor.getString(cursor.getColumnIndex("type")));
        entry.setLastModifiedBy(cursor.getString(cursor.getColumnIndex("last_modified_by")));
        
        return entry;
    }
    
    /**
     * Format date for database storage
     * @param date Date to format
     * @return Formatted date string
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }
    
    /**
     * Get current timestamp
     * @return Current timestamp string
     */
    private String getCurrentTimestamp() {
        return formatDate(new Date());
    }
}
