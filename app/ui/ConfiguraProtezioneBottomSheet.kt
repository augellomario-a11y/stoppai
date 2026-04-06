// FILE: ConfiguraProtezioneBottomSheet.kt
// SCOPO: Configurazione Protezione Base + scelta messaggio segreteria ARIA
// DIPENDENZE: bottom_sheet_config_base.xml, AriaConfigApi, AriaMessagePlayer, AriaRecorder
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.ui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R
import com.ifs.stoppai.core.AriaConfigApi
import com.ifs.stoppai.core.AriaMessagePlayer
import com.ifs.stoppai.core.AriaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ConfiguraProtezioneBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefs: SharedPreferences
    var onConfirmListener: (() -> Unit)? = null

    // Stato ARIA
    private var selectedTipo: String = "base"
    private var selectedPresetId: String? = null
    private var recorder: AriaRecorder? = null
    private var recordedFile: File? = null
    private var uploading = false

    // View references
    private var layoutPresetList: LinearLayout? = null
    private var layoutCustomControls: LinearLayout? = null
    private var btnRecord: Button? = null
    private var btnPlayCustom: Button? = null
    private var txtRecordTimer: TextView? = null
    private var txtCustomStatus: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.bottom_sheet_config_base, container, false)
        prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        setupNumeriEsteri(root)
        setupSmsReply(root)
        setupSegreteria(root)
        setupConfirmButton(root)

        // Carica config ARIA dal backend
        caricaAriaConfig()

        return root
    }

    // ============================================
    // SEZIONE 1: NUMERI ESTERI
    // ============================================
    private fun setupNumeriEsteri(root: View) {
        val swEsteri = root.findViewById<Switch>(R.id.switch_esteri)
        swEsteri.isChecked = prefs.getBoolean("consenti_esteri", false)
    }

    // ============================================
    // SEZIONE 2: SMS RISPOSTA RAPIDA
    // ============================================
    private fun setupSmsReply(root: View) {
        val swSms = root.findViewById<Switch>(R.id.switch_sms_reply)
        val layoutEditor = root.findViewById<View>(R.id.layout_sms_editor)
        val edtSms = root.findViewById<EditText>(R.id.edt_sms_text)
        val txtCounter = root.findViewById<TextView>(R.id.txt_char_counter)

        val isSmsAct = prefs.getBoolean("sms_risposta_attivo", false)
        swSms.isChecked = isSmsAct
        layoutEditor.visibility = if (isSmsAct) View.VISIBLE else View.GONE

        val defaultMsg = "Ciao, in questo momento non posso rispondere. Lasciami un messaggio e ti richiamo appena possibile."
        val savedMsg = prefs.getString("sms_testo_risposta", defaultMsg)
        edtSms.setText(savedMsg)
        txtCounter.text = "${savedMsg?.length ?: 0} / 160"

        swSms.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !prefs.getBoolean("sms_alert_mostrato", false)) {
                showCostAlert(swSms, layoutEditor)
            } else {
                layoutEditor.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        edtSms.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                txtCounter.text = "${s?.length ?: 0} / 160"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ============================================
    // SEZIONE 3: MESSAGGIO SEGRETERIA ARIA
    // ============================================
    private fun setupSegreteria(root: View) {
        val radioGroup = root.findViewById<RadioGroup>(R.id.radio_tipo_messaggio)
        layoutPresetList = root.findViewById(R.id.layout_preset_list)
        layoutCustomControls = root.findViewById(R.id.layout_custom_controls)
        btnRecord = root.findViewById(R.id.btn_record_custom)
        btnPlayCustom = root.findViewById(R.id.btn_play_custom)
        txtRecordTimer = root.findViewById(R.id.txt_record_timer)
        txtCustomStatus = root.findViewById(R.id.txt_custom_status)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_base -> {
                    selectedTipo = "base"
                    layoutPresetList?.visibility = View.GONE
                    layoutCustomControls?.visibility = View.GONE
                    AriaMessagePlayer.stop()
                }
                R.id.radio_preset -> {
                    selectedTipo = "preset"
                    layoutPresetList?.visibility = View.VISIBLE
                    layoutCustomControls?.visibility = View.GONE
                    populatePresetList()
                }
                R.id.radio_custom -> {
                    selectedTipo = "custom"
                    layoutPresetList?.visibility = View.GONE
                    layoutCustomControls?.visibility = View.VISIBLE
                    AriaMessagePlayer.stop()
                }
            }
        }

        setupRecordingControls()
    }

    private fun populatePresetList() {
        val list = layoutPresetList ?: return
        list.removeAllViews()

        val allPresets = AriaConfigApi.PRESETS_MASCHILI + AriaConfigApi.PRESETS_FEMMINILI
        allPresets.forEach { (id, label) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 4, 0, 4) }
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val rb = RadioButton(requireContext()).apply {
                text = label
                textSize = 13f
                setTextColor(0xFF333333.toInt())
                buttonTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isChecked = (selectedPresetId == id)
                setOnClickListener {
                    selectedPresetId = id
                    // Deseleziona gli altri RadioButton nel layout
                    for (i in 0 until list.childCount) {
                        val child = list.getChildAt(i) as? LinearLayout ?: continue
                        val childRb = child.getChildAt(0) as? RadioButton ?: continue
                        childRb.isChecked = (childRb.text == label)
                    }
                }
            }

            val btnPlay = Button(requireContext()).apply {
                text = "▶"
                textSize = 10f
                minWidth = 0
                minimumWidth = 0
                setPadding(20, 8, 20, 8)
                backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener {
                    if (AriaMessagePlayer.isPlaying(id)) {
                        AriaMessagePlayer.stop()
                        text = "▶"
                    } else {
                        text = "■"
                        AriaMessagePlayer.playUrl(AriaConfigApi.presetAudioUrl(id), id) {
                            try { text = "▶" } catch (_: Exception) {}
                        }
                    }
                }
            }

            row.addView(rb)
            row.addView(btnPlay)
            list.addView(row)
        }
    }

    // ============================================
    // REGISTRAZIONE CUSTOM
    // ============================================
    private fun setupRecordingControls() {
        btnRecord?.setOnClickListener {
            if (recorder?.isCurrentlyRecording() == true) {
                recorder?.stop()
                btnRecord?.text = "● REGISTRA"
            } else {
                if (!checkMicPermission()) return@setOnClickListener
                startRecording()
            }
        }

        btnPlayCustom?.setOnClickListener {
            val file = recordedFile ?: return@setOnClickListener
            if (AriaMessagePlayer.isPlaying("custom_local")) {
                AriaMessagePlayer.stop()
                btnPlayCustom?.text = "▶ ASCOLTA"
            } else {
                btnPlayCustom?.text = "■ STOP"
                AriaMessagePlayer.playLocalFile(file.absolutePath, "custom_local") {
                    try { btnPlayCustom?.text = "▶ ASCOLTA" } catch (_: Exception) {}
                }
            }
        }
    }

    private fun checkMicPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Toast.makeText(requireContext(), "Permesso microfono necessario", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
        }
        return granted
    }

    private fun startRecording() {
        recorder = AriaRecorder(requireContext()).apply {
            onTick = { sec ->
                txtRecordTimer?.text = "Registrazione... ${sec}s / ${AriaRecorder.MAX_SECONDS}s"
            }
            onFinished = { file ->
                btnRecord?.text = "● REGISTRA"
                txtRecordTimer?.text = "Registrazione completata"
                if (file != null && file.length() > 1024) {
                    recordedFile = file
                    btnPlayCustom?.isEnabled = true
                    txtCustomStatus?.text = "Registrazione pronta (${file.length() / 1024} KB)"
                }
            }
        }
        if (recorder?.start() == true) {
            btnRecord?.text = "■ STOP"
            txtRecordTimer?.text = "Registrazione... 0s / ${AriaRecorder.MAX_SECONDS}s"
        } else {
            Toast.makeText(requireContext(), "Impossibile avviare la registrazione", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================
    // CONFERMA — salva config
    // ============================================
    private fun setupConfirmButton(root: View) {
        val btnConfirm = root.findViewById<Button>(R.id.btn_confirm_base)
        val swEsteri = root.findViewById<Switch>(R.id.switch_esteri)
        val swSms = root.findViewById<Switch>(R.id.switch_sms_reply)
        val edtSms = root.findViewById<EditText>(R.id.edt_sms_text)

        btnConfirm.setOnClickListener {
            if (uploading) return@setOnClickListener

            // Valida preset
            if (selectedTipo == "preset" && selectedPresetId == null) {
                Toast.makeText(requireContext(), "Seleziona una voce preset", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Valida custom
            if (selectedTipo == "custom" && recordedFile == null) {
                Toast.makeText(requireContext(), "Registra prima il messaggio personale", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Stop eventuali player attivi
            AriaMessagePlayer.stop()

            // Salva SharedPreferences (logica attuale app)
            prefs.edit()
                .putBoolean("consenti_esteri", swEsteri.isChecked)
                .putBoolean("sms_risposta_attivo", swSms.isChecked)
                .putString("sms_testo_risposta", edtSms.text.toString())
                .putBoolean("protezione_base", true)
                .apply()

            // Salva config ARIA sul backend
            salvaAriaConfigBackend()
        }
    }

    // ============================================
    // BACKEND CALLS
    // ============================================
    private fun caricaAriaConfig() {
        val testerId = prefs.getInt("tester_id", -1)
        if (testerId == -1) return

        lifecycleScope.launch {
            val cfg = AriaConfigApi.getConfig(testerId)
            withContext(Dispatchers.Main) {
                selectedTipo = cfg.tipoMessaggio
                selectedPresetId = cfg.presetId
                val view = view ?: return@withContext
                val radioGroup = view.findViewById<RadioGroup>(R.id.radio_tipo_messaggio)
                when (cfg.tipoMessaggio) {
                    "base" -> radioGroup.check(R.id.radio_base)
                    "preset" -> radioGroup.check(R.id.radio_preset)
                    "custom" -> {
                        radioGroup.check(R.id.radio_custom)
                        if (cfg.customWavPath != null) {
                            txtCustomStatus?.text = "Registrazione già presente sul server"
                        }
                    }
                }
            }
        }
    }

    private fun salvaAriaConfigBackend() {
        val testerId = prefs.getInt("tester_id", -1)
        if (testerId == -1) {
            // No tester_id: chiudi e basta
            onConfirmListener?.invoke()
            dismiss()
            return
        }

        uploading = true
        lifecycleScope.launch {
            var ok = true

            // Se custom: prima upload WAV
            if (selectedTipo == "custom" && recordedFile != null) {
                ok = AriaConfigApi.uploadCustom(testerId, recordedFile!!)
            }

            if (ok) {
                ok = AriaConfigApi.saveConfig(
                    testerId = testerId,
                    tipoMessaggio = selectedTipo,
                    presetId = selectedPresetId
                )
            }

            withContext(Dispatchers.Main) {
                uploading = false
                if (ok) {
                    Toast.makeText(requireContext(), "Configurazione salvata", Toast.LENGTH_SHORT).show()
                    onConfirmListener?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Errore salvataggio. Riprova.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        AriaMessagePlayer.stop()
        recorder?.stop()
        super.onDestroyView()
    }

    private fun showCostAlert(sw: Switch, layout: View) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle("⚠️ Attenzione")
            .setMessage("L'invio di SMS automatici utilizza il credito del tuo piano telefonico. I costi dipendono dal contratto con il tuo operatore. StoppAI non si assume responsabilità per i costi degli SMS inviati.")
            .setPositiveButton("HO CAPITO, CONTINUA") { _, _ ->
                prefs.edit().putBoolean("sms_alert_mostrato", true).apply()
                layout.visibility = View.VISIBLE
            }
            .setNegativeButton("ANNULLA") { _, _ ->
                sw.isChecked = false
                layout.visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }
}
