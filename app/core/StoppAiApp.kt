// FILE: StoppAiApp.kt
// SCOPO: Classe Application per inizializzare componenti in startup (Contacts Cache)
// DIPENDENZE: ContactCacheManager.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.core

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.provider.CallLog

class StoppAiApp : Application() {
    private var missedCallObserver: MissedCallObserver? = null

    override fun onCreate() {
        super.onCreate()
        try {
            ContactCacheManager.loadContactsSync(this)
            ContactCacheManager.startSync(this)
            
            val handler = Handler(Looper.getMainLooper())
            missedCallObserver = MissedCallObserver(this, handler)
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                missedCallObserver!!
            )
        } catch (e: Exception) {
            android.util.Log.w("STOPPAI", "Errore cache: ${e.message}")
        }
    }
}
