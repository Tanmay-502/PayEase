package com.example.payease.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.payease.MainActivity
import com.example.payease.R
import com.example.payease.activities.AddMoneyActivity
import com.example.payease.activities.SendMoneyActivity
import com.example.payease.adapters.TransactionAdapter
import com.example.payease.database.WalletDatabase
import com.example.payease.utils.AnimationUtils
import com.example.payease.utils.CurrencyUtils
import com.example.payease.utils.SessionManager
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAvatarInitials: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnToggleBalanceVisibility: ImageView
    private lateinit var btnAddMoney: MaterialButton
    private lateinit var btnSendMoney: MaterialButton
    private lateinit var tvViewAll: TextView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var transactionAdapter: TransactionAdapter
    private var actualBalancePaise: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        db = WalletDatabase(context)
        sessionManager = SessionManager(context)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials)
        tvBalance = view.findViewById(R.id.tvBalance)
        btnToggleBalanceVisibility = view.findViewById(R.id.btnToggleBalanceVisibility)
        btnAddMoney = view.findViewById(R.id.btnAddMoney)
        btnSendMoney = view.findViewById(R.id.btnSendMoney)
        tvViewAll = view.findViewById(R.id.tvViewAll)
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        val name = sessionManager.userName
        tvWelcome.text = "$name 👋"
        tvAvatarInitials.text = getInitials(name)

        rvRecentTransactions.layoutManager = LinearLayoutManager(context)
        transactionAdapter = TransactionAdapter(emptyList())
        rvRecentTransactions.adapter = transactionAdapter

        AnimationUtils.applyTouchScale(btnAddMoney)
        AnimationUtils.applyTouchScale(btnSendMoney)
        AnimationUtils.applyTouchScale(btnToggleBalanceVisibility)

        btnToggleBalanceVisibility.setOnClickListener {
            sessionManager.isBalanceVisible = !sessionManager.isBalanceVisible
            updateBalanceDisplay()
        }

        btnAddMoney.setOnClickListener {
            startActivity(Intent(context, AddMoneyActivity::class.java))
        }

        btnSendMoney.setOnClickListener {
            startActivity(Intent(context, SendMoneyActivity::class.java))
        }

        tvViewAll.setOnClickListener {
            (activity as? MainActivity)?.selectHistoryTab()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardData()
    }

    private fun refreshDashboardData() {
        val userId = sessionManager.userId
        if (userId == -1L) return

        actualBalancePaise = db.getWalletBalancePaise(userId)
        updateBalanceDisplay()

        val recentTxns = db.getTransactionsForUser(userId, limit = 5)
        if (recentTxns.isEmpty()) {
            rvRecentTransactions.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvRecentTransactions.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            transactionAdapter.updateData(recentTxns)
        }
    }

    private fun updateBalanceDisplay() {
        if (sessionManager.isBalanceVisible) {
            tvBalance.text = CurrencyUtils.formatPaiseToRupees(actualBalancePaise)
            btnToggleBalanceVisibility.setImageResource(R.drawable.ic_eye_visible)
        } else {
            tvBalance.text = "₹ ••••••"
            btnToggleBalanceVisibility.setImageResource(R.drawable.ic_eye_hidden)
        }
        AnimationUtils.animateViewScalePunch(tvBalance)
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
