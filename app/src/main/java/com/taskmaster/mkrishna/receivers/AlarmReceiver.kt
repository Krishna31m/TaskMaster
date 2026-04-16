package com.taskmaster.mkrishna.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.taskmaster.mkrishna.R
import com.taskmaster.mkrishna.activities.AlarmRingActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "taskmaster_alarm_channel"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_IS_TASK = "is_task"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action = $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            return
        }

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId == null) {
            return
        }

        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val isTask = intent.getBooleanExtra(EXTRA_IS_TASK, false)

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_ALARM_LABEL, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        createNotificationChannel(context)

        // Important: Use FLAG_IMMUTABLE and ensure request code is unique
        val pendingIntent = PendingIntent.getActivity(
            context, 
            alarmId.hashCode(), 
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(if (isTask) "Task Reminder" else "⏰ Alarm")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false) // User must dismiss via activity
            .setFullScreenIntent(pendingIntent, true) // This is the key for physical devices
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId.hashCode(), notification)

        // Force start activity as a fallback for background starts
        try {
            context.startActivity(ringIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start activity directly: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TaskMaster Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm and task reminder notifications"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true) // Allow breaking through Do Not Disturb
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
