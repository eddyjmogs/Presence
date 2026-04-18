package com.eddy.presence.data.model

import androidx.room.Entity

@Entity(tableName = "whitelist_entries", primaryKeys = ["contextName", "packageName"])
data class WhitelistEntry(
    val contextName: String,
    val packageName: String,
)
