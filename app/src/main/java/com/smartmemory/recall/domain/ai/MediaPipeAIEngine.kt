package com.smartmemory.recall.domain.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.smartmemory.recall.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AIEngine] using Google's MediaPipe LLM Inference.
 *
 * This engine handles loading LLM models (specifically .task bundles or .bin files)
 * and performing on-device text generation. It manages the lifecycle of the
 * [LlmInference] session and exposes a reactive flow for streaming responses.
 *
 * It is designed to be robust against initialization failures and reports detailed
 * progress during the loading phase.
 */
@Singleton
class MediaPipeAIEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager,
    private val settingsRepository: SettingsRepository
) : AIEngine {

    private var llmInferenceSession: LlmInferenceSession? = null
    private var llmInference: LlmInference? = null
    private var cachedOptions: LlmInference.LlmInferenceOptions? = null // Cache for re-creating sessions
    private var initialized = false

    override val name: String = "MediaPipe LLM (Stateful)"
    override val modelName: String get() = settingsRepository.getSelectedModel().displayName

    override suspend fun initialize(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val currentModel = settingsRepository.getSelectedModel()
            val modelPath = modelManager.getModelPath(currentModel.id)
            val modelFile = File(modelPath)

            // Robust model file discovery
            val fileToLoad = if (modelFile.isDirectory) {
                modelFile.listFiles()?.find { 
                    it.name.endsWith(".bin") || it.name.endsWith(".task") || it.name.endsWith(".tflite")
                } ?: return@withContext Result.failure(Exception("No compatible model file found in ${modelFile.name}"))
            } else {
                modelFile
            }

            if (!fileToLoad.exists()) {
                return@withContext Result.failure(Exception("Model file not found: ${fileToLoad.absolutePath}"))
            }

            onProgress(0.3f)
            Log.d(TAG, "Initializing production LLM session with: ${fileToLoad.absolutePath}")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(fileToLoad.absolutePath)
                .build()
            cachedOptions = options

            onProgress(0.6f)
            // LlmInference is the heavy engine
            llmInference = LlmInference.createFromOptions(context, options)
            
            // LlmInferenceSession            // Create the session using the base engine
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .build()
            llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)

            initialized = true
            onProgress(1.0f)
            Log.i(TAG, "MediaPipe LLM Production Session initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize production MediaPipe session", e)
            initialized = false
            cleanupResources()
            Result.failure(e)
        }
    }

    /**
     * Resets the AI session and warms up context with history.
     * To ensure stability, we truncate history to the most recent items.
     */
    override suspend fun startSession(history: List<Any>): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!initialized || llmInference == null) {
                return@withContext Result.failure(Exception("Engine not initialized"))
            }

            try {
                // Re-creating the session is the industry-standard way to clear on-device context 
                // while re-using the heavy LlmInference weights.
                llmInferenceSession = null // Release old cache
                
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .build()
                
                val newSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
                llmInferenceSession = newSession

                // Robust Truncation: Only replay the last 20 messages to prevent OOM
                val productionHistory = history.filterIsInstance<com.smartmemory.recall.domain.model.ChatMessage>()
                    .takeLast(20)

                Log.d(TAG, "Warming up session with ${productionHistory.size} messages")
                
                // 1. Add System Prompt (Critical for Qwen identity/behavior)
                val systemPrompt = "<|im_start|>system\nYou are Qwen, a helpful AI assistant. You remember the user's name and details provided in the conversation.<|im_end|>\n"
                newSession.addQueryChunk(systemPrompt)

                // 2. Replay History with ChatML format
                productionHistory.forEach { item ->
                    val role = if (item.isUser) "user" else "assistant"
                    val formattedMessage = "<|im_start|>$role\n${item.text}<|im_end|>\n"
                    newSession.addQueryChunk(formattedMessage)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start robust session", e)
                Result.failure(e)
            }
        }
    }

    private val mutex = Mutex()

    override fun generateResponse(prompt: String): Flow<String> = callbackFlow {
        mutex.withLock {
            val session = llmInferenceSession
            if (!initialized || session == null) {
                trySend("Error: Engine not ready.")
                close()
                return@withLock
            }

            try {
                Log.d(TAG, "Generating streaming response (Stateful)")
                
                // 3. Format New User Query with ChatML
                val formattedPrompt = "<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
                
                session.addQueryChunk(formattedPrompt)
                
                session.generateResponseAsync { partialResult, done ->
                    if (partialResult != null) {
                        trySend(partialResult)
                    }
                    if (done) {
                        Log.d(TAG, "Generation complete")
                        close()
                    }
                }
                
                awaitClose { /* Cleanup */ }
            } catch (e: Exception) {
                Log.e(TAG, "Error in generation flow", e)
                trySend("Error: ${e.message}")
                close()
            }
        }
    }.flowOn(Dispatchers.IO)

    // Stateless generation for utility tasks (Titles, Summaries) so we don't pollute the chat context
    fun generateResponseStateless(prompt: String): Flow<String> = callbackFlow {
        mutex.withLock {
            val engine = llmInference
            if (!initialized || engine == null) {
                trySend("")
                close()
                return@withLock
            }

            try {
                Log.d(TAG, "Generating stateless response")
                // Use the base engine directly, bypassing the session history
                engine.generateResponseAsync(prompt) { partialResult, done ->
                    if (partialResult != null) {
                        trySend(partialResult)
                    }
                    if (done != null && done) {
                        close()
                    }
                }
                awaitClose { }
            } catch (e: Exception) {
                Log.e(TAG, "Error in stateless generation", e)
                close()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun unload() {
        mutex.withLock {
            cleanupResources()
            Log.d(TAG, "MediaPipe engine and session unloaded")
        }
    }

    private fun cleanupResources() {
        try {
            // llmInference?.close() 
        } catch (e: Exception) {
            Log.w(TAG, "Error closing engine", e)
        }
        llmInferenceSession = null
        llmInference = null
        cachedOptions = null
        initialized = false
    }

    override fun isInitialized(): Boolean = initialized

    companion object {
        private const val TAG = "MediaPipeAIEngine"
    }
}
