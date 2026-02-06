package com.smartmemory.recall.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartmemory.recall.domain.model.ModelStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val modelStatuses by viewModel.modelStatuses.collectAsState()
    val isNetworkRestricted by viewModel.isNetworkRestricted.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Network Restriction Warning
            if (isNetworkRestricted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ Network Access Blocked",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This app is blocked from accessing the internet. Enable \"Allow network access\" in Mobile data usage settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.openNetworkSettings() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Fix Network Access")
                            }
                            OutlinedButton(
                                onClick = { viewModel.dismissNetworkRestrictionWarning() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // AI Model Selection Section
            Text(
                text = "AI Model",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            modelStatuses.forEach { status ->
                ModelCard(
                    status = status,
                    isRecommended = status.model.id == viewModel.recommendedModelId,
                    onDownload = { viewModel.downloadModel(status.model.id) },
                    onSelect = { viewModel.selectModel(status.model.id) },
                    onDelete = { viewModel.deleteModel(status.model.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ModelCard(
    status: ModelStatus,
    isRecommended: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = status.model.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isRecommended) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Recommended",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${status.model.estimatedSizeMB} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (status) {
                is ModelStatus.NotDownloaded -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download")
                    }
                }
                is ModelStatus.Downloading -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloading... ${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is ModelStatus.Downloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (status.isSelected) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "✓ Selected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = onSelect,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Select")
                            }
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                    }
                }
                is ModelStatus.Error -> {
                    Column {
                        Text(
                            text = "Error: ${status.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
