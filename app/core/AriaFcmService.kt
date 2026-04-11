// FILE: AriaFcmService.kt
// SCOPO: Ricezione notifiche push ARIA e invio token al server (SA-092)
// DIPENDENZE: com.google.firebase:firebase-messaging-ktx
// ULTIMA MODIFICA: 2026-03-29

package com.ifs.stoppai.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ifs.stoppai.R
import com.ifs.stoppai.db.AriaMessaggio
import com.ifs.stoppai.db.CallLogEntry
import com.ifs.stoppai.db.StoppAiDatabase
import com.ifs.stoppai.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URL

/**
 * Servizio per la gestione delle notifiche Firebase Cloud Messaging
 */
class AriaFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "aria_messaggi"
        const val CHANNEL_NAME = "Messaggi ARIA"
        const val SERVER_URL = "https://stoppai.it"
    }

    /**
     * Chiamato quando viene generato un nuovo token FCM
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        inviaTokenAlServer(token)
    }

    /**
     * Chiamato quando viene ricevuta una notifica push
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // MAGIC CODE — auto-login (SA-122)
        val tipo = message.data["tipo"]
        if (tipo == "magic_code") {
            val codice = message.data["codice"] ?: return
            android.util.Log.d("STOPPAI_FCM", "Magic code ricevuto: $codice")
            // Salva in SharedPreferences — il LoginFragment fa polling
            getSharedPreferences("stoppai_prefs", MODE_PRIVATE)
                .edit().putString("magic_code_pending", codice).apply()
            // Broadcast per compatibilita'
            val intent = Intent("com.ifs.stoppai.MAGIC_CODE")
            intent.putExtra("codice", codice)
            intent.setPackage(packageName)
            sendBroadcast(intent)
            return
        }

        val numeroRaw = message.data["numero"]
            ?: message.notification?.title
            ?: "Sconosciuto"
        val testo = message.data["testo"]
            ?: message.notification?.body
            ?: "Nuovo messaggio"
        val timestamp = message.data["timestamp"]?.toLongOrNull()
            ?: (System.currentTimeMillis() / 1000)
        val wavFilename = message.data["wav_filename"]?.takeIf { it.isNotBlank() }

        // FIX: salvataggio SINCRONO con runBlocking
        runBlocking(Dispatchers.IO) {
            try {
                val db = StoppAiDatabase.getInstance(applicationContext)

                val numeroNorm = PhoneNumberUtils.normalizeNumber(numeroRaw)

                // FIX entry fantasma: se il numero è troppo corto (es. solo "+39" senza cifre reali)
                // non creare nessuna entry nel CRM — è un push con dati incompleti
                val cifrePulite = numeroNorm.replace("+", "").replace("39", "").replace("0", "")
                if (cifrePulite.length < 5) {
                    android.util.Log.w("STOPPAI_FCM", "Numero troppo corto dopo normalizzazione: $numeroNorm (raw: $numeroRaw) — skip entry CRM")
                    // Salva comunque il messaggio ARIA senza collegamento a CallLog
                    val messaggio = AriaMessaggio(
                        numero = numeroRaw,
                        testo = testo,
                        timestamp = timestamp,
                        wavFilename = wavFilename
                    )
                    db.ariaMessaggioDao().inserisci(messaggio)
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                    mostraNotifica(numeroRaw, testo)
                    return@runBlocking
                }

                val trentaMinutiFA = System.currentTimeMillis() - (30 * 60 * 1000)

                // Cerca CallLogEntry recente (finestra 30 min) — match sulle ultime 10 cifre
                val entryEsistente = db.callLogDao().getMostRecentByNumber(numeroNorm)

                android.util.Log.d("STOPPAI_FCM", "Cerco entry per $numeroNorm → trovata: ${entryEsistente?.id} (phone: ${entryEsistente?.phoneNumber}, ts: ${entryEsistente?.timestamp})")

                val callLogId: Long = if (entryEsistente != null && entryEsistente.timestamp >= trentaMinutiFA) {
                    entryEsistente.id
                } else {
                    // Non trovata → crea entry DEVIATA nel CRM
                    val nuovaEntry = CallLogEntry(
                        phoneNumber = numeroNorm,
                        callType = NumberClassifier.getTipoNumero(numeroNorm),
                        timestamp = System.currentTimeMillis(),
                        callOutcome = "DEVIATA",
                        displayName = CallLogHelper.getContactName(applicationContext, numeroNorm)
                    )
                    db.callLogDao().insertCallLog(nuovaEntry)
                }

                // Salva AriaMessaggio collegato alla CallLogEntry corretta
                val messaggio = AriaMessaggio(
                    numero = numeroNorm,
                    testo = testo,
                    timestamp = if (timestamp < 10000000000L) timestamp * 1000 else timestamp,
                    callLogId = callLogId,
                    wavFilename = wavFilename
                )
                db.ariaMessaggioDao().inserisci(messaggio)

                // Forza checkpoint WAL → garantisce scrittura su disco SQLite
                try {
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                } catch (_: Exception) {}

                sendBroadcast(Intent("com.ifs.stoppai.CALL_LOGGED"))

                android.util.Log.d("STOPPAI_FCM", "ARIA salvato e persistito: numero=$numeroNorm callLogId=$callLogId testo=${testo.take(50)}")
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_FCM", "Errore salvataggio DB SA-118: ${e.message}", e)
            }
        }

        // Risolvi nome contatto dalla rubrica per la notifica
        val nomeContatto = CallLogHelper.getContactName(applicationContext, PhoneNumberUtils.normalizeNumber(numeroRaw))
        val displayCaller = if (!nomeContatto.isNullOrBlank()) "$nomeContatto ($numeroRaw)" else numeroRaw

        mostraNotifica(displayCaller, testo)
    }

    /**
     * Mostra la notifica nel sistema Android
     */
    private fun mostraNotifica(numero: String, testo: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Creazione canale (Necessario per API >= 26)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifiche trascrizioni ARIA"
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("apri_aria", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notifica = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_logo)
            .setContentTitle("📞 Messaggio da $numero")
            .setContentText(testo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(testo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notifica)
    }

    /**
     * Invia il token FCM al server Hetzner per il database del backend
     */
    private fun inviaTokenAlServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Leggi email e tester_id dalle preferenze per associare il token al tester giusto
                val prefs = getSharedPreferences("stoppai_prefs", MODE_PRIVATE)
                val email = prefs.getString("tester_email", "") ?: ""
                val testerId = prefs.getInt("tester_id", 0)

                val url = URL("$SERVER_URL/api/tester/fcm-token")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val body = """{"token":"$token","email":"$email","tester_id":$testerId}"""
                conn.outputStream.write(body.toByteArray())

                val responseCode = conn.responseCode
                android.util.Log.d("FCM", "Token inviato al server ($responseCode) email=$email TesterId=$testerId")
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Invio token fallito: ${e.message}")
            }
        }
    }
}
