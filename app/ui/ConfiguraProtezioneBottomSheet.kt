// FILE: ConfiguraProtezioneBottomSheet.kt
// SCOPO: Configurazione definitiva Protezione Base (SA-057-RECOVERY)
// DIPENDENZE: bottom_sheet_config_base.xml
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R

class ConfiguraProtezioneBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefs: SharedPreferences
    var onConfirmListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.bottom_sheet_config_base, container, false)
        prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        val swSms = root.findViewById<Switch>(R.id.switch_sms_reply)
        val layoutEditor = root.findViewById<View>(R.id.layout_sms_editor)
        val edtSms = root.findViewById<EditText>(R.id.edt_sms_text)
        val txtCounter = root.findViewById<TextView>(R.id.txt_char_counter)
        val btnConfirm = root.findViewById<Button>(R.id.btn_confirm_base)

        // Stato iniziale SMS
        val isSmsAct = prefs.getBoolean("sms_risposta_attivo", false)
        swSms.isChecked = isSmsAct
        layoutEditor.visibility = if (isSmsAct) View.VISIBLE else View.GONE
        
        val defaultMsg = "Ciao, in questo momento non posso rispondere. Lasciami un messaggio e ti richiamo appena possibile."
        val savedMsg = prefs.getString("sms_testo_risposta", defaultMsg)
        edtSms.setText(savedMsg)
        txtCounter.text = "${savedMsg?.length ?: 0} / 160"

        swSms.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !prefs.getBoolean("sms_alert_mostrato", false)) {
                showCostAlert(swSms, layoutEditor)
            } else {
                layoutEditor.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        edtSms.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                txtCounter.text = "${s?.length ?: 0} / 160"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            prefs.edit()
                .putBoolean("sms_risposta_attivo", swSms.isChecked)
                .putString("sms_testo_risposta", edtSms.text.toString())
                .putBoolean("protezione_base", true)
                .apply()
            
            onConfirmListener?.invoke()
            dismiss()
        }

        return root
    }

    private fun showCostAlert(sw: Switch, layout: View) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle("⚠️ Attenzione")
            .setMessage("L'invio di SMS automatici utilizza il credito del tuo piano telefonico. I costi dipendono dal contratto con il tuo operatore. StoppAI non si assume responsabilità per i costi degli SMS inviati.")
            .setPositiveButton("HO CAPITO, CONTINUA") { _, _ ->
                prefs.edit().putBoolean("sms_alert_mostrato", true).apply()
                layout.visibility = View.VISIBLE
            }
            .setNegativeButton("ANNULLA") { _, _ ->
                sw.isChecked = false
                layout.visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }
}
