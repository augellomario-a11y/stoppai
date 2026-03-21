// FILE: ContactCacheManager.kt
// SCOPO: Cache in memoria contatti + preferiti per lookup rapido
// DIPENDENZE: PhoneNumberUtils.kt
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.core

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.HashSet

object ContactCacheManager {
    val cache = HashSet<String>()
    private var favoritesSet = HashSet<String>()
    private var isObserverRegistered = false

    // Carica tutti i contatti in modo sincrono
    fun loadContactsSync(context: Context) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val appCtx = context.applicationContext
        val tempSet = HashSet<String>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER)

        appCtx.contentResolver.query(
            uri, projection, null, null, null
        )?.use { cursor ->
            val numIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (numIndex >= 0) {
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numIndex)
                    if (number != null) {
                        val normalized =
                            PhoneNumberUtils.normalizeNumber(number)
                        if (normalized.isNotEmpty()) {
                            tempSet.add(normalized)
                        }
                    }
                }
            }
        }

        synchronized(cache) {
            cache.clear()
            cache.addAll(tempSet)
        }
        android.util.Log.e("STOPPAI_CACHE",
            "Caricati ${tempSet.size} contatti (sincrono)")

        // Carica anche i preferiti
        loadFavorites(appCtx)
    }

    // Carica contatti preferiti (stellina)
    fun loadFavorites(context: Context) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val tempSet = HashSet<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.STARRED} = 1",
            null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val phones = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = $id",
                    null, null)
                phones?.use { p ->
                    while (p.moveToNext()) {
                        val n = p.getString(0)
                        if (n != null) {
                            tempSet.add(
                                PhoneNumberUtils.normalizeNumber(n))
                        }
                    }
                }
            }
        }

        synchronized(favoritesSet) {
            favoritesSet = tempSet
        }
        android.util.Log.e("STOPPAI_CACHE",
            "Caricati ${tempSet.size} preferiti")
    }

    fun getSize(): Int = synchronized(cache) { cache.size }

    // Inizializza observer rubrica e sync periodico
    fun startSync(context: Context) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val appCtx = context.applicationContext
        if (!isObserverRegistered) {
            val observer = object : ContentObserver(
                Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    loadContactsSync(appCtx)
                }
            }
            appCtx.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true, observer)
            isObserverRegistered = true
        }

        // Timer di refresh forzato ogni 15 minuti
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(15L * 60L * 1000L)
                loadContactsSync(appCtx)
            }
        }
    }

    // Verifica immediata su cache ram (Zero I/O)
    fun isContact(number: String): Boolean {
        val normalized = PhoneNumberUtils.normalizeNumber(number)
        synchronized(cache) {
            return cache.contains(normalized)
        }
    }

    // Verifica se è un preferito (stellina)
    fun isFavorite(number: String): Boolean {
        val normalized = PhoneNumberUtils.normalizeNumber(number)
        synchronized(favoritesSet) {
            return favoritesSet.contains(normalized)
        }
    }
}
