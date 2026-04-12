// FILE: InfoFragment.kt
// SCOPO: Pagina informativa + tabella piani con upgrade progressivo (beta testing)
package com.ifs.stoppai.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ifs.stoppai.R
import com.ifs.stoppai.core.PlanManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InfoFragment : Fragment(R.layout.fragment_help) {

    companion object {
        /** Flag: se true, onViewCreated scrolla alla sezione prezzi */
        var scrollToPricing = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Se arrivo da UpgradeDialog "Vedi i piani", scroll alla sezione prezzi
        if (scrollToPricing) {
            scrollToPricing = false
            view.post {
                val target = view.findViewById<View>(R.id.pricing_section_top)
                val scrollView = view.parent as? android.widget.ScrollView
                if (target != null && scrollView != null) {
                    scrollView.smoothScrollTo(0, target.top)
                }
            }
        }

        val ctx = requireContext()
        val piano = PlanManager.getPianoCorrente(ctx)
        val txtPiano = view.findViewById<TextView>(R.id.txt_info_piano_attuale)
        val btnPro = view.findViewById<Button>(R.id.btn_upgrade_pro)
        val btnShield = view.findViewById<Button>(R.id.btn_upgrade_shield)

        // Mostra piano attuale
        txtPiano.text = when (piano) {
            PlanManager.SHIELD -> "Il tuo piano attuale: SHIELD ✨"
            PlanManager.PRO -> "Il tuo piano attuale: PRO"
            else -> "Il tuo piano attuale: Versione Base (FREE)"
        }

        // --- Pulsante PRO ---
        when {
            piano == PlanManager.SHIELD || piano == PlanManager.PRO -> {
                btnPro.text = if (piano == PlanManager.PRO) "✅ Piano PRO attivo" else "✅ Incluso in SHIELD"
                btnPro.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                btnPro.isEnabled = false
            }
            PlanManager.puoUpgradeAPro(ctx) -> {
                btnPro.text = "Passa a PRO"
                btnPro.isEnabled = true
                btnPro.setOnClickListener { eseguiUpgrade(PlanManager.PRO, btnPro, btnShield, txtPiano) }
            }
            else -> {
                val giorni = PlanManager.giorniRimanentiPerPro(ctx)
                btnPro.text = "Disponibile tra $giorni giorni"
                btnPro.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFBBBBBB.toInt())
                btnPro.isEnabled = false
            }
        }

        // --- Pulsante SHIELD ---
        when {
            piano == PlanManager.SHIELD -> {
                btnShield.text = "✅ Piano SHIELD attivo"
                btnShield.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                btnShield.isEnabled = false
            }
            PlanManager.puoUpgradeAShield(ctx) -> {
                btnShield.text = "Passa a SHIELD"
                btnShield.isEnabled = true
                btnShield.setOnClickListener { eseguiUpgrade(PlanManager.SHIELD, btnPro, btnShield, txtPiano) }
            }
            piano == PlanManager.PRO -> {
                val giorni = PlanManager.giorniRimanentiPerShield(ctx)
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
    }

    private fun eseguiUpgrade(piano: String, btnPro: Button, btnShield: Button, txtPiano: TextView) {
        val ctx = requireContext()

        // Upgrade locale
        PlanManager.eseguiUpgrade(ctx, piano)

        // Notifica backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = ctx.getSharedPreferences("stoppai_prefs", android.content.Context.MODE_PRIVATE)
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
                val code = conn.responseCode
                android.util.Log.d("UPGRADE", "Upgrade $piano inviato al backend ($code)")
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("UPGRADE", "Errore invio upgrade: ${e.message}")
            }
        }

        // Aggiorna UI
        val nomePiano = piano.uppercase()
        txtPiano.text = "Il tuo piano attuale: $nomePiano"
        android.widget.Toast.makeText(ctx, "Piano $nomePiano attivato! 🎉", android.widget.Toast.LENGTH_LONG).show()

        // Aggiorna pulsanti
        if (piano == PlanManager.PRO) {
            btnPro.text = "✅ Piano PRO attivo"
            btnPro.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
            btnPro.isEnabled = false
            val giorniShield = PlanManager.giorniRimanentiPerShield(ctx)
            btnShield.text = "Disponibile tra $giorniShield giorni"
            btnShield.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFBBBBBB.toInt())
            btnShield.isEnabled = false
        } else if (piano == PlanManager.SHIELD) {
            btnShield.text = "✅ Piano SHIELD attivo"
            btnShield.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
            btnShield.isEnabled = false
        }
    }
}
