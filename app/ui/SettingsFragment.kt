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
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.telephony.TelephonyManager

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val progressReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val current = intent?.getIntExtra("current", 0) ?: 0
            val total = intent?.getIntExtra("total", 0) ?: 0
            view?.let { updateProgressUI(it, current, total) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.ID_HOME_006).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.fromParts("tel", "##61#", null)
            startActivity(intent)
            
            // AGGIORNA STATO (SA-068)
            val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("segreteria_attiva", false).apply()
            android.widget.Toast.makeText(requireContext(), "Segreteria disattivata", android.widget.Toast.LENGTH_SHORT).show()
        }

        setupPermissionClickListeners(view)
        setupVolumeControl(view)
        setupUssdConfig(view)

        // Ripristina valori default (SA-067)
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
                        repo.setVolumePreferito(11) // circa 75% di 15
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
                lifecycleScope.launch(Dispatchers.IO) {
                    repo.setVolumePreferito(progress)
                }
            }
        })

        btnMinus.setOnClickListener {
            val cur = seek.progress
            if (cur > 0) {
                seek.progress = cur - 1
                lifecycleScope.launch(Dispatchers.IO) {
                    repo.setVolumePreferito(cur - 1)
                }
            }
        }

        btnPlus.setOnClickListener {
            val cur = seek.progress
            if (cur < 15) {
                seek.progress = cur + 1
                lifecycleScope.launch(Dispatchers.IO) {
                    repo.setVolumePreferito(cur + 1)
                }
            }
        }
    }

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
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivity(intent)
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
        val red = android.graphics.Color.RED
        val black = android.graphics.Color.BLACK

        val t1 = view.findViewById<TextView>(R.id.ID_PERM_001)
        val p1 = hasPerm(Manifest.permission.READ_CONTACTS)
        t1.text = "${if (p1) "🟢" else "🔴"} Accesso rubrica"
        t1.setTextColor(if (p1) black else red)

        val t2 = view.findViewById<TextView>(R.id.ID_PERM_002)
        val p2 = hasPerm(Manifest.permission.READ_PHONE_STATE) && hasPerm(Manifest.permission.READ_PHONE_NUMBERS)
        t2.text = "${if (p2) "🟢" else "🔴"} Stato telefono"
        t2.setTextColor(if (p2) black else red)

        val t3 = view.findViewById<TextView>(R.id.ID_PERM_003)
        val isScreeningActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireActivity().getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            val telecomManager = requireActivity().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.defaultDialerPackage == requireActivity().packageName
        }
        t3.text = "${if (isScreeningActive) "🟢" else "🔴"} App verifica chiamate"
        t3.setTextColor(if (isScreeningActive) black else red)

        val t4 = view.findViewById<TextView>(R.id.ID_PERM_004)
        val p4 = hasPerm(Manifest.permission.CALL_PHONE)
        t4.text = "${if (p4) "🟢" else "🔴"} Permesso telefonate"
        t4.setTextColor(if (p4) black else red)

        val t5 = view.findViewById<TextView>(R.id.ID_PERM_005)
        val p5 = Build.VERSION.SDK_INT < 33 || hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        t5.text = "${if (p5) "🟢" else "🔴"} Permesso notifiche"
        t5.setTextColor(if (p5) black else red)

        val t7 = view.findViewById<TextView>(R.id.ID_PERM_007)
        val p7 = hasPerm(Manifest.permission.SEND_SMS)
        t7.text = "${if (p7) "🟢" else "🔴"} Invio SMS"
        t7.setTextColor(if (p7) black else red)
        
        val t6 = view.findViewById<TextView>(R.id.ID_PERM_006)
        if (t6 != null) {
            val cacheSize = com.ifs.stoppai.core.ContactCacheManager.getSize()
            val rubricaOk = cacheSize > 0
            t6.text = if (rubricaOk) "🟢 Rubrica caricata ($cacheSize)" else "🔴 Rubrica in caricamento..."
            t6.setTextColor(if (rubricaOk) black else red)
        }
    }

    private fun updateProgressUI(view: View, current: Int, total: Int) {
        val t6 = view.findViewById<TextView>(R.id.ID_PERM_006)
        // ProgressBar nel layout per visualizzare l'avanzamento sincronizzazione
        val pb = view.findViewById<android.widget.ProgressBar>(R.id.ID_SETT_RUB_PROGRESS)
        
        if (current < total) {
            pb.visibility = View.VISIBLE
            pb.max = total
            pb.progress = current
            t6.text = "🔴 Caricamento rubrica: $current / $total"
            t6.setTextColor(android.graphics.Color.RED)
        } else {
            pb.visibility = View.GONE
            t6.text = "🟢 Rubrica: $total contatti"
            t6.setTextColor(android.graphics.Color.BLACK)
        }
    }

    private fun setupUssdConfig(view: View) {
        val tm = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val tvOperatore = view.findViewById<TextView>(R.id.ID_SETT_OPERATORE)
        val operatore = tm.networkOperatorName ?: "Non rilevato"
        tvOperatore.text = "Operatore rilevato: $operatore"

        val group = view.findViewById<android.widget.RadioGroup>(R.id.ID_SETT_SEC_GROUP)
        val btnConfig = view.findViewById<Button>(R.id.ID_SETT_BTN_CONFIG_USSD)
        
        btnConfig.setOnClickListener {
            val sec = when (group.checkedRadioButtonId) {
                R.id.ID_SETT_SEC_5 -> 5
                R.id.ID_SETT_SEC_10 -> 10
                R.id.ID_SETT_SEC_20 -> 20
                else -> 15
            }
            // Salva preferenza secondi
            val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("secondi_deviazione", sec)
                .putBoolean("segreteria_attiva", true)
                .apply()
            
            // Genera codice USSD (Standard italiano per deviazione su mancata risposta)
            // Codice segreteria ARIA: 04211898065
            val ussdCode = "*61*04211898065**$sec#"
            
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${Uri.encode(ussdCode)}")
                startActivity(intent)
                android.widget.Toast.makeText(requireContext(), "Codice generato: $ussdCode", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Errore apertura dialer", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
