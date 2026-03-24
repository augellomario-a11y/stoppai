// FILE: CallActionBottomSheet.kt
// SCOPO: Menu azioni CRM Premium per le chiamate (v1.1)
// DIPENDENZE: CallLogEntry.kt, bottom_sheet_call_actions.xml, dialog_crm_note.xml
// ULTIMA MODIFICA: 2026-03-23

package com.ifs.stoppai.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R
import com.ifs.stoppai.db.CallLogCrmItem
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.provider.ContactsContract

class CallActionBottomSheet(
    private val item: CallLogCrmItem,
    private val onUpdate: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.bottom_sheet_call_actions, container, false)
        val entry = item.entry

        // 0. HEADER & BADGE
        val tNumber = root.findViewById<TextView>(R.id.txt_title_number)
        val vBadge = root.findViewById<View>(R.id.view_current_status_badge)
        val btnClose = root.findViewById<TextView>(R.id.btn_close_bs)

        tNumber.text = if (entry.displayName.isNotEmpty()) entry.displayName else entry.phoneNumber
        
        val statusRes = when (entry.statusId) {
            1 -> R.drawable.shape_badge_status_green
            2 -> R.drawable.shape_badge_status_gray
            else -> R.drawable.shape_badge_status_red
        }
        vBadge.setBackgroundResource(statusRes)

        btnClose.setOnClickListener { dismiss() }

        // RISPOSTA SMS (SA-057)
        val layoutSms = root.findViewById<View>(R.id.layout_sms_response)
        val txtSmsContent = root.findViewById<TextView>(R.id.txt_sms_response_content)
        
        if (!entry.smsRisposta.isNullOrBlank()) {
            layoutSms.visibility = View.VISIBLE
            txtSmsContent.text = "\"" + entry.smsRisposta + "\""
        } else if (entry.smsInviato) {
            layoutSms.visibility = View.VISIBLE
            txtSmsContent.text = "Attesa risposta..."
            txtSmsContent.setTextColor(android.graphics.Color.GRAY)
        }

        // 1. TRASCRIZIONE AI
        root.findViewById<Button>(R.id.btn_ai_transcription).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Trascrizione AI")
                .setMessage("Funzionalità disponibile con StoppAI PRO — In arrivo con integrazione Opensolution ARIA")
                .setPositiveButton("CHIUDI", null)
                .show()
        }

        root.findViewById<Button>(R.id.btn_da_trattare).setOnClickListener { updateStatus(0) }
        root.findViewById<Button>(R.id.btn_spam).setOnClickListener { updateStatus(2) }
        root.findViewById<Button>(R.id.btn_attendibile).setOnClickListener { updateStatus(1) }

        // 5. NOTE (Premium Dialog)
        root.findViewById<Button>(R.id.btn_note).setOnClickListener {
            showNoteDialogV2()
        }

        // 👤 AGGIUNGI AI CONTATTI
        root.findViewById<Button>(R.id.btn_add_contact).setOnClickListener {
            try {
                val intent = Intent(ContactsContract.Intents.Insert.ACTION)
                intent.type = ContactsContract.RawContacts.CONTENT_TYPE
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, entry.phoneNumber)
                startActivity(intent)
                dismiss() // Chiudiamo perché l'utente esce dall'app
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Errore apertura contatti", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // CHIAMA ORA
        root.findViewById<Button>(R.id.btn_call_now).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${entry.phoneNumber}")
                startActivity(intent)
                dismiss()
            } catch (e: Exception) {}
        }

        return root
    }

    private fun showNoteDialogV2() {
        val entry = item.entry
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_crm_note, null)
        val input = dialogView.findViewById<EditText>(R.id.edt_note_input)
        
        input.setText(entry.nota)
        input.setSelection(entry.nota.length)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_save_note).setOnClickListener {
            updateNote(input.text.toString())
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_cancel_note).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateStatus(newStatus: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = StoppAiDatabase.getInstance(requireContext())
            db.callLogDao().updateStatus(item.entry.id, newStatus)
            withContext(Dispatchers.Main) {
                onUpdate()
                dismiss()
            }
        }
    }

    private fun updateNote(newNote: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = StoppAiDatabase.getInstance(requireContext())
            db.callLogDao().updateNota(item.entry.id, newNote)
            withContext(Dispatchers.Main) {
                onUpdate()
                dismiss()
            }
        }
    }
}
