// FILE: ChatFragment.kt
// SCOPO: Chat assistenza con backend StoppAI (SA-121)
package com.ifs.stoppai.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import org.json.JSONObject
import java.net.URL

class ChatFragment : Fragment(R.layout.fragment_chat) {

    companion object {
        const val BACKEND_URL = "http://46.225.14.90:6002"
    }

    private lateinit var rvChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private val messages = mutableListOf<ChatMsg>()
    private lateinit var adapter: ChatAdapter

    data class ChatMsg(val testo: String, val mittente: String, val timestamp: String)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvChat = view.findViewById(R.id.rv_chat)
        edtMessage = view.findViewById(R.id.edt_message)
        btnSend = view.findViewById(R.id.btn_send)

        adapter = ChatAdapter()
        rvChat.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvChat.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }

        loadMessages()
    }

    private fun getTesterId(): Int {
        val prefs = requireContext().getSharedPreferences("stoppai_prefs", 0)
        return prefs.getInt("tester_id", -1)
    }

    private fun loadMessages() {
        val testerId = getTesterId()
        if (testerId == -1) {
            messages.clear()
            messages.add(ChatMsg("Benvenuto nell'assistenza StoppAI! Per attivare la chat, registrati come tester.", "admin", ""))
            adapter.notifyDataSetChanged()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tester/$testerId/messaggi")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
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
                    messages.clear()
                    messages.addAll(msgs)
                    if (messages.isEmpty()) {
                        messages.add(ChatMsg("Ciao! Come possiamo aiutarti?", "admin", ""))
                    }
                    adapter.notifyDataSetChanged()
                    rvChat.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages.clear()
                    messages.add(ChatMsg("Connessione non disponibile. Riprova piu' tardi.", "admin", ""))
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun sendMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return
        edtMessage.setText("")

        val testerId = getTesterId()
        if (testerId == -1) {
            Toast.makeText(requireContext(), "Registrati come tester per usare la chat", Toast.LENGTH_SHORT).show()
            return
        }

        messages.add(ChatMsg(text, "tester", ""))
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.scrollToPosition(messages.size - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tester/$testerId/messaggi")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.write("""{"testo":"$text","mittente":"tester"}""".toByteArray())
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
            holder.txtTime.text = msg.timestamp.take(16).replace("T", " ")

            val params = holder.bubble.layoutParams as FrameLayout.LayoutParams
            val bg = GradientDrawable()
            bg.cornerRadius = 16f

            if (msg.mittente == "tester") {
                params.gravity = Gravity.END
                bg.setColor(Color.parseColor("#c8a96e"))
                holder.txtText.setTextColor(Color.parseColor("#0a0a0f"))
            } else {
                params.gravity = Gravity.START
                bg.setColor(Color.parseColor("#F0F0F0"))
                holder.txtText.setTextColor(Color.parseColor("#1a1a1a"))
            }
            holder.bubble.layoutParams = params
            holder.bubble.background = bg
        }

        override fun getItemCount() = messages.size
    }
}
