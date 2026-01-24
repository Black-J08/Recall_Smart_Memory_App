package com.smartmemory.recall.ui.feed

import com.smartmemory.recall.domain.model.MemoryItem

data class FeedUiState(
    val isLoading: Boolean = false,
    val memories: List<MemoryItem> = emptyList(),
    val error: String? = null
)
