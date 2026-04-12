// FILE: MainActivity.kt
// SCOPO: Gestione UI dashboard, permessi ed attivazione (Versione v9.2)
// DIPENDENZE: DB, CallLogAdapter.kt, Firebase
// ULTIMA MODIFICA: 2026-03-29

package com.ifs.stoppai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.media.RingtoneManager
import android.media.AudioManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ifs.stoppai.R
import com.ifs.stoppai.core.AriaFcmService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TASK-SA-125: al primo avvio assoluto lancia il wizard onboarding
        val wizardPrefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        if (!wizardPrefs.getBoolean("wizard_completed", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        checkPermissions()

        // Recupero Token FCM per notifiche ARIA
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.e("STOPPAI_FCM", "FCM Token: $token")
                inviaTokenAlServerManuale(token)
            }
        }

        // Sync piano dal backend (l'admin può cambiarlo dal CRM)
        sincronizzaPianoDalBackend()

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
                    true
                }
                R.id.nav_referral -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, InvitaFragment()).commit()
                    true
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SettingsFragment()).commit()
                    true
                }
                R.id.nav_info -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, InfoFragment()).commit()
                    true
                }
                R.id.nav_chat -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, ChatFragment()).commit()
                    true
                }
                else -> false
            }
        }
        
        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
            val loggedIn = prefs.getBoolean("logged_in", false)
            val skipped = prefs.getBoolean("login_skipped", false)
            if (!loggedIn && !skipped) {
                bottomNav.visibility = android.view.View.GONE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
            } else {
                bottomNav.selectedItemId = R.id.nav_home
            }
        }

        checkFirstLaunch()
    }

    /**
     * Forza l'invio del token FCM al server Hetzner all'avvio
     */
    private fun sincronizzaPianoDalBackend() {
        val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val email = prefs.getString("tester_email", "") ?: ""
        if (email.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = java.net.URL("${AriaFcmService.SERVER_URL}/api/tester/piano/${java.net.URLEncoder.encode(email, "UTF-8")}")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(body)
                    val pianoServer = json.optString("piano", "free")
                    val isAdmin = json.optBoolean("is_admin", false)
                    val pianoLocale = prefs.getString("tester_piano", "free")

                    // Salva flag admin
                    prefs.edit().putBoolean("is_admin", isAdmin).apply()

                    if (pianoServer != pianoLocale) {
                        prefs.edit().putString("tester_piano", pianoServer).apply()
                        android.util.Log.d("STOPPAI_SYNC", "Piano aggiornato dal server: $pianoLocale → $pianoServer")
                        // Se upgrade a PRO, salva timestamp per conteggio 5gg verso SHIELD
                        if (pianoServer == "pro" && pianoLocale != "pro") {
                            prefs.edit().putLong("pro_activation_timestamp", System.currentTimeMillis()).apply()
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_SYNC", "Errore sync piano: ${e.message}")
            }
        }
    }

    private fun inviaTokenAlServerManuale(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                val email = prefs.getString("tester_email", "") ?: ""
                val testerId = prefs.getInt("tester_id", 0)

                val url = URL("${AriaFcmService.SERVER_URL}/api/tester/fcm-token")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val body = """{"token":"$token","email":"$email","tester_id":$testerId}"""
                conn.outputStream.write(body.toByteArray())
                android.util.Log.d("STOPPAI_FCM", "FCM: Token inviato (${conn.responseCode}) email=$email id=$testerId")
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_FCM", "FCM ERROR: Invio fallito: ${e.message}")
            }
        }
    }

    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean("first_launch", true)
        if (!isFirst) return

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
            val target = (max * 0.75).toInt().coerceAtLeast(1)
            audio.setStreamVolume(AudioManager.STREAM_RING, target, 0)
        } catch (e: Exception) {}

        setStoppAiRingtone()

        AlertDialog.Builder(this)
            .setTitle("StoppAI è attivo")
            .setMessage("Per sicurezza abbiamo impostato la suoneria al 75%.\n\nStoppAI gestirà le notifiche delle chiamate in tempo reale.")
            .setPositiveButton("HO CAPITO") { _, _ ->
                prefs.edit().putBoolean("first_launch", false).apply()
            }
            .setCancelable(false)
            .show()
    }

    private fun setStoppAiRingtone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            return
        }
        try {
            val uri = Uri.parse("android.resource://$packageName/${R.raw.stoppai_ring}")
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, uri)
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
