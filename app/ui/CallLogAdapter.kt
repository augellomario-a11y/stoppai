// FILE: CallLogAdapter.kt
// SCOPO: Adattatore per RecyclerView registro bloccate con gestione UX Timestamp e Stati (v0.5)
// DIPENDENZE: CallLogEntry.kt
// ULTIMA MODIFICA: 2026-03-21

package com.ifs.stoppai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ifs.stoppai.db.CallLogEntry
import com.ifs.stoppai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogAdapter : ListAdapter<CallLogEntry, CallLogAdapter.CallViewHolder>(DiffCallback) {

    class CallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberText: TextView = view.findViewById(R.id.txt_number)
        val dateText: TextView = view.findViewById(R.id.ID_LOG_002)
        val typeText: TextView = view.findViewById(R.id.txt_type)
        
        fun bind(entry: CallLogEntry) {
            numberText.text = entry.phoneNumber
            
            val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.ITALY)
            dateText.text = sdf.format(Date(entry.timestamp))

            val statusStr = when (entry.statusId) {
                0 -> "⏳ Da gestire"
                1 -> "✅ Whitelist"
                2 -> "🚫 Blacklist"
                3 -> "👁 Ignorato"
                else -> "❓ Sconosciuto"
            }
            typeText.text = "Tipo: ${entry.callType} | $statusStr"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_call_log, parent, false)
        return CallViewHolder(v)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CallLogEntry>() {
            override fun areItemsTheSame(oldItem: CallLogEntry, newItem: CallLogEntry) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: CallLogEntry, newItem: CallLogEntry) = oldItem == newItem
        }
    }
}
