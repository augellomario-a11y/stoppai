// FILE: UssdManager.kt
// SCOPO: Gestione codici USSD per deviazione chiamate (Attivazione/Disattivazione)
// DIPENDENZE: Nessuna
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object UssdManager {
    const val USSD_ACTIVATE = "**21*04211898065#"
    const val USSD_DEACTIVATE = "##21#"
    private var isListenerRegistered = false

    fun activateForward(context: Context) {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("forward_active", true).apply()
        sendUssd(context, USSD_ACTIVATE)
        setupCallStateListener(context)
    }

    fun deactivateForward(context: Context) {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("forward_active", false)) {
            sendUssd(context, USSD_DEACTIVATE)
            prefs.edit().putBoolean("forward_active", false).apply()
        }
    }

    private fun setupCallStateListener(context: Context) {
        if (isListenerRegistered) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return

        val appCtx = context.applicationContext
        val tm = appCtx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            tm.registerTelephonyCallback(
                appCtx.mainExecutor,
                object : android.telephony.TelephonyCallback(), android.telephony.TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        if (state == TelephonyManager.CALL_STATE_IDLE) {
                            deactivateForward(appCtx)
                        }
                    }
                }
            )
        } else {
            @Suppress("DEPRECATION")
            tm.listen(object : android.telephony.PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        deactivateForward(appCtx)
                    }
                }
            }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }
        isListenerRegistered = true
    }

    private fun sendUssd(context: Context, code: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            val handler = Handler(Looper.getMainLooper())
            val callback = object : TelephonyManager.UssdResponseCallback() {}
            tm.sendUssdRequest(code, callback, handler)
        } catch (e: Exception) {
            // Fallback se necessario
        }
    }
}
