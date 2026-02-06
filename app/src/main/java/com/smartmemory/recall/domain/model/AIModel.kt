package com.smartmemory.recall.domain.model

/**
 * Represents an AI model variant available for download.
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val tier: ModelTier,
    val quantization: String,
    val modelLib: String, // The native library identifier (symbols)
    val assetId: String,  // The asset name prefix (URLs/Filenames)
    val estimatedVramMB: Int,
    val estimatedSizeMB: Int,
    val minRamGB: Int,
    val description: String,
    val downloadUrl: String,
    val preferredBackend: String = "cpu" // Scalability: Default to cpu, but allow override
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
    val QWEN_05B = AIModel(
        id = "qwen-0.5b-cpu",
        displayName = "Qwen 2.5 0.5B",
        tier = ModelTier.LITE,
        quantization = "q4f16_1",
        modelLib = "qwen2_5_0_5b_q4f16_1",
        assetId = "qwen2_5_0_5b",
        estimatedVramMB = 512,
        estimatedSizeMB = 350,
        minRamGB = 3,
        description = "Lightweight model optimized for CPU. Good for general tasks.",
        downloadUrl = "https://github.com/Black-J08/MLC_Compiled_Models/releases/download/v1.2.12",
        preferredBackend = "cpu"
    )

    val GEMMA_PLACEHOLDER = AIModel(
        id = "gemma-mediapipe-placeholder",
        displayName = "Gemma 2 (Coming Soon)",
        tier = ModelTier.LITE,
        quantization = "4bit",
        modelLib = "mediapipe_placeholder",
        assetId = "gemma_placeholder",
        estimatedVramMB = 256,
        estimatedSizeMB = 150,
        minRamGB = 2,
        description = "MediaPipe integration placeholder. Not yet available.",
        downloadUrl = "" // Placeholder
    )

    val ALL_MODELS = listOf(QWEN_05B, GEMMA_PLACEHOLDER)
}
