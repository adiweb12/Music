package com.auralyx.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auralyx.R
import com.auralyx.player.AuralyxPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_ID = "auralyx_playback"
private const val NOTIFICATION_ID = 1001

/**
 * MediaSessionService keeps ExoPlayer alive in the background and publishes
 * a rich media notification (album art, title, prev/play/next) automatically
 * via Media3's DefaultMediaNotificationProvider.
 *
 * The notification appears as soon as playback starts and persists until
 * the user dismisses it or the app has nothing to play.
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
            .setId("auralyx_session")
            .build()

        // Media3 default provider renders album art + transport controls in the notification
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .also { provider ->
                    provider.setSmallIcon(R.drawable.ic_notification)
                }
        )
    }

    override fun onGetSession(info: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /**
     * When user swipes the app away: keep playing if there's active media,
     * otherwise clean up and stop.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: run { stopSelf(); return }
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
