package com.auralyx.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.repository.MediaRepository
import com.auralyx.player.AuralyxPlayer
import com.auralyx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentlyPlayed: List<MediaItem>  = emptyList(),
    val musicVideos: List<MediaItem>     = emptyList(),
    val allSongs: List<MediaItem>        = emptyList(),
    val isLoading: Boolean               = true,
    val isScanning: Boolean              = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val player: AuralyxPlayer,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val playerState = player.state
    val defaultVideoMode = prefs.isDefaultVideoMode

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getRecentlyPlayed(),
                repository.getAllMusicVideos(),
                repository.getAllSongs()
            ) { recent, videos, songs ->
                HomeUiState(
                    recentlyPlayed = recent,
                    musicVideos    = videos,
                    allSongs       = songs,
                    isLoading      = false
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun play(item: MediaItem, queue: List<MediaItem>, videoEnabled: Boolean? = null) {
        viewModelScope.launch {
            val useVideo = videoEnabled ?: (item.isAD17 && prefs.isDefaultVideoMode.first())
            val idx      = queue.indexOf(item).coerceAtLeast(0)
            player.playQueue(queue, idx, useVideo)
            repository.updateLastPlayed(item.id, System.currentTimeMillis())
        }
    }

    fun scanStorage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            repository.scanStorage()
            _uiState.update { it.copy(isScanning = false) }
        }
    }
}
