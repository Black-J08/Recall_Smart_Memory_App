package com.smartmemory.recall.domain.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.model.MemoryItem

interface MemoryTypeStrategy {
    val type: MemoryType
    val icon: ImageVector
    val label: String
    val color: Color
    
    @Composable
    fun CaptureUI(
        onComplete: (MemoryItem) -> Unit,
        onCancel: () -> Unit
    )
}
