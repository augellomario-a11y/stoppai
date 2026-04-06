// FILE: OnboardingViewModel.kt
// SCOPO: Stato del wizard onboarding + configurazione step
// USATO DA: OnboardingActivity, OnboardingStepFragment
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.core

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    /**
     * I 9 step del wizard. Lo step 1B (caricamento rubrica) e' annidato dopo CONTACTS.
     */
    enum class Step {
        WELCOME,           // 0
        CONTACTS,          // 1 - READ_CONTACTS
        CONTACTS_LOADING,  // 1B - progress caricamento
        PHONE_STATE,       // 2 - READ_PHONE_STATE + READ_PHONE_NUMBERS
        CALL_SCREENING,    // 3 - app verifica chiamate (CRITICO)
        CALL_PHONE,        // 4 - CALL_PHONE
        NOTIFICATIONS,     // 5 - POST_NOTIFICATIONS
        SMS,               // 6 - SEND_SMS
        BATTERY,           // 7 - ignore battery optimizations
        SUMMARY            // 8 - tutto pronto
    }

    private val _currentStep = MutableLiveData(Step.WELCOME)
    val currentStep: LiveData<Step> = _currentStep

    private val _contactsLoadProgress = MutableLiveData(0 to 0)
    val contactsLoadProgress: LiveData<Pair<Int, Int>> = _contactsLoadProgress

    val deviceFamily: DeviceGuideHelper.DeviceFamily = DeviceGuideHelper.detect()
    val deviceName: String = DeviceGuideHelper.marketingName()

    /**
     * Avanza allo step successivo. Salta CONTACTS_LOADING se non triggerato manualmente.
     */
    fun next() {
        val current = _currentStep.value ?: Step.WELCOME
        val ordered = orderedSteps()
        val idx = ordered.indexOf(current)
        if (idx >= 0 && idx < ordered.size - 1) {
            _currentStep.value = ordered[idx + 1]
        }
    }

    fun goTo(step: Step) {
        _currentStep.value = step
    }

    fun stepIndex(step: Step): Int {
        // Numerazione "Passo X di N" per l'utente: salta WELCOME e CONTACTS_LOADING
        val visible = orderedSteps().filter { it != Step.WELCOME && it != Step.CONTACTS_LOADING }
        return visible.indexOf(step) + 1
    }

    fun totalVisibleSteps(): Int {
        return orderedSteps().count { it != Step.WELCOME && it != Step.CONTACTS_LOADING }
    }

    fun orderedSteps(): List<Step> = listOf(
        Step.WELCOME,
        Step.CONTACTS,
        Step.CONTACTS_LOADING,
        Step.PHONE_STATE,
        Step.CALL_SCREENING,
        Step.CALL_PHONE,
        Step.NOTIFICATIONS,
        Step.SMS,
        Step.BATTERY,
        Step.SUMMARY
    )

    fun updateContactsProgress(current: Int, total: Int) {
        _contactsLoadProgress.postValue(current to total)
    }

    /**
     * Verifica se il permesso/condizione associato allo step e' soddisfatto.
     */
    fun isStepGranted(step: Step): Boolean {
        val ctx = getApplication<Application>().applicationContext
        return when (step) {
            Step.WELCOME -> true
            Step.CONTACTS -> hasPermission(ctx, Manifest.permission.READ_CONTACTS)
            Step.CONTACTS_LOADING -> ContactCacheManager.getSize() > 0
            // Allineato a SettingsFragment: richiede ENTRAMBI i permessi
            Step.PHONE_STATE -> hasPermission(ctx, Manifest.permission.READ_PHONE_STATE) &&
                                hasPermission(ctx, Manifest.permission.READ_PHONE_NUMBERS)
            Step.CALL_SCREENING -> isDefaultCallScreeningApp(ctx)
            Step.CALL_PHONE -> hasPermission(ctx, Manifest.permission.CALL_PHONE)
            Step.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= 33)
                    hasPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                else true
            }
            Step.SMS -> hasPermission(ctx, Manifest.permission.SEND_SMS)
            Step.BATTERY -> isIgnoringBatteryOptimizations(ctx)
            Step.SUMMARY -> true
        }
    }

    /**
     * Mappa step -> stato (rosso/verde) per il riepilogo finale.
     */
    fun summarySteps(): List<Pair<Step, Boolean>> {
        return orderedSteps()
            .filter { it != Step.WELCOME && it != Step.CONTACTS_LOADING && it != Step.SUMMARY }
            .map { it to isStepGranted(it) }
    }

    fun completeWizard() {
        val prefs = getApplication<Application>().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("wizard_completed", true).apply()
    }

    private fun hasPermission(ctx: Context, perm: String): Boolean {
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica se StoppAI e' la default Call Screening App.
     * Usa il metodo ufficiale RoleManager (Android 10+) — stesso usato da SettingsFragment.
     */
    private fun isDefaultCallScreeningApp(ctx: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = ctx.getSystemService(RoleManager::class.java)
                rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
            } else {
                // Fallback per Android <10: verifica default dialer
                val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                tm?.defaultDialerPackage == ctx.packageName
            }
        } catch (e: Exception) { false }
    }

    private fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        return try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
        } catch (e: Exception) { false }
    }

    /**
     * Intent per aprire le impostazioni di sistema relative allo step (se applicabile).
     * NOTE: per CALL_SCREENING usare requestCallScreeningRole() dall'Activity, non un Intent.
     */
    fun intentForStep(step: Step): Intent? {
        val ctx = getApplication<Application>().applicationContext
        return when (step) {
            Step.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            }
            Step.BATTERY -> {
                // Popup nativo Android che aggiunge l'app alla whitelist standard.
                // Richiede il permesso REQUEST_IGNORE_BATTERY_OPTIMIZATIONS nel manifest.
                // Funziona su tutte le marche (Samsung "Senza restrizioni" NON tocca questa flag).
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                }
            }
            else -> null
        }
    }

    /**
     * Costruisce l'Intent RoleManager per richiedere il ruolo Call Screening.
     * Va lanciato dall'Activity con startActivityForResult.
     */
    fun callScreeningRoleIntent(ctx: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val rm = ctx.getSystemService(RoleManager::class.java)
                rm?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            } catch (e: Exception) { null }
        } else {
            // Fallback Android <10
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
    }
}
