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
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val txtSmsSent: TextView = view.findViewById(R.id.txt_sms_sent)
        val txtSmsReplied: TextView = view.findViewById(R.id.txt_sms_replied)
        val iconAria: TextView = view.findViewById(R.id.txt_aria_indicator)
        val iconNote: TextView = view.findViewById(R.id.txt_note_indicator)
        val imgDirection: android.widget.ImageView = view.findViewById(R.id.img_direction)
        private var jobAria: Job? = null
        
        fun bind(item: CallLogCrmItem, onClick: (CallLogCrmItem) -> Unit) {
            val entry = item.entry
            val baseName = if (entry.displayName.isNotEmpty()) entry.displayName else entry.phoneNumber
            nameNumber.text = baseName

            val isContact = entry.displayName.isNotEmpty()

            // DIREZIONE & COLORI (SA-109)
            if (entry.callDirection == "USCITA") {
                imgDirection.setImageResource(R.drawable.ic_arrow_up)
                imgDirection.setColorFilter(android.graphics.Color.parseColor("#4CAF50")) // VERDE
            } else {
                imgDirection.setImageResource(R.drawable.ic_arrow_down)
                when {
                    entry.callType == "MANCATA" || entry.callType == "DEVIATA" -> {
                        imgDirection.setColorFilter(android.graphics.Color.GRAY)
                    }
                    isContact -> {
                        imgDirection.setColorFilter(android.graphics.Color.parseColor("#4CAF50")) // VERDE
                    }
                    else -> {
                        imgDirection.setColorFilter(android.graphics.Color.RED)
                    }
                }
            }

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

            // PALLINO (SA-109)
            if (isContact) {
                badgeStatus.setBackgroundResource(R.drawable.shape_badge_status_green)
            } else {
                badgeStatus.setBackgroundResource(R.drawable.shape_badge_status_red)
            }

            // INDICATORE NOTE (SA-110)
            iconNote.visibility = if (entry.nota.isNotEmpty()) View.VISIBLE else View.GONE
            root.setOnClickListener { onClick(item) }

            // INDICATORE ARIA IN TEMPO REALE (SA-106)
            jobAria?.cancel()
            jobAria = CoroutineScope(Dispatchers.IO).launch {
                val db = StoppAiDatabase.getInstance(itemView.context)
                db.ariaMessaggioDao().getPerCallLogId(entry.id).collectLatest { list ->
                    withContext(Dispatchers.Main) {
                        iconAria.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
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
