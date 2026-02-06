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
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryEntity

        if (id != other.id) return false
        if (type != other.type) return false
        if (metadataJson != other.metadataJson) return false
        if (createdAt != other.createdAt) return false
        if (tags != other.tags) return false
        if (isFavorite != other.isFavorite) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + metadataJson.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (tags?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}

enum class MemoryType {
    TEXT, AUDIO, IMAGE, VIDEO, PDF, SCREENSHOT
}
