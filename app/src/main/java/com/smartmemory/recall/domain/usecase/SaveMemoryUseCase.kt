package com.smartmemory.recall.domain.usecase

import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.domain.repository.MemoryRepository
import javax.inject.Inject

class SaveMemoryUseCase @Inject constructor(
    private val repository: MemoryRepository
) {
    suspend operator fun invoke(memory: MemoryItem): Result<Long> {
        return repository.saveMemory(memory)
    }
}
