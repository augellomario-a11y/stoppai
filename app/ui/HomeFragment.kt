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
import android.util.Log
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.provider.ContactsContract
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
    private lateinit var txtSmsStatus: TextView
    private lateinit var txtPreferitiStatus: TextView
    private lateinit var layoutProt: View
    private lateinit var btnToggleProt: TextView
    private lateinit var txtProtSummary: TextView

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            caricaStatistiche()
        }
    }

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
        txtSmsStatus = view.findViewById(R.id.txt_sms_status)
        txtPreferitiStatus = view.findViewById(R.id.txt_preferiti_status)

        layoutProt = view.findViewById(R.id.ID_HOME_LAYOUT_PROT)
        btnToggleProt = view.findViewById(R.id.ID_HOME_BTN_TOGGLE_PROT)
        txtProtSummary = view.findViewById(R.id.ID_HOME_TXT_PROT_SUMMARY)

        setupRecyclerView()
        setupCollapsibleProtections(view)
        setupStatsLongClicks(view)
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

        // Toggle Referral Box (SA-056)
        val boxReferral = view.findViewById<View>(R.id.ID_HOME_BOX_REFERRAL)
        var isShowEarnings = true
        boxReferral.setOnClickListener {
            isShowEarnings = !isShowEarnings
            tvStatReferral.text = if (isShowEarnings) "€0,00" else "0"
            // Aggiorna anche la label sopra se necessario
            val lbl = boxReferral.findViewById<TextView>(R.id.txt_referral_label)
            lbl?.text = if (isShowEarnings) "Guadagni" else "Invitati"
        }

        // Switch Protezione Totale — apre BottomSheet
        switchTotale.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val bs = ProtezioneBottomSheet()
                bs.onAttivaListener = {
                    switchBase.isEnabled = false
                    switchTotale.isChecked = true
                    startCountdown()
                    aggiornaStatusFlags()
                }
                bs.show(parentFragmentManager, "protezione_bs")
            } else {
                prefs.edit().putBoolean("protezione_totale", false).apply()
                switchBase.isEnabled = true
                stopCountdown()
                aggiornaVolumeUI()
                aggiornaStatusFlags()
            }
        }
        aggiornaStatusFlags()
        // Switch Protezione Base — apre Configurazione (SA-057)
        switchBase.setOnClickListener {
            val isChecked = switchBase.isChecked
            val context = requireContext()
            
            if (isChecked) {
                // Se attiviamo, mostriamo PRIMA il configuratore
                val bs = ConfiguraProtezioneBottomSheet()
                bs.onConfirmListener = {
                    // Verifichiamo i permessi prima di attivare definitivamente
                    if (!Settings.System.canWrite(context)) {
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
                    } else {
                        switchBase.isChecked = true
                        prefs.edit().putBoolean("protezione_base", true).apply()
                        try {
                            // Salvare suoneria originale se non già salvata
                            val currentUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                            if (currentUri != null && !currentUri.toString().contains(context.packageName)) {
                                prefs.edit().putString("original_ringtone", currentUri.toString()).apply()
                            }
                            // Imposta StoppAI ring
                            val stoppAiUri = Uri.parse("android.resource://${context.packageName}/${R.raw.stoppai_ring}")
                            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, stoppAiUri)
                        } catch (e: Exception) {}
                        aggiornaStatusFlags()
                    }
                }
                bs.show(parentFragmentManager, "config_base_bs")
            } else {
                // Se disattiviamo (SA-068)
                val segreteriaAttiva = prefs.getBoolean("segreteria_attiva", false)
                if (segreteriaAttiva) {
                    AlertDialog.Builder(context)
                        .setTitle("⚠️ Segreteria ancora attiva")
                        .setMessage("Hai disattivato la protezione base ma la segreteria telefonica è ancora attiva.\nTutte le chiamate senza risposta continueranno ad essere deviate anche senza StoppAI attivo.\nVuoi disattivarla?")
                        .setPositiveButton("DISATTIVA SEGRETERIA") { _, _ ->
                            try {
                                val intent = Intent(Intent.ACTION_DIAL)
                                intent.data = Uri.parse("tel:${Uri.encode("##61#")}")
                                startActivity(intent)
                                prefs.edit().putBoolean("segreteria_attiva", false).apply()
                            } catch (e: Exception) {}
                            eseguiDisattivazioneProtezione(prefs, context)
                        }
                        .setNeutralButton("LASCIA ATTIVA") { _, _ ->
                            eseguiDisattivazioneProtezione(prefs, context)
                        }
                        .setNegativeButton("ANNULLA") { _, _ ->
                            switchBase.isChecked = true
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    eseguiDisattivazioneProtezione(prefs, context)
                }
            }
        }
        aggiornaVolumeUI()

        // Avvio iniziale
        if (prefs.getBoolean("protezione_totale", false)) startCountdown()
        aggiornaVolumeUI()

        // SEEDER PER TEST MINI CRM (Long click sul volume base)
        volTextBase.setOnLongClickListener {
            val db = StoppAiDatabase.getInstance(requireContext())
            lifecycleScope.launch(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val entries = listOf(
                    com.ifs.stoppai.db.CallLogEntry(phoneNumber = "+393331234567", callType = "MOBILE", timestamp = now, statusId = 0, displayName = "Mario Rossi"),
                    com.ifs.stoppai.db.CallLogEntry(phoneNumber = "+3906112233", callType = "LANDLINE", timestamp = now - 3600000, statusId = 1, displayName = "Roma Ufficio"),
                    com.ifs.stoppai.db.CallLogEntry(phoneNumber = "+39345999888", callType = "MOBILE", timestamp = now - 7200000, statusId = 2, displayName = "Pest Spam"),
                    com.ifs.stoppai.db.CallLogEntry(phoneNumber = "unknown", callType = "PRIVATE", timestamp = now - 14400000, statusId = 0, displayName = "🕵️ Numero Nascosto"),
                    com.ifs.stoppai.db.CallLogEntry(phoneNumber = "+39333999888", callType = "MOBILE", timestamp = now - 28800000, statusId = 0, nota = "Ricordati di richiamare", callDirection = "USCITA", displayName = "Luigi Bianchi")
                )
                entries.forEach { db.callLogDao().insertCallLog(it) }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(requireContext(), "Test CRM: 5 Chiamate Simulate!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    private fun eseguiDisattivazioneProtezione(prefs: android.content.SharedPreferences, context: Context) {
        prefs.edit().putBoolean("protezione_base", false).apply()
        try {
            val originalStr = prefs.getString("original_ringtone", null)
            if (originalStr != null) {
                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, Uri.parse(originalStr))
            }
        } catch (e: Exception) {}
        aggiornaStatusFlags()
    }

    private fun setupRecyclerView() {
        adapter = CallLogAdapter { item ->
            val bs = CallActionBottomSheet(item) {
                caricaStatistiche()
            }
            bs.show(parentFragmentManager, "call_actions_bs")
        }
        rvLog.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvLog.adapter = adapter
        
        val db = StoppAiDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            db.callLogDao().getAllCalls().collectLatest { list ->
                val crmItems = withContext(Dispatchers.IO) {
                    list.map { entry ->
                        val cnt = db.callLogDao().getCountForNumber(entry.phoneNumber)
                        com.ifs.stoppai.db.CallLogCrmItem(entry, cnt)
                    }
                }
                adapter.submitList(crmItems)
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
        val filter = android.content.IntentFilter("com.ifs.stoppai.CALL_LOGGED")
        requireContext().registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        
        aggiornaVolumeUI()
        aggiornaStatusFlags()
        caricaStatistiche()
        syncContactNames()
        if (prefs.getBoolean("protezione_totale", false)) {
            startCountdown()
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
        stopCountdown()
    }

    private fun aggiornaStatusFlags() {
        val pBase = switchBase.isChecked
        val pTotale = switchTotale.isChecked
        val smsOn = prefs.getBoolean("sms_risposta_attivo", false)
        val prefOn = prefs.getBoolean("includi_preferiti", false)

        txtSmsStatus.visibility = if (pBase && smsOn) View.VISIBLE else View.GONE
        txtPreferitiStatus.visibility = if (pTotale && prefOn) View.VISIBLE else View.GONE
        
        // Aggiorna summary header (SA-068)
        val sBase = if (pBase) "ON" else "OFF"
        val sTot = if (pTotale) "ON" else "OFF"
        val segRetOn = prefs.getBoolean("segreteria_attiva", false)
        val sSeg = if (segRetOn) "ON" else "OFF"
        
        txtProtSummary.text = "🛡️ Base: $sBase | Totale: $sTot\n📞 Segreteria: $sSeg"
    }

    private fun setupCollapsibleProtections(view: View) {
        val header = view.findViewById<View>(R.id.ID_HOME_HEADER_PROT)
        var isExpanded = prefs.getBoolean("home_sezione_espansa", true)
        
        val updateUI = {
            layoutProt.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnToggleProt.text = if (isExpanded) "▲" else "▼"
        }
        
        updateUI()
        
        header.setOnClickListener {
            isExpanded = !isExpanded
            prefs.edit().putBoolean("home_sezione_espansa", isExpanded).apply()
            updateUI()
        }
    }

    private fun setupStatsLongClicks(view: View) {
        val boxTotale = view.findViewById<View>(R.id.ID_HOME_BOX_TOTALE)
        val boxOggi = view.findViewById<View>(R.id.ID_HOME_BOX_OGGI)

        boxTotale.setOnLongClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Azzera tutto")
                .setMessage("Vuoi azzerare il contatore totale e cancellare tutto il registro chiamate?")
                .setPositiveButton("AZZERA") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = StoppAiDatabase.getInstance(requireContext())
                        db.callLogDao().deleteAllCalls()
                        launch(Dispatchers.Main) { 
                            caricaStatistiche() 
                            // Forza pulizia lista immediata
                            adapter.submitList(emptyList())
                        }
                    }
                }
                .setNegativeButton("ANNULLA", null).show()
            true
        }

        boxOggi.setOnLongClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Azzera oggi")
                .setMessage("Vuoi azzerare il contatore di oggi?")
                .setPositiveButton("AZZERA") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = StoppAiDatabase.getInstance(requireContext())
                        val cal = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                        }
                        db.callLogDao().deleteCallsSince(cal.timeInMillis)
                        launch(Dispatchers.Main) { caricaStatistiche() }
                    }
                }
                .setNegativeButton("ANNULLA", null).show()
            true
        }
    }

    private fun syncContactNames() {
        val context = requireContext()
        val db = StoppAiDatabase.getInstance(context)
        lifecycleScope.launch(Dispatchers.IO) {
            // Aggiorna nomi per i log di oggi senza nome.
            // In un CRM reale useremmo un Worker, qui facciamo JIT per i recenti.
            val list = db.callLogDao().getAllCallsSync() 
            list.filter { it.displayName.isEmpty() || it.displayName == it.phoneNumber }.forEach { entry ->
                val newName = getContactName(context, entry.phoneNumber)
                if (newName.isNotEmpty()) {
                    db.callLogDao().updateDisplayName(entry.id, newName)
                }
            }
        }
    }

    private fun getContactName(context: Context, phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return try {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { 
                if (it.moveToFirst()) it.getString(0) else ""
            } ?: ""
        } catch (e: Exception) { "" }
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
