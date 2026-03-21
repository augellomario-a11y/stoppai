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
            val contactPrefs = getSharedPreferences("stoppai_prefs", android.content.Context.MODE_PRIVATE)
            val volSalvato = contactPrefs.getInt("vol_originale", 0)
            val audio = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            
            if (volSalvato == 0) {
                val volAttuale = audio.getStreamVolume(android.media.AudioManager.STREAM_RING)
                contactPrefs.edit().putInt("vol_originale", if (volAttuale > 0) volAttuale else 7).apply()
                android.util.Log.e("STOPPAI_VOL", "App start - Reset vol_originale")
            }
            
            val protezioneAttiva = contactPrefs.getBoolean("protezione_base", false)
            if (protezioneAttiva) {
                audio.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
            }
        } catch (e: Exception) {}
    }
}
