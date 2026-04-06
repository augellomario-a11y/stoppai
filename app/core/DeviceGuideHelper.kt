// FILE: DeviceGuideHelper.kt
// SCOPO: Detection device + 5 famiglie + nomi marketing + istruzioni wizard
// USATO DA: OnboardingViewModel
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.core

import android.os.Build

object DeviceGuideHelper {

    enum class DeviceFamily {
        SAMSUNG,
        XIAOMI,
        PIXEL,
        HUAWEI,
        DEFAULT
    }

    /**
     * Determina la famiglia in base al produttore (case-insensitive).
     */
    fun detect(): DeviceFamily {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
        val brand = (Build.BRAND ?: "").lowercase()
        return when {
            manufacturer.contains("samsung") -> DeviceFamily.SAMSUNG
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || brand.contains("poco") -> DeviceFamily.XIAOMI
            manufacturer.contains("google") || brand.contains("pixel") -> DeviceFamily.PIXEL
            manufacturer.contains("huawei") || brand.contains("honor") -> DeviceFamily.HUAWEI
            else -> DeviceFamily.DEFAULT
        }
    }

    /**
     * Ritorna il nome commerciale del telefono. Se non in tabella, ritorna Build.MODEL.
     * Esempio: "SM-S908N" -> "Samsung Galaxy S22 Ultra"
     */
    fun marketingName(): String {
        val model = Build.MODEL ?: return "Sconosciuto"
        val brand = (Build.MANUFACTURER ?: "").trim()
            .lowercase().replaceFirstChar { it.uppercase() }
        val name = MARKETING_NAMES[model] ?: model
        // Se il nome contiene gia' il brand non duplicarlo
        return if (name.lowercase().contains(brand.lowercase())) name else "$brand $name"
    }

    /**
     * Restituisce il blocco di istruzioni testuali per uno step e una famiglia.
     * Usato dalla schermata 3 (verifica chiamate) e 7 (batteria).
     */
    fun instructionsFor(step: GuideStep, family: DeviceFamily): String {
        return when (step) {
            GuideStep.CALL_SCREENING -> when (family) {
                DeviceFamily.SAMSUNG -> "Vai in Impostazioni → App di telefonia → App verifica chiamate → seleziona StoppAI"
                DeviceFamily.PIXEL -> "Vai in Impostazioni → App predefinite → App verifica chiamate → seleziona StoppAI"
                DeviceFamily.XIAOMI -> "Vai in Impostazioni → Gestione app → StoppAI → Autorizzazioni → App verifica chiamate"
                DeviceFamily.HUAWEI -> "Vai in Impostazioni → App → App predefinite → App verifica chiamate → seleziona StoppAI"
                DeviceFamily.DEFAULT -> "Vai in Impostazioni → App → App predefinite → App verifica chiamate → seleziona StoppAI"
            }
            GuideStep.BATTERY -> when (family) {
                DeviceFamily.SAMSUNG -> "Vai in Impostazioni → Cura dispositivo → Batteria → Limiti utilizzo app → escludi StoppAI"
                DeviceFamily.PIXEL -> "Vai in Impostazioni → App → StoppAI → Batteria → Non ottimizzata"
                DeviceFamily.XIAOMI -> "Vai in Impostazioni → App → StoppAI → Risparmio energetico → Nessuna restrizione"
                DeviceFamily.HUAWEI -> "Vai in Impostazioni → Batteria → Avvio app → StoppAI → Gestione manuale (tutto attivo)"
                DeviceFamily.DEFAULT -> "Vai in Impostazioni → App → StoppAI → Batteria → Non ottimizzata"
            }
        }
    }

    enum class GuideStep {
        CALL_SCREENING,
        BATTERY
    }

    /**
     * Mappatura modello tecnico -> nome marketing per i telefoni piu' diffusi in Italia.
     * Lista non esaustiva, fallback al MODEL se non presente.
     */
    private val MARKETING_NAMES: Map<String, String> = mapOf(
        // Samsung Galaxy S series
        "SM-G980F" to "Galaxy S20",
        "SM-G981B" to "Galaxy S20 5G",
        "SM-G985F" to "Galaxy S20+",
        "SM-G988B" to "Galaxy S20 Ultra",
        "SM-G991B" to "Galaxy S21",
        "SM-G996B" to "Galaxy S21+",
        "SM-G998B" to "Galaxy S21 Ultra",
        "SM-S901B" to "Galaxy S22",
        "SM-S906B" to "Galaxy S22+",
        "SM-S908B" to "Galaxy S22 Ultra",
        "SM-S908N" to "Galaxy S22 Ultra",
        "SM-S911B" to "Galaxy S23",
        "SM-S916B" to "Galaxy S23+",
        "SM-S918B" to "Galaxy S23 Ultra",
        "SM-S921B" to "Galaxy S24",
        "SM-S926B" to "Galaxy S24+",
        "SM-S928B" to "Galaxy S24 Ultra",
        // Samsung Galaxy A
        "SM-A536B" to "Galaxy A53 5G",
        "SM-A546B" to "Galaxy A54 5G",
        "SM-A556B" to "Galaxy A55 5G",
        "SM-A136B" to "Galaxy A13",
        "SM-A146B" to "Galaxy A14 5G",
        "SM-A156B" to "Galaxy A15 5G",
        // Samsung Galaxy Note / Z
        "SM-N986B" to "Galaxy Note 20 Ultra",
        "SM-F926B" to "Galaxy Z Fold3",
        "SM-F936B" to "Galaxy Z Fold4",
        "SM-F946B" to "Galaxy Z Fold5",
        "SM-F721B" to "Galaxy Z Flip4",
        "SM-F731B" to "Galaxy Z Flip5",
        // Google Pixel
        "Pixel 6" to "Pixel 6",
        "Pixel 6 Pro" to "Pixel 6 Pro",
        "Pixel 6a" to "Pixel 6a",
        "Pixel 7" to "Pixel 7",
        "Pixel 7 Pro" to "Pixel 7 Pro",
        "Pixel 7a" to "Pixel 7a",
        "Pixel 8" to "Pixel 8",
        "Pixel 8 Pro" to "Pixel 8 Pro",
        "Pixel 8a" to "Pixel 8a",
        "Pixel 9" to "Pixel 9",
        "Pixel 9 Pro" to "Pixel 9 Pro",
        // Xiaomi / Redmi
        "M2101K6G" to "Redmi Note 10",
        "M2101K7AG" to "Redmi Note 10S",
        "21091116AG" to "Redmi Note 11",
        "22101316G" to "Redmi Note 12",
        "23090RA98G" to "Redmi Note 13",
        "2201117TG" to "Xiaomi 11T",
        "2201116TG" to "Xiaomi 11T Pro",
        "2203121C" to "Xiaomi 12",
        "2210132G" to "Xiaomi 12T",
        "2306EPN60G" to "Xiaomi 13",
        "23116PN5BG" to "Xiaomi 14",
        // Huawei / Honor
        "ELS-NX9" to "Huawei P40 Pro",
        "ANA-NX9" to "Huawei P40",
        "JAD-LX9" to "Huawei P50 Pro",
        "ALN-LX9" to "Huawei Mate 60 Pro"
    )
}
