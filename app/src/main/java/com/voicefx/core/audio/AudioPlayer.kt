package com.voicefx.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    fun play(uri: Uri): Result<Unit> {
        return try {
            stop()
            val player = getPlayer()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stop() {
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    fun isCurrentlyPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    private fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            seekTo(0)
                        }
                    }
                })
            }
        }
        return exoPlayer!!
    }

    fun release() {
        stop()
    }
}
