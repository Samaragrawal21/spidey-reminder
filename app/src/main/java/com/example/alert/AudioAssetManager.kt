package com.example.alert

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AudioAssetManager {

    private const val TAG = "AudioAssetManager"
    const val ALERT_AUDIO_URL = "https://files.catbox.moe/zhujv9.mp3"
    const val ALERT_FILE_NAME = "black_suit_alert.mp3"
    private const val LEGACY_V2_FILE_NAME = "black_suit_alert_v2.mp3"
    private const val LEGACY_BASE64_FILE_SIZE = 30633L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadLock = Any()

    /**
     * Returns the target internal storage file: context.filesDir.resolve("black_suit_alert.mp3").
     * Automatically purges any legacy Base64-generated file so the real MP3 is downloaded.
     */
    fun getAlertSoundFile(context: Context): File {
        val legacyV2 = context.filesDir.resolve(LEGACY_V2_FILE_NAME)
        if (legacyV2.exists()) {
            legacyV2.delete()
        }
        val targetFile = context.filesDir.resolve(ALERT_FILE_NAME)
        if (targetFile.exists() && targetFile.length() == LEGACY_BASE64_FILE_SIZE) {
            targetFile.delete()
        }
        return targetFile
    }

    /**
     * Returns the Uri for the local alert sound file.
     */
    fun getAlertSoundUri(context: Context): Uri {
        return Uri.fromFile(getAlertSoundFile(context))
    }

    /**
     * Checks if context.filesDir.resolve("black_suit_alert.mp3") exists.
     * If NOT, downloads the MP3 from https://files.catbox.moe/zhujv9.mp3 on a background thread
     * and saves it to context.filesDir.resolve("black_suit_alert.mp3").
     */
    fun ensureAlertSoundDownloadedAsync(
        context: Context,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val appContext = context.applicationContext ?: context
        scope.launch {
            val result = downloadAlertSoundIfNeeded(appContext)
            onComplete?.invoke(result)
        }
    }

    /**
     * Suspending helper to download https://files.catbox.moe/zhujv9.mp3 on Dispatchers.IO
     * if black_suit_alert.mp3 does not exist.
     */
    suspend fun ensureAlertSoundDownloaded(context: Context): File? = withContext(Dispatchers.IO) {
        downloadAlertSoundIfNeeded(context.applicationContext ?: context)
    }

    fun downloadAlertSoundIfNeeded(context: Context): File? {
        synchronized(downloadLock) {
            val targetFile = getAlertSoundFile(context)
            if (targetFile.exists() && targetFile.length() > 0L) {
                return targetFile
            }

            val tempFile = context.filesDir.resolve("$ALERT_FILE_NAME.tmp")
            var connection: HttpURLConnection? = null
            return try {
                val url = URL(ALERT_AUDIO_URL)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("User-Agent", "SpideyReminder/1.0 (Android)")
                }
                connection.connect()

                if (connection.responseCode in 200..299) {
                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                            output.fd.sync()
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0L) {
                        if (targetFile.exists()) {
                            targetFile.delete()
                        }
                        if (!tempFile.renameTo(targetFile)) {
                            tempFile.copyTo(targetFile, overwrite = true)
                            tempFile.delete()
                        }
                        targetFile
                    } else {
                        tempFile.delete()
                        null
                    }
                } else {
                    Log.e(TAG, "Failed to download alert MP3: HTTP ${connection.responseCode}")
                    tempFile.delete()
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading alert MP3 from $ALERT_AUDIO_URL", e)
                tempFile.delete()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
