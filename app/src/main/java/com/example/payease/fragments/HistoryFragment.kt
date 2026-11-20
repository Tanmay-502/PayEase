package com.example.payease.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.payease.R
import com.example.payease.adapters.TransactionAdapter
import com.example.payease.database.WalletDatabase
import com.example.payease.models.Transaction
import com.example.payease.models.TransactionType
import com.example.payease.utils.CurrencyUtils
import com.example.payease.utils.SessionManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HistoryFragment : Fragment() {

    private lateinit var tvTotalCredit: TextView
    private lateinit var tvTotalDebit: TextView
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipCredits: Chip
    private lateinit var chipDebits: Chip
    private lateinit var rvAllTransactions: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: TransactionAdapter
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        db = WalletDatabase(context)
        sessionManager = SessionManager(context)

        tvTotalCredit = view.findViewById(R.id.tvTotalCredit)
        tvTotalDebit = view.findViewById(R.id.tvTotalDebit)
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter)
        chipAll = view.findViewById(R.id.chipAll)
        chipCredits = view.findViewById(R.id.chipCredits)
        chipDebits = view.findViewById(R.id.chipDebits)
        rvAllTransactions = view.findViewById(R.id.rvAllTransactions)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        rvAllTransactions.layoutManager = LinearLayoutManager(context)
        adapter = TransactionAdapter(emptyList())
        rvAllTransactions.adapter = adapter

        chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyFilter()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }

    private fun loadHistoryData() {
        val userId = sessionManager.userId
        if (userId == -1L) return

        val (creditPaise, debitPaise) = db.getTransactionSummary(userId)
        tvTotalCredit.text = "+${CurrencyUtils.formatPaiseToRupees(creditPaise)}"
        tvTotalDebit.text = "-${CurrencyUtils.formatPaiseToRupees(debitPaise)}"

        allTransactions = db.getTransactionsForUser(userId)
        applyFilter()
    }

    private fun applyFilter() {
        val filteredList = when (chipGroupFilter.checkedChipId) {
            R.id.chipCredits -> allTransactions.filter { it.type == TransactionType.CREDIT }
            R.id.chipDebits -> allTransactions.filter { it.type == TransactionType.DEBIT }
            else -> allTransactions
        }

        if (filteredList.isEmpty()) {
            rvAllTransactions.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvAllTransactions.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            adapter.updateData(filteredList)
        }
    }
}
