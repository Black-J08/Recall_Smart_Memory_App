package com.smartmemory.recall.domain.ai

import android.content.Context
import com.smartmemory.recall.data.network.FileDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartmemory.recall.domain.ai.GPUBackendDetector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages LLM model weights and configuration files.
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDownloader: FileDownloader
) {
    /**
     * Returns the root directory where models are stored.
     * Checks external storage first (convenient for sideloading), then internal.
     */
    private fun getModelRootPath(modelId: String): File {
        val externalDir = context.getExternalFilesDir(null)?.let { File(it, "models/$modelId") }
        if (externalDir != null && externalDir.exists() && File(externalDir, "mlc-chat-config.json").exists()) {
            return externalDir
        }
        
        // Default to internal storage subdirectory "mlc"
        val internalRoot = File(context.filesDir, "mlc")
        return File(internalRoot, modelId)
    }

    private fun getLegacyModelRoot(modelId: String): File {
        return File(File(context.filesDir, "mlc"), modelId)
    }

    /**
     * Checks if the required model files are present locally.
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val modelDir = getModelRootPath(modelId)
        if (!modelDir.exists()) return false
        
        // Basic check for essential MLC files
        val hasConfig = File(modelDir, "mlc-chat-config.json").exists()
        val hasWeights = File(modelDir, "ndarray-cache.json").exists()
        
        // Also check for at least one param shard
        val hasParams = modelDir.listFiles()?.any { it.name.startsWith("params_shard_") } == true
        
        return hasConfig && hasWeights && hasParams
    }

    /**
     * Downloads the model files from the specified URL (now points to GitHub).
     */
    suspend fun downloadModel(modelId: String, onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelDir = getLegacyModelRoot(modelId)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            val model = com.smartmemory.recall.domain.model.AIModels.ALL_MODELS.find { it.id == modelId }
                ?: run {
                    Log.e("ModelManager", "Model not found: $modelId")
                    return@withContext Result.failure(Exception("Model not found"))
                }

            Log.d("ModelManager", "Starting download for model: $modelId from ${model.downloadUrl}")

            val baseUrl = model.downloadUrl.removeSuffix("/")
            
            // 1. Determine best backend for this device
            val backends = backendDetector.detectAvailableBackends()
            val bestBackend = when {
                backends.contains(GPUBackendDetector.Backend.VULKAN) -> "vulkan"
                backends.contains(GPUBackendDetector.Backend.OPENCL) -> "opencl"
                else -> "cpu"
            }
            
            Log.i("ModelManager", "Selected backend for download: $bestBackend")

            // 2. Prepare file list
            // New Strategy: Download weights ZIP + Backend TAR
            
            // A. Weights ZIP
            val weightsZipName = "${model.modelLib}_weights.zip"
            val weightsZipFile = File(modelDir, weightsZipName)
            val weightsUrl = "$baseUrl/$weightsZipName"
            
            if (!isModelDownloaded(modelId)) {
                // Download ZIP
                Log.d("ModelManager", "Downloading weights zip: $weightsUrl")
                fileDownloader.downloadFile(weightsUrl, weightsZipFile) { progress ->
                    onProgress(progress * 0.8f) // 80% of progress for zip
                }.getOrThrow()
                
                // Extract ZIP
                Log.d("ModelManager", "Extracting weights zip...")
                unzip(weightsZipFile, modelDir)
                weightsZipFile.delete() // Cleanup zip after extraction
            }
            
            // B. Backend-specific Library
            val libName = "${model.modelLib}_$bestBackend.tar"
            val libUrl = "$baseUrl/$libName"
            val finalLibFile = File(modelDir, "${model.modelLib}.tar")
            
            if (!finalLibFile.exists()) {
                Log.d("ModelManager", "Downloading library: $libUrl")
                fileDownloader.downloadFile(libUrl, finalLibFile) { progress ->
                    onProgress(0.8f + (progress * 0.2f)) // Remaining 20%
                }.getOrThrow()
            } else {
                 onProgress(1.0f)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ModelManager", "Unexpected error in downloadModel", e)
            Result.failure(e)
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                // Ensure parent dirs exist
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun isEssential(fileName: String): Boolean {
        // Legacy check, mainly used by isModelDownloaded now
        return fileName == "mlc-chat-config.json" || 
               fileName == "ndarray-cache.json" || 
               fileName == "params_shard_0.bin" ||
               fileName.endsWith(".tar") ||
               fileName == "tokenizer.json"
    }
    
    fun getModelPath(modelId: String): String = getModelRootPath(modelId).absolutePath

    /**
     * Deletes the local model files.
     */
    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelDir = getModelRootPath(modelId)
            if (modelDir.exists()) {
                val deleted = modelDir.deleteRecursively()
                if (deleted) {
                    Log.d("ModelManager", "Successfully deleted model: $modelId")
                    Result.success(Unit)
                } else {
                    Log.e("ModelManager", "Failed to delete model directory: $modelId")
                    Result.failure(Exception("Failed to delete model directory"))
                }
            } else {
                Result.success(Unit) // Already gone
            }
        } catch (e: Exception) {
            Log.e("ModelManager", "Error deleting model: $modelId", e)
            Result.failure(e)
        }
    }
}
