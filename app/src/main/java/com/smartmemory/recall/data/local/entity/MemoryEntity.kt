package com.smartmemory.recall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "type") val type: MemoryType,
    @ColumnInfo(name = "metadata_json") val metadataJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "tags") val tags: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false
)

enum class MemoryType {
    TEXT, AUDIO, IMAGE, VIDEO, PDF, SCREENSHOT
}
