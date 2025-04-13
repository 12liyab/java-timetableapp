package com.ktu.timetable.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ktu.timetable.MainActivity;
import com.ktu.timetable.R;

/**
 * Helper class for creating and showing notifications
 */
public class NotificationHelper {
    
    public static final String CHANNEL_ID_TIMETABLE_UPDATES = "channel_timetable_updates";
    public static final String CHANNEL_ID_REMINDERS = "channel_reminders";
    
    private static final int NOTIFICATION_ID_TIMETABLE_UPDATE = 1001;
    private static final int NOTIFICATION_ID_CLASS_REMINDER = 2001;
    
    /**
     * Create notification channels for the app
     * @param context Application context
     */
    public static void createNotificationChannels(Context context) {
        // Only needed for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            
            // Timetable updates channel
            NotificationChannel updatesChannel = new NotificationChannel(
                    CHANNEL_ID_TIMETABLE_UPDATES,
                    "Timetable Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            updatesChannel.setDescription("Notifications for timetable changes");
            
            // Reminders channel
            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Class Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            remindersChannel.setDescription("Reminders for upcoming classes");
            
            // Register the channels
            notificationManager.createNotificationChannel(updatesChannel);
            notificationManager.createNotificationChannel(remindersChannel);
        }
    }
    
    /**
     * Show notification for timetable update
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     */
    public static void showTimetableUpdateNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_TIMETABLE_UPDATES)
                .setSmallIcon(R.drawable.ktu_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID_TIMETABLE_UPDATE, builder.build());
        } catch (SecurityException e) {
            // Permission not granted
            e.printStackTrace();
        }
    }
    
    /**
     * Show notification for class reminder
     * @param context Application context
     * @param courseCode Course code
     * @param courseName Course name
     * @param classroomName Classroom name
     * @param startTime Start time
     */
    public static void showClassReminderNotification(
            Context context, 
            String courseCode, 
            String courseName, 
            String classroomName, 
            String startTime) {
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
        );
        
        String title = "Upcoming Class: " + courseCode;
        String message = courseName + " at " + classroomName + ", " + startTime;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
                .setSmallIcon(R.drawable.ktu_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID_CLASS_REMINDER, builder.build());
        } catch (SecurityException e) {
            // Permission not granted
            e.printStackTrace();
        }
    }
}
