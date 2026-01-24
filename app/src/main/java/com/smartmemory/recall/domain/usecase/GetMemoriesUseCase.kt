package com.smartmemory.recall.domain.usecase

import com.smartmemory.recall.domain.model.MemoryItem
import com.smartmemory.recall.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMemoriesUseCase @Inject constructor(
    private val repository: MemoryRepository
) {
    operator fun invoke(): Flow<Result<List<MemoryItem>>> {
        return repository.getMemories()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}
