// FILE: OnboardingActivity.kt
// SCOPO: Activity host del wizard onboarding (TASK-SA-125)
// USA: OnboardingViewModel, OnboardingStepFragment
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.ifs.stoppai.R
import com.ifs.stoppai.core.OnboardingViewModel

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    // Launcher permessi runtime
    private val contactsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.goTo(OnboardingViewModel.Step.CONTACTS_LOADING)
        else refreshCurrentStep()
    }

    private val phoneStateLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> viewModel.next() }

    private val callPhoneLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> viewModel.next() }

    private val smsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> viewModel.next() }

    // Launcher per ruolo Call Screening (RoleManager)
    private val callScreeningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Refresh dello stato indipendentemente dal result code
        refreshCurrentStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewModel.currentStep.observe(this) { step ->
            updateProgressIndicator(step)
            showStepFragment()
        }

        // Mostra il primo step
        if (savedInstanceState == null) {
            showStepFragment()
        }
    }

    private fun showStepFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.onboarding_container, OnboardingStepFragment())
            .commitAllowingStateLoss()
    }

    private fun updateProgressIndicator(step: OnboardingViewModel.Step) {
        val label = findViewById<TextView>(R.id.txt_progress_label)
        val bar = findViewById<LinearProgressIndicator>(R.id.progress_bar)
        if (step == OnboardingViewModel.Step.WELCOME || step == OnboardingViewModel.Step.CONTACTS_LOADING) {
            label.text = if (step == OnboardingViewModel.Step.WELCOME) "Inizio" else "Caricamento..."
            bar.progress = 0
        } else {
            val current = viewModel.stepIndex(step)
            val total = viewModel.totalVisibleSteps()
            label.text = "Passo $current di $total"
            bar.max = total
            bar.progress = current
        }
    }

    private fun refreshCurrentStep() {
        showStepFragment()
    }

    // ============================================
    // METODI CHIAMATI DAL FRAGMENT
    // ============================================

    fun requestContacts() {
        contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    fun requestPhoneState() {
        val perms = mutableListOf(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 26) perms.add(Manifest.permission.READ_PHONE_NUMBERS)
        phoneStateLauncher.launch(perms.toTypedArray())
    }

    fun requestCallPhone() {
        callPhoneLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    fun requestCallScreeningRole() {
        val intent = viewModel.callScreeningRoleIntent(this)
        if (intent != null) {
            try {
                callScreeningLauncher.launch(intent)
            } catch (e: Exception) {
                // Fallback: apri impostazioni app
                startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }
    }

    fun requestSms() {
        smsLauncher.launch(Manifest.permission.SEND_SMS)
    }

    fun finishWizard() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onBackPressed() {
        // Disabilita back fisico per evitare uscite accidentali a meta' wizard
    }
}
