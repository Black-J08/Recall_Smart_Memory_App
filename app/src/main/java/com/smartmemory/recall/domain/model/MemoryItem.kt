package com.smartmemory.recall.domain.model

sealed class MemoryItem {
    abstract val id: Long
    abstract val timestamp: Long
    
    data class Text(
        override val id: Long = 0,
        val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MemoryItem()
    
    data class Audio(
        override val id: Long = 0,
        val filePath: String,
        val durationMs: Long,
        val transcription: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MemoryItem()
    
    data class Image(
        override val id: Long = 0,
        val imageUrl: String,
        val caption: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MemoryItem()
}
