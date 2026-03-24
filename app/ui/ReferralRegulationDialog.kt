// FILE: ReferralRegulationDialog.kt
// SCOPO: Dialog informativo Partners (SA-056)
// DIPENDENZE: fragment_referral_regulation.xml
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ifs.stoppai.R

class ReferralRegulationDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        getSavedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_referral_regulation, container, false)
        
        root.findViewById<Button>(R.id.btn_close_reg).setOnClickListener {
            dismiss()
        }
        
        return root
    }
}
