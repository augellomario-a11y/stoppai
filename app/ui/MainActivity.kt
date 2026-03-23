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
import android.media.RingtoneManager
import android.media.AudioManager
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

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
                    true
                }
                R.id.nav_calls -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, CallsFragment()).commit()
                    true
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SettingsFragment()).commit()
                    true
                }
                R.id.nav_help -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HelpFragment()).commit()
                    true
                }
                else -> false
            }
        }
        
        // Mostra HomeFragment all'avvio
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }

        checkFirstLaunch()
    }

    // Parte 2 e 3: Primo avvio, reset volumi e suoneria
    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean("first_launch", true)
        android.util.Log.e("STOPPAI_DEBUG", "checkFirstLaunch: isFirst = $isFirst")
        if (!isFirst) return

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Reset volumi al 75%
        fun set75(stream: Int, name: String) {
            try {
                val max = audio.getStreamMaxVolume(stream)
                val target = (max * 0.75).toInt().coerceAtLeast(1)
                audio.setStreamVolume(stream, target, 0)
                android.util.Log.e("STOPPAI_DEBUG", "Set $name max=$max target=$target")
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_DEBUG", "Errore $name: ${e.message}")
            }
        }

        set75(AudioManager.STREAM_RING, "RING")

        // Se abbiamo il permesso, impostiamo la suoneria
        android.util.Log.e("STOPPAI_DEBUG", "Calling setStoppAiRingtone")
        setStoppAiRingtone()

        // Mostra popup informativo
        AlertDialog.Builder(this)
            .setTitle("StoppAI è attivo")
            .setMessage("Per sicurezza abbiamo impostato tutti i volumi al 75% così non perdi notifiche importanti.\n\nPuoi modificarli quando vuoi dalle impostazioni del telefono.\nStoppAI gestirà solo la suoneria delle chiamate.")
            .setPositiveButton("HO CAPITO") { _, _ ->
                android.util.Log.e("STOPPAI_DEBUG", "HO CAPITO Clicked")
                prefs.edit().putBoolean("first_launch", false).apply()
            }
            .setCancelable(false)
            .show()
    }

    private fun setStoppAiRingtone() {
        // Su Android 6+ serve permesso speciale per scrivere settings di sistema
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            // Chiedi il permesso all'utente (opzionale o automatico se possibile)
            // Per ora proviamo a settarla, se fallisce amen.
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            return
        }
        
        try {
            val uri = Uri.parse("android.resource://$packageName/${R.raw.stoppai_ring}")
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, uri)
        } catch (e: Exception) {
             android.util.Log.e("STOPPAI", "Errore setting ringtone: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (hasPerm(Manifest.permission.READ_CONTACTS)) {
            com.ifs.stoppai.core.ContactCacheManager.loadContactsSync(this)
            com.ifs.stoppai.core.ContactCacheManager.startSync(this)
        }
    }

    private fun hasPerm(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
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
