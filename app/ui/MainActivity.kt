// FILE: MainActivity.kt
// SCOPO: Gestione UI dashboard, permessi ed attivazione (Versione v0.5)
// DIPENDENZE: DB, CallLogAdapter.kt
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.ifs.stoppai.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val volAttuale = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
        if (volAttuale > 0) {
            prefs.edit().putInt("vol_originale", volAttuale).apply()
        }
        if (prefs.getInt("vol_originale", 0) == 0) {
            prefs.edit().putInt("vol_originale", 5).apply()
        }

        val switchBase = findViewById<Switch>(R.id.ID_HOME_001)
        switchBase.isChecked = prefs.getBoolean("protezione_base", false)
        switchBase.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("protezione_base", isChecked).apply()
            if (isChecked) {
                val volAdesso = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
                if (volAdesso > 0) {
                    prefs.edit().putInt("vol_originale", volAdesso).apply()
                }
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0) } catch(e:Exception){}
                android.util.Log.e("STOPPAI_VOL", "Protezione ON - volume -> 0")
            } else {
                val volOr = prefs.getInt("vol_originale", 5)
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, volOr, 0) } catch(e:Exception){}
                android.util.Log.e("STOPPAI_VOL", "Protezione OFF - volume -> $volOr")
            }
        }

        val switchTotale = findViewById<Switch>(R.id.ID_HOME_007)
        switchTotale.isChecked = prefs.getBoolean("protezione_totale", false)
        switchTotale.setOnCheckedChangeListener { _, isChecked ->
            val edit = prefs.edit()
            edit.putBoolean("protezione_totale", isChecked)
            if (isChecked) {
                edit.putLong("protezione_totale_scadenza", System.currentTimeMillis() + 60 * 60 * 1000)
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0) } catch(e:Exception){}
            } else {
                val volOr = prefs.getInt("vol_originale", 5)
                try { audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, volOr, 0) } catch(e:Exception){}
            }
            edit.apply()
        }

        // BTN SVUOTA REGISTRO
        val btnSvuota = findViewById<Button>(R.id.ID_HOME_004)
        btnSvuota.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Svuota registro")
                .setMessage("Sei sicuro? Questa azione non è reversibile.")
                .setPositiveButton("Svuota") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        StoppAiDatabase.getDatabase(applicationContext).clearAllTables()
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        val btnAttivaSegreteria = findViewById<Button>(R.id.ID_HOME_005)
        btnAttivaSegreteria.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:**61*0421633844*11*15%23")
            startActivity(intent)
        }

        val btnDisattivaSegreteria = findViewById<Button>(R.id.ID_HOME_006)
        btnDisattivaSegreteria.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.fromParts("tel", "##61#", null)
            startActivity(intent)
        }

        // RecyclerView Chiamate Bloccate
        val recycler = findViewById<RecyclerView>(R.id.ID_HOME_002)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = CallLogAdapter()
        recycler.adapter = adapter

        lifecycleScope.launch {
            val db = StoppAiDatabase.getDatabase(this@MainActivity)
            db.callLogDao().getAllLogs().collectLatest { logs ->
                adapter.submitList(logs)
            }
        }
        
        val labelFooter = findViewById<TextView>(R.id.ID_HOME_099)
        labelFooter.text = "StoppAI v1.6 — Switch Volume"
        
        setupPermissionClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val volOriginale = prefs.getInt("vol_originale", 5)
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, volOriginale, 0)
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        aggiornaPermessi()
        
        if (hasPerm(Manifest.permission.READ_CONTACTS)) {
            com.ifs.stoppai.core.ContactCacheManager.loadContactsSync(this)
            com.ifs.stoppai.core.ContactCacheManager.startSync(this)
        }
    }

    private fun setupPermissionClickListeners() {
        findViewById<TextView>(R.id.ID_PERM_001).setOnClickListener { 
            openAppSettingsIfMissing(Manifest.permission.READ_CONTACTS) 
        }
        findViewById<TextView>(R.id.ID_PERM_002).setOnClickListener { 
            if (!hasPerm(Manifest.permission.READ_PHONE_STATE) || !hasPerm(Manifest.permission.READ_PHONE_NUMBERS)) {
                openAppSettings()
            }
        }
        findViewById<TextView>(R.id.ID_PERM_003).setOnClickListener {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivity(intent)
            }
        }
        findViewById<TextView>(R.id.ID_PERM_004).setOnClickListener { 
            openAppSettingsIfMissing(Manifest.permission.CALL_PHONE) 
        }
        findViewById<TextView>(R.id.ID_PERM_005).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && !hasPerm(Manifest.permission.POST_NOTIFICATIONS)) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            }
        }
    }

    private fun openAppSettingsIfMissing(permission: String) {
        if (!hasPerm(permission)) openAppSettings()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun hasPerm(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun aggiornaPermessi() {
        val t1 = findViewById<TextView>(R.id.ID_PERM_001)
        val p1 = hasPerm(Manifest.permission.READ_CONTACTS)
        t1.text = "${if (p1) "🟢" else "🔴"} Accesso rubrica"

        val t2 = findViewById<TextView>(R.id.ID_PERM_002)
        val p2 = hasPerm(Manifest.permission.READ_PHONE_STATE) && hasPerm(Manifest.permission.READ_PHONE_NUMBERS)
        t2.text = "${if (p2) "🟢" else "🔴"} Stato telefono"

        val t3 = findViewById<TextView>(R.id.ID_PERM_003)
        val roleManager = getSystemService(android.app.role.RoleManager::class.java)
        val p3 = roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)
        t3.text = "${if (p3) "🟢" else "🔴"} App verifica chiamate"

        val t4 = findViewById<TextView>(R.id.ID_PERM_004)
        val p4 = hasPerm(Manifest.permission.CALL_PHONE)
        t4.text = "${if (p4) "🟢" else "🔴"} Permesso telefonate"

        val t5 = findViewById<TextView>(R.id.ID_PERM_005)
        val p5 = Build.VERSION.SDK_INT < 33 || hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        t5.text = "${if (p5) "🟢" else "🔴"} Permesso notifiche"
        
        val t6 = findViewById<TextView>(R.id.ID_PERM_006)
        if (t6 != null) {
            val cacheSize = com.ifs.stoppai.core.ContactCacheManager.getSize()
            val rubricaOk = cacheSize > 0
            t6.text = if (rubricaOk) "🟢 Rubrica caricata ($cacheSize)" else "🔴 Rubrica in caricamento..."
        }
    }

    private fun checkPermissions() {
        val req = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= 33) {
            req.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = req.filter { !hasPerm(it) }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }
}
