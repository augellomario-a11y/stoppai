// FILE: ProtezioneBottomSheet.kt
// SCOPO: Bottom sheet per selezione durata Protezione Totale
// DIPENDENZE: HomeFragment.kt, SharedPreferences
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TimePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R
import java.util.Calendar

class ProtezioneBottomSheet : BottomSheetDialogFragment() {

    // Callback per comunicare con HomeFragment
    var onAttivaListener: (() -> Unit)? = null
    var onAnnullaListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.bottom_sheet_protezione,
            container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.ID_BS_RADIO)
        val timePicker = view.findViewById<TimePicker>(R.id.ID_BS_TIMEPICKER)
        val switchPreferiti = view.findViewById<Switch>(R.id.ID_BS_007)
        val btnAttiva = view.findViewById<Button>(R.id.ID_BS_008)
        val btnAnnulla = view.findViewById<Button>(R.id.ID_BS_009)

        timePicker.setIs24HourView(true)

        // Mostra/nascondi TimePicker
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            timePicker.visibility =
                if (checkedId == R.id.ID_BS_006) View.VISIBLE
                else View.GONE
        }

        // Pulsante ATTIVA
        btnAttiva.setOnClickListener {
            val scadenza = calcolaScadenza(radioGroup, timePicker)
            val prefs = requireContext().getSharedPreferences(
                "stoppai_prefs", Context.MODE_PRIVATE)
            val audio = requireContext().getSystemService(
                Context.AUDIO_SERVICE) as android.media.AudioManager

            // Salva volume attuale se > 0
            val volAdesso = audio.getStreamVolume(
                android.media.AudioManager.STREAM_RING)
            if (volAdesso > 0) {
                prefs.edit().putInt("vol_originale", volAdesso).apply()
            }

            // Attiva protezione totale
            prefs.edit()
                .putBoolean("protezione_totale", true)
                .putLong("protezione_totale_scadenza", scadenza)
                .putBoolean("includi_preferiti", switchPreferiti.isChecked)
                .apply()

            // Abbassa volume
            try {
                audio.setStreamVolume(
                    android.media.AudioManager.STREAM_RING, 0, 0)
            } catch (e: Exception) {}

            android.util.Log.e("STOPPAI_VOL",
                "Protezione Totale ON — scadenza: $scadenza")

            onAttivaListener?.invoke()
            dismiss()
        }

        // Pulsante ANNULLA
        btnAnnulla.setOnClickListener {
            onAnnullaListener?.invoke()
            dismiss()
        }
    }

    // Calcolo scadenza in base alla selezione
    private fun calcolaScadenza(
        radioGroup: RadioGroup,
        timePicker: TimePicker
    ): Long {
        val now = System.currentTimeMillis()
        return when (radioGroup.checkedRadioButtonId) {
            R.id.ID_BS_001 -> now + 30L * 60 * 1000       // 30 min
            R.id.ID_BS_002 -> now + 60L * 60 * 1000       // 1 ora
            R.id.ID_BS_003 -> now + 120L * 60 * 1000      // 2 ore
            R.id.ID_BS_004 -> {                            // Stasera 20:00
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 20)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            R.id.ID_BS_005 -> {                            // Domani 08:00
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 8)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            R.id.ID_BS_006 -> {                            // Personalizzato
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                cal.set(Calendar.MINUTE, timePicker.minute)
                cal.set(Calendar.SECOND, 0)
                if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            else -> now + 60L * 60 * 1000                  // default 1 ora
        }
    }

    // Impedisci chiusura con swipe
    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onAnnullaListener?.invoke()
    }
}
