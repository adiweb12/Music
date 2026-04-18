package com.auralyx.domain.usecase

import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(query: String): Flow<List<MediaItem>> {
        if (query.isBlank()) return flowOf(emptyList())
        return repository.searchAll(query.trim())
    }
}
