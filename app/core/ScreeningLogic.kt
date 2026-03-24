// FILE: ScreeningLogic.kt
// SCOPO: Logica decisionale pura per lo screening chiamate
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

enum class Decisione {
    SQUILLA,
    ARIA,
    BLOCCA_E_SMS
}

object ScreeningLogic {

    /**
     * decide l'azione da intraprendere in base allo stato dei permessi e del pacchetto protezione
     */
    fun decidi(
        numero: String,
        isContact: Boolean,
        isPreferito: Boolean,
        protezioneBase: Boolean,
        protezioneTotale: Boolean,
        includiPreferiti: Boolean,
        smsAttivo: Boolean,
        tipoNumero: String
    ): Decisione {

        // 1. TUTTO SPENTO -> SQUILLA SEMPRE
        if (!protezioneBase && !protezioneTotale) {
            return Decisione.SQUILLA
        }

        // 2. SMS PRIORITARIO: Se SMS attivo e Mobile IT sconosciuto -> BLOCCO + SMS
        // Questa regola vince su tutto il resto per i numeri sconosciuti mobili
        if (smsAttivo && !isContact && tipoNumero == "MOBILE_IT") {
            return Decisione.BLOCCA_E_SMS
        }

        // 3. PROTEZIONE TOTALE ON
        if (protezioneTotale) {
            // Se Includi Preferiti è ON -> nessuno squilla (vince su tutto)
            if (includiPreferiti) {
                return Decisione.ARIA
            }
            // Se Includi Preferiti è OFF -> solo i preferiti squillano
            if (isPreferito) {
                return Decisione.SQUILLA
            }
            // Tutti gli altri (rubrica standard o sconosciuti non mobili) -> ARIA
            return Decisione.ARIA
        }

        // 4. PROTEZIONE BASE ON (Siamo qui se totale = false)
        if (isContact) {
            return Decisione.SQUILLA
        }

        // Sconosciuto (già filtrato se mobile+sms sopra) -> ARIA
        return Decisione.ARIA
    }
}
