// FILE: CallScreeningServiceImpl.kt
// SCOPO: Orchestratore screening chiamate (SA-063-REFACTOR)
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import android.telephony.TelephonyCallback
import android.telephony.PhoneStateListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class CallScreeningServiceImpl : CallScreeningService() {

    companion object {
        const val BACKEND_URL = "https://stoppai.it"
    }

    override fun onCreate() {
        super.onCreate()
        listenCallEnd()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)
        val context = applicationContext
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        // Rileva direzione chiamata (API 29+)
        val isOutgoing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_OUTGOING
        } else false
        val direction = if (isOutgoing) "USCITA" else "ENTRATA"

        // Le chiamate in uscita: logga e lascia passare senza screening
        if (isOutgoing) {
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            CallLogHelper.saveCallLog(context, normalizedNumber, "USCITA", false, "USCITA")
            return
        }

        // Raccolta Dati
        val tipo = NumberClassifier.getTipoNumero(normalizedNumber)
        val isContact = ContactCacheManager.isContact(normalizedNumber)
        val isPreferito = isPreferito(context, normalizedNumber)

        val pBase = prefs.getBoolean("protezione_base", false)
        val pTotale = prefs.getBoolean("protezione_totale", false)
        val iPreferiti = prefs.getBoolean("includi_preferiti", false)
        val sAttivo = prefs.getBoolean("sms_risposta_attivo", false)
        val cEsteri = prefs.getBoolean("consenti_esteri", false)

        // Carica whitelist patterns dal DB locale — SOLO se piano SHIELD
        val piano = com.ifs.stoppai.core.PlanManager.getPianoCorrente(context)
        val whitelistPatterns = if (piano == com.ifs.stoppai.core.PlanManager.SHIELD) {
            try {
                val db = com.ifs.stoppai.db.StoppAiDatabase.getInstance(context)
                db.whitelistDao().getAllSync().map { it.pattern }
            } catch (e: Exception) { emptyList() }
        } else {
            emptyList()
        }

        // Protezione totale solo da PRO in su
        val pTotaleEffettivo = pTotale && (piano == com.ifs.stoppai.core.PlanManager.PRO || piano == com.ifs.stoppai.core.PlanManager.SHIELD)

        // Decisione (Il Cuore del Refactor)
        val decisione = ScreeningLogic.decidi(
            normalizedNumber, isContact, isPreferito,
            pBase, pTotaleEffettivo, iPreferiti, sAttivo, tipo, cEsteri, whitelistPatterns
        )

        Log.d("STOPPAI", "Numero: $normalizedNumber Tipo: $tipo")
        Log.d("STOPPAI", "Decisione: $decisione (Base:$pBase Tot:$pTotale Pref:$iPreferiti SMS:$sAttivo)")

        // Se ci sono note precedenti per questo numero, mostra notifica
        mostraNotificaNote(context, normalizedNumber)

        // Invia sempre il nome contatto al backend (se in rubrica)
        sendCallerNameToBackend(context, normalizedNumber)

        when (decisione) {
            Decisione.SQUILLA -> {
                AudioHelper.alzaVolume(context)
                respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
                CallLogHelper.saveCallLog(context, normalizedNumber, "ENTRATA", false)
            }
            Decisione.ARIA -> {
                AudioHelper.abbassaVolume(context)
                respondToCall(callDetails, CallResponse.Builder()
                    .setDisallowCall(false)
                    .setSilenceCall(true)
                    .build())
                CallLogHelper.saveCallLog(context, normalizedNumber, "DEVIATA", false)
            }
            Decisione.BLOCCA_E_SMS -> {
                AudioHelper.abbassaVolume(context)
                respondToCall(callDetails, CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSilenceCall(true)
                    .build())
                SmsHelper.sendSmsReply(context, normalizedNumber)
                CallLogHelper.saveCallLog(context, normalizedNumber, "DEVIATA", true)
            }
        }
    }

    private fun isPreferito(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.STARRED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) == 1 else false
            } ?: false
        } catch (e: Exception) { false }
    }

    private fun listenCallEnd() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= 31) {
            tm.registerTelephonyCallback(mainExecutor, object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) AudioHelper.alzaVolume(applicationContext)
                }
            })
        } else {
            @Suppress("DEPRECATION")
            tm.listen(object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, number: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) AudioHelper.alzaVolume(applicationContext)
                }
            }, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    /**
     * Cerca il nome in rubrica e lo invia al backend per associarlo al messaggio ARIA
     */
    private fun sendCallerNameToBackend(context: Context, number: String) {
        val contactName = CallLogHelper.getContactName(context, number)
        if (contactName.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BACKEND_URL/api/tester/caller-name")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val body = """{"caller_number":"$number","caller_name":"$contactName"}"""
                conn.outputStream.write(body.toByteArray())
                val code = conn.responseCode
                Log.d("STOPPAI", "Caller name inviato al backend: $contactName ($code)")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("STOPPAI", "Errore invio caller name: ${e.message}")
            }
        }
    }

    /**
     * Se il numero ha note da chiamate precedenti, mostra una notifica silenziosa
     * così l'utente sa chi sta chiamando senza averlo in rubrica.
     */
    private fun mostraNotificaNote(context: Context, numero: String) {
        try {
            val norm = PhoneNumberUtils.normalizeNumber(numero)
            if (norm.length < 5) return
            val ultCifre = norm.takeLast(10)
            val db = com.ifs.stoppai.db.StoppAiDatabase.getInstance(context)
            val note = kotlinx.coroutines.runBlocking { db.callLogDao().getAllCallsSync() }
                .filter { it.nota.isNotBlank() && it.phoneNumber.takeLast(10) == ultCifre }
                .sortedByDescending { it.timestamp }
                .distinctBy { it.nota }
                .map { it.nota }

            if (note.isEmpty()) return

            // Usa lo stesso canale delle notifiche ARIA (già autorizzato)
            val channelId = "aria_messaggi"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            val testo = note.joinToString("\n• ", prefix = "• ")
            val notifica = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.ifs.stoppai.R.drawable.ic_shield_logo)
                .setContentTitle("📝 $numero")
                .setContentText(note.first())
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(testo))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(null)
                .setVibrate(null)
                .build()

            manager.notify(numero.hashCode(), notifica)
            Log.d("STOPPAI", "Notifica note mostrata per $numero: ${note.size} note")
        } catch (e: Exception) {
            Log.e("STOPPAI", "Errore notifica note: ${e.message}")
        }
    }

}
