// FILE: AriaTranscriptionSheet.kt
// SCOPO: BottomSheet trascrizioni ARIA con player audio, rating fedeltà e segnalazione spam
// ULTIMA MODIFICA: 2026-04-11

package com.ifs.stoppai.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
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

class AriaTranscriptionSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_NUMERO = "phone_number"
        private const val ARG_CALL_ID = "call_log_id"
        private const val BACKEND_URL = "https://stoppai.it"

        fun newInstance(phoneNumber: String, callLogId: Long): AriaTranscriptionSheet {
            return AriaTranscriptionSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_NUMERO, phoneNumber)
                    putLong(ARG_CALL_ID, callLogId)
                }
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var audioPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_aria, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val phoneNumber = arguments?.getString(ARG_NUMERO) ?: "Sconosciuto"
        val callLogId = arguments?.getLong(ARG_CALL_ID) ?: 0L

        view.findViewById<TextView>(R.id.txt_aria_numero).text = "📞 $phoneNumber"
        view.findViewById<TextView>(R.id.btn_chiudi_aria).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.btn_aria_chiudi_bottom).setOnClickListener { dismiss() }

        // Espandi di default
        dialog?.setOnShowListener {
            val bs = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bs?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        caricaMessaggi(phoneNumber, callLogId, view)
    }

    private fun caricaMessaggi(phoneNumber: String, callLogId: Long, view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = StoppAiDatabase.getInstance(requireContext())
                val messaggi = db.ariaMessaggioDao().getPerCallLogId(callLogId).first()
                val entry = db.callLogDao().getCallById(callLogId)
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY)
                val data = if (entry != null) sdf.format(Date(entry.timestamp)) else ""

                // Cerca note precedenti per questo numero nel CRM locale
                val numNorm = com.ifs.stoppai.core.PhoneNumberUtils.normalizeNumber(phoneNumber)
                val notePrecedenti = if (numNorm.length >= 5) {
                    val ultCifre = numNorm.takeLast(10)
                    db.callLogDao().getAllCallsSync()
                        .filter { it.nota.isNotBlank() && it.phoneNumber.takeLast(10) == ultCifre }
                        .distinctBy { it.nota }
                        .map { it.nota }
                } else emptyList()

                withContext(Dispatchers.Main) {
                    val txtData = view.findViewById<TextView>(R.id.txt_aria_data)
                    val txtContenuto = view.findViewById<TextView>(R.id.txt_aria_contenuto)

                    if (data.isNotEmpty()) txtData.text = "📅  $data"

                    // Mostra note precedenti per questo numero
                    if (notePrecedenti.isNotEmpty()) {
                        val txtNumero = view.findViewById<TextView>(R.id.txt_aria_numero)
                        txtNumero.append("\n📝 Note: ${notePrecedenti.joinToString(" | ")}")
                    }

                    if (messaggi.isEmpty()) {
                        txtContenuto.text = "Nessun messaggio ARIA collegato a questa chiamata."
                        return@withContext
                    }

                    // Mostra trascrizioni
                    val sb = StringBuilder()
                    messaggi.forEach { msg ->
                        sb.append("💬  ${msg.testo}\n\n─────────────────────\n\n")
                    }
                    txtContenuto.text = sb.toString().trimEnd()

                    // Setup player audio — verifica piano (PRO richiesto)
                    val msgConWav = messaggi.firstOrNull { !it.wavFilename.isNullOrBlank() }
                    if (msgConWav?.wavFilename != null) {
                        if (com.ifs.stoppai.core.PlanManager.isDisponibile(requireContext(), com.ifs.stoppai.core.PlanManager.Feature.PLAYER_AUDIO)) {
                            setupPlayer(view, msgConWav.wavFilename)
                        } else {
                            // Mostra player bloccato
                            val layout = view.findViewById<LinearLayout>(R.id.layout_player)
                            layout.visibility = View.VISIBLE
                            layout.alpha = 0.5f
                            layout.setOnClickListener {
                                com.ifs.stoppai.core.UpgradeDialog.show(requireContext(), com.ifs.stoppai.core.PlanManager.Feature.PLAYER_AUDIO)
                            }
                        }
                    }

                    // Mostra rating e spam per tutti i messaggi con testo
                    if (messaggi.any { it.testo.isNotBlank() }) {
                        setupRating(view, messaggi.first().id)
                        setupSpam(view, phoneNumber, messaggi.first().id, messaggi.first().spamVoto)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.findViewById<TextView>(R.id.txt_aria_contenuto).text =
                        "Errore nel caricamento: ${e.message}"
                }
            }
        }
    }

    // ===== PLAYER AUDIO =====
    private fun setupPlayer(view: View, wavFilename: String) {
        val layout = view.findViewById<LinearLayout>(R.id.layout_player)
        val btnPlay = view.findViewById<android.widget.ImageView>(R.id.btn_play_pause)
        val seekBar = view.findViewById<SeekBar>(R.id.seek_audio)
        val txtTime = view.findViewById<TextView>(R.id.txt_audio_time)

        layout.visibility = View.VISIBLE

        val audioUrl = "$BACKEND_URL/aria-recordings/$wavFilename"
        val txtDuration = view.findViewById<TextView>(R.id.txt_audio_duration)

        btnPlay.setOnClickListener {
            if (audioPlaying) {
                pauseAudio(btnPlay)
            } else {
                playAudio(audioUrl, btnPlay, seekBar, txtTime, txtDuration)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun playAudio(url: String, btnPlay: android.widget.ImageView, seekBar: SeekBar, txtTime: TextView, txtDuration: TextView) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener { mp ->
                        seekBar.max = mp.duration
                        txtDuration.text = formatTime(mp.duration)
                        mp.start()
                        this@AriaTranscriptionSheet.audioPlaying = true
                        btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                        updateSeekBar(seekBar, txtTime)
                    }
                    setOnCompletionListener {
                        this@AriaTranscriptionSheet.audioPlaying = false
                        btnPlay.setImageResource(android.R.drawable.ic_media_play)
                        seekBar.progress = 0
                        txtTime.text = "0:00"
                    }
                    setOnErrorListener { _, _, _ ->
                        android.widget.Toast.makeText(context,
                            "L'audio è disponibile solo per 24 ore dalla ricezione", android.widget.Toast.LENGTH_SHORT).show()
                        true
                    }
                    prepareAsync()
                }
            } else {
                mediaPlayer?.start()
                audioPlaying = true
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                updateSeekBar(seekBar, txtTime)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Errore riproduzione: ${e.message}",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseAudio(btnPlay: android.widget.ImageView) {
        mediaPlayer?.pause()
        audioPlaying = false
        btnPlay.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun formatTime(ms: Int): String {
        val secs = ms / 1000
        return "%d:%02d".format(secs / 60, secs % 60)
    }

    private fun updateSeekBar(seekBar: SeekBar, txtTime: TextView) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        seekBar.progress = mp.currentPosition
                        val secs = mp.currentPosition / 1000
                        txtTime.text = "%d:%02d".format(secs / 60, secs % 60)
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }, 500)
    }

    // ===== RATING FEDELTÀ =====
    private fun setupRating(view: View, msgId: Int) {
        val layout = view.findViewById<LinearLayout>(R.id.layout_rating)
        layout.visibility = View.VISIBLE

        val buttons = mapOf(
            R.id.btn_rate_100 to 100,
            R.id.btn_rate_80 to 80,
            R.id.btn_rate_60 to 60,
            R.id.btn_rate_40 to 40,
            R.id.btn_rate_20 to 20
        )

        buttons.forEach { (btnId, rating) ->
            view.findViewById<Button>(btnId).setOnClickListener { btn ->
                // Evidenzia il pulsante selezionato
                buttons.keys.forEach { id ->
                    view.findViewById<Button>(id).backgroundTintList =
                        android.content.res.ColorStateList.valueOf(0xFFEEEEEE.toInt())
                }
                (btn as Button).backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFFC8A96E.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())

                // Invia al backend
                inviaRating(msgId, rating)
                android.widget.Toast.makeText(context, "Valutazione: $rating%",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun inviaRating(msgId: Int, rating: Int) {
        android.util.Log.d("ARIA_RATING", "Rating: $rating% per msgId=$msgId")
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = java.net.URL("${com.ifs.stoppai.core.AriaFcmService.SERVER_URL}/api/tester/aria-rating")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                val body = """{"msg_id":$msgId,"rating":$rating}"""
                conn.outputStream.write(body.toByteArray())
                val code = conn.responseCode
                android.util.Log.d("ARIA_RATING", "Inviato al backend ($code)")
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("ARIA_RATING", "Errore invio: ${e.message}")
            }
        }
    }

    // ===== SPAM / ATTENDIBILE =====
    private fun setupSpam(view: View, phoneNumber: String, msgId: Int, currentVoto: String?) {
        val btnAttendibile = view.findViewById<Button>(R.id.btn_attendibile)
        val btnSpam = view.findViewById<Button>(R.id.btn_spam)
        val txtStatus = view.findViewById<TextView>(R.id.txt_spam_status)

        // Mostra stato corrente se già votato
        if (currentVoto != null) {
            aggiornaStatoSpam(currentVoto, btnAttendibile, btnSpam, txtStatus)
        }

        btnAttendibile.setOnClickListener {
            salvaSpamVoto(msgId, "attendibile")
            aggiornaStatoSpam("attendibile", btnAttendibile, btnSpam, txtStatus)
            android.widget.Toast.makeText(context, "Segnato come attendibile",
                android.widget.Toast.LENGTH_SHORT).show()
        }

        btnSpam.setOnClickListener {
            salvaSpamVoto(msgId, "spam")
            aggiornaStatoSpam("spam", btnAttendibile, btnSpam, txtStatus)
            segnalaSpamAlBackend(phoneNumber)
            android.widget.Toast.makeText(context, "Segnalato come spam",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun aggiornaStatoSpam(voto: String, btnAtt: Button, btnSpam: Button, txtStatus: TextView) {
        txtStatus.visibility = View.VISIBLE
        if (voto == "spam") {
            btnSpam.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
            btnSpam.setTextColor(0xFFFFFFFF.toInt())
            btnAtt.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE8F5E9.toInt())
            btnAtt.setTextColor(0xFF2E7D32.toInt())
            txtStatus.text = "🚫 Hai segnalato questo numero come spam"
            txtStatus.setTextColor(0xFFC62828.toInt())
        } else {
            btnAtt.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())
            btnAtt.setTextColor(0xFFFFFFFF.toInt())
            btnSpam.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFEBEE.toInt())
            btnSpam.setTextColor(0xFFC62828.toInt())
            txtStatus.text = "✅ Hai segnato questo numero come attendibile"
            txtStatus.setTextColor(0xFF2E7D32.toInt())
        }
    }

    private fun salvaSpamVoto(msgId: Int, voto: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = StoppAiDatabase.getInstance(requireContext())
                db.openHelper.writableDatabase.execSQL(
                    "UPDATE aria_messaggi SET spamVoto = ? WHERE id = ?",
                    arrayOf(voto, msgId)
                )
            } catch (e: Exception) {
                android.util.Log.e("ARIA_SPAM", "Errore salvataggio spam: ${e.message}")
            }
        }
    }

    private fun segnalaSpamAlBackend(phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = java.net.URL("$BACKEND_URL/api/tester/spam-report")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                val body = """{"caller_number":"$phoneNumber"}"""
                conn.outputStream.write(body.toByteArray())
                val code = conn.responseCode
                android.util.Log.d("ARIA_SPAM", "Spam report inviato ($code) per $phoneNumber")
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("ARIA_SPAM", "Errore invio spam: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}
