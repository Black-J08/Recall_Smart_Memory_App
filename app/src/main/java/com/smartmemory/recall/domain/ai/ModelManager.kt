package com.smartmemory.recall.domain.ai

import android.content.Context
import android.util.Log
import com.smartmemory.recall.data.network.FileDownloader
import com.smartmemory.recall.domain.model.AIModels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDownloader: FileDownloader
) {

    fun getModelPath(modelId: String): String {
        return File(context.filesDir, "models/$modelId").absolutePath
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val modelDir = File(context.filesDir, "models/$modelId")
        if (!modelDir.exists()) return false

        // Check for the main model file
        val modelFiles = modelDir.listFiles()
        return modelFiles?.any { it.name.endsWith(".bin") || it.name.endsWith(".task") } == true
    }

    suspend fun downloadModel(modelId: String, onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val model = AIModels.ALL_MODELS.find { it.id == modelId }
                ?: return@withContext Result.failure(Exception("Model definition not found"))

            val modelDir = File(context.filesDir, "models/$modelId")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            val fileName = model.filename
            val targetFile = File(modelDir, fileName)

            Log.d("ModelManager", "Downloading model to: ${targetFile.absolutePath}")

            if (targetFile.exists()) {
                Log.d("ModelManager", "Model file already exists, verifying size or overwriting skipped for now.")
                onProgress(1.0f)
                return@withContext Result.success(Unit)
            }

            fileDownloader.downloadFile(model.downloadUrl, targetFile) { progress ->
                onProgress(progress)
            }.getOrThrow()

            Log.i("ModelManager", "Download complete.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ModelManager", "Download failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val modelDir = File(context.filesDir, "models/$modelId")
        if (modelDir.exists() && modelDir.deleteRecursively()) {
            Result.success(Unit)
        } else {
            Result.success(Unit) // Consider success if already gone
        }
    }
}
