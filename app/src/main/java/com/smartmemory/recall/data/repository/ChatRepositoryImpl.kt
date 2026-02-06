package com.smartmemory.recall.data.repository

import com.smartmemory.recall.data.local.dao.ChatDao
import com.smartmemory.recall.data.local.entity.ChatMessageEntity
import com.smartmemory.recall.data.local.entity.ChatSessionEntity
import com.smartmemory.recall.domain.model.ChatMessage
import com.smartmemory.recall.domain.model.ChatSession
import com.smartmemory.recall.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {

    override fun getChatSessions(): Flow<List<ChatSession>> {
        return chatDao.getAllSessions().map { entities ->
            entities.map { ChatSession(it.id, it.title, it.lastModified) }
        }
    }

    override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { ChatMessage(it.id, it.text, it.isUser, it.timestamp) }
        }
    }

    override suspend fun saveSession(session: ChatSession): Result<Unit> = runCatching {
        chatDao.insertSession(ChatSessionEntity(session.id, session.title, session.lastModified))
    }

    override suspend fun saveMessage(sessionId: String, message: ChatMessage): Result<Unit> = runCatching {
        chatDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                sessionId = sessionId,
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp
            )
        )
        // Also update the session's lastModified timestamp
        // We'd need to fetch the session first or use a custom query
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        chatDao.deleteSession(sessionId)
    }

    override suspend fun updateSessionTitle(sessionId: String, newTitle: String): Result<Unit> = runCatching {
        chatDao.updateTitle(sessionId, newTitle)
    }

    override suspend fun deleteEmptySessions(): Result<Unit> = runCatching {
        chatDao.deleteEmptySessions()
    }
}
