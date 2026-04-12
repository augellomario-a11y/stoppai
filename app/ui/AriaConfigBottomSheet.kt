// FILE: AriaConfigBottomSheet.kt
// SCOPO: Configurazione messaggio segreteria ARIA (preset + custom + lock piano)
// ULTIMA MODIFICA: 2026-04-12

package com.ifs.stoppai.ui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R
import com.ifs.stoppai.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AriaConfigBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefs: SharedPreferences
    var onSaveListener: ((String) -> Unit)? = null

    private var selectedTipo: String = "base"
    private var selectedPresetId: String? = null
    private var recorder: AriaRecorder? = null
    private var recordedFile: File? = null
    private var hasRemoteCustom: Boolean = false
    private var uploading = false

    private var layoutPresetList: LinearLayout? = null
    private var layoutCustomControls: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.bottom_sheet_aria_config, container, false)
        prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        setupRadioGroup(root)
        setupSaveButton(root)
        setupLockBadges(root)
        caricaAriaConfig()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener {
            val bs = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bs?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
    }

    private fun setupLockBadges(root: View) {
        val ctx = requireContext()
        val radioPreset = root.findViewById<RadioButton>(R.id.radio_preset)
        val radioCustom = root.findViewById<RadioButton>(R.id.radio_custom)

        if (PlanManager.isDisponibile(ctx, PlanManager.Feature.ARIA_PRESET)) {
            radioPreset.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_lock_open, 0)
        }
        if (PlanManager.isDisponibile(ctx, PlanManager.Feature.ARIA_CUSTOM)) {
            radioCustom.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_lock_open, 0)
        }
    }

    private fun setupRadioGroup(root: View) {
        val radioGroup = root.findViewById<RadioGroup>(R.id.radio_tipo_messaggio)
        layoutPresetList = root.findViewById(R.id.layout_preset_list)
        layoutCustomControls = root.findViewById(R.id.layout_custom_controls)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_base -> {
                    selectedTipo = "base"
                    layoutPresetList?.visibility = View.GONE
                    layoutCustomControls?.visibility = View.GONE
                    AriaMessagePlayer.stop()
                }
                R.id.radio_preset -> {
                    if (!PlanManager.isDisponibile(requireContext(), PlanManager.Feature.ARIA_PRESET)) {
                        radioGroup.check(R.id.radio_base)
                        UpgradeDialog.show(requireContext(), PlanManager.Feature.ARIA_PRESET)
                        return@setOnCheckedChangeListener
                    }
                    selectedTipo = "preset"
                    layoutPresetList?.visibility = View.VISIBLE
                    layoutCustomControls?.visibility = View.GONE
                    populatePresetList()
                }
                R.id.radio_custom -> {
                    if (!PlanManager.isDisponibile(requireContext(), PlanManager.Feature.ARIA_CUSTOM)) {
                        radioGroup.check(R.id.radio_base)
                        UpgradeDialog.show(requireContext(), PlanManager.Feature.ARIA_CUSTOM)
                        return@setOnCheckedChangeListener
                    }
                    selectedTipo = "custom"
                    layoutPresetList?.visibility = View.GONE
                    layoutCustomControls?.visibility = View.VISIBLE
                    AriaMessagePlayer.stop()
                }
            }
        }

        setupRecordingControls(root)
    }

    private fun populatePresetList() {
        val list = layoutPresetList ?: return
        list.removeAllViews()

        val allPresets = AriaConfigApi.PRESETS_MASCHILI + AriaConfigApi.PRESETS_FEMMINILI
        allPresets.forEach { (id, label) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val radio = RadioButton(requireContext()).apply {
                text = label
                textSize = 14f
                setTextColor(0xFF333333.toInt())
                buttonTintList = android.content.res.ColorStateList.valueOf(0xFFC8A96E.toInt())
                isChecked = (id == selectedPresetId)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    selectedPresetId = id
                    populatePresetList()
                }
            }

            val density = requireContext().resources.displayMetrics.density
            val btnPlay = Button(requireContext()).apply {
                text = "▶"
                textSize = 18f
                setTextColor(0xFF1a1a2e.toInt())
                backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC8A96E.toInt())
                val lp = LinearLayout.LayoutParams((48 * density).toInt(), (40 * density).toInt())
                lp.marginStart = (8 * density).toInt()
                layoutParams = lp
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    if (AriaMessagePlayer.isPlaying(id)) {
                        AriaMessagePlayer.stop()
                        text = "▶"
                    } else {
                        AriaMessagePlayer.playUrl(AriaConfigApi.presetAudioUrl(id), id) {
                            text = "▶"
                        }
                        text = "⏸"
                    }
                }
            }

            row.addView(radio)
            row.addView(btnPlay)
            list.addView(row)
        }
    }

    private fun setupRecordingControls(root: View) {
        val btnRecord = root.findViewById<Button>(R.id.btn_record_custom)
        val btnPlay = root.findViewById<Button>(R.id.btn_play_custom)
        val txtStatus = root.findViewById<TextView>(R.id.txt_custom_status)
        val txtTimer = root.findViewById<TextView>(R.id.txt_record_timer)

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
                return@setOnClickListener
            }

            if (recorder != null) {
                recordedFile = recorder?.stop()
                recorder = null
                btnRecord.text = "● REGISTRA"
                btnRecord.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
                btnPlay.isEnabled = true
                txtStatus.text = "Registrazione completata"
                txtTimer.text = "Max 30 secondi"
            } else {
                recorder = AriaRecorder(requireContext())
                recorder?.start()
                btnRecord.text = "⏹ STOP"
                btnRecord.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF333333.toInt())
                txtStatus.text = "Registrazione in corso..."
            }
        }

        btnPlay.setOnClickListener {
            val file = recordedFile
            if (file != null && file.exists()) {
                if (AriaMessagePlayer.isPlaying("custom_local")) {
                    AriaMessagePlayer.stop()
                    btnPlay.text = "▶ ASCOLTA"
                } else {
                    AriaMessagePlayer.playLocalFile(file.absolutePath, "custom_local") {
                        btnPlay.text = "▶ ASCOLTA"
                    }
                    btnPlay.text = "⏸ STOP"
                }
            } else if (hasRemoteCustom) {
                val testerId = prefs.getInt("tester_id", -1)
                if (testerId > 0) {
                    val url = AriaConfigApi.customAudioUrl(testerId)
                    if (AriaMessagePlayer.isPlaying("custom_remote")) {
                        AriaMessagePlayer.stop()
                        btnPlay.text = "▶ ASCOLTA"
                    } else {
                        AriaMessagePlayer.playUrl(url, "custom_remote") {
                            btnPlay.text = "▶ ASCOLTA"
                        }
                        btnPlay.text = "⏸ STOP"
                    }
                }
            }
        }
    }

    private fun setupSaveButton(root: View) {
        root.findViewById<Button>(R.id.btn_salva_aria).setOnClickListener {
            if (selectedTipo == "preset" && selectedPresetId == null) {
                Toast.makeText(requireContext(), "Seleziona una voce preset", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedTipo == "custom" && recordedFile == null && !hasRemoteCustom) {
                Toast.makeText(requireContext(), "Registra prima il messaggio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AriaMessagePlayer.stop()
            prefs.edit().putString("aria_tipo_messaggio", selectedTipo).apply()
            if (selectedPresetId != null) {
                prefs.edit().putString("aria_preset_id", selectedPresetId).apply()
            }

            salvaAriaConfigBackend()

            val label = when (selectedTipo) {
                "preset" -> "Voce preregistrata"
                "custom" -> "Personalizzato"
                else -> "Standard"
            }
            onSaveListener?.invoke(label)
            Toast.makeText(requireContext(), "Messaggio ARIA: $label", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun caricaAriaConfig() {
        val testerId = prefs.getInt("tester_id", -1)
        if (testerId == -1) {
            view?.findViewById<Button>(R.id.btn_salva_aria)?.isEnabled = true
            return
        }

        lifecycleScope.launch {
            val cfg = AriaConfigApi.getConfig(testerId)
            withContext(Dispatchers.Main) {
                if (cfg != null) {
                    selectedTipo = cfg.tipoMessaggio
                    selectedPresetId = cfg.presetId
                    hasRemoteCustom = !cfg.customWavPath.isNullOrBlank()

                    val radioGroup = view?.findViewById<RadioGroup>(R.id.radio_tipo_messaggio)
                    when (cfg.tipoMessaggio) {
                        "preset" -> radioGroup?.check(R.id.radio_preset)
                        "custom" -> radioGroup?.check(R.id.radio_custom)
                        else -> radioGroup?.check(R.id.radio_base)
                    }

                    if (hasRemoteCustom) {
                        view?.findViewById<Button>(R.id.btn_play_custom)?.isEnabled = true
                        view?.findViewById<TextView>(R.id.txt_custom_status)?.text = "Registrazione presente sul server"
                    }
                }
                view?.findViewById<Button>(R.id.btn_salva_aria)?.isEnabled = true
            }
        }
    }

    private fun salvaAriaConfigBackend() {
        val testerId = prefs.getInt("tester_id", -1)
        if (testerId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            if (selectedTipo == "custom" && recordedFile != null) {
                AriaConfigApi.uploadCustom(testerId, recordedFile!!)
            }
            AriaConfigApi.saveConfig(testerId, selectedTipo, selectedPresetId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AriaMessagePlayer.stop()
        recorder?.stop()
    }
}
