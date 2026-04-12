// FILE: InfoFragment.kt
// SCOPO: Pagina informativa sull'app con guida funzionalità + tabella piani
package com.ifs.stoppai.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ifs.stoppai.R
import com.ifs.stoppai.core.PlanManager

class InfoFragment : Fragment(R.layout.fragment_help) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mostra piano attuale
        val piano = PlanManager.getPianoCorrente(requireContext())
        val trial = PlanManager.isTrialAttivo(requireContext())
        val txtPiano = view.findViewById<TextView>(R.id.txt_info_piano_attuale)

        txtPiano.text = when {
            trial -> "Il tuo piano attuale: SHIELD TRIAL (14 giorni di prova)"
            piano == "shield" -> "Il tuo piano attuale: SHIELD ✨"
            piano == "pro" -> "Il tuo piano attuale: PRO"
            else -> "Il tuo piano attuale: FREE"
        }

        // Pulsanti upgrade
        view.findViewById<Button>(R.id.btn_upgrade_pro)?.setOnClickListener {
            if (piano == "pro" || piano == "shield" || trial) {
                android.widget.Toast.makeText(requireContext(),
                    if (trial) "Hai già tutte le funzionalità durante il trial"
                    else "Hai già il piano ${piano.uppercase()}",
                    android.widget.Toast.LENGTH_SHORT).show()
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://stoppai.it/prezzi.html")))
                } catch (e: Exception) {}
            }
        }

        view.findViewById<Button>(R.id.btn_upgrade_shield)?.setOnClickListener {
            if (piano == "shield" || trial) {
                android.widget.Toast.makeText(requireContext(),
                    if (trial) "Hai già tutte le funzionalità durante il trial"
                    else "Hai già il piano SHIELD",
                    android.widget.Toast.LENGTH_SHORT).show()
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://stoppai.it/prezzi.html")))
                } catch (e: Exception) {}
            }
        }
    }
}
