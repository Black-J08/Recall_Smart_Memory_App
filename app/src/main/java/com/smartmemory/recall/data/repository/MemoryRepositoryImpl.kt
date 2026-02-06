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

import com.smartmemory.recall.domain.ai.EmbeddingService

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val dao: MemoryDao,
    private val serializer: MemoryJsonSerializer,
    private val embeddingService: EmbeddingService
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
        var entity = serializer.serialize(item)
        
        // Generate embedding
        val textToEmbed = extractTextContent(item)
        if (!textToEmbed.isNullOrBlank()) {
            val embeddingResult = embeddingService.embed(textToEmbed)
            if (embeddingResult.isSuccess) {
                entity = entity.copy(embedding = embeddingResult.getOrNull())
            }
        }
        
        dao.insertMemory(entity)
    }
    
    override suspend fun deleteMemory(id: Long): Result<Unit> = runCatching {
        // Implementation pending: need lookup by ID first
        // For MVP we might skip generic delete or fetch-then-delete
    }
    
    override suspend fun updateMemory(item: MemoryItem): Result<Unit> = runCatching {
        var entity = serializer.serialize(item)
        
        val textToEmbed = extractTextContent(item)
        if (!textToEmbed.isNullOrBlank()) {
            val embeddingResult = embeddingService.embed(textToEmbed)
            if (embeddingResult.isSuccess) {
                entity = entity.copy(embedding = embeddingResult.getOrNull())
            }
        }
        
        dao.updateMemory(entity)
    }

    private fun extractTextContent(item: MemoryItem): String? {
        return when (item) {
            is MemoryItem.Text -> item.text
            is MemoryItem.Audio -> item.transcription // Embed transcription if available
            is MemoryItem.Image -> item.caption // Embed caption if available
            // Future types
            else -> null
        }
    }
}
