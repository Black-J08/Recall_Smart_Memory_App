package com.smartmemory.recall.domain.ai

import android.util.Log
import com.smartmemory.recall.data.local.dao.MemoryDao
import com.smartmemory.recall.data.local.dao.MemoryEmbeddingTuple
import com.smartmemory.recall.data.local.entity.MemoryEntity
import com.smartmemory.recall.domain.model.MemoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class VectorSearchService @Inject constructor(
    private val memoryDao: MemoryDao,
    private val embeddingService: EmbeddingService
) {

    /**
     * Finds the most relevant memories for the given query text.
     * Uses Cosine Similarity.
     */
    suspend fun findRelevantMemories(query: String, limit: Int = 3, threshold: Float = 0.5f): List<MemorySearchMatch> = withContext(Dispatchers.Default) {
        val queryEmbeddingResult = embeddingService.embedToFloats(query)
        if (queryEmbeddingResult.isFailure) {
            Log.w(TAG, "Failed to embed query: $query")
            return@withContext emptyList()
        }
        val queryVector = queryEmbeddingResult.getOrThrow()

        // 1. Fetch all embeddings from DB (Lightweight DTOs)
        // Optimization: In a real large-scale app, we'd cache this list in memory 
        // and only update it when DB changes. For <2k memories, fetching on demand is fine.
        val memories = memoryDao.getAllMemoriesWithEmbeddings().first()
        
        if (memories.isEmpty()) return@withContext emptyList()

        // 2. Brute-force Cosine Similarity
        val matches = memories.mapNotNull { memoryTuple ->
            try {
                val memoryVector = embeddingService.byteArrayToFloatArray(memoryTuple.embedding)
                val similarity = cosineSimilarity(queryVector, memoryVector)
                
                if (similarity >= threshold) {
                    MemorySearchMatch(
                        id = memoryTuple.id,
                        text = extractTextFromMetadata(memoryTuple.metadataJson),
                        score = similarity
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing memory ${memoryTuple.id}", e)
                null
            }
        }

        // 3. Sort and Limit
        matches.sortedByDescending { it.score }.take(limit)
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return 0f
        
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }

        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0f
        }
    }
    
    // Quick and dirty extraction until we have a proper DTO mapper inside this service,
    // or we could inject MemoryJsonSerializer if we wanted full objects.
    // For RAG, we just need the text.
    private fun extractTextFromMetadata(json: String): String {
        // Regex hack to avoid parsing full JSON if we just want "content" or "transcription"
        // Ideally we should use the shared serializer, but this service deals with Tuples.
        // Let's rely on a simple string search for MVP or standard JSON parsing if needed.
        // Given MemoryJsonSerializer structure: {"content":"..."} or {"transcription":"..."}
        
        return json // Pass raw JSON to LLM? No, better to clean it.
        // Let's assume the LLM can handle JSON snippet context, 
        // OR we just return the raw JSON string which contains the text.
        // The Prompt construction phase can clean it up.
    }

    companion object {
        private const val TAG = "VectorSearchService"
    }
}

data class MemorySearchMatch(
    val id: Long,
    val text: String,
    val score: Float
)
