package com.taskmaster.app.receivers

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
import com.taskmaster.app.R
import com.taskmaster.app.activities.AlarmRingActivity

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

        // 1. Handle Boot Completed
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            // Here you would typically reschedule alarms from Firestore if needed.
            // For now, we just return to avoid triggering the ring screen on boot.
            return
        }

        // 2. Security Check: Only trigger if a user is logged in
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w(TAG, "No user logged in. Ignoring alarm.")
            return
        }

        // 3. Trigger Alarm Logic
        // We expect EXTRA_ALARM_ID to be present for a valid alarm trigger.
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId == null) {
            Log.w(TAG, "Received alarm intent without alarmId. Ignoring.")
            return
        }

        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val isTask = intent.getBooleanExtra(EXTRA_IS_TASK, false)

        // Launch full-screen alarm ring activity
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_ALARM_LABEL, label)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        // Create notification channel
        createNotificationChannel(context)

        val pendingIntent = PendingIntent.getActivity(
            context, alarmId.hashCode(), ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(if (isTask) "Task Reminder" else "⏰ Alarm")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId.hashCode(), notification)

        // Also start activity directly
        context.startActivity(ringIntent)
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
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
