package com.smartmemory.recall.di

import android.content.Context
import androidx.room.Room
import com.smartmemory.recall.data.local.RecallDatabase
import com.smartmemory.recall.data.local.dao.ChatDao
import com.smartmemory.recall.data.local.dao.MemoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideRecallDatabase(
        @ApplicationContext context: Context
    ): RecallDatabase {
        return Room.databaseBuilder(
            context,
            RecallDatabase::class.java,
            "recall_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideMemoryDao(database: RecallDatabase): MemoryDao {
        return database.memoryDao()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: RecallDatabase): ChatDao {
        return database.chatDao()
    }
}
