package com.smartmemory.recall.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartmemory.recall.domain.model.ChatMessage
import com.smartmemory.recall.domain.ai.AIEngineState
import com.smartmemory.recall.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.retryInitialization()
    }
    
    ChatContent(
        state = state,
        onMenuClick = onMenuClick,
        onSettingsClick = onSettingsClick,
        onSendMessage = { viewModel.sendMessage(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    state: ChatUiState,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recall AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "History")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // AI Engine Status Indicator
            AnimatedVisibility(
                visible = state.aiState is AIEngineState.Loading || state.aiState is AIEngineState.Error
            ) {
                when (val aiState = state.aiState) {
                    is AIEngineState.Loading -> {
                        LinearProgressIndicator(
                            progress = { aiState.progress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is AIEngineState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    else -> Unit
                }
            }

            val listState = rememberLazyListState()
            
            // Scroll to bottom (which is index 0 in reverse layout) on new message
            val isTyping = state.typingSessionIds.contains(state.currentSessionId)
            LaunchedEffect(state.messages.size, isTyping) {
                 // With reverse layout, the "bottom" is the start of the list, so we scroll to 0
                if (state.messages.isNotEmpty() || isTyping) {
                    listState.animateScrollToItem(0)
                }
            }

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }

                if (state.pendingAiResponse != null) {
                    val streamingMessage = ChatMessage(
                        text = state.pendingAiResponse!!,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    item {
                        ChatBubble(streamingMessage, isFirstInGroup = true)
                    }
                }

                // In reverse layout, list starts from bottom.
                // We reverse the messages so the latest message is at index 0 (bottom).
                itemsIndexed(state.messages.asReversed()) { index, message ->
                    // Check if previous message (visually above, so next in list) was from same sender
                    // In reversed list, "previous" message is at index + 1
                    val prevMessage = state.messages.asReversed().getOrNull(index + 1)
                    val isFirstInGroup = prevMessage?.isUser != message.isUser
                    
                    ChatBubble(message, isFirstInGroup)
                }
            }

            ChatInputBar(
                onSendMessage = onSendMessage,
                onAttachClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Attach feature coming soon!")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isFirstInGroup: Boolean) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    
    // Dynamic rounded corners
    val topStart = if (!isUser && !isFirstInGroup) 4.dp else 20.dp
    val topEnd = if (isUser && !isFirstInGroup) 4.dp else 20.dp
    val bottomStart = 20.dp
    val bottomEnd = 20.dp
    
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = if (isUser) 2.dp else 0.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                
                if (message.timestamp > 0) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onAttachClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme
    
    val rainbowBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick) {
            Icon(Icons.Default.Add, contentDescription = "Attach", tint = colorScheme.onSurfaceVariant)
        }
        
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
            cursorBrush = SolidColor(colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                     if (text.isEmpty()) {
                        Text(
                            "Message...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        val isSendEnabled = text.isNotBlank()
        
        IconButton(
            onClick = {
                if (isSendEnabled) {
                    onSendMessage(text)
                    text = ""
                }
            },
            enabled = isSendEnabled
        ) {
            val buttonColor = if (isSendEnabled) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(buttonColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (isSendEnabled) colorScheme.onPrimary else colorScheme.surface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // Simple pulsing dot animation could be added here
        Text(
            "Recall is thinking...",
            style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
