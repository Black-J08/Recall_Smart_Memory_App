package com.smartmemory.recall.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmemory.recall.domain.usecase.GetMemoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getMemoriesUseCase: GetMemoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadMemories()
    }

    private fun loadMemories() {
        viewModelScope.launch {
            getMemoriesUseCase().collect { result ->
                result.onSuccess { memories ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            memories = memories
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            }
        }
    }
}
