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

data class HomeUiState(val recentlyPlayed:List<MediaItem>=emptyList(), val musicVideos:List<MediaItem>=emptyList(), val allSongs:List<MediaItem>=emptyList(), val isLoading:Boolean=true, val isScanning:Boolean=false)

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: MediaRepository, private val player: AuralyxPlayer, private val prefs: PreferencesManager) : ViewModel() {
    private val _ui = MutableStateFlow(HomeUiState()); val uiState = _ui.asStateFlow()
    val playerState = player.state

    init {
        viewModelScope.launch {
            combine(repo.getRecentlyPlayed(), repo.getAllMusicVideos(), repo.getAllSongs()) { r,v,s ->
                HomeUiState(r,v,s,false)
            }.collect { _ui.value = it }
        }
    }

    fun play(item: MediaItem, queue: List<MediaItem>, videoEnabled: Boolean? = null) {
        viewModelScope.launch {
            val video = videoEnabled ?: (item.isAD17 && prefs.isDefaultVideoMode.first())
            player.playQueue(queue, queue.indexOf(item).coerceAtLeast(0), video)
            repo.updateLastPlayed(item.id, System.currentTimeMillis())
        }
    }

    fun scanStorage() {
        viewModelScope.launch {
            _ui.update { it.copy(isScanning=true) }
            repo.scanStorage()
            _ui.update { it.copy(isScanning=false) }
        }
    }
}
