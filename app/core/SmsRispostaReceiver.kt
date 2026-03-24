// FILE: SmsRispostaReceiver.kt
// SCOPO: Intercettazione risposte SMS per il CRM (SA-057)
// DIPENDENZE: StoppAiDatabase, NotificationManager
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.ifs.stoppai.R
import com.ifs.stoppai.db.StoppAiDatabase
import com.ifs.stoppai.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsRispostaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: continue
            val body = sms.displayMessageBody
            val normalizedSender = PhoneNumberUtils.normalizeNumber(sender)
            
            checkAndSaveResponse(context, normalizedSender, body)
        }
    }

    private fun checkAndSaveResponse(context: Context, sender: String, body: String) {
        val db = StoppAiDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            // Cerchiamo l'ultimo log per questo numero con SMS inviato
            val lastEntry = db.callLogDao().getLastEntryForNumber(sender)
            if (lastEntry != null && lastEntry.smsInviato) {
                // Salviamo la risposta
                db.callLogDao().updateSmsRisposta(lastEntry.id, body)
                // Notifichiamo l'utente
                showNotification(context, sender, body)
            }
        }
    }

    private fun showNotification(context: Context, sender: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sms_assistant"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Assistente SMS", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_logo) // Usiamo shield logo come icone
            .setContentTitle("💬 Risposta ricevuta")
            .setContentText("$sender: ${body.take(50)}...")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
