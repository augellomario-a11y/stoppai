// FILE: AppSettingsRepository.kt
// SCOPO: Repository layer per centralizzare l'accesso alle impostazioni salvate su DB
// DIPENDENZE: AppSettingsDao.kt, AppSettingsEntity.kt
// ULTIMA MODIFICA: 2026-03-22

package com.ifs.stoppai.db

class AppSettingsRepository(private val dao: AppSettingsDao) {

    // Recupera il volume originale salvato o restituisce il default 7
    fun getVolumePreferito(): Int {
        val saved = dao.getValue("volume_preferito")
        return saved?.toIntOrNull() ?: 10
    }

    fun setVolumePreferito(value: Int) {
        dao.setValue(AppSettingsEntity("volume_preferito", value.toString()))
    }

    // Recupera il numero di deviazione o restituisce il default Telnyx +3905411770178
    fun getDeviationNumber(): String {
        return dao.getValue("deviation_number") ?: "3272238457"
    }

    // Salva il numero di deviazione su database
    fun setDeviationNumber(number: String) {
        dao.setValue(AppSettingsEntity("deviation_number", number))
    }

    // Svuota tutte le impostazioni
    fun clearAll() {
        dao.clearAll()
    }
}
