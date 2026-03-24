// FILE: NumberClassifier.kt
// SCOPO: Classificazione dei numeri di telefono
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

object NumberClassifier {

    /**
     * Identifica il tipo di numero per decidere l'azione di screening
     */
    fun getTipoNumero(number: String): String {
        if (number.isEmpty()) return "NASCOSTO"
        
        // Pulizia base per il tipo
        val n = number.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        
        val isItaliano = n.startsWith("+39") || n.startsWith("0039") || (!n.startsWith("+") && n.length >= 9)
        
        if (!isItaliano && n.startsWith("+")) return "ESTERO"
        
        val noPrefisso = when {
            n.startsWith("+39") -> n.removePrefix("+39")
            n.startsWith("0039") -> n.removePrefix("0039")
            else -> n
        }
        
        return when {
            noPrefisso.startsWith("3") -> "MOBILE_IT"
            noPrefisso.startsWith("0") -> "FISSO_IT"
            else -> "MOBILE_IT" // Safe default per blocco/SMS se non chiaro
        }
    }
}
