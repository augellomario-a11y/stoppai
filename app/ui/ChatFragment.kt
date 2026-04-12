// FILE: ChatFragment.kt
// SCOPO: Chat assistenza con backend StoppAI (SA-121)
package com.ifs.stoppai.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ifs.stoppai.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

class ChatFragment : Fragment(R.layout.fragment_chat) {

    companion object {
        const val BACKEND_URL = "https://stoppai.it"
        const val POLL_INTERVAL = 3000L
    }

    private lateinit var rvChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private val messages = mutableListOf<ChatMsg>()
    private lateinit var adapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var polling = false

    data class ChatMsg(val testo: String, val mittente: String, val timestamp: String)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verifica piano: Chat richiede PRO
        if (!com.ifs.stoppai.core.PlanManager.isDisponibile(requireContext(), com.ifs.stoppai.core.PlanManager.Feature.CHAT_SUPPORTO)) {
            view.findViewById<android.view.View>(R.id.rv_chat)?.visibility = android.view.View.GONE
            view.findViewById<android.view.View>(R.id.edt_message)?.visibility = android.view.View.GONE
            view.findViewById<android.view.View>(R.id.btn_send)?.visibility = android.view.View.GONE
            val lock = android.widget.TextView(requireContext()).apply {
                text = "🔒 La chat con il team è disponibile con il piano PRO\n\nTocca qui per scoprire i piani"
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                gravity = android.view.Gravity.CENTER
                setPadding(40, 100, 40, 100)
                setOnClickListener {
                    com.ifs.stoppai.core.UpgradeDialog.show(requireContext(), com.ifs.stoppai.core.PlanManager.Feature.CHAT_SUPPORTO)
                }
            }
            (view as? android.view.ViewGroup)?.addView(lock, 0)
            return
        }

        rvChat = view.findViewById(R.id.rv_chat)
        edtMessage = view.findViewById(R.id.edt_message)
        btnSend = view.findViewById(R.id.btn_send)

        adapter = ChatAdapter()
        rvChat.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvChat.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }

        loadMessages()
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    private fun startPolling() {
        polling = true
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!polling || !isAdded) return
                loadMessages()
                handler.postDelayed(this, POLL_INTERVAL)
            }
        }, POLL_INTERVAL)
    }

    private fun stopPolling() {
        polling = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun getTesterId(): Int {
        val prefs = requireContext().getSharedPreferences("stoppai_prefs", 0)
        return prefs.getInt("tester_id", -1)
    }

    private fun loadMessages() {
        val testerId = getTesterId()
        if (testerId == -1) {
            if (messages.isEmpty()) {
                messages.add(ChatMsg("Per attivare la chat, accedi con la tua email.", "admin", ""))
                adapter.notifyDataSetChanged()
            }
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tester/$testerId/messaggi")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val arr = JSONArray(response)
                val msgs = mutableListOf<ChatMsg>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    msgs.add(ChatMsg(
                        obj.getString("testo"),
                        obj.getString("mittente"),
                        obj.optString("timestamp", "")
                    ))
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    val wasAtBottom = !rvChat.canScrollVertically(1)
                    val changed = msgs.size != messages.size
                    messages.clear()
                    messages.addAll(msgs)
                    if (messages.isEmpty()) {
                        messages.add(ChatMsg("Ciao! Come possiamo aiutarti?", "admin", ""))
                    }
                    adapter.notifyDataSetChanged()
                    if (changed && wasAtBottom) {
                        rvChat.scrollToPosition(messages.size - 1)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun sendMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return
        edtMessage.setText("")

        val testerId = getTesterId()
        if (testerId == -1) {
            Toast.makeText(requireContext(), "Accedi per usare la chat", Toast.LENGTH_SHORT).show()
            return
        }

        messages.add(ChatMsg(text, "tester", "adesso"))
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.scrollToPosition(messages.size - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tester/$testerId/messaggi")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
                conn.outputStream.write("""{"testo":"$escaped","mittente":"tester"}""".toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("STOPPAI_CHAT", "Errore invio: $e")
            }
        }
    }

    inner class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MsgViewHolder>() {
        inner class MsgViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val bubble: LinearLayout = view.findViewById(R.id.msg_bubble)
            val txtText: TextView = view.findViewById(R.id.txt_msg_text)
            val txtTime: TextView = view.findViewById(R.id.txt_msg_time)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return MsgViewHolder(v)
        }

        override fun onBindViewHolder(holder: MsgViewHolder, position: Int) {
            val msg = messages[position]
            holder.txtText.text = msg.testo

            val ts = msg.timestamp
            holder.txtTime.text = if (ts.length >= 16) ts.substring(11, 16) else if (ts == "adesso") "adesso" else ""

            val params = holder.bubble.layoutParams as FrameLayout.LayoutParams
            val bg = GradientDrawable()

            if (msg.mittente == "tester") {
                params.gravity = Gravity.END
                bg.cornerRadii = floatArrayOf(36f,36f, 36f,36f, 8f,8f, 36f,36f)
                bg.setColor(Color.parseColor("#c8a96e"))
                holder.txtText.setTextColor(Color.parseColor("#0a0a0f"))
                holder.txtTime.setTextColor(Color.parseColor("#8a7540"))
            } else {
                params.gravity = Gravity.START
                bg.cornerRadii = floatArrayOf(36f,36f, 36f,36f, 36f,36f, 8f,8f)
                bg.setColor(Color.parseColor("#EEEEEE"))
                holder.txtText.setTextColor(Color.parseColor("#1a1a1a"))
                holder.txtTime.setTextColor(Color.parseColor("#999999"))
            }

            holder.bubble.layoutParams = params
            holder.bubble.background = bg
            holder.bubble.elevation = 2f
        }

        override fun getItemCount() = messages.size
    }
}
