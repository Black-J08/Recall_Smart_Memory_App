package com.smartmemory.recall.domain.ai

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects available GPU backends on the device for MLC LLM.
 * Priority: Vulkan > OpenCL > CPU
 */
@Singleton
class GPUBackendDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    enum class Backend {
        VULKAN,
        OPENCL,
        CPU
    }
    
    /**
     * Detects the best available backend for this device.
     * Returns a prioritized list of backends to try.
     */
    fun detectAvailableBackends(): List<Backend> {
        val availableBackends = mutableListOf<Backend>()
        
        // Check for Vulkan support
        if (hasVulkanSupport()) {
            availableBackends.add(Backend.VULKAN)
            Log.d(TAG, "Vulkan support detected")
        }
        
        // Check for OpenCL support
        if (hasOpenCLSupport()) {
            availableBackends.add(Backend.OPENCL)
            Log.d(TAG, "OpenCL support detected")
        }
        
        // CPU is always available as fallback
        availableBackends.add(Backend.CPU)
        Log.d(TAG, "CPU fallback available")
        
        return availableBackends
    }
    
    /**
     * Checks if the device supports Vulkan.
     * Vulkan is available on Android 7.0+ (API 24+)
     */
    private fun hasVulkanSupport(): Boolean {
        return try {
            // Vulkan is supported on API 24+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val packageManager = context.packageManager
                // Check for Vulkan feature
                packageManager.hasSystemFeature("android.hardware.vulkan.version")
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting Vulkan support", e)
            false
        }
    }
    
    /**
     * Checks if the device supports OpenCL.
     * This is a heuristic check as Android doesn't provide a direct API.
     */
    private fun hasOpenCLSupport(): Boolean {
        return try {
            // OpenCL is commonly available on devices with Qualcomm/Mali GPUs
            // We'll attempt to detect it by checking for common GPU vendors
            val glRenderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)
            val hasQualcomm = glRenderer?.contains("Adreno", ignoreCase = true) == true
            val hasMali = glRenderer?.contains("Mali", ignoreCase = true) == true
            
            Log.d(TAG, "GPU Renderer: $glRenderer")
            
            // Most Adreno and Mali GPUs support OpenCL
            hasQualcomm || hasMali
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting OpenCL support", e)
            false
        }
    }
    
    companion object {
        private const val TAG = "GPUBackendDetector"
    }
}
