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

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onCreate() {
        super.onCreate()
        listenCallEnd()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)
        val context = applicationContext
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        // Raccolta Dati
        val tipo = NumberClassifier.getTipoNumero(normalizedNumber)
        val isContact = ContactCacheManager.isContact(normalizedNumber)
        val isPreferito = isPreferito(context, normalizedNumber)

        val pBase = prefs.getBoolean("protezione_base", false)
        val pTotale = prefs.getBoolean("protezione_totale", false)
        val iPreferiti = prefs.getBoolean("includi_preferiti", false)
        val sAttivo = prefs.getBoolean("sms_risposta_attivo", false)

        // Decisione (Il Cuore del Refactor)
        val decisione = ScreeningLogic.decidi(
            normalizedNumber, isContact, isPreferito, 
            pBase, pTotale, iPreferiti, sAttivo, tipo
        )

        Log.d("STOPPAI", "Numero: $normalizedNumber Tipo: $tipo")
        Log.d("STOPPAI", "Decisione: $decisione (Base:$pBase Tot:$pTotale Pref:$iPreferiti SMS:$sAttivo)")

        when (decisione) {
            Decisione.SQUILLA -> {
                AudioHelper.alzaVolume(context)
                respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            }
            Decisione.ARIA -> {
                AudioHelper.abbassaVolume(context)
                respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
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
}
