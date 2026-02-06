package com.smartmemory.recall.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartmemory.recall.data.local.dao.ChatDao
import com.smartmemory.recall.data.local.dao.MemoryDao
import com.smartmemory.recall.data.local.entity.ChatMessageEntity
import com.smartmemory.recall.data.local.entity.ChatSessionEntity
import com.smartmemory.recall.data.local.entity.MemoryEntity

@Database(
    entities = [
        MemoryEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class
    ],

    version = 3,
    exportSchema = true
)
abstract class RecallDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun chatDao(): ChatDao
}
