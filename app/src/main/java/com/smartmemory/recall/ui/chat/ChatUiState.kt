package com.smartmemory.recall.ui.chat

import com.smartmemory.recall.domain.model.ChatMessage
import com.smartmemory.recall.domain.model.ChatSession

import com.smartmemory.recall.domain.ai.AIEngineState

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val typingSessionIds: Set<String> = emptySet(),
    val pendingAiResponse: String? = null, // Transient state for streaming
    val error: String? = null,
    val aiState: AIEngineState = AIEngineState.Idle
)
