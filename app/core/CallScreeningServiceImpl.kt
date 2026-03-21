// FILE: CallScreeningServiceImpl.kt
// SCOPO: Logica di screening con supporto preferiti e timing protezione
// DIPENDENZE: ContactCacheManager.kt, CallLogEntry.kt
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.core

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import android.telephony.TelephonyCallback
import android.telephony.PhoneStateListener
import com.ifs.stoppai.db.CallLogEntry
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallScreeningServiceImpl : CallScreeningService() {

    // Inizializza listener fine chiamata
    override fun onCreate() {
        super.onCreate()
        listenCallEnd()
    }

    // OnScreenCall - Punto di ingresso intercettazione
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        val prefs = applicationContext.getSharedPreferences(
            "stoppai_prefs", Context.MODE_PRIVATE)
        var isTotaleActive = prefs.getBoolean("protezione_totale", false)
        val scadenza = prefs.getLong("protezione_totale_scadenza", 0L)

        // Controlla se è scaduta
        if (isTotaleActive && System.currentTimeMillis() > scadenza) {
            prefs.edit().putBoolean("protezione_totale", false).apply()
            isTotaleActive = false
        }

        // Protezione Totale attiva
        if (isTotaleActive) {
            val includiPreferiti = prefs.getBoolean("includi_preferiti", false)

            if (!includiPreferiti) {
                // Controlla se è un preferito (stellina)
                val isFavorite = ContactCacheManager.isFavorite(normalizedNumber)
                if (isFavorite) {
                    // Preferito → lascia squillare
                    android.util.Log.e("STOPPAI_SCREEN",
                        "Preferito durante Prot.Totale: $phoneNumber")
                    alzaVolume()
                    respondToCall(callDetails,
                        CallResponse.Builder()
                            .setDisallowCall(false)
                            .build())
                    return
                }
            }

            // Silenzio per tutti (o includi preferiti attivo)
            respondToCall(callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .build())
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                saveCallLog(normalizedNumber)
            }
            return
        }

        // Protezione Base — risposta permessa
        respondToCall(callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .build()
        )

        // Controlla rubrica in background
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            val isContact = ContactCacheManager.isContact(normalizedNumber)

            android.util.Log.e("STOPPAI_SCREEN",
                "Numero chiamante: $phoneNumber")
            android.util.Log.e("STOPPAI_SCREEN",
                "Cache size: ${ContactCacheManager.getSize()}")
            android.util.Log.e("STOPPAI_SCREEN",
                "È contatto: $isContact")

            if (isContact) {
                // È in rubrica → alza volume
                alzaVolume()
            } else {
                // Non è in rubrica — volume rimane a 0
                saveCallLog(normalizedNumber)
            }
        }
    }

    // Alza volume per contatto noto
    private fun alzaVolume() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val audio = applicationContext.getSystemService(
                    Context.AUDIO_SERVICE) as android.media.AudioManager
                val volOriginale = applicationContext
                    .getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                    .getInt("vol_originale", 5)
                audio.setStreamVolume(
                    android.media.AudioManager.STREAM_RING, volOriginale, 0)
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI", "Volume: ${e.message}")
            }
        }
    }

    // Listener fine chiamata — riporta volume a 0
    private fun listenCallEnd() {
        val tm = applicationContext.getSystemService(
            Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= 31) {
            tm.registerTelephonyCallback(
                mainExecutor,
                object : TelephonyCallback(),
                    TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        if (state == TelephonyManager.CALL_STATE_IDLE) {
                            silenceRing()
                        }
                    }
                })
        } else {
            @Suppress("DEPRECATION")
            tm.listen(
                object : PhoneStateListener() {
                    override fun onCallStateChanged(
                        state: Int, number: String?) {
                        if (state == TelephonyManager.CALL_STATE_IDLE) {
                            silenceRing()
                        }
                    }
                },
                PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    // Riporta volume suoneria a 0
    private fun silenceRing() {
        try {
            val prefs = applicationContext.getSharedPreferences(
                "stoppai_prefs", Context.MODE_PRIVATE)
            val protezioneAttiva = prefs.getBoolean("protezione_base", false)
            val protezioneTotale = prefs.getBoolean("protezione_totale", false)
            if (!protezioneAttiva && !protezioneTotale) return
            val audio = applicationContext.getSystemService(
                Context.AUDIO_SERVICE) as android.media.AudioManager
            audio.setStreamVolume(
                android.media.AudioManager.STREAM_RING, 0, 0)
            android.util.Log.e("STOPPAI_VOL",
                "Chiamata terminata → volume 0")
        } catch (e: Exception) {
            android.util.Log.e("STOPPAI", "silenceRing: ${e.message}")
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
