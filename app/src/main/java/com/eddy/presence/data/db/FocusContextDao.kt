package com.eddy.presence.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eddy.presence.data.model.FocusContext
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusContextDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(context: FocusContext)

    @Query("DELETE FROM focus_contexts WHERE name = :name")
    suspend fun delete(name: String)

    @Query("SELECT * FROM focus_contexts ORDER BY name ASC")
    fun getAll(): Flow<List<FocusContext>>
}
