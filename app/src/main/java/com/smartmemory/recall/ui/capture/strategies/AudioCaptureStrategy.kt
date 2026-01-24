package com.smartmemory.recall.ui.capture.strategies

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.ui.theme.MemoryAudioDark
import javax.inject.Inject

class AudioCaptureStrategy @Inject constructor() : MemoryTypeStrategy {
    override val type = MemoryType.AUDIO
    override val icon: ImageVector = Icons.Default.Audiotrack
    override val label = "Audio Recording"
    override val color = MemoryAudioDark
    
    @Composable
    override fun CaptureUI(onComplete: (MemoryItem) -> Unit, onCancel: () -> Unit) {
        // Placeholder for Audio Recorder UI
        Text("Audio Recorder coming soon...")
    }
}
