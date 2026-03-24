// FILE: InvitaFragment.kt
// SCOPO: Gestione Programma Partner e Referral
// DIPENDENZE: fragment_invita.xml, SharedPreferences
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ifs.stoppai.R

class InvitaFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private var isEarningsView = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_invita, container, false)
        prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        setupStats(root)
        setupEarningsToggle(root)
        setupSoglia(root)
        
        root.findViewById<Button>(R.id.btn_how_it_works).setOnClickListener {
            showRegulation()
        }
        
        root.findViewById<Button>(R.id.btn_join_now).setOnClickListener {
            showRegistrationForm()
        }

        // POPUP IN SVILUPPO (SA-057-FIX)
        root.findViewById<Button>(R.id.btn_request_payment).setOnClickListener {
            showInDevelopmentPopup("Il pagamento automatico delle commissioni sarà disponibile con il backend Hetzner in arrivo.")
        }
        root.findViewById<TextView>(R.id.btn_stripe_mgmt).setOnClickListener {
            showInDevelopmentPopup("La gestione pagamenti con Stripe sarà disponibile nella prossima versione.")
        }

        return root
    }

    private fun showInDevelopmentPopup(message: String) {
        AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle("🚧 In sviluppo")
            .setMessage(message)
            .setPositiveButton("HO CAPITO", null)
            .create()
            .apply {
                show()
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
    }

    private fun setupStats(root: View) {
        val pending = root.findViewById<View>(R.id.stat_pending)
        pending.findViewById<TextView>(R.id.txt_stat_label).text = "In attesa"
        
        val installed = root.findViewById<View>(R.id.stat_installed)
        installed.findViewById<TextView>(R.id.txt_stat_label).text = "Installati"
        
        val active = root.findViewById<View>(R.id.stat_active)
        active.findViewById<TextView>(R.id.txt_stat_label).text = "Attivi"
    }

    private fun setupEarningsToggle(root: View) {
        val txtEarnings = root.findViewById<TextView>(R.id.txt_earnings_toggle)
        root.findViewById<View>(R.id.card_earnings_toggle).setOnClickListener {
            isEarningsView = !isEarningsView
            txtEarnings.text = if (isEarningsView) "💶 €0,00 guadagnati" else "👥 0 persone invitate"
        }
    }

    private fun setupSoglia(root: View) {
        val seek = root.findViewById<SeekBar>(R.id.seek_soglia)
        val label = root.findViewById<TextView>(R.id.txt_soglia_label)
        
        val savedSoglia = prefs.getInt("soglia_pagamento", 25)
        seek.progress = savedSoglia - 10
        label.text = "Soglia pagamento: €$savedSoglia"

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 10
                label.text = "Soglia pagamento: €$value"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val value = seek.progress + 10
                prefs.edit().putInt("soglia_pagamento", value).apply()
            }
        })
    }

    private fun showRegulation() {
        val dialog = ReferralRegulationDialog()
        dialog.show(parentFragmentManager, "regolamento")
    }

    private fun showRegistrationForm() {
        val dialog = PartecipaProgrammaDialog()
        dialog.show(parentFragmentManager, "registrazione_premium")
    }
}
