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
        tipoNumero: String,
        consentiEsteri: Boolean = false
    ): Decisione {

        // 1. TUTTO SPENTO -> SQUILLA SEMPRE
        if (!protezioneBase && !protezioneTotale) {
            return Decisione.SQUILLA
        }

        // 2. Contatto in rubrica -> SQUILLA (vale per tutti, base e totale esclusa)
        //    In protezione totale gestito sotto con logica preferiti
        if (isContact && !protezioneTotale) {
            return Decisione.SQUILLA
        }

        // 3. SMS PRIORITARIO: Se SMS attivo e Mobile IT sconosciuto -> BLOCCO + SMS
        if (smsAttivo && !isContact && tipoNumero == "MOBILE_IT") {
            return Decisione.BLOCCA_E_SMS
        }

        // 4. Numeri esteri sconosciuti
        if (tipoNumero == "ESTERO" && !isContact) {
            // Se consenti esteri ON -> SQUILLA, altrimenti -> ARIA
            return if (consentiEsteri) Decisione.SQUILLA else Decisione.ARIA
        }

        // 5. PROTEZIONE TOTALE ON
        if (protezioneTotale) {
            if (includiPreferiti) {
                return Decisione.ARIA
            }
            if (isPreferito) {
                return Decisione.SQUILLA
            }
            return Decisione.ARIA
        }

        // 6. PROTEZIONE BASE ON — sconosciuto italiano -> ARIA
        return Decisione.ARIA
    }
}
