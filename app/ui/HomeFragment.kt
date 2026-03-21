// FILE: HomeFragment.kt
// SCOPO: Fragment Home con switch, countdown e timer scadenza
// DIPENDENZE: ProtezioneBottomSheet.kt, StoppAiDatabase
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ifs.stoppai.R
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var audioManager: android.media.AudioManager
    private lateinit var switchTotale: Switch
    private lateinit var switchBase: Switch
    private lateinit var tvCountdown: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val act = requireActivity()
        prefs = act.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        audioManager = act.getSystemService(
            Context.AUDIO_SERVICE) as android.media.AudioManager

        switchBase = view.findViewById(R.id.ID_HOME_001)
        switchBase.isChecked = prefs.getBoolean("protezione_base", false)

        switchTotale = view.findViewById(R.id.ID_HOME_007)
        switchTotale.isChecked = prefs.getBoolean("protezione_totale", false)
        tvCountdown = view.findViewById(R.id.ID_HOME_008)

        if (switchTotale.isChecked) {
            switchBase.isEnabled = false
        }

        // Switch Protezione Totale — apre BottomSheet
        switchTotale.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val bs = ProtezioneBottomSheet()
                bs.onAttivaListener = {
                    switchBase.isEnabled = false
                    switchTotale.isChecked = true
                    startCountdown()
                }
                bs.onAnnullaListener = {
                    switchTotale.isChecked = false
                }
                bs.show(parentFragmentManager, "protezione_bs")
            } else {
                prefs.edit()
                    .putBoolean("protezione_totale", false)
                    .apply()
                switchBase.isEnabled = true
                stopCountdown()
                if (switchBase.isChecked) {
                    try {
                        audioManager.setStreamVolume(
                            android.media.AudioManager.STREAM_RING, 0, 0)
                    } catch (e: Exception) {}
                } else {
                    val volOr = prefs.getInt("vol_originale", 5)
                    try {
                        audioManager.setStreamVolume(
                            android.media.AudioManager.STREAM_RING, volOr, 0)
                    } catch (e: Exception) {}
                }
            }
        }

        // Switch Protezione Base
        switchBase.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("protezione_base", isChecked).apply()
            if (isChecked) {
                val volSalvato = prefs.getInt("vol_originale", 0)
                if (volSalvato == 0) {
                    val volAttuale = audioManager.getStreamVolume(
                        android.media.AudioManager.STREAM_RING)
                    val volDaSalvare = if (volAttuale > 0) volAttuale else 7
                    prefs.edit().putInt("vol_originale", volDaSalvare).apply()
                }
                val volAdesso = audioManager.getStreamVolume(
                    android.media.AudioManager.STREAM_RING)
                if (volAdesso > 0) {
                    prefs.edit().putInt("vol_originale", volAdesso).apply()
                }
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_RING, 0, 0)
                } catch (e: Exception) {}
            } else {
                val volOr = prefs.getInt("vol_originale", 5)
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_RING, volOr, 0)
                } catch (e: Exception) {}
            }
        }

        // BTN SVUOTA REGISTRO
        val btnSvuota = view.findViewById<Button>(R.id.ID_HOME_004)
        btnSvuota.setOnClickListener {
            AlertDialog.Builder(act)
                .setTitle("Svuota registro")
                .setMessage("Sei sicuro? Questa azione non è reversibile.")
                .setPositiveButton("Svuota") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        StoppAiDatabase.getDatabase(act.applicationContext)
                            .clearAllTables()
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        // Statistiche rapide
        val tvStats = view.findViewById<TextView>(R.id.ID_HOME_003)
        lifecycleScope.launch {
            val db = StoppAiDatabase.getDatabase(act)
            db.callLogDao().getAllLogs().collectLatest { logs ->
                tvStats.text = "Oggi: ${logs.size} chiamate gestite e archiviate"
            }
        }

        val labelFooter = view.findViewById<TextView>(R.id.ID_HOME_099)
        labelFooter.text = "StoppAI v2.9 —\n[TASK-SA-034] — Final Fix"

        // Avvia countdown se protezione totale è attiva
        if (prefs.getBoolean("protezione_totale", false)) {
            startCountdown()
        }
    }

    // Avvia timer countdown aggiornamento ogni secondo
    private fun startCountdown() {
        stopCountdown()
        countdownRunnable = object : Runnable {
            override fun run() {
                aggiornaCountdown()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(countdownRunnable!!)
    }

    // Ferma timer countdown
    private fun stopCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        tvCountdown.visibility = View.GONE
        tvCountdown.text = ""
    }

    // Aggiorna testo countdown
    private fun aggiornaCountdown() {
        val isTotale = prefs.getBoolean("protezione_totale", false)
        if (!isTotale) {
            stopCountdown()
            return
        }
        val scadenza = prefs.getLong("protezione_totale_scadenza", 0L)
        val rimanenti = scadenza - System.currentTimeMillis()

        if (rimanenti <= 0) {
            // Scaduta — disattiva
            prefs.edit()
                .putBoolean("protezione_totale", false)
                .apply()
            switchTotale.isChecked = false
            switchBase.isEnabled = true
            stopCountdown()
            // Ripristina volume se base non è attiva
            if (!switchBase.isChecked) {
                val volOr = prefs.getInt("vol_originale", 5)
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_RING, volOr, 0)
                } catch (e: Exception) {}
            }
            return
        }

        val ore = (rimanenti / 3600000).toInt()
        val minuti = ((rimanenti % 3600000) / 60000).toInt()
        val secondi = ((rimanenti % 60000) / 1000).toInt()

        tvCountdown.visibility = View.VISIBLE
        tvCountdown.text = if (ore > 0) {
            "⏱ Scade tra ${ore}h ${minuti}m ${secondi.toString().padStart(2, '0')}s"
        } else {
            "⏱ Scade tra ${minuti}:${secondi.toString().padStart(2, '0')}"
        }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("protezione_totale", false)) {
            startCountdown()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCountdown()
    }
}
