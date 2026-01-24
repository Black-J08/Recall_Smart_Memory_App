package com.smartmemory.recall.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartmemory.recall.domain.model.MemoryItem
import androidx.compose.runtime.*
import com.smartmemory.recall.ui.capture.CaptureBottomSheet
import com.smartmemory.recall.ui.core.components.CapturePillButton
import com.smartmemory.recall.ui.theme.*

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showCaptureSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            CapturePillButton(
                onClick = { showCaptureSheet = true }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (showCaptureSheet) {
            CaptureBottomSheet(onDismiss = { showCaptureSheet = false })
        }
        
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp,
                modifier = modifier.padding(top = padding.calculateTopPadding()) // Respect scaffold top, ignore bottom to allow scroll behind FAB
            ) {
                items(state.memories) { memory ->
                    MemoryCard(memory)
                }
            }
        }
    }
}

@Composable
fun MemoryCard(memory: MemoryItem) {
    // Determine Color and Icon based on type
    val (borderColor, icon, title) = when (memory) {
        is MemoryItem.Text -> Triple(MemoryTextDark, Icons.AutoMirrored.Filled.ShortText, "Text")
        is MemoryItem.Audio -> Triple(MemoryAudioDark, Icons.Default.Audiotrack, "Audio")
        is MemoryItem.Image -> Triple(Color.Gray, Icons.Default.Image, "Image")
    }
    
    // In High Contrast Dark, text is White, Surface is DarkGrey.
    // We add a colored border to indicate type.

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = borderColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = borderColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            when (memory) {
                is MemoryItem.Text -> {
                    Text(
                        text = memory.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                is MemoryItem.Audio -> {
                    // Visual Waveform Placeholder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(borderColor.copy(alpha = 0.2f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                         Text(
                            text = "${memory.durationMs / 1000}s",
                            style = MaterialTheme.typography.bodyLarge,
                            color = borderColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is MemoryItem.Image -> {
                    AsyncImage(
                        model = memory.imageUrl,
                        contentDescription = memory.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (!memory.caption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = memory.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = android.text.format.DateUtils.getRelativeTimeSpanString(
                    memory.timestamp,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}
