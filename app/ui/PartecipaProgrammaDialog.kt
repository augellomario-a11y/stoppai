// FILE: PartecipaProgrammaDialog.kt
// SCOPO: Form partner stabile con memoria e cursore fix (SA-065-FORM-FIX)
// DIPENDENZE: bottom_sheet_partecipa.xml
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.ifs.stoppai.R

class PartecipaProgrammaDialog : BottomSheetDialogFragment() {

    private val fieldMap = mapOf(
        R.id.et_nome to "partner_nome",
        R.id.et_cognome to "partner_cognome",
        R.id.et_email to "partner_email",
        R.id.et_telefono to "partner_telefono",
        R.id.et_nascita to "partner_nascita",
        R.id.et_cf to "partner_cf",
        R.id.et_indirizzo to "partner_indirizzo",
        R.id.et_cap to "partner_cap",
        R.id.et_citta to "partner_citta",
        R.id.et_provincia to "partner_provincia",
        R.id.et_iban to "partner_iban",
        R.id.et_intestatario to "partner_intestatario"
    )

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme_Fullscreen

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_partecipa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        setupForm(view)
        loadSavedData(view)
    }

    private fun setupForm(v: View) {
        val capsFilter = InputFilter.AllCaps()
        v.findViewById<EditText>(R.id.et_cf).filters = arrayOf(capsFilter)
        v.findViewById<EditText>(R.id.et_iban).filters = arrayOf(capsFilter)
        v.findViewById<EditText>(R.id.et_provincia).filters = arrayOf(capsFilter, InputFilter.LengthFilter(2))

        fieldMap.forEach { (id, key) ->
            val et = v.findViewById<EditText>(id)
            if (id == R.id.et_cf || id == R.id.et_iban) {
                // WATCHER SPECIALE PER CF E IBAN (MAIUSCOLO + CURSORE END)
                et.addTextChangedListener(object : TextWatcher {
                    private var isFormatting = false
                    override fun afterTextChanged(s: Editable?) {
                        if (isFormatting) return
                        isFormatting = true
                        val testo = s.toString().uppercase()
                        val posizione = testo.length
                        s?.replace(0, s.length, testo)
                        et.setSelection(posizione)
                        saveValue(key, testo)
                        isFormatting = false
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            } else {
                // WATCHER GENERICO PER ALTRI CAMPI
                et.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        s?.let { et.setSelection(it.length) }
                        saveValue(key, et.text.toString())
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            }
            et.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveValue(key, et.text.toString()) }
        }

        v.findViewById<Button>(R.id.btn_invia).setOnClickListener {
            if (validaCampi(v)) { mostraSuccesso(); clearAllData(); dismiss() }
            else { Snackbar.make(v, "Compila tutti i campi obbligatori", Snackbar.LENGTH_LONG).setBackgroundTint(android.graphics.Color.RED).show() }
        }
        v.findViewById<Button>(R.id.btn_annulla).setOnClickListener { dismiss() }
    }

    private fun loadSavedData(v: View) {
        val prefs = requireContext().getSharedPreferences("form_partner", Context.MODE_PRIVATE)
        fieldMap.forEach { (id, key) -> v.findViewById<EditText>(id).setText(prefs.getString(key, "")) }
        v.findViewById<CheckBox>(R.id.chk_regolamento).isChecked = prefs.getBoolean("partner_consenso1", false)
        v.findViewById<CheckBox>(R.id.chk_gdpr).isChecked = prefs.getBoolean("partner_consenso2", false)
        v.findViewById<CheckBox>(R.id.chk_maggiorenne).isChecked = prefs.getBoolean("partner_consenso3", false)
    }

    private fun saveValue(key: String, value: String) {
        val ctx = context ?: return
        ctx.getSharedPreferences("form_partner", Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    override fun onPause() {
        super.onPause()
        val v = view ?: return
        val prefs = requireContext().getSharedPreferences("form_partner", Context.MODE_PRIVATE).edit()
        prefs.putBoolean("partner_consenso1", v.findViewById<CheckBox>(R.id.chk_regolamento).isChecked)
        prefs.putBoolean("partner_consenso2", v.findViewById<CheckBox>(R.id.chk_gdpr).isChecked)
        prefs.putBoolean("partner_consenso3", v.findViewById<CheckBox>(R.id.chk_maggiorenne).isChecked)
        prefs.apply()
    }

    private fun clearAllData() {
        requireContext().getSharedPreferences("form_partner", Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun validaCampi(v: View): Boolean {
        fieldMap.keys.forEach { if (v.findViewById<EditText>(it).text.isNullOrBlank()) return false }
        val c1 = v.findViewById<CheckBox>(R.id.chk_regolamento).isChecked
        val c2 = v.findViewById<CheckBox>(R.id.chk_gdpr).isChecked
        val c3 = v.findViewById<CheckBox>(R.id.chk_maggiorenne).isChecked
        return (c1 && c2 && c3)
    }

    private fun mostraSuccesso() {
        AlertDialog.Builder(requireContext()).setTitle("✅ Richiesta inviata!").setMessage("Ti contatteremo entro 48 ore all'email inserita.").setPositiveButton("CHIUDI", null).show()
    }
}
