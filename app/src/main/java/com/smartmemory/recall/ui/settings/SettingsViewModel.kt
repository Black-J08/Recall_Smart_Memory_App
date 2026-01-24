package com.smartmemory.recall.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmemory.recall.domain.ai.HardwareProfiler
import com.smartmemory.recall.domain.ai.ModelManager
import com.smartmemory.recall.domain.model.AIModels
import com.smartmemory.recall.domain.model.ModelStatus
import com.smartmemory.recall.domain.repository.SettingsRepository
import com.smartmemory.recall.util.NetworkRestrictionDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val modelManager: ModelManager,
    private val hardwareProfiler: HardwareProfiler,
    private val networkRestrictionDetector: NetworkRestrictionDetector
) : ViewModel() {
    
    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
    
    private val _modelStatuses = MutableStateFlow<List<ModelStatus>>(emptyList())
    val modelStatuses: StateFlow<List<ModelStatus>> = _modelStatuses.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    
    private val _isNetworkRestricted = MutableStateFlow(false)
    val isNetworkRestricted: StateFlow<Boolean> = _isNetworkRestricted.asStateFlow()
    
    val recommendedModelId: String = hardwareProfiler.getDeviceProfile().recommendedModel.id

    init {
        refreshModelStatuses()
    }

    fun toggleDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
    }
    
    fun refreshModelStatuses() {
        viewModelScope.launch {
            val selectedModelId = settingsRepository.selectedModelId.value
            val statuses = AIModels.ALL_MODELS.map { model ->
                val isDownloaded = modelManager.isModelDownloaded(model.id)
                val progress = _downloadProgress.value[model.id]
                
                when {
                    progress != null -> ModelStatus.Downloading(model, progress)
                    isDownloaded -> ModelStatus.Downloaded(model, model.id == selectedModelId)
                    else -> ModelStatus.NotDownloaded(model)
                }
            }
            _modelStatuses.value = statuses
        }
    }
    
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            val model = AIModels.ALL_MODELS.find { it.id == modelId } ?: return@launch
            
            // Update to downloading state
            _downloadProgress.value = _downloadProgress.value + (modelId to 0f)
            refreshModelStatuses()
            
            modelManager.downloadModel(modelId) { progress ->
                _downloadProgress.value = _downloadProgress.value + (modelId to progress)
                refreshModelStatuses()
            }.onSuccess {
                _downloadProgress.value = _downloadProgress.value - modelId
                refreshModelStatuses()
            }.onFailure { error ->
                _downloadProgress.value = _downloadProgress.value - modelId
                
                // Check if this is a network restriction issue
                if (error is java.net.UnknownHostException && networkRestrictionDetector.isNetworkRestricted()) {
                    _isNetworkRestricted.value = true
                }
                
                _modelStatuses.value = _modelStatuses.value.map {
                    if (it.model.id == modelId) {
                        ModelStatus.Error(model, error.message ?: "Download failed")
                    } else it
                }
            }
        }
    }
    
    fun selectModel(modelId: String) {
        settingsRepository.setSelectedModel(modelId)
        refreshModelStatuses()
    }
    
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelManager.deleteModel(modelId).onSuccess {
                refreshModelStatuses()
            }.onFailure { error ->
                // Optionally handle deletion failure UI feedback
                refreshModelStatuses()
            }
        }
    }
    
    fun openNetworkSettings() {
        networkRestrictionDetector.openNetworkSettings()
    }
    
    fun dismissNetworkRestrictionWarning() {
        _isNetworkRestricted.value = false
    }
}
