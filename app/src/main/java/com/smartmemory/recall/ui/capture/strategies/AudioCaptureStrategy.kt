package com.smartmemory.recall.ui.capture.strategies

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.smartmemory.recall.R
import com.smartmemory.recall.data.local.AudioRecorder
import com.smartmemory.recall.data.local.MediaStorageManager
import com.smartmemory.recall.data.local.entity.MemoryType
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.ui.capture.components.WaveformVisualizer
import com.smartmemory.recall.ui.theme.MemoryAudioDark
import kotlinx.coroutines.delay
import javax.inject.Inject

class AudioCaptureStrategy @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val mediaStorageManager: MediaStorageManager
) : MemoryTypeStrategy {
    override val type = MemoryType.AUDIO
    override val icon = Icons.Default.Audiotrack
    override val label = R.string.audio_memory_label
    override val color = MemoryAudioDark
    
    @Composable
    override fun CaptureUI(onComplete: (MemoryItem) -> Unit, onCancel: () -> Unit) {
        val context = LocalContext.current
        var isRecording by remember { mutableStateOf(false) }
        var amplitudes by remember { mutableStateOf(listOf<Int>()) }
        var recordingFile by remember { mutableStateOf<java.io.File?>(null) }
        var startTime by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        
        var hasAudioPermission by remember { 
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) 
        }
        
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasAudioPermission = isGranted
        }

        LaunchedEffect(isRecording) {
            if (isRecording) {
                startTime = System.currentTimeMillis()
                while (isRecording) {
                    amplitudes = amplitudes + audioRecorder.getMaxAmplitude()
                    duration = System.currentTimeMillis() - startTime
                    delay(100)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (isRecording) stringResource(R.string.recording_status) else stringResource(R.string.ready_to_record),
                style = MaterialTheme.typography.titleMedium
            )
            
            if (isRecording) {
                WaveformVisualizer(
                    amplitudes = amplitudes,
                    color = color,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Text(
                    formatDuration(duration),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!isRecording) {
                    Button(
                        onClick = {
                            if (hasAudioPermission) {
                                val file = mediaStorageManager.createAudioFile()
                                recordingFile = file
                                audioRecorder.start(file)
                                isRecording = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.start_recording))
                    }
                    
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel_label))
                    }
                } else {
                    Button(
                        onClick = {
                            isRecording = false
                            audioRecorder.stop()
                            recordingFile?.let { file ->
                                onComplete(MemoryItem.Audio(
                                    filePath = file.absolutePath,
                                    durationMs = duration
                                ))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.stop_recording))
                    }
                }
            }
        }
    }
    
    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
