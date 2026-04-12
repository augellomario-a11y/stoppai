// FILE: CallerOverlayService.kt
// SCOPO: Overlay sopra la chiamata — mostra note (verde) o spam alert (rosso)
// ULTIMA MODIFICA: 2026-04-13

package com.ifs.stoppai.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class CallerOverlayService : Service() {

    companion object {
        const val EXTRA_TIPO = "tipo"       // "nota" | "spam" | "alert"
        const val EXTRA_TITOLO = "titolo"
        const val EXTRA_TESTO = "testo"
        const val EXTRA_NUMERO = "numero"
        const val TIPO_NOTA = "nota"
        const val TIPO_SPAM = "spam"
        const val TIPO_ALERT = "alert"      // Futuro: link pericolosi

        fun mostra(context: Context, tipo: String, titolo: String, testo: String, numero: String) {
            val intent = Intent(context, CallerOverlayService::class.java).apply {
                putExtra(EXTRA_TIPO, tipo)
                putExtra(EXTRA_TITOLO, titolo)
                putExtra(EXTRA_TESTO, testo)
                putExtra(EXTRA_NUMERO, numero)
            }
            context.startService(intent)
        }
    }

    private var overlayView: LinearLayout? = null
    private var windowManager: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tipo = intent?.getStringExtra(EXTRA_TIPO) ?: TIPO_NOTA
        val titolo = intent?.getStringExtra(EXTRA_TITOLO) ?: ""
        val testo = intent?.getStringExtra(EXTRA_TESTO) ?: ""
        val numero = intent?.getStringExtra(EXTRA_NUMERO) ?: ""

        // Rimuovi overlay precedente se esiste
        rimuoviOverlay()

        // Verifica permesso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Log.w("STOPPAI_OVERLAY", "Permesso overlay non concesso")
            stopSelf()
            return START_NOT_STICKY
        }

        mostraOverlay(tipo, titolo, testo, numero)

        // Chiudi automaticamente dopo 12 secondi
        Handler(Looper.getMainLooper()).postDelayed({
            rimuoviOverlay()
            stopSelf()
        }, 12000)

        return START_NOT_STICKY
    }

    private fun mostraOverlay(tipo: String, titolo: String, testo: String, numero: String) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Colori in base al tipo
        val coloreSfondo: Int
        val coloreTop: Int
        val coloreTesto: Int
        val icona: String

        when (tipo) {
            TIPO_SPAM -> {
                coloreSfondo = Color.parseColor("#1a0000")
                coloreTop = Color.parseColor("#e53935")
                coloreTesto = Color.WHITE
                icona = "\uD83D\uDEA8"  // 🚨
            }
            TIPO_ALERT -> {
                coloreSfondo = Color.parseColor("#1a1200")
                coloreTop = Color.parseColor("#FF9800")
                coloreTesto = Color.WHITE
                icona = "\u26A0\uFE0F"  // ⚠️
            }
            else -> { // TIPO_NOTA
                coloreSfondo = Color.parseColor("#001a0e")
                coloreTop = Color.parseColor("#2E7D32")
                coloreTesto = Color.WHITE
                icona = "\u2705"  // ✅
            }
        }

        // Card principale
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 28, 40, 28)

            val shape = GradientDrawable().apply {
                setColor(coloreSfondo)
                cornerRadius = 28f
                setStroke(4, coloreTop)
            }
            background = shape
        }

        // Barra colorata in alto
        val barra = LinearLayout(this).apply {
            val shape = GradientDrawable().apply {
                setColor(coloreTop)
                cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
            }
            background = shape
            setPadding(32, 16, 32, 16)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        // Icona + titolo nella barra
        val txtBarraTitolo = TextView(this).apply {
            text = "$icona  StoppAI"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        barra.addView(txtBarraTitolo)
        card.addView(barra)

        // Titolo grande
        val txtTitolo = TextView(this).apply {
            text = titolo
            textSize = 20f
            setTextColor(coloreTesto)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.START
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 8
            layoutParams = lp
        }
        card.addView(txtTitolo)

        // Testo nota/dettaglio
        if (testo.isNotBlank()) {
            val txtDettaglio = TextView(this).apply {
                text = testo
                textSize = 16f
                setTextColor(Color.parseColor("#CCCCCC"))
                setLineSpacing(6f, 1f)
                gravity = Gravity.START
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8
                layoutParams = lp
            }
            card.addView(txtDettaglio)
        }

        // Numero piccolo in basso
        val txtNumero = TextView(this).apply {
            text = numero
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.END
        }
        card.addView(txtNumero)

        // Tocca per chiudere
        card.setOnClickListener {
            rimuoviOverlay()
            stopSelf()
        }

        // Layout params per la finestra overlay
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            // Posiziona al 50% dello schermo — sotto il popup chiamata
            val displayMetrics = this@CallerOverlayService.resources.displayMetrics
            y = displayMetrics.heightPixels / 2
        }

        overlayView = card

        try {
            windowManager?.addView(card, params)
            Log.d("STOPPAI_OVERLAY", "Overlay $tipo mostrato per $numero")
        } catch (e: Exception) {
            Log.e("STOPPAI_OVERLAY", "Errore overlay: ${e.message}")
        }
    }

    private fun rimuoviOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        rimuoviOverlay()
        super.onDestroy()
    }
}
