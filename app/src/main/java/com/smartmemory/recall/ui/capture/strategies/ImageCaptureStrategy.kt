package com.smartmemory.recall.ui.capture.strategies

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.domain.model.MemoryItem
import javax.inject.Inject

class ImageCaptureStrategy @Inject constructor() : MemoryTypeStrategy {
    override val type = MemoryType.IMAGE
    override val icon: ImageVector = Icons.Default.Image
    override val label = "Image Memory"
    override val color = Color.Gray
    
    @Composable
    override fun CaptureUI(onComplete: (MemoryItem) -> Unit, onCancel: () -> Unit) {
        // Placeholder for Image Picker UI
        Text("Image Picker coming soon...")
    }
}
