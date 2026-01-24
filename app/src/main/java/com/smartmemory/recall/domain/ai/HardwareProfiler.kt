package com.smartmemory.recall.domain.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.smartmemory.recall.domain.model.AIModel
import com.smartmemory.recall.domain.model.AIModels
import com.smartmemory.recall.domain.model.ModelTier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects device hardware capabilities and recommends the best AI model.
 */
@Singleton
class HardwareProfiler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class DeviceProfile(
        val totalRamGB: Int,
        val gpuVendor: String,
        val androidVersion: Int,
        val recommendedModel: AIModel
    )

    fun getDeviceProfile(): DeviceProfile {
        val totalRamGB = getTotalRamGB()
        val gpuVendor = getGPUVendor()
        val androidVersion = Build.VERSION.SDK_INT

        val recommendedModel = when {
            totalRamGB >= 8 -> AIModels.QWEN_3B_PRO
            totalRamGB >= 6 -> AIModels.QWEN_15B_STANDARD
            else -> AIModels.QWEN_05B_LITE
        }

        return DeviceProfile(
            totalRamGB = totalRamGB,
            gpuVendor = gpuVendor,
            androidVersion = androidVersion,
            recommendedModel = recommendedModel
        )
    }

    private fun getTotalRamGB(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024 * 1024)).toInt()
    }

    private fun getGPUVendor(): String {
        // Simplified GPU detection based on SoC manufacturer
        return when {
            Build.MANUFACTURER.equals("Qualcomm", ignoreCase = true) ||
            Build.HARDWARE.contains("qcom", ignoreCase = true) -> "Adreno (Qualcomm)"
            Build.HARDWARE.contains("mt", ignoreCase = true) ||
            Build.MANUFACTURER.equals("MediaTek", ignoreCase = true) -> "Mali (MediaTek)"
            Build.HARDWARE.contains("exynos", ignoreCase = true) -> "Mali (Samsung)"
            else -> "Unknown"
        }
    }

    fun meetsMinimumSpecs(): Boolean {
        return getTotalRamGB() >= 3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
