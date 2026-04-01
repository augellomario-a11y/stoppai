// FILE: BackendSyncService.kt
// SCOPO: Sincronizzazione device info + statistiche con backend (SA-123)
package com.ifs.stoppai.core

import android.content.Context
import android.os.Build
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Calendar

object BackendSyncService {

    private const val BACKEND_URL = "http://46.225.14.90:6002"

    suspend fun sync(context: Context) {
        val prefs = context.getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        val testerId = prefs.getInt("tester_id", -1)
        if (testerId == -1) return

        withContext(Dispatchers.IO) {
            try {
                val db = StoppAiDatabase.getInstance(context)
                val dao = db.callLogDao()
                val ariaDao = db.ariaMessaggioDao()

                // Timestamp inizio giornata
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val inizioGiorno = cal.timeInMillis
                val sempre = 0L

                // Device info
                val modello = "${Build.MANUFACTURER} ${Build.MODEL}"
                val versioneAndroid = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val versioneApp = getVersionName(context)

                // Statistiche totali (dall'installazione)
                val chiamateTotali = dao.getTotalCalls()
                val chiamateOggi = dao.getCallsToday(inizioGiorno)

                // Conosciuti non risposti oggi
                val conosciutiNR = dao.countConosciutiNonRispostiSince(inizioGiorno)

                // Sconosciuti mobile oggi
                val sconMobileNR = dao.countSconosciutiMobileNonRispostiSince(inizioGiorno)
                val sconMobileSms = dao.countSconosciutiMobileSmsSince(inizioGiorno)
                val sconMobileSegr = dao.countSconosciutiMobileSegrSince(inizioGiorno)

                // Sconosciuti fissi oggi
                val sconFissiNR = dao.countSconosciutiFissiNonRispostiSince(inizioGiorno)
                val sconFissiSegr = dao.countSconosciutiFissiSegrSince(inizioGiorno)

                // Privati oggi
                val privatiNR = dao.countPrivatiNonRispostiSince(inizioGiorno)
                val privatiSegr = dao.countPrivatiSegrSince(inizioGiorno)

                // Per "msg lasciato" contiamo le entry con aria_messaggi collegate
                // Per ora mettiamo segreteria = deviata, msg_lasciato sara' implementato dopo

                val body = JSONObject().apply {
                    put("tester_id", testerId)
                    put("device", JSONObject().apply {
                        put("modello", modello)
                        put("versione_android", versioneAndroid)
                        put("versione_app", versioneApp)
                    })
                    put("stats", JSONObject().apply {
                        put("chiamate_totali", chiamateTotali)
                        put("chiamate_oggi", chiamateOggi)
                        put("conosciuti_non_risposti", conosciutiNR)
                        put("sconosciuti_mobile_non_risposti", sconMobileNR)
                        put("sconosciuti_mobile_sms", sconMobileSms)
                        put("sconosciuti_mobile_segreteria", sconMobileSegr)
                        put("sconosciuti_fissi_non_risposti", sconFissiNR)
                        put("sconosciuti_fissi_segreteria", sconFissiSegr)
                        put("privati_non_risposti", privatiNR)
                        put("privati_segreteria", privatiSegr)
                    })
                }

                val url = URL("$BACKEND_URL/api/tester/sync")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.outputStream.write(body.toString().toByteArray())
                val code = conn.responseCode
                conn.disconnect()

                android.util.Log.d("STOPPAI_SYNC", "Sync completato: HTTP $code")

                // Salva timestamp ultimo sync
                prefs.edit().putLong("last_sync", System.currentTimeMillis()).apply()

            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_SYNC", "Errore sync: ${e.message}")
            }
        }
    }

    private fun getVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (Build ${pInfo.longVersionCode})"
        } catch (e: Exception) { "sconosciuta" }
    }
}
