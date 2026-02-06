package com.smartmemory.recall.domain.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var textEmbedder: TextEmbedder? = null
    private val modelName = "universal_sentence_encoder.tflite"

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (textEmbedder != null) return@withContext Result.success(Unit)

        try {
            Log.d(TAG, "Initializing Embedding Service with $modelName")
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelName)
                .build()

            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()

            textEmbedder = TextEmbedder.createFromOptions(context, options)
            Log.d(TAG, "TextEmbedder initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextEmbedder", e)
            Result.failure(e)
        }
    }

    suspend fun embed(text: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        if (textEmbedder == null) {
            val initResult = initialize()
            if (initResult.isFailure) {
                return@withContext Result.failure(initResult.exceptionOrNull()!!)
            }
        }

        try {
            // MediaPipe Text Embedder is synchronous
            val embeddingResult = textEmbedder!!.embed(text)
            // USE typically returns one head, so we take the first embedding
            val floatEmbedding = embeddingResult.embeddingResult().embeddings().first().floatEmbedding()
            
            // Convert FloatArray to ByteArray (BLOB)
            val byteArray = floatArrayToByteArray(floatEmbedding)
            
            Result.success(byteArray)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding for text: ${text.take(20)}...", e)
            Result.failure(e)
        }
    }
    
    suspend fun embedToFloats(text: String): Result<FloatArray> = withContext(Dispatchers.Default) {
        embed(text).map { byteArrayToFloatArray(it) }
    }

    private fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val storageBuffer = ByteBuffer.allocate(floats.size * 4)
        storageBuffer.order(ByteOrder.LITTLE_ENDIAN) // Standard for Android/ARM
        val floatBuffer = storageBuffer.asFloatBuffer()
        floatBuffer.put(floats)
        return storageBuffer.array()
    }
    
    fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val storageBuffer = ByteBuffer.wrap(bytes)
        storageBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer = storageBuffer.asFloatBuffer()
        val floats = FloatArray(floatBuffer.remaining())
        floatBuffer.get(floats)
        return floats
    }

    fun close() {
        textEmbedder?.close()
        textEmbedder = null
    }

    companion object {
        private const val TAG = "EmbeddingService"
    }
}
