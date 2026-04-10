// FILE: OnboardingStepFragment.kt
// SCOPO: Fragment riutilizzabile parametrizzato per ogni step del wizard
// USATO DA: OnboardingActivity
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.ifs.stoppai.R
import com.ifs.stoppai.core.ContactCacheManager
import com.ifs.stoppai.core.DeviceGuideHelper
import com.ifs.stoppai.core.OnboardingViewModel

class OnboardingStepFragment : Fragment() {

    private val viewModel: OnboardingViewModel by activityViewModels()
    private var contactsReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_onboarding_step, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val step = viewModel.currentStep.value ?: OnboardingViewModel.Step.WELCOME
        renderStep(view, step)
    }

    override fun onResume() {
        super.onResume()
        // Al ritorno da intent Android, ricontrolla lo stato del permesso
        val v = view ?: return
        val step = viewModel.currentStep.value ?: return
        updateStatusIcon(v, step)
    }

    private fun renderStep(v: View, step: OnboardingViewModel.Step) {
        val icon = v.findViewById<TextView>(R.id.txt_step_icon)
        val title = v.findViewById<TextView>(R.id.txt_step_title)
        val body = v.findViewById<TextView>(R.id.txt_step_body)
        val device = v.findViewById<TextView>(R.id.txt_device_detected)
        val warning = v.findViewById<TextView>(R.id.txt_step_warning)
        val loading = v.findViewById<LinearLayout>(R.id.layout_loading_progress)
        val btnPrimary = v.findViewById<Button>(R.id.btn_primary)
        val btnSecondary = v.findViewById<Button>(R.id.btn_secondary)

        // Reset visibilita'
        device.visibility = View.GONE
        warning.visibility = View.GONE
        loading.visibility = View.GONE
        btnSecondary.visibility = View.GONE

        when (step) {
            OnboardingViewModel.Step.WELCOME -> {
                icon.text = "👋"
                title.text = "Benvenuto in StoppAI"
                body.text = "Ti guidiamo in pochi passi rapidi per proteggere il tuo telefono.\nCi vogliono meno di 2 minuti."
                device.text = "📱 Rilevato: ${viewModel.deviceName}"
                device.visibility = View.VISIBLE
                btnPrimary.text = "Iniziamo →"
                btnPrimary.setOnClickListener { viewModel.next() }
            }
            OnboardingViewModel.Step.CONTACTS -> {
                icon.text = "📒"
                title.text = "Chi conosci passa sempre"
                body.text = "StoppAI usa la tua rubrica per far squillare solo chi conosci.\nNessun dato viene copiato o inviato."
                btnPrimary.text = "Concedi accesso →"
                btnPrimary.setOnClickListener {
                    (activity as? OnboardingActivity)?.requestContacts()
                }
                btnSecondary.text = "Ho già fatto →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener {
                    if (viewModel.isStepGranted(step)) viewModel.goTo(OnboardingViewModel.Step.CONTACTS_LOADING)
                    else viewModel.next()
                }
            }
            OnboardingViewModel.Step.CONTACTS_LOADING -> {
                icon.text = "⏳"
                title.text = "Caricamento rubrica"
                body.text = "Sto leggendo i tuoi contatti..."
                loading.visibility = View.VISIBLE
                btnPrimary.visibility = View.GONE
                startContactsLoad(v)
            }
            OnboardingViewModel.Step.PHONE_STATE -> {
                icon.text = "📞"
                title.text = "Riconosco chi ti chiama"
                body.text = "Questo permesso permette a StoppAI di vedere il numero del chiamante nel momento esatto in cui arriva la chiamata."
                btnPrimary.text = "Concedi accesso →"
                btnPrimary.setOnClickListener { (activity as? OnboardingActivity)?.requestPhoneState() }
                btnSecondary.text = "Ho già fatto →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener { viewModel.next() }
            }
            OnboardingViewModel.Step.CALL_SCREENING -> {
                icon.text = "🛡️"
                title.text = "Il permesso più importante"
                val instructions = DeviceGuideHelper.instructionsFor(
                    DeviceGuideHelper.GuideStep.CALL_SCREENING, viewModel.deviceFamily
                )
                body.text = "Senza questo, StoppAI non può bloccare nessuna chiamata.\n\nAndroid ti chiederà conferma."
                btnPrimary.text = "Imposta StoppAI →"
                btnPrimary.setOnClickListener {
                    (activity as? OnboardingActivity)?.requestCallScreeningRole()
                }
                btnSecondary.text = "Ho già fatto →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener {
                    if (!viewModel.isStepGranted(step)) {
                        warning.text = "⚠️ Senza questo permesso StoppAI non può proteggerti.\n\n$instructions"
                        warning.visibility = View.VISIBLE
                    }
                    viewModel.next()
                }
            }
            OnboardingViewModel.Step.CALL_PHONE -> {
                icon.text = "📲"
                title.text = "Gestisco le chiamate per te"
                body.text = "Questo permesso permette a StoppAI di deviare le chiamate indesiderate senza che il telefono squilli mai."
                btnPrimary.text = "Concedi accesso →"
                btnPrimary.setOnClickListener { (activity as? OnboardingActivity)?.requestCallPhone() }
                btnSecondary.text = "Ho già fatto →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener { viewModel.next() }
            }
            OnboardingViewModel.Step.NOTIFICATIONS -> {
                icon.text = "🔔"
                title.text = "Tienimi informato"
                body.text = "Quando blocco una chiamata ti mando una notifica silenziosa.\nSai sempre cosa è successo, senza essere disturbato."
                btnPrimary.text = "Attiva notifiche →"
                btnPrimary.setOnClickListener {
                    viewModel.intentForStep(step)?.let { startActivity(it) }
                }
                btnSecondary.text = "Salta →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener { viewModel.next() }
            }
            OnboardingViewModel.Step.SMS -> {
                icon.text = "✉️"
                title.text = "Rispondo per te via SMS"
                body.text = "In alcuni casi StoppAI può rispondere automaticamente a numeri sconosciuti con un messaggio.\nFunzione opzionale."
                btnPrimary.text = "Concedi accesso →"
                btnPrimary.setOnClickListener { (activity as? OnboardingActivity)?.requestSms() }
                btnSecondary.text = "Salta →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener { viewModel.next() }
            }
            OnboardingViewModel.Step.BATTERY -> {
                icon.text = "🔋"
                title.text = "Rimani sempre attivo"
                body.text = "Android spegne le app inattive per risparmiare batteria.\n\nAl prossimo tocco comparirà un popup di sistema: rispondi SÌ per permettere a StoppAI di restare sempre attivo."
                btnPrimary.text = "Consenti esecuzione →"
                btnPrimary.setOnClickListener {
                    try {
                        viewModel.intentForStep(step)?.let { startActivity(it) }
                    } catch (e: Exception) {
                        // Fallback: apri la lista manuale
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
                btnSecondary.text = "Ho già fatto →"
                btnSecondary.visibility = View.VISIBLE
                btnSecondary.setOnClickListener { viewModel.next() }
            }
            OnboardingViewModel.Step.ARIA_FORWARDING -> {
                icon.text = "🎙️"
                title.text = "Attiva la segreteria ARIA"
                val alreadyDone = viewModel.isStepGranted(step)
                if (alreadyDone) {
                    body.text = "La segreteria ARIA è attiva.\nLe chiamate sconosciute verranno deviate ad ARIA automaticamente."
                    btnPrimary.text = "✅ Segreteria ARIA attiva — Avanti →"
                    btnPrimary.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2a5a30.toInt())
                    btnPrimary.setTextColor(0xFF9bdf9b.toInt())
                    btnPrimary.setOnClickListener { viewModel.next() }
                    btnSecondary.visibility = View.GONE
                    warning.visibility = View.GONE
                } else {
                    body.text = "ARIA risponde al posto tuo quando non puoi.\n\nTocca il pulsante e attendi 10 secondi.\nFacciamo tutto noi."
                    warning.visibility = View.GONE
                    btnPrimary.text = "Attiva segreteria ARIA →"
                    btnPrimary.setOnClickListener {
                        btnPrimary.isEnabled = false
                        btnPrimary.text = "⏳ Configurazione in corso..."
                        btnPrimary.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1a1a24.toInt())
                        btnPrimary.setTextColor(0xFFc8a96e.toInt())
                        com.ifs.stoppai.core.UssdManager.activateForward(requireContext())
                        // Dopo 12 secondi: mostra completato
                        v.postDelayed({
                            btnPrimary.isEnabled = true
                            btnPrimary.text = "✅ Segreteria ARIA attiva — Avanti →"
                            btnPrimary.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2a5a30.toInt())
                            btnPrimary.setTextColor(0xFF9bdf9b.toInt())
                            btnPrimary.setOnClickListener { viewModel.next() }
                            btnSecondary.visibility = View.GONE
                            warning.visibility = View.GONE
                            updateStatusIcon(v, step)
                        }, 12000)
                    }
                    btnSecondary.visibility = View.GONE
                }
            }
            OnboardingViewModel.Step.SUMMARY -> {
                icon.text = "✅"
                title.text = "Sei protetto."
                body.text = "StoppAI è attivo.\nLe chiamate indesiderate non disturberanno più il tuo telefono."

                // Costruisco la lista permessi allineata a sinistra come righe separate
                val parent = body.parent as? LinearLayout
                if (parent != null) {
                    // Rimuovi eventuali righe summary precedenti (rerender)
                    val toRemove = mutableListOf<View>()
                    for (i in 0 until parent.childCount) {
                        val child = parent.getChildAt(i)
                        if (child.tag == "summary_row") toRemove.add(child)
                    }
                    toRemove.forEach { parent.removeView(it) }

                    val bodyIndex = parent.indexOfChild(body)
                    var insertAt = bodyIndex + 1

                    // Container interno che forza l'allineamento a sinistra del gruppo
                    val listContainer = LinearLayout(requireContext()).apply {
                        tag = "summary_row"
                        orientation = LinearLayout.VERTICAL
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.topMargin = 24
                        lp.bottomMargin = 16
                        layoutParams = lp
                        setPadding(48, 0, 48, 0)
                    }

                    viewModel.summarySteps().forEach { (s, granted) ->
                        val row = TextView(requireContext()).apply {
                            text = "${if (granted) "🟢" else "🔴"}  ${stepLabel(s)}"
                            textSize = 16f
                            setTextColor(0xFFf5f3ee.toInt())
                            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            lp.topMargin = 10
                            layoutParams = lp
                        }
                        listContainer.addView(row)
                    }
                    parent.addView(listContainer, insertAt)
                }

                btnPrimary.text = "Vai all'app →"
                btnPrimary.setOnClickListener {
                    viewModel.completeWizard()
                    (activity as? OnboardingActivity)?.finishWizard()
                }
            }
        }

        updateStatusIcon(v, step)
    }

    private fun updateStatusIcon(v: View, step: OnboardingViewModel.Step) {
        val statusView = v.findViewById<TextView>(R.id.txt_step_status)
        val btnPrimary = v.findViewById<Button>(R.id.btn_primary)
        val showStatus = step in listOf(
            OnboardingViewModel.Step.CONTACTS,
            OnboardingViewModel.Step.PHONE_STATE,
            OnboardingViewModel.Step.CALL_SCREENING,
            OnboardingViewModel.Step.CALL_PHONE,
            OnboardingViewModel.Step.NOTIFICATIONS,
            OnboardingViewModel.Step.SMS,
            OnboardingViewModel.Step.BATTERY,
            OnboardingViewModel.Step.ARIA_FORWARDING
        )
        if (showStatus) {
            val granted = viewModel.isStepGranted(step)
            statusView.text = if (granted) "🟢 Attivo" else "🔴 Non attivo"
            statusView.visibility = View.VISIBLE
            // Feedback visivo: pulsante verde quando il permesso è concesso + nasconde secondario
            if (granted && step != OnboardingViewModel.Step.ARIA_FORWARDING) {
                btnPrimary.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2a5a30.toInt())
                btnPrimary.setTextColor(0xFF9bdf9b.toInt())
                btnPrimary.text = "✅ Fatto — Avanti →"
                btnPrimary.setOnClickListener { viewModel.next() }
                val btnSec = v.findViewById<Button>(R.id.btn_secondary)
                btnSec.visibility = View.GONE
            }
        } else {
            statusView.visibility = View.GONE
        }
    }

    private fun startContactsLoad(v: View) {
        val ctx = requireContext().applicationContext
        val countText = v.findViewById<TextView>(R.id.txt_loading_count)
        val progressBar = v.findViewById<LinearProgressIndicator>(R.id.loading_progress_bar)
        progressBar.isIndeterminate = true

        // Receiver progress
        contactsReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val cur = intent?.getIntExtra("current", 0) ?: 0
                val tot = intent?.getIntExtra("total", 0) ?: 0
                viewModel.updateContactsProgress(cur, tot)
                if (tot > 0) {
                    progressBar.isIndeterminate = false
                    progressBar.max = tot
                    progressBar.progress = cur
                    countText.text = "$cur / $tot contatti caricati"
                }
                if (tot > 0 && cur >= tot) {
                    // Caricamento completato -> avanza
                    view?.postDelayed({ viewModel.next() }, 600)
                }
            }
        }
        val filter = IntentFilter("com.ifs.stoppai.CONTACTS_SYNC_PROGRESS")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(contactsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(contactsReceiver, filter)
        }

        // Avvia caricamento sincrono in background
        Thread {
            ContactCacheManager.loadContactsSync(ctx)
            // Fallback: se broadcast non arriva o tot==0
            view?.postDelayed({
                if (viewModel.currentStep.value == OnboardingViewModel.Step.CONTACTS_LOADING) {
                    viewModel.next()
                }
            }, 1500)
        }.start()
    }

    private fun stepLabel(s: OnboardingViewModel.Step): String = when (s) {
        OnboardingViewModel.Step.CONTACTS -> "Accesso rubrica"
        OnboardingViewModel.Step.PHONE_STATE -> "Stato telefono"
        OnboardingViewModel.Step.CALL_SCREENING -> "App verifica chiamate"
        OnboardingViewModel.Step.CALL_PHONE -> "Permesso telefonate"
        OnboardingViewModel.Step.NOTIFICATIONS -> "Notifiche"
        OnboardingViewModel.Step.SMS -> "Invio SMS"
        OnboardingViewModel.Step.BATTERY -> "Ottimizzazione batteria"
        OnboardingViewModel.Step.ARIA_FORWARDING -> "Segreteria ARIA"
        else -> s.name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            contactsReceiver?.let { requireContext().applicationContext.unregisterReceiver(it) }
        } catch (_: Exception) {}
        contactsReceiver = null
    }
}
