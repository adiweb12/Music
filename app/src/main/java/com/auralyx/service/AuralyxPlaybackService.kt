package com.auralyx.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auralyx.player.AuralyxPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_ID = "auralyx_playback_channel"
private const val NOTIFICATION_ID = 1001

/**
 * Foreground service that keeps playback alive when the app is in background.
 * Uses Media3 MediaSessionService for proper MediaSession + notification handling.
 */
@UnstableApi
@AndroidEntryPoint
class AuralyxPlaybackService : MediaSessionService() {

    @Inject lateinit var auralyxPlayer: AuralyxPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSession.Builder(this, auralyxPlayer.exoPlayer)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady != true || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(com.auralyx.R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(com.auralyx.R.string.channel_description)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
