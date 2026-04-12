// FILE: ConfiguraProtezioneBottomSheet.kt
// SCOPO: Configurazione Protezione Base (solo SMS)
// Segreteria ARIA spostata in SettingsFragment
// ULTIMA MODIFICA: 2026-04-12

package com.ifs.stoppai.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R

class ConfiguraProtezioneBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefs: SharedPreferences
    var onConfirmListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.bottom_sheet_config_base, container, false)
        prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        setupSmsReply(root)
        setupConfirmButton(root)

        return root
    }

    private fun setupSmsReply(root: View) {
        val swSms = root.findViewById<Switch>(R.id.switch_sms_reply)
        val layoutEditor = root.findViewById<View>(R.id.layout_sms_editor)
        val edtSms = root.findViewById<EditText>(R.id.edt_sms_text)
        val txtCounter = root.findViewById<TextView>(R.id.txt_char_counter)

        val isSmsAct = prefs.getBoolean("sms_risposta_attivo", false)
        swSms.isChecked = isSmsAct
        layoutEditor.visibility = if (isSmsAct) View.VISIBLE else View.GONE

        val smsText = prefs.getString("sms_testo_risposta",
            "Ciao, sono l'assistente ARIA. Non posso rispondere ora, lasciami un messaggio.") ?: ""
        edtSms.setText(smsText)
        txtCounter.text = "${smsText.length} / 160"

        swSms.setOnCheckedChangeListener { _, isChecked ->
            layoutEditor.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        edtSms.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                txtCounter.text = "${s?.length ?: 0} / 160"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupConfirmButton(root: View) {
        val btnConfirm = root.findViewById<Button>(R.id.btn_confirm_base)
        val swSms = root.findViewById<Switch>(R.id.switch_sms_reply)
        val edtSms = root.findViewById<EditText>(R.id.edt_sms_text)

        btnConfirm.setOnClickListener {
            prefs.edit()
                .putBoolean("sms_risposta_attivo", swSms.isChecked)
                .putString("sms_testo_risposta", edtSms.text.toString())
                .putBoolean("protezione_base", true)
                .apply()

            onConfirmListener?.invoke()
            dismiss()
        }
    }
}
