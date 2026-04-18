package com.eddy.presence.data.repository

import com.eddy.presence.data.db.FocusContextDao
import com.eddy.presence.data.model.FocusContext
import kotlinx.coroutines.flow.Flow

class FocusContextRepository(private val dao: FocusContextDao) {
    fun getAll(): Flow<List<FocusContext>> = dao.getAll()
    suspend fun create(name: String) = dao.insert(FocusContext(name = name))
    suspend fun delete(name: String) = dao.delete(name)
}
