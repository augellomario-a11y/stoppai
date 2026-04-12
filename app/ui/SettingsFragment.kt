// FILE: SettingsFragment.kt
package com.ifs.stoppai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ifs.stoppai.R
import android.app.role.RoleManager
import android.telecom.TelecomManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.telephony.TelephonyManager

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private fun getAriaNum(): String = com.ifs.stoppai.core.UssdManager.getAriaNumber(requireContext())

    private val progressReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val current = intent?.getIntExtra("current", 0) ?: 0
            val total = intent?.getIntExtra("total", 0) ?: 0
            view?.let { updateProgressUI(it, current, total) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAccountSection(view)
        setupAriaSection(view)
        setupAdvancedFeatures(view)
        setupPermissionClickListeners(view)
        setupVolumeControl(view)

        // Ripristina valori default
        view.findViewById<Button>(R.id.ID_SETT_001).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Ripristina valori default")
                .setMessage("Tutte le impostazioni verranno riportate ai valori di fabbrica. Continuare?")
                .setPositiveButton("Ripristina") { _, _ ->
                    val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("protezione_base", false)
                        .putBoolean("protezione_totale", false)
                        .putBoolean("sms_risposta_attivo", false)
                        .putString("sms_text_custom", "Ciao, sono l'assistente ARIA. Non posso rispondere ora, lasciami un messaggio.")
                        .putInt("payment_threshold", 25)
                        .apply()
                    val db = com.ifs.stoppai.db.StoppAiDatabase.getInstance(requireContext().applicationContext)
                    val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
                    lifecycleScope.launch(Dispatchers.IO) {
                        repo.setVolumePreferito(11)
                        withContext(Dispatchers.Main) {
                            setupVolumeControl(view)
                            android.widget.Toast.makeText(requireContext(), "Valori default ripristinati", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    // ===== SEZIONE ACCOUNT =====
    private fun setupAccountSection(view: View) {
        val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val txtStatus = view.findViewById<TextView>(R.id.txt_account_status)
        val btnLogin = view.findViewById<Button>(R.id.btn_account_login)
        val btnLogout = view.findViewById<Button>(R.id.btn_account_logout)

        val loggedIn = prefs.getBoolean("logged_in", false)
        val email = prefs.getString("tester_email", null)
        val nome = prefs.getString("tester_nome", null)

        if (loggedIn && !email.isNullOrBlank()) {
            val display = if (!nome.isNullOrBlank()) "$nome ($email)" else email
            txtStatus.text = "Loggato come:\n$display"
            btnLogin.visibility = View.GONE
            btnLogout.visibility = View.VISIBLE
        } else {
            txtStatus.text = "Non sei loggato. Accedi per sincronizzare le tue chiamate."
            btnLogin.visibility = View.VISIBLE
            btnLogout.visibility = View.GONE
        }

        btnLogin.setOnClickListener {
            prefs.edit().remove("login_skipped").remove("logged_in").apply()
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Esci dall'account")
                .setMessage("Vuoi davvero uscire? Le tue chiamate locali rimarranno, ma non saranno sincronizzate.")
                .setPositiveButton("Esci") { _, _ ->
                    prefs.edit()
                        .remove("logged_in").remove("tester_id")
                        .remove("tester_email").remove("tester_nome")
                        .remove("login_skipped").apply()
                    android.widget.Toast.makeText(requireContext(), "Logout effettuato", android.widget.Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    // ===== SEZIONE SEGRETERIA ARIA =====
    private fun setupAriaSection(view: View) {
        val tm = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val operatore = tm.networkOperatorName ?: "Non rilevato"
        view.findViewById<TextView>(R.id.ID_SETT_OPERATORE).text = "Operatore: $operatore"

        val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val ariaAttiva = prefs.getBoolean("forward_active", false)
        val txtStatus = view.findViewById<TextView>(R.id.txt_aria_status)
        txtStatus.text = if (ariaAttiva) "Stato: attiva" else "Stato: non attiva"

        view.findViewById<Button>(R.id.btn_gestisci_aria).setOnClickListener {
            mostraMenuSegreteria()
        }
    }

    private fun mostraMenuSegreteria() {
        val opzioni = arrayOf(
            "Verifica deviazioni attive",
            "Attiva deviazione su non risposta",
            "Attiva deviazione su occupato",
            "Attiva deviazione su non raggiungibile",
            "Attiva tutte le deviazioni",
            "Disattiva tutte le deviazioni"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Gestisci segreteria ARIA")
            .setItems(opzioni) { _, which ->
                when (which) {
                    0 -> verificaDeviazioni()
                    1 -> scegliSecondiEAttiva()
                    2 -> eseguiUssd("*67*${getAriaNum()}#", "Deviazione su occupato")
                    3 -> eseguiUssd("*62*${getAriaNum()}#", "Deviazione su non raggiungibile")
                    4 -> attivaTutte()
                    5 -> disattivaTutte()
                }
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    private val codiciVerifica = listOf(
        "*#61#" to "Non risposta",
        "*#67#" to "Occupato",
        "*#62#" to "Non raggiungibile"
    )
    private val risultatiVerifica = StringBuilder()
    private var verificaIndex = 0

    private fun verificaDeviazioni() {
        val ctx = requireContext()
        if (android.content.pm.PackageManager.PERMISSION_GRANTED !=
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CALL_PHONE)) {
            android.widget.Toast.makeText(ctx, "Servono i permessi telefonate", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        risultatiVerifica.clear()
        verificaIndex = 0
        android.widget.Toast.makeText(ctx, "Verifica in corso...", android.widget.Toast.LENGTH_SHORT).show()
        eseguiVerificaSingola()
    }

    private fun eseguiVerificaSingola() {
        if (verificaIndex >= codiciVerifica.size) {
            mostraRisultatiDeviazioni(risultatiVerifica.toString())
            return
        }
        val ctx = try { requireContext() } catch (e: Exception) { return }
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val (codice, nome) = codiciVerifica[verificaIndex]

        try {
            tm.sendUssdRequest(codice, object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                    risultatiVerifica.append("$nome:\n$response\n\n")
                    verificaIndex++
                    // Aspetta 2 secondi prima del prossimo (il modem deve chiudere la sessione USSD)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ eseguiVerificaSingola() }, 2000)
                }
                override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                    risultatiVerifica.append("$nome: errore (codice $failureCode)\n\n")
                    verificaIndex++
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ eseguiVerificaSingola() }, 2000)
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        } catch (e: Exception) {
            risultatiVerifica.append("$nome: non supportato\n\n")
            verificaIndex++
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ eseguiVerificaSingola() }, 1000)
        }
    }

    private fun mostraRisultatiDeviazioni(testo: String) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Stato deviazioni")
                .setMessage(testo.trimEnd())
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            // Fragment potrebbe non essere più attivo
        }
    }

    private fun scegliSecondiEAttiva() {
        val opzioni = arrayOf("5 secondi", "10 secondi", "15 secondi", "20 secondi")
        val secondi = intArrayOf(5, 10, 15, 20)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Dopo quanti secondi deviare?")
            .setItems(opzioni) { _, which ->
                val sec = secondi[which]
                val codice = "*61*${getAriaNum()}**11*${sec}#"
                val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("secondi_deviazione", sec).apply()
                eseguiUssd(codice, "Deviazione non risposta (${sec}s)")
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun getCodiciAttivazione(): List<Pair<String, String>> {
        val num = getAriaNum()
        return listOf(
            "##002#" to "Passo 1/4: Disattiva segreteria operatore",
            "*61*${num}**11*10#" to "Passo 2/4: Deviazione su non risposta (10s)",
            "*67*${num}#" to "Passo 3/4: Deviazione su occupato",
            "*62*${num}#" to "Passo 4/4: Deviazione su non raggiungibile"
        )
    }
    private var passoCorrente = 0

    private fun attivaTutte() {
        passoCorrente = 0
        eseguiPassoAttivazione()
    }

    private fun eseguiPassoAttivazione() {
        if (passoCorrente >= getCodiciAttivazione().size) {
            // Completato tutti i passi
            val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("forward_active", true).apply()
            view?.findViewById<TextView>(R.id.txt_aria_status)?.text = "Stato: attiva"
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Attivazione completata")
                .setMessage("Tutte le deviazioni sono state configurate.\n\nUsa 'Verifica deviazioni attive' per confermare.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val (codice, descrizione) = getCodiciAttivazione()[passoCorrente]
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(descrizione)
            .setMessage("Premi 'Esegui' per aprire il dialer con il codice:\n\n$codice\n\nPoi premi il tasto verde di chiamata.")
            .setPositiveButton("Esegui") { _, _ ->
                eseguiUssd(codice, descrizione)
                passoCorrente++
                // Mostra il passo successivo dopo 3 secondi
                view?.postDelayed({ eseguiPassoAttivazione() }, 3000)
            }
            .setNegativeButton("Annulla", null)
            .setCancelable(false)
            .show()
    }

    private fun disattivaTutte() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Disattiva segreteria")
            .setMessage("Verranno disattivate tutte le deviazioni verso ARIA.\n\nATTENZIONE: anche la segreteria del tuo operatore verrà disattivata. Dovrai riconfigurarla manualmente se necessario.")
            .setPositiveButton("Disattiva") { _, _ ->
                com.ifs.stoppai.core.UssdManager.deactivateForward(requireContext())
                android.widget.Toast.makeText(requireContext(), "Segreteria disattivata", android.widget.Toast.LENGTH_SHORT).show()
                view?.findViewById<TextView>(R.id.txt_aria_status)?.text = "Stato: non attiva"
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eseguiUssd(codice: String, descrizione: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            // Uri.encode strappa il '+', lo preserviamo manualmente
            val encoded = Uri.encode(codice, "+*#")
            intent.data = Uri.parse("tel:$encoded")
            startActivity(intent)
            android.widget.Toast.makeText(requireContext(), "$descrizione: $codice", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Errore apertura dialer", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ===== NUMERI ESTERI + MESSAGGIO ARIA (con lock) =====
    private fun setupAdvancedFeatures(view: View) {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)

        // --- Filtro numeri esteri (SHIELD) ---
        val lockFiltro = view.findViewById<android.widget.ImageView>(R.id.lock_filtro_esteri)
        val switchEsteri = view.findViewById<android.widget.Switch>(R.id.switch_consenti_esteri)
        val disponibileFiltro = com.ifs.stoppai.core.PlanManager.isDisponibile(ctx, com.ifs.stoppai.core.PlanManager.Feature.FILTRO_ESTERI)

        if (disponibileFiltro) {
            lockFiltro.setImageResource(R.drawable.ic_lock_open)
            switchEsteri.isChecked = prefs.getBoolean("consenti_esteri", false)
            switchEsteri.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("consenti_esteri", isChecked).apply()
                android.widget.Toast.makeText(ctx,
                    if (isChecked) "Numeri esteri consentiti" else "Numeri esteri bloccati",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            switchEsteri.isEnabled = false
            switchEsteri.setOnClickListener {
                com.ifs.stoppai.core.UpgradeDialog.show(ctx, com.ifs.stoppai.core.PlanManager.Feature.FILTRO_ESTERI)
            }
        }

        // --- White list prefissi esteri (SHIELD) ---
        val lockWhitelist = view.findViewById<android.widget.ImageView>(R.id.lock_whitelist_esteri)
        val rowWhitelist = view.findViewById<View>(R.id.row_whitelist_esteri)
        if (com.ifs.stoppai.core.PlanManager.isDisponibile(ctx, com.ifs.stoppai.core.PlanManager.Feature.WHITELIST_ESTERI)) {
            lockWhitelist.setImageResource(R.drawable.ic_lock_open)
        }
        rowWhitelist.setOnClickListener {
            if (com.ifs.stoppai.core.PlanManager.isDisponibile(ctx, com.ifs.stoppai.core.PlanManager.Feature.WHITELIST_ESTERI)) {
                android.widget.Toast.makeText(ctx, "White list prefissi — in arrivo", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                com.ifs.stoppai.core.UpgradeDialog.show(ctx, com.ifs.stoppai.core.PlanManager.Feature.WHITELIST_ESTERI)
            }
        }

        // --- Messaggio segreteria ARIA ---
        val txtAriaAttuale = view.findViewById<TextView>(R.id.txt_aria_msg_attuale)
        val tipoMsg = prefs.getString("aria_tipo_messaggio", "base") ?: "base"
        txtAriaAttuale.text = when (tipoMsg) {
            "preset" -> "Messaggio attuale: Voce preregistrata"
            "custom" -> "Messaggio attuale: Personalizzato"
            else -> "Messaggio attuale: Standard"
        }

        view.findViewById<Button>(R.id.btn_configura_aria_msg).setOnClickListener {
            val bs = AriaConfigBottomSheet()
            bs.onSaveListener = { label ->
                txtAriaAttuale.text = "Messaggio attuale: $label"
            }
            bs.show(parentFragmentManager, "aria_config_bs")
        }
    }

    private fun mostraSceltaMessaggioAria(txtAttuale: TextView) {
        val ctx = requireContext()
        val opzioni = arrayOf(
            "Messaggio standard",
            "🔒 Voce preregistrata (8 preset) — PRO",
            "🔒 Messaggio personale registrato — SHIELD"
        )

        // Se il piano lo consente, togli il lucchetto dalle opzioni
        val proPlan = com.ifs.stoppai.core.PlanManager.isDisponibile(ctx, com.ifs.stoppai.core.PlanManager.Feature.ARIA_PRESET)
        val shieldPlan = com.ifs.stoppai.core.PlanManager.isDisponibile(ctx, com.ifs.stoppai.core.PlanManager.Feature.ARIA_CUSTOM)
        val labels = arrayOf(
            "Messaggio standard",
            if (proPlan) "Voce preregistrata (8 preset)" else "🔒 Voce preregistrata (8 preset) — PRO",
            if (shieldPlan) "Messaggio personale registrato" else "🔒 Messaggio personale registrato — SHIELD"
        )

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Messaggio segreteria ARIA")
            .setItems(labels) { _, which ->
                val prefs = ctx.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                when (which) {
                    0 -> {
                        prefs.edit().putString("aria_tipo_messaggio", "base").apply()
                        txtAttuale.text = "Messaggio attuale: Standard"
                        android.widget.Toast.makeText(ctx, "Messaggio standard impostato", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (proPlan) {
                            mostraListaPreset(txtAttuale)
                        } else {
                            com.ifs.stoppai.core.UpgradeDialog.show(ctx, com.ifs.stoppai.core.PlanManager.Feature.ARIA_PRESET)
                        }
                    }
                    2 -> {
                        if (shieldPlan) {
                            prefs.edit().putString("aria_tipo_messaggio", "custom").apply()
                            txtAttuale.text = "Messaggio attuale: Personalizzato"
                            android.widget.Toast.makeText(ctx, "Messaggio personalizzato selezionato.\nPer registrare, usa la sezione dedicata.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            com.ifs.stoppai.core.UpgradeDialog.show(ctx, com.ifs.stoppai.core.PlanManager.Feature.ARIA_CUSTOM)
                        }
                    }
                }
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    private fun mostraListaPreset(txtAttuale: TextView) {
        val ctx = requireContext()
        val presets = arrayOf(
            "Uomo 1", "Uomo 2", "Uomo 3", "Uomo 4",
            "Donna 1", "Donna 2", "Donna 3", "Donna 4"
        )
        val presetIds = arrayOf(
            "uomo_1", "uomo_2", "uomo_3", "uomo_4",
            "donna_1", "donna_2", "donna_3", "donna_4"
        )

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Scegli voce preset")
            .setItems(presets) { _, which ->
                val prefs = ctx.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("aria_tipo_messaggio", "preset")
                    .putString("aria_preset_id", presetIds[which])
                    .apply()
                txtAttuale.text = "Messaggio attuale: ${presets[which]}"
                android.widget.Toast.makeText(ctx, "${presets[which]} selezionato", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // ===== VOLUME =====
    private fun setupVolumeControl(view: View) {
        val db = com.ifs.stoppai.db.StoppAiDatabase.getInstance(requireContext().applicationContext)
        val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
        val seek = view.findViewById<SeekBar>(R.id.ID_SETT_VOL_SEEK)
        val tvVal = view.findViewById<TextView>(R.id.ID_SETT_VOL_VAL)
        val btnMinus = view.findViewById<TextView>(R.id.ID_SETT_VOL_MINUS)
        val btnPlus = view.findViewById<TextView>(R.id.ID_SETT_VOL_PLUS)

        lifecycleScope.launch(Dispatchers.IO) {
            val vol = repo.getVolumePreferito()
            withContext(Dispatchers.Main) {
                seek.progress = vol
                tvVal.text = "$vol / 15"
            }
        }

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVal.text = "$progress / 15"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 7
                lifecycleScope.launch(Dispatchers.IO) { repo.setVolumePreferito(progress) }
            }
        })

        btnMinus.setOnClickListener {
            val cur = seek.progress
            if (cur > 0) {
                seek.progress = cur - 1
                lifecycleScope.launch(Dispatchers.IO) { repo.setVolumePreferito(cur - 1) }
            }
        }
        btnPlus.setOnClickListener {
            val cur = seek.progress
            if (cur < 15) {
                seek.progress = cur + 1
                lifecycleScope.launch(Dispatchers.IO) { repo.setVolumePreferito(cur + 1) }
            }
        }
    }

    // ===== PERMESSI =====
    override fun onResume() {
        super.onResume()
        view?.let { aggiornaPermessi(it) }
        val filter = android.content.IntentFilter("com.ifs.stoppai.CONTACTS_SYNC_PROGRESS")
        requireContext().registerReceiver(progressReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(progressReceiver)
    }

    private fun setupPermissionClickListeners(view: View) {
        view.findViewById<TextView>(R.id.ID_PERM_001).setOnClickListener {
            openAppSettingsIfMissing(Manifest.permission.READ_CONTACTS)
        }
        view.findViewById<TextView>(R.id.ID_PERM_002).setOnClickListener {
            if (!hasPerm(Manifest.permission.READ_PHONE_STATE) || !hasPerm(Manifest.permission.READ_PHONE_NUMBERS)) {
                openAppSettings()
            }
        }
        view.findViewById<TextView>(R.id.ID_PERM_003).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = requireActivity().getSystemService(RoleManager::class.java)
                if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    startActivityForResult(intent, 200)
                }
            } else {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }
        view.findViewById<TextView>(R.id.ID_PERM_004).setOnClickListener {
            openAppSettingsIfMissing(Manifest.permission.CALL_PHONE)
        }
        view.findViewById<TextView>(R.id.ID_PERM_005).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && !hasPerm(Manifest.permission.POST_NOTIFICATIONS)) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                startActivity(intent)
            }
        }
        view.findViewById<TextView>(R.id.ID_PERM_007).setOnClickListener {
            openAppSettingsIfMissing(Manifest.permission.SEND_SMS)
        }
    }

    private fun openAppSettingsIfMissing(permission: String) {
        if (!hasPerm(permission)) openAppSettings()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${requireActivity().packageName}")
        startActivity(intent)
    }

    private fun hasPerm(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun aggiornaPermessi(view: View) {
        val ok = android.graphics.Color.parseColor("#333333")
        val ko = android.graphics.Color.parseColor("#CC0000")

        val t1 = view.findViewById<TextView>(R.id.ID_PERM_001)
        val p1 = hasPerm(Manifest.permission.READ_CONTACTS)
        t1.text = "${if (p1) "🟢" else "🔴"} Accesso rubrica"
        t1.setTextColor(if (p1) ok else ko)

        val t2 = view.findViewById<TextView>(R.id.ID_PERM_002)
        val p2 = hasPerm(Manifest.permission.READ_PHONE_STATE) && hasPerm(Manifest.permission.READ_PHONE_NUMBERS)
        t2.text = "${if (p2) "🟢" else "🔴"} Stato telefono"
        t2.setTextColor(if (p2) ok else ko)

        val t3 = view.findViewById<TextView>(R.id.ID_PERM_003)
        val isScreeningActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireActivity().getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            val telecomManager = requireActivity().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.defaultDialerPackage == requireActivity().packageName
        }
        t3.text = "${if (isScreeningActive) "🟢" else "🔴"} App verifica chiamate"
        t3.setTextColor(if (isScreeningActive) ok else ko)

        val t4 = view.findViewById<TextView>(R.id.ID_PERM_004)
        val p4 = hasPerm(Manifest.permission.CALL_PHONE)
        t4.text = "${if (p4) "🟢" else "🔴"} Permesso telefonate"
        t4.setTextColor(if (p4) ok else ko)

        val t5 = view.findViewById<TextView>(R.id.ID_PERM_005)
        val p5 = Build.VERSION.SDK_INT < 33 || hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        t5.text = "${if (p5) "🟢" else "🔴"} Permesso notifiche"
        t5.setTextColor(if (p5) ok else ko)

        val t7 = view.findViewById<TextView>(R.id.ID_PERM_007)
        val p7 = hasPerm(Manifest.permission.SEND_SMS)
        t7.text = "${if (p7) "🟢" else "🔴"} Invio SMS"
        t7.setTextColor(if (p7) ok else ko)

        val t6 = view.findViewById<TextView>(R.id.ID_PERM_006)
        if (t6 != null) {
            val cacheSize = com.ifs.stoppai.core.ContactCacheManager.getSize()
            val rubricaOk = cacheSize > 0
            t6.text = if (rubricaOk) "🟢 Rubrica caricata ($cacheSize)" else "🔴 Rubrica in caricamento..."
            t6.setTextColor(if (rubricaOk) ok else ko)
        }
    }

    private fun updateProgressUI(view: View, current: Int, total: Int) {
        val t6 = view.findViewById<TextView>(R.id.ID_PERM_006)
        val pb = view.findViewById<android.widget.ProgressBar>(R.id.ID_SETT_RUB_PROGRESS)
        if (current < total) {
            pb.visibility = View.VISIBLE
            pb.max = total
            pb.progress = current
            t6.text = "🔴 Caricamento rubrica: $current / $total"
            t6.setTextColor(android.graphics.Color.parseColor("#CC0000"))
        } else {
            pb.visibility = View.GONE
            t6.text = "🟢 Rubrica: $total contatti"
            t6.setTextColor(android.graphics.Color.parseColor("#333333"))
        }
    }
}
