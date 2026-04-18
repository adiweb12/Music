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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central player controller that wraps ExoPlayer.
 * Manages queue, playback state, and audio/video mode toggling.
 * Exposed as a singleton so both the Service and ViewModels share state.
 */
@Singleton
class AuralyxPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    val exoPlayer: ExoPlayer,
    private val trackSelector: DefaultTrackSelector
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                // Auto-advance handled by ExoPlayer internally via queue
            }

            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                val idx = exoPlayer.currentMediaItemIndex
                val queue = _state.value.queue
                if (idx in queue.indices) {
                    _state.update { it.copy(currentItem = queue[idx], queueIndex = idx) }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(error = error.message) }
            }
        })
    }

    // ── Public API ───────────────────────────────────────────────────────

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0, videoEnabled: Boolean = false) {
        scope.launch {
            val resolvedItems = items.map { item ->
                if (item.isAD17) {
                    val tempPath = ThumbnailUtils.resolveAD17Path(context, item.path)
                    item.copy(path = tempPath)
                } else item
            }

            withContext(Dispatchers.Main) {
                _state.update { it.copy(queue = resolvedItems, queueIndex = startIndex, isVideoEnabled = videoEnabled) }
                val exoItems = resolvedItems.map { buildExoItem(it) }
                exoPlayer.setMediaItems(exoItems, startIndex, 0)
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
        val duration = exoPlayer.duration
        if (duration > 0) seekTo((fraction * duration).toLong())
    }

    fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem()
    }

    fun skipToPrev() {
        when {
            exoPlayer.currentPosition > 3000 -> exoPlayer.seekTo(0)
            exoPlayer.hasPreviousMediaItem() -> exoPlayer.seekToPreviousMediaItem()
        }
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
        val newShuffle = !_state.value.shuffleEnabled
        _state.update { it.copy(shuffleEnabled = newShuffle) }
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    fun addToQueue(item: MediaItem) {
        val resolved = scope.async(Dispatchers.IO) {
            if (item.isAD17) item.copy(path = ThumbnailUtils.resolveAD17Path(context, item.path))
            else item
        }
        scope.launch {
            val r = resolved.await()
            _state.update { it.copy(queue = it.queue + r) }
            exoPlayer.addMediaItem(buildExoItem(r))
        }
    }

    fun release() {
        scope.cancel()
        exoPlayer.release()
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun buildExoItem(item: MediaItem): ExoMediaItem {
        val uri = if (File(item.path).exists()) Uri.fromFile(File(item.path))
                  else Uri.parse(item.path)
        return ExoMediaItem.Builder()
            .setUri(uri)
            .setMediaId(item.id.toString())
            .build()
    }

    /**
     * Audio-only mode: disable video renderer via track selector parameters.
     * Video mode: re-enable it.
     */
    private fun applyVideoMode(videoEnabled: Boolean) {
        val params = trackSelector.buildUponParameters()
        if (videoEnabled) {
            params.clearVideoSizeConstraints()
                  .setMaxVideoSizeSd()
        } else {
            params.setMaxVideoSize(0, 0)   // effectively disables video track
        }
        trackSelector.setParameters(params)
    }

    private fun startProgressTracking() {
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

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }
}
