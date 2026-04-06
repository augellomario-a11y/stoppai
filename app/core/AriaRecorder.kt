// FILE: AriaRecorder.kt
// SCOPO: Registrazione audio segreteria personalizzata (max 30s)
// NOTE: Android MediaRecorder non produce WAV nativamente.
//       Registriamo in AAC/MP4 poi convertiamo in WAV PCM con pipeline semplice.
//       In alternativa per non introdurre dipendenze esterne, usiamo AudioRecord
//       direttamente in PCM e scriviamo header WAV manuale.
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.core

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Registratore audio PCM WAV. Produce file mono 16kHz 16bit compatibili con Asterisk
 * (che poi fara' resampling a 8kHz automatico).
 */
class AriaRecorder(private val context: Context) {

    companion object {
        const val MAX_SECONDS = 30
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var startTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopTimer: Runnable? = null

    /**
     * Callback chiamato ogni secondo con i secondi trascorsi (per timer UI).
     */
    var onTick: ((secondsElapsed: Int) -> Unit)? = null

    /**
     * Callback chiamato quando la registrazione termina (per stop automatico o manuale).
     */
    var onFinished: ((file: File?) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording) return false
        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (bufferSize <= 0) {
                Log.e("STOPPAI_REC", "BufferSize non valido: $bufferSize")
                return false
            }

            outputFile = File(context.cacheDir, "aria_custom_rec.wav")
            outputFile?.delete()

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("STOPPAI_REC", "AudioRecord non inizializzato")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true
            startTime = System.currentTimeMillis()

            recordingThread = Thread { writePcmToWav(bufferSize) }
            recordingThread?.start()

            // Timer tick
            startTickLoop()

            // Stop automatico dopo 30s
            stopTimer = Runnable { stop() }
            mainHandler.postDelayed(stopTimer!!, MAX_SECONDS * 1000L)

            Log.d("STOPPAI_REC", "Registrazione iniziata")
            return true
        } catch (e: Exception) {
            Log.e("STOPPAI_REC", "Errore start: ${e.message}")
            cleanup()
            return false
        }
    }

    fun stop(): File? {
        if (!isRecording) return outputFile
        isRecording = false

        stopTimer?.let { mainHandler.removeCallbacks(it) }
        stopTimer = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try { recordingThread?.join(500) } catch (_: Exception) {}
        recordingThread = null

        outputFile?.let { fixWavHeader(it) }
        Log.d("STOPPAI_REC", "Registrazione terminata: ${outputFile?.length()} bytes")
        mainHandler.post { onFinished?.invoke(outputFile) }
        return outputFile
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    private fun startTickLoop() {
        val runnable = object : Runnable {
            override fun run() {
                if (!isRecording) return
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                onTick?.invoke(elapsed.coerceAtMost(MAX_SECONDS))
                if (isRecording) mainHandler.postDelayed(this, 500)
            }
        }
        mainHandler.post(runnable)
    }

    private fun writePcmToWav(bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        try {
            FileOutputStream(outputFile).use { fos ->
                // Header placeholder (verra' riempito in fixWavHeader dopo lo stop)
                fos.write(ByteArray(44))
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) fos.write(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            Log.e("STOPPAI_REC", "Errore writePcmToWav: ${e.message}")
        }
    }

    private fun fixWavHeader(file: File) {
        try {
            val totalAudioLen = file.length() - 44
            val totalDataLen = totalAudioLen + 36
            val sampleRate = SAMPLE_RATE.toLong()
            val channels = 1
            val byteRate = (16 * sampleRate * channels / 8)

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(byteArrayOf(
                    'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte()
                ))
                raf.write(intToByteArray(totalDataLen.toInt()))
                raf.write(byteArrayOf(
                    'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
                    'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte()
                ))
                raf.write(intToByteArray(16))  // PCM chunk size
                raf.write(shortToByteArray(1)) // PCM format
                raf.write(shortToByteArray(channels.toShort()))
                raf.write(intToByteArray(sampleRate.toInt()))
                raf.write(intToByteArray(byteRate.toInt()))
                raf.write(shortToByteArray((channels * 16 / 8).toShort())) // block align
                raf.write(shortToByteArray(16)) // bits per sample
                raf.write(byteArrayOf(
                    'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()
                ))
                raf.write(intToByteArray(totalAudioLen.toInt()))
            }
        } catch (e: Exception) {
            Log.e("STOPPAI_REC", "Errore fixWavHeader: ${e.message}")
        }
    }

    private fun intToByteArray(v: Int): ByteArray = byteArrayOf(
        (v and 0xff).toByte(),
        ((v shr 8) and 0xff).toByte(),
        ((v shr 16) and 0xff).toByte(),
        ((v shr 24) and 0xff).toByte()
    )

    private fun shortToByteArray(v: Short): ByteArray {
        val i = v.toInt()
        return byteArrayOf((i and 0xff).toByte(), ((i shr 8) and 0xff).toByte())
    }

    private fun cleanup() {
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
        isRecording = false
    }
}
