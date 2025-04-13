package com.ktu.timetable.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ktu.timetable.models.User;

/**
 * Utility class for Firebase operations
 */
public class FirebaseUtil {
    
    private static final String TAG = "FirebaseUtil";
    
    // Collection names
    public static final String USERS_COLLECTION = "users";
    public static final String DEPARTMENTS_COLLECTION = "departments";
    public static final String COURSES_COLLECTION = "courses";
    public static final String LECTURERS_COLLECTION = "lecturers";
    public static final String CLASSROOMS_COLLECTION = "classrooms";
    public static final String TIMETABLE_COLLECTION = "timetable";
    public static final String NOTIFICATIONS_COLLECTION = "notifications";
    
    // Firebase instances
    private static FirebaseAuth auth;
    private static FirebaseFirestore firestore;
    
    /**
     * Get the FirebaseAuth instance
     * @return FirebaseAuth instance
     */
    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }
    
    /**
     * Get the FirebaseFirestore instance
     * @return FirebaseFirestore instance
     */
    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }
    
    /**
     * Get the current authenticated user
     * @return FirebaseUser instance or null if not authenticated
     */
    public static FirebaseUser getCurrentUser() {
        return getAuth().getCurrentUser();
    }
    
    /**
     * Check if a user is logged in
     * @return true if a user is logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
    
    /**
     * Get the current user's ID
     * @return User ID or null if not authenticated
     */
    public static String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Get the users collection reference
     * @return CollectionReference for users
     */
    public static CollectionReference getUsersCollection() {
        return getFirestore().collection(USERS_COLLECTION);
    }
    
    /**
     * Get the departments collection reference
     * @return CollectionReference for departments
     */
    public static CollectionReference getDepartmentsCollection() {
        return getFirestore().collection(DEPARTMENTS_COLLECTION);
    }
    
    /**
     * Get the courses collection reference
     * @return CollectionReference for courses
     */
    public static CollectionReference getCoursesCollection() {
        return getFirestore().collection(COURSES_COLLECTION);
    }
    
    /**
     * Get the lecturers collection reference
     * @return CollectionReference for lecturers
     */
    public static CollectionReference getLecturersCollection() {
        return getFirestore().collection(LECTURERS_COLLECTION);
    }
    
    /**
     * Get the classrooms collection reference
     * @return CollectionReference for classrooms
     */
    public static CollectionReference getClassroomsCollection() {
        return getFirestore().collection(CLASSROOMS_COLLECTION);
    }
    
    /**
     * Get the timetable collection reference
     * @return CollectionReference for timetable
     */
    public static CollectionReference getTimetableCollection() {
        return getFirestore().collection(TIMETABLE_COLLECTION);
    }
    
    /**
     * Get the notifications collection reference
     * @return CollectionReference for notifications
     */
    public static CollectionReference getNotificationsCollection() {
        return getFirestore().collection(NOTIFICATIONS_COLLECTION);
    }
    
    /**
     * Get the current user's document reference
     * @return DocumentReference for the current user or null if not authenticated
     */
    public static DocumentReference getCurrentUserDocument() {
        String userId = getCurrentUserId();
        return userId != null ? getUsersCollection().document(userId) : null;
    }
    
    /**
     * Get timetable entries for a specific department and level
     * @param departmentId Department ID
     * @param level Student level (e.g., "100", "200", etc.)
     * @return Query for the timetable entries
     */
    public static Query getTimetableByDepartmentAndLevel(String departmentId, String level) {
        return getTimetableCollection()
                .whereEqualTo("departmentId", departmentId)
                .whereEqualTo("level", level);
    }
    
    /**
     * Get timetable entries for a specific lecturer
     * @param lecturerId Lecturer ID
     * @return Query for the timetable entries
     */
    public static Query getTimetableByLecturer(String lecturerId) {
        return getTimetableCollection()
                .whereEqualTo("lecturerId", lecturerId);
    }
    
    /**
     * Get timetable entries for a specific classroom
     * @param classroomId Classroom ID
     * @return Query for the timetable entries
     */
    public static Query getTimetableByClassroom(String classroomId) {
        return getTimetableCollection()
                .whereEqualTo("classroomId", classroomId);
    }
    
    /**
     * Subscribe to FCM topic for notifications
     * @param topic Topic to subscribe to
     */
    public static void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Failed to subscribe to topic: " + topic, task.getException());
                        } else {
                            Log.d(TAG, "Subscribed to topic: " + topic);
                        }
                    }
                });
    }
    
    /**
     * Unsubscribe from FCM topic
     * @param topic Topic to unsubscribe from
     */
    public static void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Failed to unsubscribe from topic: " + topic, task.getException());
                        } else {
                            Log.d(TAG, "Unsubscribed from topic: " + topic);
                        }
                    }
                });
    }
    
    /**
     * Subscribe to department and level topics for notifications
     * @param departmentId Department ID
     * @param level Student level
     */
    public static void subscribeToDepartmentAndLevel(String departmentId, String level) {
        if (departmentId != null) {
            subscribeToTopic("department_" + departmentId);
            
            if (level != null) {
                subscribeToTopic("department_" + departmentId + "_level_" + level);
            }
        }
    }
    
    /**
     * Subscribe to lecturer topic for notifications
     * @param lecturerId Lecturer ID
     */
    public static void subscribeToLecturer(String lecturerId) {
        if (lecturerId != null) {
            subscribeToTopic("lecturer_" + lecturerId);
        }
    }
}
