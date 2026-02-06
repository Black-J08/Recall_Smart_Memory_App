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
import java.util.zip.GZIPInputStream
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
    private val fileDownloader: FileDownloader,
    private val backendDetector: GPUBackendDetector
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
     * Uses a flat search to handle any ZIP structure.
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val modelDir = getModelRootPath(modelId)
        if (!modelDir.exists()) return false
        
        val allFiles = modelDir.walkTopDown().filter { it.isFile }.toList()
        
        val hasConfig = allFiles.any { it.name == "mlc-chat-config.json" }
        val hasWeights = allFiles.any { it.name == "ndarray-cache.json" }
        val hasParams = allFiles.any { it.name.startsWith("params_shard_") }
        
        val isLibInstalled = File(modelDir, "lib_downloaded.marker").exists()
        
        val result = hasConfig && hasWeights && hasParams && isLibInstalled
        if (result) {
            Log.d("ModelManager", "Model $modelId fully verified.")
        } else {
            Log.w("ModelManager", "Model $modelId incomplete. Config: $hasConfig, Weights: $hasWeights, Params: $hasParams, Lib: $isLibInstalled")
        }
        return result
    }

    /**
     * Downloads the model files from the specified URL (GitHub Release).
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

            if (model.id.contains("placeholder")) {
                 return@withContext Result.success(Unit)
            }

            Log.d("ModelManager", "Starting download for model: $modelId")
            val baseUrl = model.downloadUrl.removeSuffix("/")
            val backend = model.preferredBackend

            // 1. Weights
            val weightsZipName = "${model.assetId}-weights.zip"
            val weightsZipFile = File(modelDir, weightsZipName)
            val weightsUrl = "$baseUrl/$weightsZipName"
            
            // Check if weights are already there by checking a specific file
            val weightMarker = File(modelDir, "ndarray-cache.json") 
            // We use a more granular check than isModelDownloaded here
            val needsWeights = !modelDir.walkTopDown().any { it.name == "ndarray-cache.json" }

            if (needsWeights) {
                Log.d("ModelManager", "Downloading weights zip: $weightsUrl")
                fileDownloader.downloadFile(weightsUrl, weightsZipFile) { progress ->
                    onProgress(progress * 0.6f) 
                }.getOrThrow()
                
                Log.d("ModelManager", "Extracting weights zip...")
                unzip(weightsZipFile, modelDir) { progress ->
                    onProgress(0.6f + (progress * 0.2f))
                }
                weightsZipFile.delete()
                autoStandardizeModelDir(modelDir) // FIX: Standardize after extraction
            } else {
                Log.i("ModelManager", "Weights already present.")
                onProgress(0.8f)
            }
            
            // 2. Library
            val libZipName = "${model.assetId}-lib-$backend.zip" 
            val libZipFile = File(modelDir, libZipName)
            val libUrl = "$baseUrl/$libZipName"
            val libMarker = File(modelDir, "lib_downloaded.marker")
            
            if (!libMarker.exists()) {
                Log.d("ModelManager", "Downloading library zip: $libUrl")
                fileDownloader.downloadFile(libUrl, libZipFile) { progress ->
                    onProgress(0.8f + (progress * 0.1f)) 
                }.getOrThrow()
                
                Log.d("ModelManager", "Extracting library zip...")
                unzip(libZipFile, modelDir) { progress ->
                    onProgress(0.9f + (progress * 0.1f))
                }
                libZipFile.delete()
                libMarker.createNewFile()
                // Library usually doesn't have nested folders, but safe to call
                // autoStandardizeModelDir(modelDir) 
            } else {
                 Log.i("ModelManager", "Library marker found.")
                 onProgress(1.0f)
            }
            
            Log.i("ModelManager", "Download process complete for $modelId")
            onProgress(1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ModelManager", "Unexpected error in downloadModel", e)
            Result.failure(e)
        }
    }

    private fun unzip(zipFile: File, targetDir: File, onProgress: ((Float) -> Unit)? = null) {
        val buffer = ByteArray(8192)
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    entry = zis.nextEntry
                    onProgress?.invoke(0.5f)
                }
            }
        }
        Log.d("ModelManager", "Unzip complete for ${zipFile.name}")
    }

    /**
     * Standardizes the model directory structure.
     * If the ZIP extracted into a single subdirectory (e.g., "qwen-0.5b/params/..."),
     * this moves everything up to the root level so the engine can find it.
     */
    private fun autoStandardizeModelDir(modelDir: File) {
        val children = modelDir.listFiles() ?: return

        // Case 1: Config is already at root. Ideal state.
        if (File(modelDir, "mlc-chat-config.json").exists()) {
            Log.d("ModelManager", "Model directory structure is already standard.")
            return
        }

        // Case 2: Config is inside a nested folder.
        // Find the folder that contains mlc-chat-config.json
        val nestedRoot = modelDir.walkTopDown()
            .find { it.name == "mlc-chat-config.json" }
            ?.parentFile

        if (nestedRoot != null && nestedRoot != modelDir) {
            Log.i("ModelManager", "Found nested model root: ${nestedRoot.absolutePath}. Standardizing...")
            
            // Move all files from nested root to modelDir
            nestedRoot.listFiles()?.forEach { file ->
                val destFile = File(modelDir, file.name)
                if (file.isDirectory) {
                     // Move directory
                     file.renameTo(destFile)
                } else {
                     // Move file
                     file.renameTo(destFile)
                }
            }
            
            // Cleanup empty nested directories
            nestedRoot.deleteRecursively()
            Log.d("ModelManager", "Model directory standardized.")
        } else {
            Log.w("ModelManager", "Could not autofix model directory. mlc-chat-config.json not found.")
        }
    }

    /**
     * Extracts a .tar (or .tar.gz / .tgz) file.
     * Automatically detects GZIP compression based on the first two bytes (0x1F, 0x8B).
     */
    private fun untar(tarFile: File, targetDir: File) {
        val inputStream = if (isGzip(tarFile)) {
            GZIPInputStream(FileInputStream(tarFile))
        } else {
            FileInputStream(tarFile)
        }
        
        inputStream.use { rawFis ->
            val buffer = ByteArray(512)
            while (true) {
                // Read exactly 512 bytes for the header
                if (!readFully(rawFis, buffer)) break
                
                // Header is 512 bytes. Check for end of archive (two null blocks)
                val fileName = String(buffer, 0, 100).trim { it <= ' ' || it == '\u0000' }
                if (fileName.isEmpty()) break // End of archive
                
                // Read file size (octal string at offset 124, length 12)
                val sizeString = String(buffer, 124, 12).trim { it <= ' ' || it == '\u0000' }
                val size = if (sizeString.isNotEmpty()) sizeString.toLong(8) else 0L
                
                // Type flag at offset 156
                val type = buffer[156].toInt().toChar()
                
                if (type == '0' || type == '\u0000' || type == '5') { // File or Directory
                    val outFile = File(targetDir, fileName)
                    if (type == '5' || fileName.endsWith("/")) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var remaining = size
                            val fileBuffer = ByteArray(8192)
                            while (remaining > 0) {
                                val toRead = remaining.coerceAtMost(fileBuffer.size.toLong()).toInt()
                                val read = rawFis.read(fileBuffer, 0, toRead)
                                if (read == -1) break
                                fos.write(fileBuffer, 0, read)
                                remaining -= read
                            }
                        }
                        // Skip padding to next 512-byte boundary
                        val padding = (512 - (size % 512)) % 512
                        if (padding > 0 && padding < 512) {
                            readFully(rawFis, ByteArray(padding.toInt()))
                        }
                    }
                } else {
                    // Skip other types (links, etc.)
                    val totalToSkip = size + (512 - (size % 512)) % 512
                    readFully(rawFis, ByteArray(totalToSkip.toInt()))
                }
            }
        }
    }

    private fun isGzip(file: File): Boolean {
        FileInputStream(file).use { fis ->
            val header = ByteArray(2)
            if (fis.read(header) < 2) return false
            return header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()
        }
    }

    private fun readFully(inputStream: java.io.InputStream, buffer: ByteArray): Boolean {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = inputStream.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) return false
            totalRead += read
        }
        return true
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
