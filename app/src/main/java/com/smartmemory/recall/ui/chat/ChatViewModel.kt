package com.smartmemory.recall.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmemory.recall.domain.model.ChatMessage
import com.smartmemory.recall.domain.model.ChatSession
import com.smartmemory.recall.domain.repository.ChatRepository
import com.smartmemory.recall.domain.ai.AIEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    private val settingsRepository: SettingsRepository,
    // [NEW] RAG Service
    private val vectorSearchService: com.smartmemory.recall.domain.ai.VectorSearchService
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
                // Initialize an empty session if no chat is selected yet
                aiEngine.startSession(emptyList<Any>())
            }.onFailure { e ->
                _uiState.update { it.copy(aiState = AIEngineState.Error(e.message ?: "Unknown AI error")) }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            // Clean up empty sessions first
            chatRepository.deleteEmptySessions()
            
            chatRepository.getChatSessions().collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
                
                // If no session is selected and there are sessions, select the latest one
                if (_uiState.value.currentSessionId == null && sessions.isNotEmpty()) {
                    selectSession(sessions.first().id)
                }
            }
        }
    }

    private var messageCollectionJob: Job? = null
    private var sessionSetupJob: Job? = null

    fun selectSession(sessionId: String) {
        // Cancel previous message collector to prevent state races
        messageCollectionJob?.cancel()
        
        _uiState.update { it.copy(currentSessionId = sessionId, isLoading = true) }
        
        messageCollectionJob = viewModelScope.launch {
            // Note: Room is table-level reactive. Any change to chat_messages table triggers this flow.
            // By ensuring only ONE collector is active (for the current session), we prevent
            // updates in other sessions from overwriting the UI state.
            chatRepository.getMessagesForSession(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
        
        // Robust Session Restoration: Warm up context with recent history
        // Cancel any pending session switch to prevent race conditions
        sessionSetupJob?.cancel()
        sessionSetupJob = viewModelScope.launch {
            try {
                // Fetch current history snapshot once
                val messages = chatRepository.getMessagesForSession(sessionId).first()
                Log.d("ChatViewModel", "Restoring context for session $sessionId with ${messages.size} messages")
                aiEngine.startSession(messages)
            } catch (e: CancellationException) {
                // Normal cancellation
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to restore session context", e)
            }
        }
    }

    fun createNewChat() {
        // Don't create a new chat if the current one is already empty
        val currentSessionId = _uiState.value.currentSessionId
        if (currentSessionId != null) {
            val currentMessages = _uiState.value.messages
            if (currentMessages.isEmpty()) {
                return
            }
        }

        val newSession = ChatSession(title = "New Conversation")
        viewModelScope.launch {
            chatRepository.saveSession(newSession).onSuccess {
                // selectSession handles the startSession call now with cancellation support
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
                    sendMessage(text)
                }
            }
            return
        }

        val userMessage = ChatMessage(text = text, isUser = true)
        
        viewModelScope.launch {
            chatRepository.saveMessage(sessionId, userMessage).onSuccess {
                sessionSetupJob?.join()
                
                // [NEW] RAG Implementation
                // 1. Retrieve relevant memories
                val relevantMemories = vectorSearchService.findRelevantMemories(text)
                Log.d("ChatViewModel", "Found ${relevantMemories.size} relevant memories")
                
                // 2. Construct Augmented Prompt
                val augmentedPrompt = if (relevantMemories.isNotEmpty()) {
                    buildString {
                        append("Context information is below.\n---------------------\n")
                        relevantMemories.forEachIndexed { index, match ->
                            append("[Memory ${index + 1}]: ${match.text}\n")
                        }
                        append("---------------------\n")
                        append("Given the context information and not prior knowledge, answer the query.\n")
                        append("Query: $text")
                    }
                } else {
                    text
                }
                
                generateAIResponse(sessionId, augmentedPrompt, originalUserText = text)
            }
        }
    }

    // Updated signature to keep tracking original text specifically if needed, 
    // though here we just use the prompt for generation.
    private fun generateAIResponse(sessionId: String, prompt: String, originalUserText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(typingSessionIds = it.typingSessionIds + sessionId) }
            
            Log.d("ChatViewModel", "Requesting response from engine for: $originalUserText (Context added)")
            var fullResponse = ""
            
            aiEngine.generateResponse(prompt).collect { chunk ->
                Log.d("ChatViewModel", "Received chunk: $chunk")
                fullResponse += chunk
                
                _uiState.update { state ->
                    if (state.currentSessionId != sessionId) return@update state
                    state.copy(pendingAiResponse = fullResponse)
                }
            }
            
            _uiState.update { 
                it.copy(
                    typingSessionIds = it.typingSessionIds - sessionId,
                    pendingAiResponse = null
                ) 
            }
            
            val aiMessage = ChatMessage(text = fullResponse, isUser = false)
            chatRepository.saveMessage(sessionId, aiMessage)
            
            val currentSession = _uiState.value.sessions.find { it.id == sessionId }
            if (currentSession?.title == "New Conversation") {
                generateTitle(sessionId, originalUserText, fullResponse)
            }
        }
    }


    
    private fun generateTitle(sessionId: String, userPrompt: String, aiResponse: String) {
        viewModelScope.launch {
            val titlePrompt = "Generate a short, concise title (max 5 words) for this conversation. Output strictly just the title text, no quotes or prefixes. Conversation:\nUser: $userPrompt\nAI: $aiResponse"
            var title = ""
            try {
                // Use stateless generation to avoid polluting the chat context
                (aiEngine as? com.smartmemory.recall.domain.ai.MediaPipeAIEngine)?.generateResponseStateless(titlePrompt)?.collect { chunk ->
                    title += chunk
                }
                
                title = title.trim().replace("\"", "").replace("Title:", "").trim()
                if (title.isNotEmpty()) {
                    chatRepository.updateSessionTitle(sessionId, title)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to generate title", e)
            }
        }
    }
}
