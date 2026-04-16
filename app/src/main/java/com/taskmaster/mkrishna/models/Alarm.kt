package com.taskmaster.mkrishna.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Alarm(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val label: String = "",
    val date: String = "",      // human-readable days e.g. "Mon, Wed, Fri" or "Everyday"
    val time: String = "",      // "hh:mm a"
    val timestamp: Long = 0L,

    // ── CRITICAL: use @PropertyName so Firestore stores "enabled" / "repeat"
    // ── but Kotlin sees isEnabled / isRepeat — this fixes the mapper warning
    @get:PropertyName("enabled")
    @set:PropertyName("enabled")
    var isEnabled: Boolean = true,

    @get:PropertyName("repeat")
    @set:PropertyName("repeat")
    var isRepeat: Boolean = false,

    @ServerTimestamp
    val createdAt: Date? = null
) {
    // No-arg constructor required by Firestore
    constructor() : this("", "", "", "", "", 0L, true, false)
}