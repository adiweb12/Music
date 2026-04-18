package com.auralyx.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.model.PlayerState
import com.auralyx.domain.model.RepeatMode
import com.auralyx.utils.ThumbnailUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton player controller wrapping ExoPlayer.
 * Shared by the UI (ViewModels) and AuralyxPlaybackService via Hilt.
 *
 * Key behaviours:
 *  - playQueue       : resolves .aD17 paths, sets ExoPlayer media items, starts playback
 *  - setVideoEnabled : toggles video renderer via DefaultTrackSelector
 *  - state flow      : emits PlayerState updates consumed by all screens
 */
@Singleton
class AuralyxPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    val exoPlayer: ExoPlayer,
    private val trackSelector: DefaultTrackSelector
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val scope        = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob  : Job? = null
    private var resolveJob   : Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressLoop() else stopProgressLoop()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        duration    = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
            }

            override fun onMediaItemTransition(item: ExoMediaItem?, reason: Int) {
                val idx   = exoPlayer.currentMediaItemIndex
                val queue = _state.value.queue
                if (idx in queue.indices) {
                    _state.update { it.copy(currentItem = queue[idx], queueIndex = idx) }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(error = error.message, isBuffering = false) }
            }
        })
    }

    // ── Public API ────────────────────────────────────────────────────────

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0, videoEnabled: Boolean = false) {
        resolveJob?.cancel()
        resolveJob = scope.launch {
            // Resolve .aD17 files in parallel
            val resolved = items.map { item ->
                async(Dispatchers.IO) {
                    if (item.isAD17) item.copy(path = ThumbnailUtils.resolveAD17Path(context, item.path))
                    else item
                }
            }.awaitAll()

            withContext(Dispatchers.Main) {
                _state.update { it.copy(queue = resolved, queueIndex = startIndex, isVideoEnabled = videoEnabled) }
                exoPlayer.setMediaItems(resolved.map { buildExoItem(it) }, startIndex, 0)
                exoPlayer.prepare()
                applyVideoMode(videoEnabled)
                exoPlayer.play()
            }
        }
    }

    fun playSingle(item: MediaItem, videoEnabled: Boolean = false) =
        playQueue(listOf(item), 0, videoEnabled)

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _state.update { it.copy(progress = positionMs) }
    }

    fun seekToFraction(fraction: Float) {
        val dur = exoPlayer.duration.takeIf { it > 0 } ?: return
        seekTo((fraction * dur).toLong())
    }

    fun skipToNext() { if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem() }

    fun skipToPrev() {
        if (exoPlayer.currentPosition > 3_000) exoPlayer.seekTo(0)
        else if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem()
    }

    fun setVideoEnabled(enabled: Boolean) {
        _state.update { it.copy(isVideoEnabled = enabled) }
        applyVideoMode(enabled)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffleEnabled
        _state.update { it.copy(shuffleEnabled = next) }
        exoPlayer.shuffleModeEnabled = next
    }

    fun addToQueue(item: MediaItem) {
        scope.launch(Dispatchers.IO) {
            val resolved = if (item.isAD17) item.copy(path = ThumbnailUtils.resolveAD17Path(context, item.path)) else item
            withContext(Dispatchers.Main) {
                _state.update { it.copy(queue = it.queue + resolved) }
                exoPlayer.addMediaItem(buildExoItem(resolved))
            }
        }
    }

    fun release() {
        scope.cancel()
        exoPlayer.release()
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun buildExoItem(item: MediaItem): ExoMediaItem {
        val uri = if (File(item.path).exists()) Uri.fromFile(File(item.path)) else Uri.parse(item.path)
        return ExoMediaItem.Builder()
            .setUri(uri)
            .setMediaId(item.id.toString())
            .build()
    }

    /**
     * Audio-only: set max video size to 0×0 (disables the video renderer).
     * Video: clear constraints to allow any resolution.
     */
    private fun applyVideoMode(videoEnabled: Boolean) {
        val params = trackSelector.buildUponParameters()
        if (videoEnabled) {
            params.clearVideoSizeConstraints()
        } else {
            params.setMaxVideoSize(0, 0)
        }
        trackSelector.setParameters(params)
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _state.update {
                    it.copy(
                        progress = exoPlayer.currentPosition.coerceAtLeast(0),
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressLoop() { progressJob?.cancel() }
}
