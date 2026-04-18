package com.auralyx.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralyx.domain.model.*
import com.auralyx.domain.repository.MediaRepository
import com.auralyx.player.AuralyxPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab { SONGS, ALBUMS, ARTISTS, FOLDERS, VIDEOS }

data class LibraryUiState(
    val tab: LibraryTab            = LibraryTab.SONGS,
    val songs: List<MediaItem>     = emptyList(),
    val albums: List<Album>        = emptyList(),
    val artists: List<Artist>      = emptyList(),
    val folders: List<Folder>      = emptyList(),
    val videos: List<MediaItem>    = emptyList(),
    val isLoading: Boolean         = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val player: AuralyxPlayer
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    val playerState = player.state

    init {
        viewModelScope.launch {
            combine(
                repository.getAllSongs(),
                repository.getAllAlbums(),
                repository.getAllArtists(),
                repository.getAllFolders(),
                repository.getAllMusicVideos()
            ) { songs, albums, artists, folders, videos ->
                _state.value.copy(
                    songs    = songs,
                    albums   = albums,
                    artists  = artists,
                    folders  = folders,
                    videos   = videos,
                    isLoading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun selectTab(tab: LibraryTab) = _state.update { it.copy(tab = tab) }

    fun play(item: MediaItem, queue: List<MediaItem>, videoEnabled: Boolean = false) {
        viewModelScope.launch {
            val idx = queue.indexOf(item).coerceAtLeast(0)
            player.playQueue(queue, idx, videoEnabled)
            repository.updateLastPlayed(item.id, System.currentTimeMillis())
        }
    }
}
