package com.smartmemory.recall.domain.model

/**
 * Represents an AI model variant available for download.
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val tier: ModelTier,
    val quantization: String,
    val modelLib: String, // The native library identifier
    val estimatedVramMB: Int,
    val estimatedSizeMB: Int,
    val minRamGB: Int,
    val description: String,
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
    val QWEN_05B_LITE = AIModel(
        id = "qwen-0.5b-lite",
        displayName = "Qwen 0.5B (Lite)",
        tier = ModelTier.LITE,
        quantization = "q4f16_1",
        modelLib = "qwen2_5_0_5b",
        estimatedVramMB = 512,
        estimatedSizeMB = 350,
        minRamGB = 3,
        description = "Fast and efficient. Automatically selects best GPU/CPU backend.",
        downloadUrl = "https://github.com/Black-J08/MLC_Compiled_Models/releases/download/v1.0.0"
    )

    val GEMMA_270M = AIModel(
        id = "gemma-3-270m",
        displayName = "Gemma-3 270M (MediaPipe)",
        tier = ModelTier.LITE,
        quantization = "4bit",
        modelLib = "mediapipe",
        estimatedVramMB = 256,
        estimatedSizeMB = 150,
        minRamGB = 2,
        description = "Ultra-lightweight Google model. Reliable on all devices.",
        downloadUrl = "https://huggingface.co/google/gemma-3-270m-it-4bit"
    )

    val QWEN_05B_CPU = AIModel(
        id = "qwen-0.5b-lite-cpu",
        displayName = "Qwen 0.5B (Safe Mode)",
        tier = ModelTier.LITE,
        quantization = "q4f16_1",
        modelLib = "qwen2_5_0_5b",
        estimatedVramMB = 512,
        estimatedSizeMB = 350,
        minRamGB = 3,
        description = "Forced CPU mode. Guaranteed to work on any device.",
        downloadUrl = "https://github.com/Black-J08/MLC_Compiled_Models/releases/download/v1.0.0"
    )

    val ALL_MODELS = listOf(GEMMA_270M, QWEN_05B_LITE, QWEN_05B_CPU)
}
