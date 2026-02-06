package com.smartmemory.recall.ui.capture.strategies

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartmemory.recall.R
import com.smartmemory.recall.data.local.MediaStorageManager
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.ui.capture.CameraScreen
import javax.inject.Inject

class ImageCaptureStrategy @Inject constructor(
    private val mediaStorageManager: MediaStorageManager
) : MemoryTypeStrategy {
    override val type = MemoryType.IMAGE
    override val icon: ImageVector = Icons.Default.Image
    override val label = R.string.image_memory_label
    override val color = Color.Gray
    
    @Composable
    override fun CaptureUI(onComplete: (MemoryItem) -> Unit, onCancel: () -> Unit) {
        var showCamera by remember { mutableStateOf(false) }
        
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    onComplete(MemoryItem.Image(imageUrl = uri.toString()))
                } else {
                    onCancel()
                }
            }
        )

        if (showCamera) {
            CameraScreen(
                mediaStorageManager = mediaStorageManager,
                onImageCaptured = { uri ->
                    onComplete(MemoryItem.Image(imageUrl = uri.toString()))
                },
                onCancel = { showCamera = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.capture_image_title),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { showCamera = true },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.camera_label))
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.gallery_label))
                    }
                }
                
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        }
    }
}
