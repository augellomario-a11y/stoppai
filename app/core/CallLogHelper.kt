// FILE: CallLogHelper.kt
// SCOPO: Gestione registrazione chiamate nel DB locale (SA-063)
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.core

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.ifs.stoppai.db.CallLogEntry
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CallLogHelper {

    /**
     * Registra una chiamata nel database in background
     */
    fun saveCallLog(context: Context, rawNumber: String, outcome: String, smsSent: Boolean) {
        if (rawNumber.isBlank()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val displayName = getContactName(context, rawNumber)
            val logNumber = rawNumber.ifEmpty { "Numero nascosto" }
            val db = StoppAiDatabase.getInstance(context)
            
            val entry = CallLogEntry(
                phoneNumber = logNumber,
                callType = NumberClassifier.getTipoNumero(rawNumber),
                timestamp = System.currentTimeMillis(),
                callOutcome = outcome,
                displayName = displayName,
                smsInviato = smsSent,
                callDirection = "ENTRATA"
            )
            
            db.callLogDao().insertCallLog(entry)
            
            // INVIA BROADCAST PER AGGIORNAMENTO UI (SA-066)
            val intent = android.content.Intent("com.ifs.stoppai.CALL_LOGGED")
            context.sendBroadcast(intent)
        }
    }

    fun getContactName(context: Context, phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return try {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else ""
            } ?: ""
        } catch (e: Exception) { "" }
    }
}
