// FILE: AudioHelper.kt
// SCOPO: Gestione volume selettivo (SA-063-REFACTOR)
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.ifs.stoppai.db.StoppAiDatabase

object AudioHelper {

    /**
     * Ripristina il volume della suoneria al livello preferito
     */
    fun alzaVolume(context: Context) {
        Handler(Looper.getMainLooper()).post {
            try {
                val db = StoppAiDatabase.getInstance(context)
                val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
                val volTarget = repo.getVolumePreferito()
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                
                // SOLO STREAM_RING (Niente setRingerMode per evitare bug DND)
                am.setStreamVolume(AudioManager.STREAM_RING, volTarget, 0)
            } catch (e: Exception) {}
        }
    }

    /**
     * Silenzia selettivamente la suoneria (ZERO SQUILLI)
     */
    fun abbassaVolume(context: Context) {
        Handler(Looper.getMainLooper()).post {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                
                // SOLO STREAM_RING a 0
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            } catch (e: Exception) {}
        }
    }
}
