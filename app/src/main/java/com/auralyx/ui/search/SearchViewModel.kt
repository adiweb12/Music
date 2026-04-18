package com.auralyx.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.usecase.SearchMediaUseCase
import com.auralyx.player.AuralyxPlayer
import com.auralyx.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchMediaUseCase,
    private val player: AuralyxPlayer,
    private val repository: MediaRepository
) : ViewModel() {

    private val _query   = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val playerState = player.state

    val results: StateFlow<List<MediaItem>> = _query
        .debounce(300)
        .flatMapLatest { q -> searchUseCase(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }

    fun play(item: MediaItem, queue: List<MediaItem>) {
        viewModelScope.launch {
            val videoEnabled = item.isAD17
            val idx          = queue.indexOf(item).coerceAtLeast(0)
            player.playQueue(queue, idx, videoEnabled)
            repository.updateLastPlayed(item.id, System.currentTimeMillis())
        }
    }
}
