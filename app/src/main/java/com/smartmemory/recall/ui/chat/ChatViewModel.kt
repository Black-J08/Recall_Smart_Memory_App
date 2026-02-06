package com.smartmemory.recall.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmemory.recall.domain.model.ChatMessage
import com.smartmemory.recall.domain.model.ChatSession
import com.smartmemory.recall.domain.repository.ChatRepository
import com.smartmemory.recall.domain.ai.AIEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.smartmemory.recall.domain.ai.AIEngineState
import com.smartmemory.recall.domain.ai.ModelManager
import com.smartmemory.recall.domain.repository.SettingsRepository

/**
 * ViewModel responsible for managing the chat UI state and interaction logic.
 *
 * Key Responsibilities:
 * - Handling user input and [sendMessage] actions.
 * - Managing chat sessions (auto-creation, selection).
 * - Orchestrating AI generation via [AIEngine].
 * - Maintaining UI state ([ChatUiState]) including loading, typing indicators, and errors.
 * - Robust error handling (e.g., auto-recovering from missing sessions).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiEngine: AIEngine,
    private val modelManager: ModelManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
        initializeAI()
    }

    fun retryInitialization() {
        val currentState = _uiState.value.aiState
        if (currentState !is AIEngineState.Ready && currentState !is AIEngineState.Loading) {
            initializeAI()
        }
    }

    private fun initializeAI() {
        viewModelScope.launch {
            // Check if the selected model is actually downloaded before initializing
            val selectedModel = settingsRepository.getSelectedModel()
            if (!modelManager.isModelDownloaded(selectedModel.id)) {
                _uiState.update { 
                    it.copy(aiState = AIEngineState.Error("Model ${selectedModel.displayName} not downloaded. Please download it from Settings."))
                }
                return@launch
            }
            
            _uiState.update { it.copy(aiState = AIEngineState.Loading(0f)) }
            aiEngine.initialize { progress ->
                _uiState.update { it.copy(aiState = AIEngineState.Loading(progress)) }
            }.onSuccess {
                _uiState.update { it.copy(aiState = AIEngineState.Ready) }
            }.onFailure { e ->
                _uiState.update { it.copy(aiState = AIEngineState.Error(e.message ?: "Unknown AI error")) }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            chatRepository.getChatSessions().collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
                
                // If no session is selected and there are sessions, select the latest one
                if (_uiState.value.currentSessionId == null && sessions.isNotEmpty()) {
                    selectSession(sessions.first().id)
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId, isLoading = true) }
        
        viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }

    fun createNewChat() {
        val newSession = ChatSession(title = "New Conversation")
        viewModelScope.launch {
            chatRepository.saveSession(newSession).onSuccess {
                selectSession(newSession.id)
            }
        }
    }

    fun sendMessage(text: String) {
        Log.d("ChatViewModel", "sendMessage called with: $text")
        if (text.isBlank()) return

        val sessionId = _uiState.value.currentSessionId
        if (sessionId == null) {
            Log.w("ChatViewModel", "No active session, creating new one...")
            val newSession = ChatSession(title = "New Conversation")
            viewModelScope.launch {
                chatRepository.saveSession(newSession).onSuccess {
                    selectSession(newSession.id)
                    // Recursive call now that session exists
                    sendMessage(text)
                }
            }
            return
        }

        val userMessage = ChatMessage(text = text, isUser = true)
        
        viewModelScope.launch {
            chatRepository.saveMessage(sessionId, userMessage).onSuccess {
                generateAIResponse(sessionId, text)
            }
        }
    }

    private fun generateAIResponse(sessionId: String, prompt: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAITyping = true) }
            
            Log.d("ChatViewModel", "Requesting response from engine for: $prompt")
            var fullResponse = ""
            
            aiEngine.generateResponse(prompt).collect { chunk ->
                Log.d("ChatViewModel", "Received chunk: $chunk")
                fullResponse += chunk
                
                // Update UI state with partial message for streaming effect
                _uiState.update { state ->
                    val existingMessages = state.messages
                    val lastMessage = existingMessages.lastOrNull()
                    
                    if (lastMessage != null && !lastMessage.isUser && lastMessage.timestamp == 0L) {
                        // Update the existing "streaming" message
                        val updatedMessages = existingMessages.toMutableList()
                        updatedMessages[updatedMessages.size - 1] = lastMessage.copy(text = fullResponse)
                        state.copy(messages = updatedMessages)
                    } else {
                        // Add a new message for streaming (timestamp 0 identifies it as temporary)
                        state.copy(messages = existingMessages + ChatMessage(text = fullResponse, isUser = false, timestamp = 0L))
                    }
                }
            }
            
            _uiState.update { it.copy(isAITyping = false) }
            
            // Save final message to Room
            val aiMessage = ChatMessage(text = fullResponse, isUser = false)
            chatRepository.saveMessage(sessionId, aiMessage)
        }
    }
}
