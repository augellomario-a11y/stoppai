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
    }
}
