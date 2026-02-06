package com.smartmemory.recall.di

import com.smartmemory.recall.domain.ai.AIEngine
import com.smartmemory.recall.domain.ai.MockAIEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAIEngine(
        mediaPipeAIEngine: com.smartmemory.recall.domain.ai.MediaPipeAIEngine
    ): AIEngine
}
