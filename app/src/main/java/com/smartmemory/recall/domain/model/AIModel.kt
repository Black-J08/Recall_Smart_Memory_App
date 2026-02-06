package com.smartmemory.recall.domain.model

/**
 * Represents an AI model variant available for download.
 */
/**
 * Represents a configuration for a specific AI Model.
 *
 * @property id Unique identifier for the model (used for file storage folders).
 * @property displayName Human-readable name for UI.
 * @property filename The exact filename of the model binary (must match release artifact).
 * @property downloadUrl Direct URL to the GitHub Release asset.
 * @property sizeDescription Short string describing model size params.
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val tier: ModelTier,
    val sizeDescription: String,
    val estimatedSizeMB: Int,
    val description: String,
    val filename: String,
    val downloadUrl: String
)

enum class ModelTier {
    LITE,
    STANDARD,
    PRO
}

/**
 * Companion object with predefined model configurations.
 */
object AIModels {
    val QWEN_2_5_0_5B = AIModel(
        id = "qwen_2_5_0_5b_q8",
        displayName = "Qwen 2.5 0.5B (Q8)",
        tier = ModelTier.LITE,
        sizeDescription = "0.5B Params (Q8)",
        estimatedSizeMB = 550,
        description = "Lightweight model (Task Bundle). optimized for MediaPipe.",
        filename = "qwen2.5-0.5b-q8.task",
        downloadUrl = "https://github.com/Black-J08/Recall-Models/releases/download/v1.0.0/qwen2.5-0.5b-q8.task"
    )

    val ALL_MODELS = listOf(QWEN_2_5_0_5B)
}
