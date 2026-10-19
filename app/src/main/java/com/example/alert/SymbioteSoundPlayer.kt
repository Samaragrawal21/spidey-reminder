package com.example.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File

object SymbioteSoundPlayer {

    private const val TAG = "SymbioteSoundPlayer"
    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    private var playbackRequested: Boolean = false
    private val lock = Any()

    fun start(context: Context) {
        val appContext = context.applicationContext ?: context
        synchronized(lock) {
            playbackRequested = true
            stopAndReleaseInternal()
            val soundFile = AudioAssetManager.getAlertSoundFile(appContext)
            if (soundFile.exists() && soundFile.length() > 0L) {
                startPlayerWithFileLocked(soundFile)
                return
            }
        }

        // If the file hasn't finished downloading yet, download on background thread and play if still active
        AudioAssetManager.ensureAlertSoundDownloadedAsync(appContext) { downloadedFile ->
            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0L) {
                synchronized(lock) {
                    if (playbackRequested && mediaPlayer == null) {
                        startPlayerWithFileLocked(downloadedFile)
                    }
                }
            }
        }
    }

    private fun startPlayerWithFileLocked(soundFile: File) {
        try {
            val player = MediaPlayer()
            player.setDataSource(soundFile.absolutePath)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = true
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alert audio from ${soundFile.absolutePath}", e)
        }
    }

    fun stopAndRelease() {
        synchronized(lock) {
            playbackRequested = false
            stopAndReleaseInternal()
        }
    }

    private fun stopAndReleaseInternal() {
        val current = mediaPlayer
        mediaPlayer = null
        if (current != null) {
            try {
                if (current.isPlaying) {
                    current.stop()
                }
            } catch (ignored: Exception) {
            } finally {
                try {
                    current.release()
                } catch (ignored: Exception) {
                }
            }
        }
    }
}
