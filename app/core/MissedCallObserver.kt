// FILE: MissedCallObserver.kt
// SCOPO: Monitoraggio delle chiamate perse da contatti (SA-103)
// ULTIMA MODIFICA: 2026-03-30

package com.ifs.stoppai.core

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog
import android.util.Log

class MissedCallObserver(private val context: Context, handler: Handler) : ContentObserver(handler) {
    
    // Iniziamo a tracciare dalla creazione
    private var lastQueryTime = System.currentTimeMillis() - 5000 // un piccolo buffer di ms

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        
        try {
            val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE)
            val selection = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?"
            val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString(), lastQueryTime.toString())
            val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 1"

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                    
                    if (numIndex != -1 && dateIndex != -1) {
                        val number = cursor.getString(numIndex) ?: ""
                        val date = cursor.getLong(dateIndex)
                        
                        // Aggiorniamo il timestamp per non processare due volte la stessa chiamata
                        lastQueryTime = date 
                        Log.d("STOPPAI", "Rilevata MISSED call da: $number")
                        
                        val normalizedNumber = PhoneNumberUtils.normalizeNumber(number)
                        
                        // Controlliamo se è in rubrica
                        if (ContactCacheManager.isContact(normalizedNumber)) {
                            Log.d("STOPPAI", "Il numero è in rubrica. Salvo in CRM come MANCATA.")
                            CallLogHelper.saveCallLog(context, normalizedNumber, "MANCATA", false)
                        } else {
                            Log.d("STOPPAI", "Numero non in rubrica. Ignoro (gestito dallo screening).")
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("STOPPAI", "Permesso READ_CALL_LOG mancante", e)
        } catch (e: Exception) {
            Log.e("STOPPAI", "Errore in MissedCallObserver", e)
        }
    }
}
