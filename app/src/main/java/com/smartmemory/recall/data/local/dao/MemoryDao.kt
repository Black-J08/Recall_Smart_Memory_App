package com.smartmemory.recall.data.local.dao

import androidx.room.*
import com.smartmemory.recall.data.local.entity.MemoryEntity
import com.smartmemory.recall.data.local.entity.MemoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY created_at DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memories WHERE type = :type ORDER BY created_at DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memories WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteMemories(): Flow<List<MemoryEntity>>
    
    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long
    
    @Update
    suspend fun updateMemory(memory: MemoryEntity)
    
    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
    
    @Query("SELECT * FROM memories WHERE metadata_json LIKE '%' || :query || '%'")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>
}
