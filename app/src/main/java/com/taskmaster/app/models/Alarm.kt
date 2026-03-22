package com.taskmaster.app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Alarm(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val label: String = "",
    val date: String = "",        // "dd MMM yyyy"
    val time: String = "",        // "hh:mm a"
    val timestamp: Long = 0L,     // epoch millis for AlarmManager
    val isEnabled: Boolean = true,
    val isRepeat: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null
) {
    constructor() : this("", "", "", "", "", 0L, true, false)
}
