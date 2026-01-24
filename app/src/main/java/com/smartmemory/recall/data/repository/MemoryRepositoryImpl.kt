package com.smartmemory.recall.data.repository

import com.smartmemory.recall.data.local.MemoryJsonSerializer
import com.smartmemory.recall.data.local.dao.MemoryDao
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val dao: MemoryDao,
    private val serializer: MemoryJsonSerializer
) : MemoryRepository {
    
    override fun getMemories(): Flow<List<MemoryItem>> =
        dao.getAllMemories().map { entities ->
            entities.mapNotNull { serializer.deserialize(it) }
        }
    
    override fun getMemoriesByType(type: MemoryType): Flow<List<MemoryItem>> =
        dao.getMemoriesByType(type).map { entities ->
            entities.mapNotNull { serializer.deserialize(it) }
        }
    
    override suspend fun saveMemory(item: MemoryItem): Result<Long> = runCatching {
        val entity = serializer.serialize(item)
        dao.insertMemory(entity)
    }
    
    override suspend fun deleteMemory(id: Long): Result<Unit> = runCatching {
        // For now, we'll implement this when needed
        // Would need to query by ID first, then delete
    }
    
    override suspend fun updateMemory(item: MemoryItem): Result<Unit> = runCatching {
        val entity = serializer.serialize(item)
        dao.updateMemory(entity)
    }
}
