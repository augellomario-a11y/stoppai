// FILE: HomeFragment.kt
// SCOPO: Fragment Home con switch, countdown e timer scadenza
// DIPENDENZE: ProtezioneBottomSheet.kt, StoppAiDatabase
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.ui

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import java.io.InputStream
import java.io.OutputStream
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var audioManager: android.media.AudioManager
    private lateinit var switchTotale: Switch
    private lateinit var switchBase: Switch
    private lateinit var tvCountdown: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    
    // View per il volume e registro
    private lateinit var volIconBase: ImageView
    private lateinit var volTextBase: TextView
    private lateinit var volIconTotale: ImageView
    private lateinit var volTextTotale: TextView
    private lateinit var rvLog: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: CallLogAdapter
    
    // Stats
    private lateinit var tvStatTotale: TextView
    private lateinit var tvStatOggi: TextView
    private lateinit var tvStatReferral: TextView

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

        // Binding nuovi indicatori volume e registro
        volIconBase = view.findViewById(R.id.ID_HOME_010_ICON)
        volTextBase = view.findViewById(R.id.ID_HOME_010_TEXT)
        volIconTotale = view.findViewById(R.id.ID_HOME_011_ICON)
        volTextTotale = view.findViewById(R.id.ID_HOME_011_TEXT)
        rvLog = view.findViewById(R.id.ID_HOME_012)
        tvEmpty = view.findViewById(R.id.ID_HOME_013_EMPTY)
        
        tvStatTotale = view.findViewById(R.id.ID_HOME_STAT_001)
        tvStatOggi = view.findViewById(R.id.ID_HOME_STAT_002)
        tvStatReferral = view.findViewById(R.id.ID_HOME_STAT_003)

        setupRecyclerView()
        aggiornaVolumeUI()
        caricaStatistiche()

        if (switchTotale.isChecked) {
            switchBase.isEnabled = false
        }

        // Click sugli altoparlanti per cambiare volume target
        // Rimosso listener per le icone come da istruzioni
        // val volClick = View.OnClickListener { showVolumeDialog() }
        // volIconBase.setOnClickListener(volClick)
        // volTextBase.setOnClickListener(volClick)
        // volIconTotale.setOnClickListener(volClick)
        // volTextTotale.setOnClickListener(volClick)

        // Switch Protezione Totale — apre BottomSheet
        switchTotale.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val bs = ProtezioneBottomSheet()
                bs.onAttivaListener = {
                    switchBase.isEnabled = false
                    switchTotale.isChecked = true
                }
                bs.show(parentFragmentManager, "protezione_bs")
            } else {
                prefs.edit().putBoolean("protezione_totale", false).apply()
                switchBase.isEnabled = true
                stopCountdown()
                aggiornaVolumeUI()
            }
        }

        // Switch Protezione Base
        switchBase.setOnCheckedChangeListener { _, isChecked ->
            val context = requireContext()
            
            // Verifica permesso scrittura impostazioni
            if (isChecked && !Settings.System.canWrite(context)) {
                switchBase.isChecked = false
                AlertDialog.Builder(context)
                    .setTitle("Permesso necessario")
                    .setMessage("Per impostare la suoneria StoppAI, l'app deve poter modificare le impostazioni di sistema.")
                    .setPositiveButton("Vai alle impostazioni") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        startActivity(intent)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
                return@setOnCheckedChangeListener
            }

            prefs.edit().putBoolean("protezione_base", isChecked).apply()

            try {
                if (isChecked) {
                    // 1. SALVARE SUONERIA ORIGINALE
                    val currentUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                    if (currentUri != null && !currentUri.toString().contains(context.packageName)) {
                        prefs.edit().putString("suoneria_originale", currentUri.toString()).apply()
                    }

                    // 2. IMPOSTARE SUONERIA STOPPAI (VIA MEDIASTORE)
                    val stoppaiUri = prepareRingtoneUri(context)
                    if (stoppaiUri != null) {
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, stoppaiUri)
                        android.widget.Toast.makeText(context, "Suoneria StoppAI attivata", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    alzaVolume()
                    
                    // 3. RIPRISTINARE SUONERIA ORIGINALE
                    val uriOriginaleStr = prefs.getString("suoneria_originale", null)
                    if (uriOriginaleStr != null) {
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, Uri.parse(uriOriginaleStr))
                        android.widget.Toast.makeText(context, "Suoneria originale ripristinata", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_RING", "Errore: ${e.message}")
            }

            aggiornaVolumeUI()
        }

        // Avvio iniziale
        if (prefs.getBoolean("protezione_totale", false)) startCountdown()
        aggiornaVolumeUI()
    }

    private fun setupRecyclerView() {
        adapter = CallLogAdapter()
        rvLog.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvLog.adapter = adapter
        
        val db = StoppAiDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            db.callLogDao().getAllCalls().collectLatest { list ->
                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun caricaStatistiche() {
        val db = StoppAiDatabase.getInstance(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            val tot = db.callLogDao().getTotalCalls()
            
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val oggiCnt = db.callLogDao().getCallsToday(calendar.timeInMillis)
            
            withContext(Dispatchers.Main) {
                tvStatTotale.text = tot.toString()
                tvStatOggi.text = oggiCnt.toString()
                tvStatReferral.text = "0"
            }
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
            prefs.edit().putBoolean("protezione_totale", false).apply()
            switchTotale.isChecked = false
            switchBase.isEnabled = true
            stopCountdown()
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
        caricaStatistiche()
        if (prefs.getBoolean("protezione_totale", false)) {
            startCountdown()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCountdown()
    }

    private fun showVolumeDialog() {
        val db = StoppAiDatabase.getInstance(requireContext())
        val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
        val volAttuale = repo.getVolumePreferito()
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }
        val tvInfo = TextView(requireContext()).apply {
            text = "$volAttuale / 15"; textSize = 20f; gravity = android.view.Gravity.CENTER; setPadding(0, 0, 0, 40)
        }
        val seekBar = android.widget.SeekBar(requireContext()).apply {
            max = 15; progress = volAttuale
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: android.widget.SeekBar?, p: Int, b: Boolean) { tvInfo.text = "$p / 15" }
                override fun onStartTrackingTouch(s: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(s: android.widget.SeekBar?) {}
            })
        }
        layout.addView(tvInfo); layout.addView(seekBar)
        AlertDialog.Builder(requireContext()).setTitle("Volume suoneria target").setView(layout)
            .setPositiveButton("SALVA") { _, _ ->
                val nuovoVol = seekBar.progress
                lifecycleScope.launch(Dispatchers.IO) {
                    repo.setVolumePreferito(nuovoVol)
                    launch(Dispatchers.Main) { 
                        aggiornaVolumeUI() 
                    }
                }
            }.setNegativeButton("ANNULLA", null).show()
    }

    private fun alzaVolume() {
        val db = StoppAiDatabase.getInstance(requireContext())
        val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
        lifecycleScope.launch(Dispatchers.IO) {
            val target = repo.getVolumePreferito()
            launch(Dispatchers.Main) {
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, target, 0)
                aggiornaVolumeUI()
            }
        }
    }

    private fun abbassaVolume() {
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
        aggiornaVolumeUI()
    }

    private fun aggiornaVolumeUI() {
        val db = StoppAiDatabase.getInstance(requireContext())
        val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
        lifecycleScope.launch(Dispatchers.IO) {
            val target = repo.getVolumePreferito()
            launch(Dispatchers.Main) {
                val isB = switchBase.isChecked
                val isT = switchTotale.isChecked
                val text = "Vol: $target/15"
                val actC = android.graphics.Color.RED
                val inC = android.graphics.Color.GRAY
                
                volTextBase.text = text
                volTextTotale.text = text
                
                volIconBase.setImageResource(if (isB) R.drawable.ic_speaker_active else R.drawable.ic_speaker_inactive)
                volIconTotale.setImageResource(if (isT) R.drawable.ic_speaker_active else R.drawable.ic_speaker_inactive)
                
                volTextBase.setTextColor(if (isB) actC else inC)
                volTextTotale.setTextColor(if (isT) actC else inC)
            }
        }
    }

    private fun prepareRingtoneUri(context: Context): Uri? {
        try {
            val fileName = "stoppai_ring.mp3"
            val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            context.contentResolver.query(queryUri, null, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    return Uri.withAppendedPath(queryUri, id.toString())
                }
            }
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
            }
            val uri = context.contentResolver.insert(queryUri, values)
            if (uri != null) {
                context.resources.openRawResource(R.raw.stoppai_ring).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                return uri
            }
        } catch (e: Exception) { android.util.Log.e("STOPPAI_RING", "Fail: ${e.message}") }
        return null
    }

}
