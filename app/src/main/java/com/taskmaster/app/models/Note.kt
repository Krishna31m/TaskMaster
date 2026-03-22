package com.taskmaster.app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Note(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val color: String = "#FFE4E1",   // hex color string
    @ServerTimestamp
    val createdAt: Date? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "#FFE4E1")
}
