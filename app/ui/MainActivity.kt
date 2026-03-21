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
