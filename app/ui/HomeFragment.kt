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
    
    // View per il volume
    private lateinit var volIconBase: TextView
    private lateinit var volTextBase: TextView
    private lateinit var volIconTotale: TextView
    private lateinit var volTextTotale: TextView

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

        // Binding nuovi indicatori volume
        volIconBase = view.findViewById(R.id.ID_HOME_010_ICON)
        volTextBase = view.findViewById(R.id.ID_HOME_010_TEXT)
        volIconTotale = view.findViewById(R.id.ID_HOME_011_ICON)
        volTextTotale = view.findViewById(R.id.ID_HOME_011_TEXT)
        
        aggiornaVolumeUI()

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
                
                val db = StoppAiDatabase.getInstance(requireActivity().applicationContext)
                val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
                
                if (switchBase.isChecked) {
                    try {
                        audioManager.setStreamVolume(
                            android.media.AudioManager.STREAM_RING, 0, 0)
                        aggiornaVolumeUI()
                    } catch (e: Exception) {}
                } else {
                    val volOr = repo.getVolume()
                    try {
                        audioManager.setStreamVolume(
                            android.media.AudioManager.STREAM_RING, volOr, 0)
                        aggiornaVolumeUI()
                    } catch (e: Exception) {}
                }
            }
        }

        // Switch Protezione Base
        switchBase.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("protezione_base", isChecked).apply()
            val db = StoppAiDatabase.getInstance(requireActivity().applicationContext)
            val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
            
            if (isChecked) {
                val volAttuale = audioManager.getStreamVolume(
                    android.media.AudioManager.STREAM_RING)
                // Salva il volume attuale nel DB prima di azzerarlo se è > 0
                if (volAttuale > 0) {
                    repo.setVolume(volAttuale)
                }
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_RING, 0, 0)
                    aggiornaVolumeUI()
                } catch (e: Exception) {}
            } else {
                val volOr = repo.getVolume()
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_RING, volOr, 0)
                    aggiornaVolumeUI()
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
                        StoppAiDatabase.getInstance(act.applicationContext)
                            .clearAllTables()
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        // Statistiche rapide
        val tvStats = view.findViewById<TextView>(R.id.ID_HOME_003)
        lifecycleScope.launch {
            val db = StoppAiDatabase.getInstance(act)
            db.callLogDao().getAllLogs().collectLatest { logs ->
                tvStats.text = "Oggi: ${logs.size} chiamate gestite e archiviate"
            }
        }

        val labelFooter = view.findViewById<TextView>(R.id.ID_HOME_099)
        labelFooter.text = "StoppAI v3.1 —\n[TASK-SA-044] — Speaker Status"

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
                val db = StoppAiDatabase.getInstance(requireActivity().applicationContext)
                val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
                val volOr = repo.getVolume()
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_RING, volOr, 0)
                    aggiornaVolumeUI()
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

    override fun onStart() {
        super.onStart()
        aggiornaVolumeUI()
    }

    override fun onResume() {
        super.onResume()
        aggiornaVolumeUI()
        if (prefs.getBoolean("protezione_totale", false)) {
            startCountdown()
        }
    }

    // Legge il volume reale dal sistema e aggiorna le icone/testo accanto agli switch
    private fun aggiornaVolumeUI() {
        try {
            val vol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING)
            
            // Icona emoji basata sul volume
            val icon = if (vol == 0) "🔇" else "🔊"
            val text = "Vol: $vol/$maxVol"
            
            volIconBase.text = icon
            volTextBase.text = text
            volIconTotale.text = icon
            volTextTotale.text = text
        } catch (e: Exception) {
            android.util.Log.e("STOPPAI", "Errore aggiornamento volume UI: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        stopCountdown()
    }
}
