// FILE: CallsFragment.kt
package com.ifs.stoppai.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ifs.stoppai.R
import com.ifs.stoppai.db.StoppAiDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CallsFragment : Fragment(R.layout.fragment_calls) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.ID_CALLS_001)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CallLogAdapter()
        recycler.adapter = adapter

        lifecycleScope.launch {
            val db = StoppAiDatabase.getInstance(requireContext())
            db.callLogDao().getAllCalls().collectLatest { logs ->
                adapter.submitList(logs)
            }
        }
    }
}
