package com.smartmemory.recall.domain.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaPipe-based AI Engine using Gemma-3 270M model.
 * Provides a lightweight, reliable alternative to MLC LLM.
 */
@Singleton
class MediaPipeAIEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager
) : AIEngine {
    override val name: String = "MediaPipe (Gemma-3 270M)"
    override val modelName: String = "Gemma-3 270M"
    
    private var llmInference: LlmInference? = null
    private var initialized = false
    
    override suspend fun initialize(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            
            // Check if Gemma-3 270M model is downloaded
            val modelPath = getGemmaModelPath()
            if (!File(modelPath).exists()) {
                return@withContext Result.failure(
                    Exception("Gemma-3 270M model not found. Please download it from Settings.")
                )
            }
            
            onProgress(0.3f)
            
            // Create LlmInference options
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.8f)
                .setRandomSeed(101)
                .build()
            
            onProgress(0.6f)
            
            // Initialize MediaPipe LLM Inference
            llmInference = LlmInference.createFromOptions(context, options)
            
            onProgress(1.0f)
            initialized = true
            
            Log.i(TAG, "MediaPipe AI Engine initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe AI Engine", e)
            llmInference = null
            Result.failure(e)
        }
    }
    
    override fun generateResponse(prompt: String): Flow<String> = flow {
        val inference = llmInference
        if (!initialized || inference == null) {
            emit("Error: Engine not initialized.")
            return@flow
        }
        
        try {
            // MediaPipe LLM Inference generates response synchronously
            val response = inference.generateResponse(prompt)
            
            // Emit the response (MediaPipe doesn't support streaming yet)
            emit(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error during response generation", e)
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.Default)
    
    override suspend fun unload() {
        try {
            llmInference?.close()
            llmInference = null
            initialized = false
            Log.d(TAG, "MediaPipe AI Engine unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading MediaPipe engine", e)
        }
    }
    
    override fun isInitialized(): Boolean = initialized
    
    /**
     * Returns the path to the Gemma-3 270M model file.
     */
    private fun getGemmaModelPath(): String {
        // Model will be stored in app's files directory
        return File(context.filesDir, "models/gemma-3-270m.bin").absolutePath
    }
    
    companion object {
        private const val TAG = "MediaPipeAIEngine"
        const val GEMMA_MODEL_ID = "gemma-3-270m"
    }
}
