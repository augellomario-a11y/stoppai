// FILE: AriaTranscriptionSheet.kt
// SCOPO: BottomSheet dedicato alla visualizzazione trascrizioni ARIA (SA-095)
// DIPENDENZE: bottom_sheet_aria.xml, AriaMessaggioDao.kt, StoppAiDatabase.kt
// ULTIMA MODIFICA: 2026-03-30

package com.ifs.stoppai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BottomSheet espandibile per la visualizzazione delle trascrizioni AI del chiamante
 */
class AriaTranscriptionSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_NUMERO = "phone_number"
        private const val ARG_CALL_ID = "call_log_id"

        /**
         * Factory method — passa il numero del chiamante e l'ID della chiamata (Long)
         */
        fun newInstance(phoneNumber: String, callLogId: Long): AriaTranscriptionSheet {
            return AriaTranscriptionSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_NUMERO, phoneNumber)
                    putLong(ARG_CALL_ID, callLogId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_aria, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera numero e ID chiamata dagli argomenti
        val phoneNumber = arguments?.getString(ARG_NUMERO) ?: "Sconosciuto"
        val callLogId = arguments?.getLong(ARG_CALL_ID) ?: 0L

        val txtNumero = view.findViewById<TextView>(R.id.txt_aria_numero)
        val txtContenuto = view.findViewById<TextView>(R.id.txt_aria_contenuto)
        val btnChiudiTop = view.findViewById<TextView>(R.id.btn_chiudi_aria)
        val btnChiudiBottom = view.findViewById<Button>(R.id.btn_aria_chiudi_bottom)

        txtNumero.text = "📞 $phoneNumber"
        btnChiudiTop.setOnClickListener { dismiss() }
        btnChiudiBottom.setOnClickListener { dismiss() }

        // Espandi il BottomSheet di default
        dialog?.setOnShowListener {
            val bottomSheet = dialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        // Carica i messaggi dal DB in background con link ID specifico
        caricaMessaggi(phoneNumber, callLogId, txtContenuto)
    }

    /**
     * Interroga il DB Room e popola la TextView con le trascrizioni formattate.
     * Cerca i messaggi legati direttamente al callLogId fornito.
     */
    private fun caricaMessaggi(phoneNumber: String, callLogId: Long, txtContenuto: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            val testo = try {
                val db = StoppAiDatabase.getInstance(requireContext())

                // Recupera messaggi collegati a questo ID specifico
                val messaggi = db.ariaMessaggioDao()
                    .getPerCallLogId(callLogId)
                    .first()

                if (messaggi.isEmpty()) {
                    "Nessun messaggio ARIA collegato a questa specifica chiamata."
                } else {
                    // Recupera il timestamp dalla chiamata originale (SA-107)
                    val entry = db.callLogDao().getCallById(callLogId)
                    val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.ITALY)
                    val data = if (entry != null) sdf.format(Date(entry.timestamp)) else "Orario sconosciuto"
                    
                    val sb = StringBuilder()
                    sb.append("📅  $data\n\n")
                    messaggi.forEach { msg ->
                        sb.append("💬  ${msg.testo}\n")
                        sb.append("\n─────────────────────\n\n")
                    }
                    sb.toString().trimEnd()
                }
            } catch (e: Exception) {
                "Errore nel caricamento: ${e.message}"
            }

            withContext(Dispatchers.Main) {
                txtContenuto.text = testo
            }
        }
    }
}
