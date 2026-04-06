// FILE: AriaConfigApi.kt
// SCOPO: Helper chiamate backend per configurazione segreteria ARIA
// ULTIMA MODIFICA: 2026-04-06

package com.ifs.stoppai.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

object AriaConfigApi {

    private const val BACKEND_URL = "http://46.225.14.90:6002"

    data class AriaConfig(
        val tipoMessaggio: String = "base",  // base / preset / custom
        val presetId: String? = null,
        val customWavPath: String? = null,
        val customUploadedAt: String? = null,  // timestamp ISO dell'ultima registrazione
        val customSmsTesto: String? = null
    )

    /**
     * Legge la config corrente del tester dal backend.
     */
    suspend fun getConfig(testerId: Int): AriaConfig = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BACKEND_URL/api/tester/$testerId/aria-config")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            AriaConfig(
                tipoMessaggio = json.optString("tipo_messaggio", "base"),
                presetId = json.optString("preset_id").takeIf { it.isNotBlank() && it != "null" },
                customWavPath = json.optString("custom_wav_path").takeIf { it.isNotBlank() && it != "null" },
                customUploadedAt = json.optString("custom_uploaded_at").takeIf { it.isNotBlank() && it != "null" },
                customSmsTesto = json.optString("custom_sms_testo").takeIf { it.isNotBlank() && it != "null" }
            )
        } catch (e: Exception) {
            Log.e("STOPPAI_ARIA", "Errore getConfig: ${e.message}")
            AriaConfig()
        }
    }

    /**
     * Salva la scelta tipo messaggio (base / preset / custom).
     */
    suspend fun saveConfig(
        testerId: Int,
        tipoMessaggio: String,
        presetId: String? = null,
        customSmsTesto: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BACKEND_URL/api/tester/$testerId/aria-config")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val body = JSONObject().apply {
                put("tipo_messaggio", tipoMessaggio)
                if (presetId != null) put("preset_id", presetId)
                if (customSmsTesto != null) put("custom_sms_testo", customSmsTesto)
            }
            conn.outputStream.write(body.toString().toByteArray())
            val code = conn.responseCode
            conn.disconnect()
            Log.d("STOPPAI_ARIA", "saveConfig: HTTP $code")
            code in 200..299
        } catch (e: Exception) {
            Log.e("STOPPAI_ARIA", "Errore saveConfig: ${e.message}")
            false
        }
    }

    /**
     * Upload file WAV custom al backend (multipart).
     */
    suspend fun uploadCustom(testerId: Int, wavFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val boundary = "----StoppAI" + System.currentTimeMillis()
            val url = URL("$BACKEND_URL/api/tester/$testerId/aria-config/custom-upload")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000

            val dos = DataOutputStream(conn.outputStream)
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"wav\"; filename=\"custom.wav\"\r\n")
            dos.writeBytes("Content-Type: audio/wav\r\n\r\n")

            FileInputStream(wavFile).use { fis ->
                val buf = ByteArray(4096)
                var n = fis.read(buf)
                while (n > 0) {
                    dos.write(buf, 0, n)
                    n = fis.read(buf)
                }
            }
            dos.writeBytes("\r\n--$boundary--\r\n")
            dos.flush()
            dos.close()

            val code = conn.responseCode
            conn.disconnect()
            Log.d("STOPPAI_ARIA", "uploadCustom: HTTP $code")
            code in 200..299
        } catch (e: Exception) {
            Log.e("STOPPAI_ARIA", "Errore uploadCustom: ${e.message}")
            false
        }
    }

    fun presetAudioUrl(presetId: String): String = "$BACKEND_URL/aria-preset/$presetId.wav"
    fun customAudioUrl(testerId: Int): String = "$BACKEND_URL/aria-custom/custom_tester_$testerId.wav"

    /**
     * Lista preset disponibili (in futuro si puo' leggere dal backend).
     */
    val PRESETS_MASCHILI = listOf(
        "uomo_1" to "Uomo 1",
        "uomo_2" to "Uomo 2",
        "uomo_3" to "Uomo 3",
        "uomo_4" to "Uomo 4"
    )

    val PRESETS_FEMMINILI = listOf(
        "donna_1" to "Donna 1",
        "donna_2" to "Donna 2",
        "donna_3" to "Donna 3",
        "donna_4" to "Donna 4"
    )
}
