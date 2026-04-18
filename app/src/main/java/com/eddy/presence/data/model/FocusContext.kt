package com.eddy.presence.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_contexts")
data class FocusContext(
    @PrimaryKey val name: String,
)
