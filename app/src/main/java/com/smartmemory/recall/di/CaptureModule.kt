package com.smartmemory.recall.di

import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.ui.capture.strategies.AudioCaptureStrategy
import com.smartmemory.recall.ui.capture.strategies.ImageCaptureStrategy
import com.smartmemory.recall.ui.capture.strategies.TextCaptureStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {
    
    @Binds
    @IntoSet
    abstract fun bindTextCaptureStrategy(
        impl: TextCaptureStrategy
    ): MemoryTypeStrategy

    @Binds
    @IntoSet
    abstract fun bindAudioCaptureStrategy(
        impl: AudioCaptureStrategy
    ): MemoryTypeStrategy

    @Binds
    @IntoSet
    abstract fun bindImageCaptureStrategy(
        impl: ImageCaptureStrategy
    ): MemoryTypeStrategy
}
