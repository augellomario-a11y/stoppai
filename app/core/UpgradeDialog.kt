// FILE: UpgradeDialog.kt
// SCOPO: Popup upgrade quando l'utente tocca una funzionalità bloccata
// ULTIMA MODIFICA: 2026-04-13

package com.ifs.stoppai.core

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch

object UpgradeDialog {

    /**
     * Mostra il popup di upgrade per una funzionalità bloccata.
     * Include: icona lucchetto, nome feature, descrizione, piano richiesto, prezzo, bottone upgrade.
     */
    fun show(context: Context, feature: PlanManager.Feature) {
        val piano = PlanManager.getNomePiano(feature)
        val prezzo = PlanManager.getPrezzo(feature)
        val colore = PlanManager.getColore(feature)

        // Tracking: invia click sul lucchetto al backend per statistiche
        trackUpgradeClick(context, feature)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Icona lucchetto grande
        val iconLock = ImageView(context).apply {
            setImageResource(
                if (feature.pianoMinimo == PlanManager.SHIELD) com.ifs.stoppai.R.drawable.ic_lock_shield
                else com.ifs.stoppai.R.drawable.ic_lock_pro
            )
            val size = (64 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
                bottomMargin = 20
            }
        }
        layout.addView(iconLock)

        // Titolo funzionalità
        val txtTitolo = TextView(context).apply {
            text = feature.nome
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1a1a2e"))
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }
        layout.addView(txtTitolo)

        // Descrizione
        val txtDesc = TextView(context).apply {
            text = feature.descrizione
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            setLineSpacing(6f, 1f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            layoutParams = lp
        }
        layout.addView(txtDesc)

        // Badge piano richiesto
        val txtPiano = TextView(context).apply {
            text = "A partire dal piano $piano"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(colore)
            gravity = Gravity.CENTER
            setPadding(24, 10, 24, 10)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 8
            layoutParams = lp
        }
        layout.addView(txtPiano)

        // Prezzo
        val txtPrezzo = TextView(context).apply {
            text = prezzo
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(colore)
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 8
            layoutParams = lp
        }
        layout.addView(txtPrezzo)

        AlertDialog.Builder(context)
            .setView(layout)
            .setPositiveButton("Vedi i piani") { _, _ ->
                PricingSheet.show(context)
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    /** Invia al backend il click su un lucchetto per le statistiche */
    private fun trackUpgradeClick(context: Context, feature: PlanManager.Feature) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("stoppai_prefs", android.content.Context.MODE_PRIVATE)
                val testerId = prefs.getInt("tester_id", 0)
                if (testerId == 0) return@launch

                val url = java.net.URL("https://stoppai.it/api/tester/upgrade-click")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                val body = """{"tester_id":$testerId,"feature":"${feature.name}"}"""
                conn.outputStream.write(body.toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Aggiunge un badge lucchetto a una View esistente.
     * Se la feature è disponibile: lucchetto aperto verde.
     * Se bloccata: lucchetto chiuso col colore del piano richiesto.
     */
    fun addBadge(context: Context, container: LinearLayout, feature: PlanManager.Feature) {
        val disponibile = PlanManager.isDisponibile(context, feature)
        val icon = ImageView(context).apply {
            setImageResource(
                if (disponibile) com.ifs.stoppai.R.drawable.ic_lock_open
                else if (feature.pianoMinimo == PlanManager.SHIELD) com.ifs.stoppai.R.drawable.ic_lock_shield
                else com.ifs.stoppai.R.drawable.ic_lock_pro
            )
            val size = (20 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                marginStart = 8
            }
            if (!disponibile) {
                setOnClickListener { show(context, feature) }
            }
        }
        container.addView(icon)
    }
}
