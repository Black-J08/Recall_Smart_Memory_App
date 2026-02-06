package com.smartmemory.recall.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for all local AI engines (MLC, MediaPipe, etc.)
 */
interface AIEngine {
    /**
     * Engine Name (e.g., "MLC LLM", "MediaPipe")
     */
    val name: String

    /**
     * Current model name
     */
    val modelName: String

    /**
     * Initializes the engine and loads the model.
     * @param onProgress Callback for loading progress (0.0 to 1.0)
     */
    suspend fun initialize(onProgress: (Float) -> Unit): Result<Unit>

    /**
     * Starts a new session or resets the current one with the given history.
     */
    suspend fun startSession(history: List<Any>): Result<Unit>

    /**
     * Generates a streaming response for the given prompt.
     */
    fun generateResponse(prompt: String): Flow<String>

    /**
     * Unloads the model and frees resources.
     */
    suspend fun unload()
    
    /**
     * Checks if the engine is currently initialized.
     */
    fun isInitialized(): Boolean
}

sealed class AIEngineState {
    object Idle : AIEngineState()
    data class Loading(val progress: Float) : AIEngineState()
    object Ready : AIEngineState()
    data class Error(val message: String) : AIEngineState()
}
