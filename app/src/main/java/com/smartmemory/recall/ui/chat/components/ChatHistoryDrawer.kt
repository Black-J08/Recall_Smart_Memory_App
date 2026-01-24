package com.smartmemory.recall.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartmemory.recall.domain.model.ChatSession
import com.smartmemory.recall.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryDrawer(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onNewChat: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0A0A0A),
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Recent Conversations",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(sessions) { session ->
                    val isSelected = session.id == currentSessionId
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable { onSessionSelected(session.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(if (isSelected) Violet else Color.Transparent)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) Color.White else Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else Color.Gray,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
