package com.eddy.presence.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eddy.presence.data.model.WhitelistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WhitelistEntry)

    @Delete
    suspend fun delete(entry: WhitelistEntry)

    @Query("SELECT packageName FROM whitelist_entries WHERE contextName = :contextName")
    fun getPackagesForContext(contextName: String): Flow<List<String>>
}
