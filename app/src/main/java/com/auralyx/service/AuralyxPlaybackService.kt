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

@UnstableApi
@AndroidEntryPoint
class AuralyxPlaybackService : MediaSessionService() {

    @Inject lateinit var auralyxPlayer: AuralyxPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSession.Builder(this, auralyxPlayer.exoPlayer).setId("auralyx").build()
        
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIF_ID)
            .build()
            
        // Using system icon until ic_notification is added to drawables
        provider.setSmallIcon(android.R.drawable.ic_media_play)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(root: Intent?) {
        val player = mediaSession?.player ?: run { stopSelf(); return }
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW)
                .apply { 
                    description = "Auralyx Controls"
                    setShowBadge(false)
                }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    companion object { 
        private const val CHANNEL_ID = "auralyx_playback"
        private const val NOTIF_ID = 1001 
    }
}

