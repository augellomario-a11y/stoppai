// FILE: LoginFragment.kt
// SCOPO: Login Magic Link con auto-fill da notifica push (SA-122)
package com.ifs.stoppai.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ifs.stoppai.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class LoginFragment : Fragment(R.layout.fragment_login) {

    companion object {
        const val BACKEND_URL = "http://46.225.14.90:6002"
    }

    private var email = ""
    private var fcmToken = ""
    private var pollingActive = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stepEmail = view.findViewById<LinearLayout>(R.id.step_email)
        val stepCode = view.findViewById<LinearLayout>(R.id.step_code)
        val edtEmail = view.findViewById<EditText>(R.id.edt_email)
        val btnRequest = view.findViewById<Button>(R.id.btn_request_code)
        val btnVerify = view.findViewById<Button>(R.id.btn_verify_code)
        val txtError = view.findViewById<TextView>(R.id.txt_error)
        val txtSkip = view.findViewById<TextView>(R.id.txt_skip)

        // Recupera token FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) fcmToken = task.result
        }

        btnRequest.setOnClickListener {
            email = edtEmail.text.toString().trim()
            if (email.isEmpty() || !email.contains("@")) {
                showError("Inserisci un'email valida")
                return@setOnClickListener
            }
            btnRequest.isEnabled = false
            btnRequest.text = "INVIO IN CORSO..."
            richiediCodice()
        }

        btnVerify.setOnClickListener { verificaCodice() }

        txtSkip.setOnClickListener {
            // Continua senza login
            val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("login_skipped", true).apply()
            navigateToHome()
        }
    }

    override fun onPause() {
        super.onPause()
        pollingActive = false
    }

    private fun startPollingMagicCode() {
        pollingActive = true
        val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
        // Pulisci eventuali codici vecchi
        prefs.edit().remove("magic_code_pending").apply()

        fun poll() {
            if (!pollingActive || !isAdded) return
            val codice = prefs.getString("magic_code_pending", null)
            if (codice != null && codice.length == 6) {
                prefs.edit().remove("magic_code_pending").apply()
                android.util.Log.d("STOPPAI_LOGIN", "Magic code auto-ricevuto via polling: $codice")
                view?.findViewById<EditText>(R.id.edt_code)?.setText(codice)
                view?.findViewById<TextView>(R.id.txt_code_info)?.text = "Codice ricevuto automaticamente!"
                view?.postDelayed({ verificaCodice() }, 500)
            } else {
                view?.postDelayed({ poll() }, 1000) // Controlla ogni secondo
            }
        }
        poll()
    }

    private fun richiediCodice() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tester/auth/request")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.write("""{"email":"$email","fcm_token":"$fcmToken"}""".toByteArray())

                val code = conn.responseCode
                val body = if (code < 400) conn.inputStream.bufferedReader().readText()
                           else conn.errorStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)

                withContext(Dispatchers.Main) {
                    if (code == 200) {
                        view?.findViewById<LinearLayout>(R.id.step_email)?.visibility = View.GONE
                        view?.findViewById<LinearLayout>(R.id.step_code)?.visibility = View.VISIBLE
                        view?.findViewById<TextView>(R.id.txt_code_info)?.text =
                            "Codice inviato a $email\nAttendi la compilazione automatica..."
                        hideError()
                        startPollingMagicCode()
                    } else {
                        view?.findViewById<Button>(R.id.btn_request_code)?.isEnabled = true
                        view?.findViewById<Button>(R.id.btn_request_code)?.text = "INVIA CODICE"
                        showError(json.optString("error", "Errore sconosciuto"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view?.findViewById<Button>(R.id.btn_request_code)?.isEnabled = true
                    view?.findViewById<Button>(R.id.btn_request_code)?.text = "INVIA CODICE"
                    showError("Errore di connessione")
                }
            }
        }
    }

    private fun verificaCodice() {
        val codice = view?.findViewById<EditText>(R.id.edt_code)?.text.toString().trim()
        if (codice.length != 6) {
            showError("Inserisci il codice a 6 cifre")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tester/auth/verify")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.write("""{"email":"$email","codice":"$codice"}""".toByteArray())

                val code = conn.responseCode
                val body = if (code < 400) conn.inputStream.bufferedReader().readText()
                           else conn.errorStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)

                withContext(Dispatchers.Main) {
                    if (code == 200 && json.optBoolean("success")) {
                        // Salva sessione
                        val prefs = requireContext().getSharedPreferences("stoppai_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putInt("tester_id", json.getInt("tester_id"))
                            .putString("tester_email", json.getString("email"))
                            .putString("tester_nome", json.getString("nome"))
                            .putString("tester_piano", json.getString("piano"))
                            .putString("tester_stato", json.getString("stato"))
                            .putBoolean("logged_in", true)
                            .apply()

                        android.util.Log.d("STOPPAI_LOGIN", "Login OK: tester_id=${json.getInt("tester_id")} piano=${json.getString("piano")}")
                        navigateToHome()
                    } else {
                        showError(json.optString("error", "Codice non valido"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Errore di connessione")
                }
            }
        }
    }

    private fun navigateToHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
        // Riattiva bottom nav
        activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.let {
            it.visibility = View.VISIBLE
            it.selectedItemId = R.id.nav_home
        }
    }

    private fun showError(msg: String) {
        view?.findViewById<TextView>(R.id.txt_error)?.apply { text = msg; visibility = View.VISIBLE }
    }
    private fun hideError() {
        view?.findViewById<TextView>(R.id.txt_error)?.visibility = View.GONE
    }
}
