// FILE: CallLogAdapter.kt
// SCOPO: Adattatore per RecyclerView CRM (v1.0)
// DIPENDENZE: CallLogEntry.kt, item_call_log.xml
// ULTIMA MODIFICA: 2026-03-23

package com.ifs.stoppai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ifs.stoppai.db.CallLogCrmItem
import com.ifs.stoppai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogAdapter(
    private val onClick: (CallLogCrmItem) -> Unit
) : ListAdapter<CallLogCrmItem, CallLogAdapter.CallViewHolder>(DiffCallback) {

    class CallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconType: TextView = view.findViewById(R.id.txt_icon_type)
        val nameNumber: TextView = view.findViewById(R.id.txt_name_number)
        val timeLabel: TextView = view.findViewById(R.id.txt_time)
        val badgeStatus: View = view.findViewById(R.id.view_status_badge)
        val root: View = view.findViewById(R.id.root_item_log)
        val txtDirection: TextView = view.findViewById(R.id.txt_direction)
        val txtSmsSent: TextView = view.findViewById(R.id.txt_sms_sent)
        val txtSmsReplied: TextView = view.findViewById(R.id.txt_sms_replied)
        
        fun bind(item: CallLogCrmItem, onClick: (CallLogCrmItem) -> Unit) {
            val entry = item.entry
            val baseName = if (entry.displayName.isNotEmpty()) entry.displayName else entry.phoneNumber
            nameNumber.text = "$baseName (${item.count})"

            // DIREZIONE
            txtDirection.text = if (entry.callDirection == "USCITA") "↗" else "↙"
            txtDirection.setTextColor(if (entry.callDirection == "USCITA") android.graphics.Color.GRAY else android.graphics.Color.parseColor("#4CAF50"))

            // STATO SMS
            txtSmsSent.visibility = if (entry.smsInviato) View.VISIBLE else View.GONE
            txtSmsReplied.visibility = if (!entry.smsRisposta.isNullOrBlank()) View.VISIBLE else View.GONE

            iconType.text = when {
                entry.phoneNumber.isEmpty() || entry.phoneNumber.contains("nascosto", true) -> "🕵️"
                entry.phoneNumber.length < 8 -> "☎️"
                else -> "📱"
            }

            val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
            timeLabel.text = sdf.format(Date(entry.timestamp))

            if (entry.nota.isNotEmpty()) {
                badgeStatus.setBackgroundResource(R.drawable.shape_badge_status_blue)
            } else {
                when (entry.statusId) {
                    1 -> badgeStatus.setBackgroundResource(R.drawable.shape_badge_status_green)
                    2 -> badgeStatus.setBackgroundResource(R.drawable.shape_badge_status_gray)
                    else -> badgeStatus.setBackgroundResource(R.drawable.shape_badge_status_red)
                }
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_call_log, parent, false)
        return CallViewHolder(v)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CallLogCrmItem>() {
            override fun areItemsTheSame(oldItem: CallLogCrmItem, newItem: CallLogCrmItem) = oldItem.entry.id == newItem.entry.id
            override fun areContentsTheSame(oldItem: CallLogCrmItem, newItem: CallLogCrmItem) = oldItem == newItem
        }
    }
}
