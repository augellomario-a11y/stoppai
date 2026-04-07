// FILE: BackendSyncService.kt
// SCOPO: Sincronizzazione device info + statistiche con backend (SA-123)
package com.ifs.stoppai.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Calendar

object BackendSyncService {

    private const val BACKEND_URL = "https://stoppai.it"

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

                android.util.Log.d("STOPPAI_SYNC", "Stats: tot=$chiamateTotali oggi=$chiamateOggi conNR=$conosciutiNR mobNR=$sconMobileNR mobSMS=$sconMobileSms mobSegr=$sconMobileSegr fisNR=$sconFissiNR fisSegr=$sconFissiSegr privNR=$privatiNR privSegr=$privatiSegr")

                // Batteria
                val batteryInfo = getBatteryInfo(context)

                val body = JSONObject().apply {
                    put("tester_id", testerId)
                    put("device", JSONObject().apply {
                        put("modello", modello)
                        put("versione_android", versioneAndroid)
                        put("versione_app", versioneApp)
                    })
                    put("batteria", batteryInfo)
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

    private fun getBatteryInfo(context: Context): JSONObject {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (scale > 0) (level * 100) / scale else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val inCarica = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatura = tempRaw / 10.0

        return JSONObject().apply {
            put("livello", pct)
            put("in_carica", inCarica)
            put("temperatura", temperatura)
        }
    }

    private fun getVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (Build ${pInfo.longVersionCode})"
        } catch (e: Exception) { "sconosciuta" }
    }
}
