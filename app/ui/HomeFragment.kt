// FILE: HomeFragment.kt
package com.ifs.stoppai.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ifs.stoppai.R
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val act = requireActivity()
        val prefs = act.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val audioManager = act.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        val switchBase = view.findViewById<Switch>(R.id.ID_HOME_001)
        switchBase.isChecked = prefs.getBoolean("protezione_base", false)

        val switchTotale = view.findViewById<Switch>(R.id.ID_HOME_007)
        switchTotale.isChecked = prefs.getBoolean("protezione_totale", false)
        if (switchTotale.isChecked) {
            switchBase.isEnabled = false
        }
        
        switchTotale.setOnCheckedChangeListener { _, isChecked ->
            val edit = prefs.edit()
            edit.putBoolean("protezione_totale", isChecked)
            if (isChecked) {
                val volAdesso = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
                if (volAdesso > 0) {
                    edit.putInt("vol_originale", volAdesso)
                }
                edit.putLong("protezione_totale_scadenza", System.currentTimeMillis() + 60 * 60 * 1000)
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0) } catch(e:Exception){}
                
                switchBase.isEnabled = false
            } else {
                switchBase.isEnabled = true
                if (switchBase.isChecked) {
                    try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0) } catch(e:Exception){}
                } else {
                    val volOr = prefs.getInt("vol_originale", 5)
                    try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, volOr, 0) } catch(e:Exception){}
                }
            }
            edit.apply()
        }

        switchBase.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("protezione_base", isChecked).apply()
            if (isChecked) {
                // Forza reset se vol_originale è 0
                val volSalvato = prefs.getInt("vol_originale", 0)
                if (volSalvato == 0) {
                    val volAttuale = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
                    val volDaSalvare = if (volAttuale > 0) volAttuale else 7
                    prefs.edit().putInt("vol_originale", volDaSalvare).apply()
                    android.util.Log.e("STOPPAI_VOL", "Reset vol_originale a $volDaSalvare")
                }
                
                val volAdesso = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
                if (volAdesso > 0) {
                    prefs.edit().putInt("vol_originale", volAdesso).apply()
                }
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0) } catch(e:Exception){}
            } else {
                val volOr = prefs.getInt("vol_originale", 5)
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, volOr, 0) } catch(e:Exception){}
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
                        StoppAiDatabase.getDatabase(act.applicationContext).clearAllTables()
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
                val totale = logs.size
                tvStats.text = "Oggi: $totale chiamate gestite e archiviate"
            }
        }
        
        val labelFooter = view.findViewById<TextView>(R.id.ID_HOME_099)
        labelFooter.text = "StoppAI v2.4 —\n[TASK-SA-025] — Call End Volume"
    }
}
