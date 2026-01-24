package com.smartmemory.recall.ui.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureBottomSheet(
    viewModel: CaptureViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        if (uiState.activeStrategy == null) {
            CaptureOptionsList(
                strategies = viewModel.availableStrategies,
                onStrategySelected = { viewModel.selectStrategy(it) }
            )
        } else {
            uiState.activeStrategy?.CaptureUI(
                onComplete = { memory ->
                    viewModel.saveMemory(memory)
                    onDismiss()
                },
                onCancel = { viewModel.clearStrategy() }
            )
        }
    }
}

@Composable
fun CaptureOptionsList(
    strategies: List<MemoryTypeStrategy>,
    onStrategySelected: (MemoryTypeStrategy) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Capture Memory",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn {
            items(strategies) { strategy ->
                CaptureOptionItem(
                    icon = strategy.icon,
                    label = strategy.label,
                    color = strategy.color,
                    onClick = { onStrategySelected(strategy) }
                )
            }
        }
    }
}

@Composable
fun CaptureOptionItem(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
