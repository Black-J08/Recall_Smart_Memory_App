package com.smartmemory.recall.domain.ai

import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import android.util.Log
import com.smartmemory.recall.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MLCAIEngine @Inject constructor(
    private val modelManager: ModelManager,
    private val settingsRepository: SettingsRepository,
    private val backendDetector: GPUBackendDetector
) : AIEngine {
    override val name: String = "MLC LLM Engine"
    override val modelName: String get() = settingsRepository.getSelectedModel().displayName
    
    private var mlcEngine: MLCEngine? = null
    private var initialized = false
    private var currentBackend: GPUBackendDetector.Backend? = null
    private val failedBackends = mutableSetOf<GPUBackendDetector.Backend>()

    override suspend fun initialize(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentModel = settingsRepository.getSelectedModel()
            val modelDir = modelManager.getModelPath(currentModel.id)
            
            if (!modelManager.isModelDownloaded(currentModel.id)) {
                return@withContext Result.failure(Exception("Model ${currentModel.displayName} not found. Please download it first."))
            }

            // Only attempt initialization if not already initialized
            if (mlcEngine == null) {
                onProgress(0.1f)
                
                // Get available backends based on detector and specific model needs
                var availableBackends = backendDetector.detectAvailableBackends()
                    .filter { it !in failedBackends }
                
                // If this is a CPU model, force CPU backend or prioritize it
                if (currentModel.id.contains("-cpu")) {
                    Log.i(TAG, "CPU model detected, forcing CPU backend for stability")
                    availableBackends = listOf(GPUBackendDetector.Backend.CPU)
                }
                
                if (availableBackends.isEmpty()) {
                    return@withContext Result.failure(Exception("All available backends have failed. Please restart the app."))
                }
                
                // Try each backend in priority order
                var engineCreated = false
                var lastError: Throwable? = null
                
                for (backend in availableBackends) {
                    Log.d(TAG, "Attempting to initialize with backend: $backend")
                    
                    try {
                        // Give the system a moment to settle between attempts
                        delay(200)
                        
                        // Attempt to create MLCEngine
                        // We wrap this in a more defensive block because of async crashes
                        mlcEngine = MLCEngine()
                        currentBackend = backend
                        engineCreated = true
                        Log.i(TAG, "Successfully initialized MLCEngine with backend: $backend")
                        break
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed constructor for backend $backend: ${t.message}", t)
                        failedBackends.add(backend)
                        mlcEngine = null
                        lastError = t
                        
                        // Brief pause before trying next fallback
                        delay(500)
                    }
                }
                
                if (!engineCreated) {
                    return@withContext Result.failure(
                        Exception("Failed to initialize AI engine. Device might be missing required libraries.", lastError)
                    )
                }
                
                onProgress(0.4f)
            }

            onProgress(0.6f)
            
            // Load the model
            try {
                mlcEngine?.reload(modelDir, currentModel.modelLib)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reload model", e)
                mlcEngine = null
                return@withContext Result.failure(Exception("Failed to load model: ${e.message}", e))
            }
            
            onProgress(1.0f)
            
            initialized = true
            Log.i(TAG, "MLCAIEngine initialized successfully with backend: $currentBackend")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during initialization", e)
            mlcEngine = null
            Result.failure(e)
        }
    }

    override fun generateResponse(prompt: String): Flow<String> = flow {
        val engine = mlcEngine
        if (!initialized || engine == null) {
            emit("Error: Engine not initialized.")
            return@flow
        }

        try {
            val messages = listOf(
                OpenAIProtocol.ChatCompletionMessage(
                    role = OpenAIProtocol.ChatCompletionRole.user,
                    content = prompt
                )
            )

            val channel: ReceiveChannel<OpenAIProtocol.ChatCompletionStreamResponse> = 
                engine.chat.completions.create(messages = messages)

            for (response in channel) {
                val delta = response.choices.firstOrNull()?.delta?.content?.asText()
                if (delta != null) {
                    emit(delta)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during response generation", e)
            emit("Error during generation: ${e.message}")
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun unload() {
        try {
            mlcEngine?.unload()
            initialized = false
            Log.d(TAG, "MLCAIEngine unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading engine", e)
        }
    }

    override fun isInitialized(): Boolean = initialized
    
    /**
     * Returns the current backend being used, or null if not initialized
     */
    fun getCurrentBackend(): GPUBackendDetector.Backend? = currentBackend
    
    companion object {
        private const val TAG = "MLCAIEngine"
    }
}
