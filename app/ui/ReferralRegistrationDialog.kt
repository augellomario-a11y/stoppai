// FILE: ReferralRegistrationDialog.kt
// SCOPO: Form registrazione Partners (SA-056)
// DIPENDENZE: fragment_referral_registration.xml
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R

class ReferralRegistrationDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        getSavedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_referral_registration, container, false)
        
        root.findViewById<Button>(R.id.btn_cancel_reg).setOnClickListener { dismiss() }
        
        root.findViewById<Button>(R.id.btn_send_request).setOnClickListener {
            validateAndSubmit(root)
        }
        
        return root
    }

    private fun validateAndSubmit(root: View) {
        val fields = listOf(
            R.id.edt_nome, R.id.edt_cognome, R.id.edt_email, R.id.edt_tel,
            R.id.edt_nascita, R.id.edt_cf, R.id.edt_indirizzo, R.id.edt_cap,
            R.id.edt_citta, R.id.edt_prov, R.id.edt_iban, R.id.edt_intestatario
        )
        
        var allFilled = true
        for (id in fields) {
            val edt = root.findViewById<EditText>(id)
            if (edt.text.isNullOrBlank()) {
                allFilled = false
                break
            }
        }
        
        val consents = listOf(R.id.chk_regolamento, R.id.chk_gdpr, R.id.chk_maggiorenne)
        for (id in consents) {
            val chk = root.findViewById<CheckBox>(id)
            if (!chk.isChecked) {
                allFilled = false
                break
            }
        }
        
        if (!allFilled) {
            android.widget.Toast.makeText(requireContext(), "Compila tutti i campi obbligatori", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Successo Placeholder
        AlertDialog.Builder(requireContext())
            .setTitle("✅ Richiesta inviata!")
            .setMessage("Ti contatteremo entro 48 ore all'email inserita.")
            .setPositiveButton("CHIUDI") { _, _ -> dismiss() }
            .show()
    }
}
