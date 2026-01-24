package com.smartmemory.recall.domain.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation for testing UI and RAG pipeline without real LLM overhead.
 */
@Singleton
class MockAIEngine @Inject constructor() : AIEngine {
    override val name: String = "Mock Engine"
    override val modelName: String = "Mock-Qwen-0.5B"
    
    private var initialized = false

    override suspend fun initialize(onProgress: (Float) -> Unit): Result<Unit> {
        for (i in 1..5) {
            delay(200)
            onProgress(i * 0.2f)
        }
        initialized = true
        return Result.success(Unit)
    }

    override fun generateResponse(prompt: String): Flow<String> = flow {
        val fullResponse = "This is a mock response from Recall AI. Since I'm in mock mode, I'm just acknowledging your prompt: \"$prompt\". In a real scenario, I would be running on your phone's GPU!"
        
        val words = fullResponse.split(" ")
        for (word in words) {
            emit("$word ")
            delay(100)
        }
    }

    override suspend fun unload() {
        initialized = false
    }

    override fun isInitialized(): Boolean = initialized
}
