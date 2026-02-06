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

    @Query("SELECT id, metadata_json, embedding FROM memories WHERE embedding IS NOT NULL")
    fun getAllMemoriesWithEmbeddings(): Flow<List<MemoryEmbeddingTuple>>
}

/**
 * Lightweight DTO for vector search.
 * We only need the ID to retrieve the full item later (if needed), 
 * the text content (extracted from metadata), and the vector.
 */
data class MemoryEmbeddingTuple(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "metadata_json") val metadataJson: String,
    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryEmbeddingTuple

        if (id != other.id) return false
        if (metadataJson != other.metadataJson) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + metadataJson.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
