package com.smartmemory.recall.data.local.dao

import androidx.room.*
import com.smartmemory.recall.data.local.entity.ChatMessageEntity
import com.smartmemory.recall.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    @Query("SELECT * FROM chat_sessions ORDER BY last_modified DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)
}
