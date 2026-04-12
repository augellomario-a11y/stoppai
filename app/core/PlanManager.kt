// FILE: PlanManager.kt
// SCOPO: Gestione piani FREE/PRO/SHIELD — verifica accesso funzionalità + upgrade progressivo
// ULTIMA MODIFICA: 2026-04-13

package com.ifs.stoppai.core

import android.content.Context

object PlanManager {

    const val FREE = "free"
    const val PRO = "pro"
    const val SHIELD = "shield"

    // Giorni minimi per poter fare upgrade
    const val GIORNI_PER_PRO = 5      // FREE → PRO dopo 5 giorni
    const val GIORNI_PER_SHIELD = 5   // PRO → SHIELD dopo 5 giorni di PRO

    enum class Feature(val pianoMinimo: String, val nome: String, val descrizione: String) {
        PROTEZIONE_TOTALE(PRO, "Protezione Totale", "Silenzia tutte le chiamate, anche dei tuoi contatti. Utile durante riunioni importanti."),
        FILTRO_ESTERI(SHIELD, "Numeri esteri", "Consenti tutte le chiamate internazionali sconosciute. Le chiamate non vengono mai bloccate, solo silenziate."),
        WHITELIST_ESTERI(SHIELD, "White list", "Consenti chiamate solo da prefissi e numeri selezionati. Le altre restano silenziate."),
        SMS_PERSONALIZZABILE(PRO, "SMS personalizzabile", "Personalizza il testo dell'SMS automatico inviato agli sconosciuti."),
        ARIA_ILLIMITATO(PRO, "ARIA illimitato", "Inoltro illimitato alla segreteria ARIA con trascrizione automatica."),
        ARIA_PRESET(PRO, "Scelta preset ARIA", "Scegli tra 8 messaggi di benvenuto professionali per la segreteria."),
        ARIA_CUSTOM(SHIELD, "Messaggio personale ARIA", "Registra il tuo messaggio di benvenuto personalizzato."),
        PLAYER_AUDIO(PRO, "Ascolto messaggi audio", "Ascolta i messaggi originali lasciati in segreteria."),
        CHAT_SUPPORTO(PRO, "Chat con il team", "Contatta direttamente il team StoppAI per assistenza."),
        CRM_COMPLETO(PRO, "Mini CRM completo", "Storico completo di tutte le chiamate gestite da ARIA.")
    }

    fun getPianoCorrente(context: Context): String {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        return prefs.getString("tester_piano", FREE) ?: FREE
    }

    /**
     * Giorni dall'installazione dell'app.
     */
    fun giorniDaInstallazione(context: Context): Int {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val installTime = prefs.getLong("install_timestamp", 0L)
        if (installTime == 0L) {
            prefs.edit().putLong("install_timestamp", System.currentTimeMillis()).apply()
            return 0
        }
        return ((System.currentTimeMillis() - installTime) / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Giorni dall'attivazione del piano PRO.
     */
    fun giorniDaPro(context: Context): Int {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val proTime = prefs.getLong("pro_activation_timestamp", 0L)
        if (proTime == 0L) return 0
        return ((System.currentTimeMillis() - proTime) / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Giorni rimanenti prima di poter fare upgrade a PRO.
     * Restituisce 0 se già disponibile.
     */
    fun giorniRimanentiPerPro(context: Context): Int {
        val giorni = giorniDaInstallazione(context)
        return maxOf(0, GIORNI_PER_PRO - giorni)
    }

    /**
     * Giorni rimanenti prima di poter fare upgrade a SHIELD.
     * Restituisce 0 se già disponibile. Richiede PRO attivo.
     */
    fun giorniRimanentiPerShield(context: Context): Int {
        val piano = getPianoCorrente(context)
        if (piano != PRO) return -1 // Deve prima essere PRO
        val giorni = giorniDaPro(context)
        return maxOf(0, GIORNI_PER_SHIELD - giorni)
    }

    /**
     * L'utente è flaggato come admin? (nessun limite temporale sugli upgrade)
     */
    fun isAdmin(context: Context): Boolean {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_admin", false)
    }

    /**
     * Può fare upgrade a PRO? (≥5 giorni da installazione + piano FREE)
     * Admin: salta il limite temporale.
     */
    fun puoUpgradeAPro(context: Context): Boolean {
        if (getPianoCorrente(context) != FREE) return false
        return isAdmin(context) || giorniRimanentiPerPro(context) == 0
    }

    /**
     * Può fare upgrade a SHIELD? (≥5 giorni da PRO + piano PRO)
     * Admin: salta il limite temporale, può saltare anche da FREE a SHIELD.
     */
    fun puoUpgradeAShield(context: Context): Boolean {
        val piano = getPianoCorrente(context)
        if (isAdmin(context)) return piano == PRO || piano == FREE
        return piano == PRO && giorniRimanentiPerShield(context) == 0
    }

    /**
     * Esegue l'upgrade del piano (salva in SharedPreferences).
     */
    fun eseguiUpgrade(context: Context, nuovoPiano: String) {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("tester_piano", nuovoPiano)
            .apply()
        if (nuovoPiano == PRO) {
            prefs.edit().putLong("pro_activation_timestamp", System.currentTimeMillis()).apply()
        }
    }

    /**
     * Trial disabilitato per la fase beta.
     * Tutti partono come FREE, upgrade progressivo dopo 5+5 giorni.
     */
    fun isTrialAttivo(context: Context): Boolean {
        return false // Disabilitato per beta testing
    }

    fun isDisponibile(context: Context, feature: Feature): Boolean {
        val piano = getPianoCorrente(context)
        return when (feature.pianoMinimo) {
            FREE -> true
            PRO -> piano == PRO || piano == SHIELD
            SHIELD -> piano == SHIELD
            else -> false
        }
    }

    fun getColore(feature: Feature): Int {
        return when (feature.pianoMinimo) {
            PRO -> 0xFF1976D2.toInt()
            SHIELD -> 0xFFC8A96E.toInt()
            else -> 0xFF888888.toInt()
        }
    }

    fun getNomePiano(feature: Feature): String {
        return when (feature.pianoMinimo) {
            PRO -> "PRO"
            SHIELD -> "SHIELD"
            else -> "FREE"
        }
    }

    fun getPrezzo(feature: Feature): String {
        return when (feature.pianoMinimo) {
            PRO -> "2,99€/mese · 18€/anno"
            SHIELD -> "4,99€/mese · 29€/anno"
            else -> "Gratuito"
        }
    }
}
