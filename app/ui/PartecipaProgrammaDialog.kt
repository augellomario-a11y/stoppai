// FILE: PartecipaProgrammaDialog.kt
// SCOPO: Form registrazione Partners Partner Program (SA-056-FIX)
// DIPENDENZE: dialog_partecipa_programma.xml
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
import com.google.android.material.snackbar.Snackbar
import com.ifs.stoppai.R

class PartecipaProgrammaDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        getSavedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_partecipa_programma, container, false)
        
        root.findViewById<TextView>(R.id.btn_cancel_request).setOnClickListener { dismiss() }
        
        root.findViewById<Button>(R.id.btn_send_request_premium).setOnClickListener {
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
            val snack = Snackbar.make(root, "Compila tutti i campi obbligatori", Snackbar.LENGTH_LONG)
            snack.setBackgroundTint(android.graphics.Color.RED)
            snack.show()
            return
        }
        
        // Successo Placeholder
        dismiss()
        AlertDialog.Builder(requireContext())
            .setTitle("✅ Richiesta inviata!")
            .setMessage("Ti contatteremo entro 48 ore all'indirizzo email inserito.")
            .setPositiveButton("CHIUDI", null)
            .show()
    }
}
