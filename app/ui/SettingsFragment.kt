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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ifs.stoppai.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.ID_HOME_005).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:**61*0421633844*11*15%23")
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.ID_HOME_006).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.fromParts("tel", "##61#", null)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.ID_SETT_001).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Ripristina impostazioni")
                .setMessage("Sei sicuro? Tutte le impostazioni verranno cancellate.")
                .setPositiveButton("Ripristina") { _, _ ->
                    val db = com.ifs.stoppai.db.StoppAiDatabase.getInstance(requireContext().applicationContext)
                    val repo = com.ifs.stoppai.db.AppSettingsRepository(db.appSettingsDao())
                    repo.clearAll()
                    val audioManager = requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 7, 0)
                    android.widget.Toast.makeText(requireContext(), "Impostazioni ripristinate", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
        view.findViewById<Button>(R.id.ID_SETT_002).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Svuota registro")
                .setMessage("Sei sicuro? Questa azione non è reversibile.")
                .setPositiveButton("Svuota") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        com.ifs.stoppai.db.StoppAiDatabase.getInstance(requireContext().applicationContext).clearAllTables()
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(requireContext(), "Registro svuotato", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
        setupPermissionClickListeners(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { aggiornaPermessi(it) }
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
            val roleManager = requireActivity().getSystemService(android.app.role.RoleManager::class.java)
            if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
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
        val t1 = view.findViewById<TextView>(R.id.ID_PERM_001)
        val p1 = hasPerm(Manifest.permission.READ_CONTACTS)
        t1.text = "${if (p1) "🟢" else "🔴"} Accesso rubrica"

        val t2 = view.findViewById<TextView>(R.id.ID_PERM_002)
        val p2 = hasPerm(Manifest.permission.READ_PHONE_STATE) && hasPerm(Manifest.permission.READ_PHONE_NUMBERS)
        t2.text = "${if (p2) "🟢" else "🔴"} Stato telefono"

        val t3 = view.findViewById<TextView>(R.id.ID_PERM_003)
        val roleManager = requireActivity().getSystemService(android.app.role.RoleManager::class.java)
        val p3 = roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)
        t3.text = "${if (p3) "🟢" else "🔴"} App verifica chiamate"

        val t4 = view.findViewById<TextView>(R.id.ID_PERM_004)
        val p4 = hasPerm(Manifest.permission.CALL_PHONE)
        t4.text = "${if (p4) "🟢" else "🔴"} Permesso telefonate"

        val t5 = view.findViewById<TextView>(R.id.ID_PERM_005)
        val p5 = Build.VERSION.SDK_INT < 33 || hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        t5.text = "${if (p5) "🟢" else "🔴"} Permesso notifiche"
        
        val t6 = view.findViewById<TextView>(R.id.ID_PERM_006)
        if (t6 != null) {
            val cacheSize = com.ifs.stoppai.core.ContactCacheManager.getSize()
            val rubricaOk = cacheSize > 0
            t6.text = if (rubricaOk) "🟢 Rubrica caricata ($cacheSize)" else "🔴 Rubrica in caricamento..."
        }
    }
}
