// FILE: SmsHelper.kt
// SCOPO: Gestione invio SMS di risposta (SA-063-LOGICA-REFACTOR)
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsHelper {

    /**
     * Invia un SMS di risposta automatica al numero specificato
     */
    fun sendSmsReply(context: Context, number: String) {
        if (number.isBlank()) return
        
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val msg = prefs.getString("sms_testo_risposta", "Ciao, in questo momento non posso rispondere. Lasciami un messaggio e ti richiamo appena possibile.")
        
        try {
            val sms = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            sms.sendTextMessage(number, null, msg, null, null)
            Log.d("STOPPAI", "SMS Inviato a: $number")
        } catch (e: Exception) {
            Log.e("STOPPAI", "Errore invio SMS: ${e.message}")
        }
    }
}
