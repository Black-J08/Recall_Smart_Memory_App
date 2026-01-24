package com.smartmemory.recall.domain.model

/**
 * Represents the download and availability status of an AI model.
 */
sealed class ModelStatus {
    abstract val model: AIModel
    
    data class NotDownloaded(override val model: AIModel) : ModelStatus()
    data class Downloading(override val model: AIModel, val progress: Float) : ModelStatus()
    data class Downloaded(override val model: AIModel, val isSelected: Boolean) : ModelStatus()
    data class Error(override val model: AIModel, val message: String) : ModelStatus()
}
