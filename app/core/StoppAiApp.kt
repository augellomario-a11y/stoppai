// FILE: StoppAiApp.kt
// SCOPO: Classe Application per inizializzare componenti in startup (Contacts Cache)
// DIPENDENZE: ContactCacheManager.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.core

import android.app.Application

class StoppAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            ContactCacheManager.loadContactsSync(this)
            ContactCacheManager.startSync(this)
        } catch (e: Exception) {
            android.util.Log.w("STOPPAI", "Errore cache: ${e.message}")
        }
        
        try {
            val audio = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            val volOriginale = audio.getStreamVolume(android.media.AudioManager.STREAM_RING)
            val prefs = getSharedPreferences("stoppai_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putInt("vol_originale", volOriginale).apply()
            
            val protezioneAttiva = prefs.getBoolean("protezione_base", false)
            if (protezioneAttiva) {
                audio.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
            }
        } catch (e: Exception) {}
    }
}
