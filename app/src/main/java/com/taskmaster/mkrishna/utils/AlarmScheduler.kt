package com.taskmaster.mkrishna.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.taskmaster.mkrishna.receivers.AlarmReceiver

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Schedule a standalone alarm (from AlarmActivity).
     */
    fun scheduleAlarm(context: Context, alarmId: String, label: String, triggerAtMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_IS_TASK, false)
        }
        schedule(context, alarmId.hashCode(), intent, triggerAtMillis)
    }

    /**
     * Schedule a task reminder alarm (from AddEditTaskActivity).
     */
    fun scheduleTaskReminder(context: Context, taskId: String, taskTitle: String, triggerAtMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, taskTitle)
            putExtra(AlarmReceiver.EXTRA_IS_TASK, true)
        }
        // Use a unique request code distinct from alarm IDs
        schedule(context, "task_$taskId".hashCode(), intent, triggerAtMillis)
    }

    private fun schedule(context: Context, requestCode: Int, intent: Intent, triggerAtMillis: Long) {
        // SAFETY: If the time is in the past, don't schedule it. 
        // This prevents immediate triggering on app launch for old alarms.
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Attempted to schedule alarm in the past. Ignoring.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // For Android 12+, we need SCHEDULE_EXACT_ALARM permission
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel a scheduled alarm by its request code.
     */
    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
