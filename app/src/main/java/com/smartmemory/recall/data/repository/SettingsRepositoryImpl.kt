package com.smartmemory.recall.data.repository

import com.smartmemory.recall.domain.model.AIModels
import com.smartmemory.recall.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    private val _selectedModelId = MutableStateFlow(AIModels.QWEN_05B.id)
    override val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    override val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    override fun setSelectedModel(modelId: String) {
        _selectedModelId.value = modelId
    }

    override fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }
}
