// FILE: CallScreeningServiceImpl.kt
// SCOPO: Logica di screening sincrona obbligatoria per tolleranza zero ritardi
// DIPENDENZE: ContactCacheManager.kt, CallLogEntry.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.core

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import com.ifs.stoppai.db.CallLogEntry
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallScreeningServiceImpl : CallScreeningService() {

    // OnScreenCall - Punto di ingresso intercettazione
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        val prefs = applicationContext.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        var isTotaleActive = prefs.getBoolean("protezione_totale", false)
        val scadenza = prefs.getLong("protezione_totale_scadenza", 0L)

        // Controlla se è scaduta
        if (isTotaleActive && System.currentTimeMillis() > scadenza) {
            prefs.edit().putBoolean("protezione_totale", false).apply()
            isTotaleActive = false
        }

        if (isTotaleActive) {
            // Protezione totale — silenzio per tutti
            // Non alzare il volume per nessuno
            respondToCall(callDetails,
                CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipNotification(true)
                .build())
            // Salva nel DB
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                saveCallLog(normalizedNumber)
            }
            return
        }

        // Risposta sempre permessa
        // Non blocchiamo mai nessuno
        respondToCall(callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipNotification(false)
                .build()
        )

        // Controlla rubrica in background
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            val isContact = ContactCacheManager.isContact(normalizedNumber)

            android.util.Log.e("STOPPAI_SCREEN", "Numero chiamante: $phoneNumber")
            android.util.Log.e("STOPPAI_SCREEN", "Cache size: ${ContactCacheManager.getSize()}")
            android.util.Log.e("STOPPAI_SCREEN", "È contatto: $isContact")

            if (isContact) {
                // È in rubrica → alza volume
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        val audio = applicationContext.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                        val volOriginale = applicationContext.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE).getInt("vol_originale", 5)
                        audio.setStreamVolume(android.media.AudioManager.STREAM_RING, volOriginale, 0)
                        
                        // Rispegni dopo 35 secondi
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                audio.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
                            } catch (e: Exception) {}
                        }, 35000)
                    } catch (e: Exception) {
                        android.util.Log.e("STOPPAI", "Volume: ${e.message}")
                    }
                }
            } else {
                // Non è in rubrica
                // Non fare nulla
                // Volume rimane a 0
                // Salva nel DB
                saveCallLog(normalizedNumber)
            }
        }
    }

    // Salva record chiamata loggata asincronamente
    private fun saveCallLog(rawNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val norm = PhoneNumberUtils.normalizeNumber(rawNumber)
            val callType = when {
                norm.isBlank() -> "PRIVATE"
                norm.startsWith("+390") -> "LANDLINE"
                else -> "MOBILE"
            }
            val db = StoppAiDatabase.getDatabase(applicationContext)
            val entry = CallLogEntry(
                phoneNumber = rawNumber.ifEmpty { "Sconosciuto/Privato" },
                callType = callType,
                timestamp = System.currentTimeMillis(),
                statusId = 0, // DA_GESTIRE
                notes = ""
            )
            db.callLogDao().insertCallLog(entry)
        }
    }
}
