// FILE: PlanManager.kt
// SCOPO: Gestione piani FREE/PRO/SHIELD — verifica accesso funzionalità + badge lock
// ULTIMA MODIFICA: 2026-04-12

package com.ifs.stoppai.core

import android.content.Context

object PlanManager {

    // Piani disponibili
    const val FREE = "free"
    const val PRO = "pro"
    const val SHIELD = "shield"

    // Funzionalità con piano minimo richiesto
    enum class Feature(val pianoMinimo: String, val nome: String, val descrizione: String) {
        PROTEZIONE_TOTALE(PRO, "Protezione Totale", "Silenzia tutte le chiamate, anche dei tuoi contatti. Utile durante riunioni importanti."),
        FILTRO_ESTERI(SHIELD, "Filtro numeri esteri", "Blocca automaticamente le chiamate provenienti da numeri internazionali."),
        WHITELIST_ESTERI(SHIELD, "White list prefissi esteri", "Consenti chiamate solo da prefissi internazionali selezionati."),
        SMS_PERSONALIZZABILE(PRO, "SMS personalizzabile", "Personalizza il testo dell'SMS automatico inviato agli sconosciuti."),
        ARIA_ILLIMITATO(PRO, "ARIA illimitato", "Inoltro illimitato alla segreteria ARIA con trascrizione automatica."),
        ARIA_PRESET(PRO, "Scelta preset ARIA", "Scegli tra 8 messaggi di benvenuto professionali per la segreteria."),
        ARIA_CUSTOM(SHIELD, "Messaggio personale ARIA", "Registra il tuo messaggio di benvenuto personalizzato."),
        PLAYER_AUDIO(PRO, "Ascolto messaggi audio", "Ascolta i messaggi originali lasciati in segreteria."),
        CHAT_SUPPORTO(PRO, "Chat con il team", "Contatta direttamente il team StoppAI per assistenza."),
        CRM_COMPLETO(PRO, "Mini CRM completo", "Storico completo di tutte le chiamate gestite da ARIA.")
    }

    /**
     * Restituisce il piano corrente dell'utente dalle SharedPreferences.
     */
    fun getPianoCorrente(context: Context): String {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        return prefs.getString("tester_piano", FREE) ?: FREE
    }

    /**
     * Verifica se il trial di 14 giorni è ancora attivo.
     * Durante il trial, tutte le funzionalità SHIELD sono sbloccate.
     */
    fun isTrialAttivo(context: Context): Boolean {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val installTime = prefs.getLong("install_timestamp", 0L)
        if (installTime == 0L) {
            // Prima esecuzione: salva timestamp
            prefs.edit().putLong("install_timestamp", System.currentTimeMillis()).apply()
            return true
        }
        val giorni = (System.currentTimeMillis() - installTime) / (24 * 60 * 60 * 1000)
        return giorni < 14
    }

    /**
     * Verifica se una funzionalità è disponibile per il piano corrente.
     * Durante il trial, tutto è sbloccato.
     */
    fun isDisponibile(context: Context, feature: Feature): Boolean {
        if (isTrialAttivo(context)) return true
        val piano = getPianoCorrente(context)
        return when (feature.pianoMinimo) {
            FREE -> true
            PRO -> piano == PRO || piano == SHIELD
            SHIELD -> piano == SHIELD
            else -> false
        }
    }

    /**
     * Restituisce il colore del badge per il piano richiesto.
     * PRO = blu (#1976D2), SHIELD = oro (#c8a96e)
     */
    fun getColore(feature: Feature): Int {
        return when (feature.pianoMinimo) {
            PRO -> 0xFF1976D2.toInt()   // Blu PRO
            SHIELD -> 0xFFC8A96E.toInt() // Oro SHIELD
            else -> 0xFF888888.toInt()   // Grigio FREE
        }
    }

    /**
     * Restituisce il nome del piano richiesto per la UI.
     */
    fun getNomePiano(feature: Feature): String {
        return when (feature.pianoMinimo) {
            PRO -> "PRO"
            SHIELD -> "SHIELD"
            else -> "FREE"
        }
    }

    /**
     * Restituisce il prezzo del piano richiesto.
     */
    fun getPrezzo(feature: Feature): String {
        return when (feature.pianoMinimo) {
            PRO -> "2,99€/mese · 18€/anno"
            SHIELD -> "4,99€/mese · 29€/anno"
            else -> "Gratuito"
        }
    }
}
