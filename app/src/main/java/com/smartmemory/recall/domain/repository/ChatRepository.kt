package com.smartmemory.recall.domain.repository

import com.smartmemory.recall.domain.model.ChatMessage
import com.smartmemory.recall.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatSessions(): Flow<List<ChatSession>>
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>
    suspend fun saveSession(session: ChatSession): Result<Unit>
    suspend fun saveMessage(sessionId: String, message: ChatMessage): Result<Unit>
    suspend fun deleteSession(sessionId: String): Result<Unit>
}
