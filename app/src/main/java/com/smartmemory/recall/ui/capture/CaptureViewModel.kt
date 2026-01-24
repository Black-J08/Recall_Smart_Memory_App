package com.smartmemory.recall.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmemory.recall.domain.capture.MemoryTypeStrategy
import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.domain.usecase.SaveMemoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val saveMemoryUseCase: SaveMemoryUseCase,
    private val strategiesSet: Set<@JvmSuppressWildcards MemoryTypeStrategy>
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()
    
    val availableStrategies = strategiesSet.toList()
    
    fun selectStrategy(strategy: MemoryTypeStrategy) {
        _uiState.update { it.copy(activeStrategy = strategy) }
    }
    
    fun clearStrategy() {
        _uiState.update { it.copy(activeStrategy = null) }
    }
    
    fun saveMemory(memory: MemoryItem) {
        viewModelScope.launch {
            saveMemoryUseCase(memory).onSuccess {
                _uiState.update { it.copy(isSuccess = true, activeStrategy = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
    
    fun resetState() {
        _uiState.update { CaptureUiState() }
    }
}

data class CaptureUiState(
    val activeStrategy: MemoryTypeStrategy? = null,
    val isSuccess: Boolean = false,
    val error: String? = null
)
