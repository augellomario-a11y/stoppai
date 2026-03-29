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
import com.ifs.stoppai.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

/**
 * Servizio per la gestione delle notifiche Firebase Cloud Messaging
 */
class AriaFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "aria_messaggi"
        const val CHANNEL_NAME = "Messaggi ARIA"
        const val SERVER_URL = "http://46.225.14.90:8085"
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

        val numero = message.data["numero"]
            ?: message.notification?.title
            ?: "Sconosciuto"
        val testo = message.data["testo"]
            ?: message.notification?.body
            ?: "Nuovo messaggio"

        mostraNotifica(numero, testo)
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
                val url = URL("$SERVER_URL/fcm-token")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                
                val body = """{"token":"$token"}"""
                conn.outputStream.write(body.toByteArray())
                
                val responseCode = conn.responseCode
                println("FCM: Token inviato al server ($responseCode)")
                conn.disconnect()
            } catch (e: Exception) {
                println("FCM ERROR: Invio token fallito: $e")
            }
        }
    }
}
