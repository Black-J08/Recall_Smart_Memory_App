package com.smartmemory.recall.data.local

import com.smartmemory.recall.data.local.entity.MemoryEntity
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.model.MemoryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryJsonSerializer @Inject constructor() {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    fun serialize(item: MemoryItem): MemoryEntity {
        return when (item) {
            is MemoryItem.Text -> MemoryEntity(
                id = item.id,
                type = MemoryType.TEXT,
                metadataJson = json.encodeToString(TextMetadata(item.text)),
                createdAt = item.timestamp
            )
            is MemoryItem.Audio -> MemoryEntity(
                id = item.id,
                type = MemoryType.AUDIO,
                metadataJson = json.encodeToString(
                    AudioMetadata(item.filePath, item.durationMs, item.transcription)
                ),
                createdAt = item.timestamp
            )
            is MemoryItem.Image -> MemoryEntity(
                id = item.id,
                type = MemoryType.IMAGE,
                metadataJson = json.encodeToString(
                    ImageMetadata(item.imageUrl, item.caption)
                ),
                createdAt = item.timestamp
            )
        }
    }
    
    fun deserialize(entity: MemoryEntity): MemoryItem? {
        return try {
            when (entity.type) {
                MemoryType.TEXT -> {
                    val metadata = json.decodeFromString<TextMetadata>(entity.metadataJson)
                    MemoryItem.Text(
                        id = entity.id,
                        text = metadata.content,
                        timestamp = entity.createdAt
                    )
                }
                MemoryType.AUDIO -> {
                    val metadata = json.decodeFromString<AudioMetadata>(entity.metadataJson)
                    MemoryItem.Audio(
                        id = entity.id,
                        filePath = metadata.filePath,
                        durationMs = metadata.durationMs,
                        transcription = metadata.transcription,
                        timestamp = entity.createdAt
                    )
                }
                MemoryType.IMAGE -> {
                    val metadata = json.decodeFromString<ImageMetadata>(entity.metadataJson)
                    MemoryItem.Image(
                        id = entity.id,
                        imageUrl = metadata.filePath,
                        caption = metadata.caption,
                        timestamp = entity.createdAt
                    )
                }
                else -> null // Future types not yet implemented
            }
        } catch (e: Exception) {
            null // Skip corrupted entries
        }
    }
    
    @Serializable
    private data class TextMetadata(val content: String)
    
    @Serializable
    private data class AudioMetadata(
        val filePath: String,
        val durationMs: Long,
        val transcription: String? = null
    )
    
    @Serializable
    private data class ImageMetadata(
        val filePath: String,
        val caption: String? = null
    )
}
