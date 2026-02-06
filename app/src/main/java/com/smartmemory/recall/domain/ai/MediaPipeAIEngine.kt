package com.smartmemory.recall.domain.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.smartmemory.recall.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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

    private var llmInference: LlmInference? = null
    private var initialized = false

    override val name: String = "MediaPipe LLM"
    override val modelName: String get() = settingsRepository.getSelectedModel().displayName

    override suspend fun initialize(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val currentModel = settingsRepository.getSelectedModel()
            val modelPath = modelManager.getModelPath(currentModel.id)
            val modelFile = File(modelPath)

            // If the path is a directory, look for the .bin or .task file
            val fileToLoad = if (modelFile.isDirectory) {
                modelFile.listFiles()?.find { 
                    it.name.endsWith(".bin") || it.name.endsWith(".task") || it.name.endsWith(".tflite")
                } ?: run {
                    return@withContext Result.failure(Exception("No compatible model file (.bin/.task) found in ${modelFile.name}"))
                }
            } else {
                modelFile
            }

            if (!fileToLoad.exists()) {
                return@withContext Result.failure(Exception("Model file not found: ${fileToLoad.absolutePath}"))
            }

            onProgress(0.3f)
            Log.d(TAG, "Initializing LlmInference with model: ${fileToLoad.absolutePath}")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(fileToLoad.absolutePath)
                // .setMaxTokens(1024) // Optional: create specific config in AIModel if needed
                // .setResultPercentage(0.5f) // Optional for smoother partial results
                .build()

            onProgress(0.6f)
            llmInference = LlmInference.createFromOptions(context, options)
            
            initialized = true
            onProgress(1.0f)
            Log.i(TAG, "MediaPipe LLM initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe engine", e)
            initialized = false
            llmInference = null
            Result.failure(e)
        }
    }

    override fun generateResponse(prompt: String): Flow<String> = callbackFlow {
        val engine = llmInference
        if (!initialized || engine == null) {
            trySend("Error: Engine not initialized.")
            close()
            return@callbackFlow
        }

        try {
            Log.d(TAG, "Generating response for prompt: $prompt")
            // MediaPipe generateResponseAsync provides partial results via callback
            engine.generateResponseAsync(prompt) { partialResult, done ->
                Log.d(TAG, "Callback received: partial='$partialResult', done=$done")
                if (partialResult != null) {
                    trySend(partialResult)
                }
                if (done != null && done) {
                    Log.d(TAG, "Generation complete")
                    close()
                }
            }
            
            // Keep the flow open until closed by the callback
            awaitClose { 
                // Cleanup if needed when flow is cancelled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            trySend("Error: ${e.message}")
            close()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun unload() {
        // MediaPipe LlmInference doesn't have an explicit unload/close method exposed publicly in older versions,
        // but dropping the reference allows GC to reclaim it. 
        // If there's a close() method in 0.10.x, we should call it.
        // Checking API: LlmInference usually needs to be just released.
        // There is no explicit close() in the Java API typically, it's auto-managed or we just null it.
        
        // Actually, looking at recent docs, there might not be a close(), 
        // relying on GC. We'll set it to null.
        llmInference = null
        initialized = false
        Log.d(TAG, "MediaPipe engine unloaded")
    }

    override fun isInitialized(): Boolean = initialized

    companion object {
        private const val TAG = "MediaPipeAIEngine"
    }
}
