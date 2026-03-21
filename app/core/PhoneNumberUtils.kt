// FILE: PhoneNumberUtils.kt
// SCOPO: Normalizzazione numeri in formato raw o +39
// DIPENDENZE: Nessuna
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.core

object PhoneNumberUtils {
    // Rimuove spazi e assicura formato +39
    fun normalizeNumber(number: String?): String {
        if (number.isNullOrBlank()) return ""
        var cleaned = number.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2)
        }
        if (!cleaned.startsWith("+")) {
            cleaned = "+39$cleaned"
        }
        return cleaned
    }
}
