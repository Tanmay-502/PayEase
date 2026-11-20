package com.example.payease.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.payease.R
import com.example.payease.models.Transaction
import com.example.payease.models.TransactionType
import com.example.payease.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onItemClick: ((Transaction) -> Unit)? = null
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun updateData(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        private val ivTxnTypeIcon: ImageView = itemView.findViewById(R.id.ivTxnTypeIcon)
        private val tvRecipient: TextView = itemView.findViewById(R.id.tvRecipient)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvRefAndDate: TextView = itemView.findViewById(R.id.tvRefAndDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(txn: Transaction) {
            tvRecipient.text = txn.recipientName
            tvDescription.text = if (txn.description.isNotBlank()) txn.description else "No description"

            val formattedDate = dateFormat.format(Date(txn.timestamp))
            tvRefAndDate.text = "${txn.referenceId} • $formattedDate"

            val formattedAmount = CurrencyUtils.formatPaiseToRupees(txn.amountPaise)

            val roundedBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f
            }

            if (txn.type == TransactionType.CREDIT) {
                ivTxnTypeIcon.setImageResource(R.drawable.ic_arrow_credit)
                tvAmount.text = "+$formattedAmount"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.credit_green))
                roundedBg.setColor(Color.parseColor("#E8F8F3"))
            } else {
                ivTxnTypeIcon.setImageResource(R.drawable.ic_arrow_debit)
                tvAmount.text = "-$formattedAmount"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.debit_red))
                roundedBg.setColor(Color.parseColor("#FDE8E8"))
            }
            iconContainer.background = roundedBg

            itemView.setOnClickListener {
                onItemClick?.invoke(txn)
            }
        }
    }
}
