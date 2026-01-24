package com.smartmemory.recall.di

import com.smartmemory.recall.data.repository.ChatRepositoryImpl
import com.smartmemory.recall.data.repository.MemoryRepositoryImpl
import com.smartmemory.recall.data.repository.SettingsRepositoryImpl
import com.smartmemory.recall.domain.repository.ChatRepository
import com.smartmemory.recall.domain.repository.MemoryRepository
import com.smartmemory.recall.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(
        memoryRepositoryImpl: MemoryRepositoryImpl
    ): MemoryRepository
}
