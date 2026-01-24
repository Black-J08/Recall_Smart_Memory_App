package com.smartmemory.recall.domain.repository

import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getMemories(): Flow<List<MemoryItem>>
    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryItem>>
    suspend fun saveMemory(item: MemoryItem): Result<Long>
    suspend fun deleteMemory(id: Long): Result<Unit>
    suspend fun updateMemory(item: MemoryItem): Result<Unit>
}
