package com.smartmemory.recall.ui.capture.strategies

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.ui.capture.components.TextInputDialog
import com.smartmemory.recall.ui.theme.MemoryTextDark
import javax.inject.Inject

class TextCaptureStrategy @Inject constructor() : MemoryTypeStrategy {
    override val type = MemoryType.TEXT
    override val icon: ImageVector = Icons.Default.ShortText
    override val label = "Text Note"
    override val color = MemoryTextDark
    
    @Composable
    override fun CaptureUI(onComplete: (MemoryItem) -> Unit, onCancel: () -> Unit) {
        TextInputDialog(
            onSave = { text -> 
                onComplete(MemoryItem.Text(text = text))
            },
            onDismiss = onCancel
        )
    }
}
