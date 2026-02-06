package com.smartmemory.recall.domain.repository

import com.smartmemory.recall.domain.model.AIModel
import com.smartmemory.recall.domain.model.AIModels
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val selectedModelId: StateFlow<String>
    val isDarkMode: StateFlow<Boolean>
    
    fun setSelectedModel(modelId: String)
    fun setDarkMode(enabled: Boolean)
    
    fun getSelectedModel(): AIModel {
        return AIModels.ALL_MODELS.find { it.id == selectedModelId.value } ?: AIModels.QWEN_05B
    }
}
