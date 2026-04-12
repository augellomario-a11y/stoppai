// FILE: PricingSheet.kt
// SCOPO: BottomSheet con i 3 piani (FREE/PRO/SHIELD) — si apre da UpgradeDialog "Vedi i piani"
// ULTIMA MODIFICA: 2026-04-13

package com.ifs.stoppai.core

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ifs.stoppai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PricingSheet {

    fun show(context: Context) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_pricing, null)
        dialog.setContentView(view)

        val piano = PlanManager.getPianoCorrente(context)

        // Badge piano attuale
        val txtPiano = view.findViewById<TextView>(R.id.txt_pricing_piano_attuale)
        txtPiano.text = when (piano) {
            PlanManager.SHIELD -> "Il tuo piano attuale: SHIELD \u2728"
            PlanManager.PRO -> "Il tuo piano attuale: PRO"
            else -> "Il tuo piano attuale: Versione Base (FREE)"
        }

        // --- Pulsante PRO ---
        val btnPro = view.findViewById<Button>(R.id.btn_pricing_pro)
        when {
            piano == PlanManager.SHIELD || piano == PlanManager.PRO -> {
                btnPro.text = if (piano == PlanManager.PRO) "\u2705 Piano PRO attivo" else "\u2705 Incluso in SHIELD"
                btnPro.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                btnPro.isEnabled = false
            }
            PlanManager.puoUpgradeAPro(context) -> {
                btnPro.text = "Passa a PRO"
                btnPro.isEnabled = true
                btnPro.setOnClickListener {
                    eseguiUpgrade(context, PlanManager.PRO)
                    dialog.dismiss()
                }
            }
            else -> {
                val giorni = PlanManager.giorniRimanentiPerPro(context)
                btnPro.text = "Disponibile tra $giorni giorni"
                btnPro.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFBBBBBB.toInt())
                btnPro.isEnabled = false
            }
        }

        // --- Pulsante SHIELD ---
        val btnShield = view.findViewById<Button>(R.id.btn_pricing_shield)
        when {
            piano == PlanManager.SHIELD -> {
                btnShield.text = "\u2705 Piano SHIELD attivo"
                btnShield.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                btnShield.isEnabled = false
            }
            PlanManager.puoUpgradeAShield(context) -> {
                btnShield.text = "Passa a SHIELD"
                btnShield.isEnabled = true
                btnShield.setOnClickListener {
                    eseguiUpgrade(context, PlanManager.SHIELD)
                    dialog.dismiss()
                }
            }
            piano == PlanManager.PRO -> {
                val giorni = PlanManager.giorniRimanentiPerShield(context)
                btnShield.text = "Disponibile tra $giorni giorni"
                btnShield.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFBBBBBB.toInt())
                btnShield.isEnabled = false
            }
            else -> {
                btnShield.text = "Prima passa a PRO"
                btnShield.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFBBBBBB.toInt())
                btnShield.isEnabled = false
            }
        }

        dialog.show()
    }

    private fun eseguiUpgrade(context: Context, piano: String) {
        PlanManager.eseguiUpgrade(context, piano)

        // Notifica backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                val testerId = prefs.getInt("tester_id", 0)
                val email = prefs.getString("tester_email", "") ?: ""

                val url = java.net.URL("https://stoppai.it/api/tester/upgrade")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                val body = """{"tester_id":$testerId,"email":"$email","piano":"$piano"}"""
                conn.outputStream.write(body.toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {}
        }

        val nome = piano.uppercase()
        Toast.makeText(context, "Upgrade a $nome completato!", Toast.LENGTH_LONG).show()
    }
}
