package com.eddy.presence.data.repository

import com.eddy.presence.data.db.WhitelistDao
import com.eddy.presence.data.model.WhitelistEntry
import kotlinx.coroutines.flow.Flow

class WhitelistRepository(private val dao: WhitelistDao) {

    fun getPackagesForContext(contextName: String): Flow<List<String>> =
        dao.getPackagesForContext(contextName)

    suspend fun setWhitelisted(contextName: String, packageName: String, whitelisted: Boolean) {
        val entry = WhitelistEntry(contextName = contextName, packageName = packageName)
        if (whitelisted) dao.insert(entry) else dao.delete(entry)
    }
}
