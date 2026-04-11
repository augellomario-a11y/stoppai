// FILE: UssdManager.kt
// SCOPO: Gestione codici USSD per deviazione chiamate condizionali verso ARIA
// DIPENDENZE: Nessuna
// ULTIMA MODIFICA: 2026-04-10
// NOTE: Il +39 è OBBLIGATORIO per compatibilità con tutti gli operatori italiani (Iliad lo richiede esplicitamente)

package com.ifs.stoppai.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object UssdManager {
    // Numero ARIA con +39 — funziona su tutti gli operatori italiani (Ho., Iliad, TIM, Vodafone, Wind)
    private const val ARIA_NUMBER = "+3904211898065"

    fun getAriaNumber(context: Context): String = ARIA_NUMBER

    // I codici USSD vengono generati dinamicamente in base all'operatore
    // *61* = su non risposta, *67* = su occupato, *62* = su non raggiungibile

    // Disattivazione singole
    private const val USSD_CANCEL_NO_REPLY = "##61#"
    private const val USSD_CANCEL_BUSY = "##67#"
    private const val USSD_CANCEL_UNREACHABLE = "##62#"

    // Disattivazione totale (cancella tutte le deviazioni, inclusa segreteria operatore)
    private const val USSD_CANCEL_ALL = "##002#"

    /**
     * Attiva le 3 deviazioni condizionali verso ARIA.
     * Prima cancella tutto (segreteria operatore inclusa), poi imposta le nostre.
     * I codici vengono inviati in sequenza con breve delay tra uno e l'altro.
     */
    fun activateForward(context: Context) {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val handler = Handler(Looper.getMainLooper())
        val num = getAriaNumber(context)

        // Step 1: cancella tutte le deviazioni esistenti
        sendUssd(context, USSD_CANCEL_ALL)

        // Step 2-4: imposta le 3 deviazioni condizionali con delay
        handler.postDelayed({ sendUssd(context, "*61*${num}**11*5#") }, 3000)
        handler.postDelayed({ sendUssd(context, "*67*${num}#") }, 6000)
        handler.postDelayed({
            sendUssd(context, "*62*${num}#")
            prefs.edit().putBoolean("forward_active", true).apply()
        }, 9000)
    }

    /**
     * Disattiva tutte le deviazioni verso ARIA.
     * Il tester tornerà senza segreteria — dovrà riattivare quella del suo operatore se vuole.
     */
    fun deactivateForward(context: Context) {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("forward_active", false)) {
            sendUssd(context, USSD_CANCEL_ALL)
            prefs.edit().putBoolean("forward_active", false).apply()
        }
    }

    /**
     * Verifica se le deviazioni sono state attivate (flag locale).
     * Non verifica lo stato reale sulla rete — per quello serve *#61# manuale.
     */
    fun isForwardActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("forward_active", false)
    }

    private fun sendUssd(context: Context, code: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            val handler = Handler(Looper.getMainLooper())
            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager, request: String, response: CharSequence
                ) {
                    android.util.Log.d("UssdManager", "USSD OK: $request → $response")
                }
                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager, request: String, failureCode: Int
                ) {
                    android.util.Log.e("UssdManager", "USSD FAIL: $request code=$failureCode")
                }
            }
            tm.sendUssdRequest(code, callback, handler)
        } catch (e: Exception) {
            android.util.Log.e("UssdManager", "USSD exception: ${e.message}")
        }
    }
}
