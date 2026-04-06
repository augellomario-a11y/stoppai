// FILE: AriaMessagePlayer.kt
// SCOPO: Preview audio messaggi segreteria (preset e custom)
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.core

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * Singleton per riprodurre preview audio dei messaggi segreteria.
 * Gestisce uno solo stream alla volta: se parte un nuovo play, stoppa il precedente.
 */
object AriaMessagePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingTag: String? = null
    private var onCompletionExternal: (() -> Unit)? = null

    /**
     * Riproduce un URL audio remoto. Tag identifica univocamente la riga UI in play.
     */
    fun playUrl(url: String, tag: String, onCompletion: (() -> Unit)? = null) {
        stop()
        currentlyPlayingTag = tag
        onCompletionExternal = onCompletion

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    Log.d("STOPPAI_ARIA", "Playback completato: $tag")
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("STOPPAI_ARIA", "Errore playback: $what/$extra")
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("STOPPAI_ARIA", "Errore playUrl: ${e.message}")
            stop()
        }
    }

    /**
     * Riproduce un file locale (per preview registrazione custom appena fatta).
     */
    fun playLocalFile(path: String, tag: String, onCompletion: (() -> Unit)? = null) {
        stop()
        currentlyPlayingTag = tag
        onCompletionExternal = onCompletion

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    Log.d("STOPPAI_ARIA", "Playback locale completato: $tag")
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("STOPPAI_ARIA", "Errore playback locale: $what/$extra")
                    stop()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("STOPPAI_ARIA", "Errore playLocalFile: ${e.message}")
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        val cb = onCompletionExternal
        onCompletionExternal = null
        currentlyPlayingTag = null
        cb?.invoke()
    }

    fun isPlaying(tag: String): Boolean {
        return currentlyPlayingTag == tag && mediaPlayer?.isPlaying == true
    }

    fun currentTag(): String? = currentlyPlayingTag
}
