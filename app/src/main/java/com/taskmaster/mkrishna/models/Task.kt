package com.taskmaster.mkrishna.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Task(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",       // "dd MMM yyyy"
    val time: String = "",       // "hh:mm a"
    val priority: String = "medium",  // "high", "medium", "low"
    val isCompleted: Boolean = false,
    val hasAlarm: Boolean = false,
    val alarmTimestamp: Long = 0L,
    @ServerTimestamp
    val createdAt: Date? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firestore
    constructor() : this("", "", "", "", "", "", "medium", false, false, 0L)
}
