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
import android.provider.ContactsContract
import android.net.Uri
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
                    saveCallLog(normalizedNumber, "PASSATA")
                    respondToCall(callDetails,
                        CallResponse.Builder()
                            .setDisallowCall(false)
                            .build())
                    return
                }
            }

            // Silenzio per tutti (o includi preferiti attivo)
            abbassaVolume()
            saveCallLog(normalizedNumber, "DEVIATA")
            respondToCall(callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .build())
            return
        }

        // 2. Logica PROTEZIONE BASE
        val isContact = ContactCacheManager.isContact(normalizedNumber)

        if (prefs.getBoolean("protezione_base", false)) {
            if (isContact) {
                alzaVolume()
                saveCallLog(normalizedNumber, "PASSATA")
                respondToCall(callDetails,
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .build())
            } else {
                abbassaVolume()
                saveCallLog(normalizedNumber, "DEVIATA")
                respondToCall(callDetails,
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .build())
            }
        } else {
            // Protezione OFF → passa tutto normalmente
            alzaVolume()
            saveCallLog(normalizedNumber, "PASSATA")
            respondToCall(callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .build())
        }
    }

    // Alza volume al target desiderato
    private fun alzaVolume() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val db = StoppAiDatabase.getInstance(applicationContext)
                val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
                val volTarget = repo.getVolumePreferito()

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, volTarget, 0)
                android.util.Log.e("STOPPAI_SCREEN", "Volume ripristinato a: $volTarget")
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_SCREEN", "Errore alzaVolume: ${e.message}")
            }
        }
    }

    // Forza silenzio totale
    private fun abbassaVolume() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_SCREEN", "Errore abbassaVolume: ${e.message}")
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
    private fun saveCallLog(rawNumber: String, outcome: String = "PASSATA") {
        CoroutineScope(Dispatchers.IO).launch {
            val displayName = getContactName(rawNumber)
            val logNumber = rawNumber.ifEmpty { "Numero nascosto" }
            
            val norm = PhoneNumberUtils.normalizeNumber(rawNumber)
            val callType = when {
                norm.isBlank() -> "PRIVATE"
                norm.startsWith("+390") -> "LANDLINE"
                else -> "MOBILE"
            }
            val db = StoppAiDatabase.getInstance(applicationContext)
            val entry = CallLogEntry(
                phoneNumber = logNumber,
                callType = callType,
                timestamp = System.currentTimeMillis(),
                statusId = 0,
                nota = "",
                ariaNote = "",
                callOutcome = outcome,
                displayName = displayName
            )
            db.callLogDao().insertCallLog(entry)
        }
    }

    private fun getContactName(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else ""
            } ?: ""
        } catch (e: Exception) { "" }
    }
}
