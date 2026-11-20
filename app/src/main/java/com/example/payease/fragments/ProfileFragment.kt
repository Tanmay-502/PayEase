package com.example.payease.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.payease.R
import com.example.payease.activities.LoginActivity
import com.example.payease.database.WalletDatabase
import com.example.payease.utils.AnimationUtils
import com.example.payease.utils.CurrencyUtils
import com.example.payease.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

class ProfileFragment : Fragment() {

    private lateinit var tvAvatarBig: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileMobile: TextView
    private lateinit var tvProfileBalance: TextView
    private lateinit var switchHideBalance: SwitchMaterial
    private lateinit var btnLogout: MaterialButton

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        db = WalletDatabase(context)
        sessionManager = SessionManager(context)

        tvAvatarBig = view.findViewById(R.id.tvAvatarBig)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail)
        tvProfileMobile = view.findViewById(R.id.tvProfileMobile)
        tvProfileBalance = view.findViewById(R.id.tvProfileBalance)
        switchHideBalance = view.findViewById(R.id.switchHideBalance)
        btnLogout = view.findViewById(R.id.btnLogout)

        AnimationUtils.applyTouchScale(btnLogout)

        switchHideBalance.isChecked = !sessionManager.isBalanceVisible
        switchHideBalance.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.isBalanceVisible = !isChecked
        }

        loadUserData()

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val name = sessionManager.userName
        tvProfileName.text = name
        tvProfileEmail.text = sessionManager.userEmail
        tvProfileMobile.text = "+91 ${sessionManager.userMobile}"
        tvAvatarBig.text = getInitials(name)

        val balancePaise = db.getWalletBalancePaise(sessionManager.userId)
        tvProfileBalance.text = CurrencyUtils.formatPaiseToRupees(balancePaise)
    }

    private fun showLogoutConfirmation() {
        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Logout Account")
            .setMessage("Are you sure you want to logout from PayEase?")
            .setPositiveButton("Logout") { _, _ ->
                sessionManager.logout()
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() && parts[0].isNotEmpty() -> "${parts[0].first().uppercaseChar()}"
            else -> "U"
        }
    }
}
