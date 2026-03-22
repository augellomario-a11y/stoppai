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
            // Inizializza volume nel DB se non esiste ancora
            val db = com.ifs.stoppai.db.StoppAiDatabase.getInstance(this)
            val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
            val audio = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            
            val volAttuale = audio.getStreamVolume(android.media.AudioManager.STREAM_RING)
            
            // Se nel DB c'è il default 7 ma il sistema ha un volume diverso e superiore a 0, lo salviamo come originale
            if (repo.getVolume() == 7 && volAttuale > 0) {
                repo.setVolume(volAttuale)
                android.util.Log.e("STOPPAI_VOL", "App start - Salvato vol_originale: $volAttuale")
            }
            
            val prefs = getSharedPreferences("stoppai_prefs", android.content.Context.MODE_PRIVATE)
            val protezioneAttiva = prefs.getBoolean("protezione_base", false)
            val protezioneTotale = prefs.getBoolean("protezione_totale", false)
            
            if (protezioneAttiva || protezioneTotale) {
                audio.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
            }
        } catch (e: Exception) {}
    }
}
