package com.example.payease.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.payease.R
import com.example.payease.database.WalletDatabase
import com.example.payease.utils.AnimationUtils
import com.example.payease.utils.CurrencyUtils
import com.example.payease.utils.SessionManager
import com.example.payease.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge

class SendMoneyActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvAvailableBalance: TextView

    private lateinit var tilRecipientName: TextInputLayout
    private lateinit var tilRecipientMobile: TextInputLayout
    private lateinit var tilAmount: TextInputLayout
    private lateinit var tilDescription: TextInputLayout

    private lateinit var etRecipientName: TextInputEditText
    private lateinit var etRecipientMobile: TextInputEditText
    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText

    private lateinit var btnSendMoneySubmit: MaterialButton

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager

    private var availableBalancePaise: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_send_money)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = WalletDatabase(this)
        sessionManager = SessionManager(this)

        btnBack = findViewById(R.id.btnBack)
        tvAvailableBalance = findViewById(R.id.tvAvailableBalance)

        tilRecipientName = findViewById(R.id.tilRecipientName)
        tilRecipientMobile = findViewById(R.id.tilRecipientMobile)
        tilAmount = findViewById(R.id.tilAmount)
        tilDescription = findViewById(R.id.tilDescription)

        etRecipientName = findViewById(R.id.etRecipientName)
        etRecipientMobile = findViewById(R.id.etRecipientMobile)
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)

        btnSendMoneySubmit = findViewById(R.id.btnSendMoneySubmit)

        btnBack.setOnClickListener { finish() }

        loadBalance()

        AnimationUtils.applyTouchScale(btnSendMoneySubmit)

        var lastClickTime = 0L
        btnSendMoneySubmit.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClickTime < 1000) return@setOnClickListener
            lastClickTime = now
            handleTransfer()
        }
    }

    private fun loadBalance() {
        availableBalancePaise = db.getWalletBalancePaise(sessionManager.userId)
        tvAvailableBalance.text = CurrencyUtils.formatPaiseToRupees(availableBalancePaise)
    }

    private fun handleTransfer() {
        val name = etRecipientName.text.toString().trim()
        val mobile = etRecipientMobile.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val note = etDescription.text.toString().trim()

        tilRecipientName.error = null
        tilRecipientMobile.error = null
        tilAmount.error = null

        if (name.isEmpty()) {
            tilRecipientName.error = "Recipient name is required"
            return
        }

        if (!ValidationUtils.isValidRecipientIdentifier(mobile)) {
            tilRecipientMobile.error = "Enter a valid 10-digit mobile number or UPI ID (e.g. name@upi)"
            return
        }

        val amountPaise = CurrencyUtils.rupeesToPaise(amountStr)
        if (amountPaise <= 0) {
            tilAmount.error = "Enter a valid amount greater than zero"
            return
        }

        if (amountPaise > availableBalancePaise) {
            val balanceStr = CurrencyUtils.formatPaiseToRupees(availableBalancePaise)
            tilAmount.error = "Insufficient balance! Your available balance is $balanceStr"
            return
        }

        val formattedAmount = CurrencyUtils.formatPaiseToRupees(amountPaise)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Payment")
            .setMessage("Are you sure you want to send $formattedAmount to $name ($mobile)?")
            .setPositiveButton("Pay Now") { _, _ ->
                processTransfer(name, mobile, amountPaise, note, formattedAmount)
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun processTransfer(
        recipientName: String,
        recipientMobile: String,
        amountPaise: Long,
        note: String,
        formattedAmount: String
    ) {
        val (success, resultMessage) = db.sendMoney(
            userId = sessionManager.userId,
            recipientName = recipientName,
            recipientMobile = recipientMobile,
            amountPaise = amountPaise,
            description = note
        )

        if (success) {
            showSuccessDialog(formattedAmount, recipientName, resultMessage, isDebit = true)
        } else {
            Toast.makeText(this, "Transfer Failed: $resultMessage", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccessDialog(amountStr: String, recipientName: String, refId: String, isDebit: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_success, null)
        val ivSuccessCheck = dialogView.findViewById<ImageView>(R.id.ivSuccessCheck)
        val tvSuccessTitle = dialogView.findViewById<TextView>(R.id.tvSuccessTitle)
        val tvSuccessAmount = dialogView.findViewById<TextView>(R.id.tvSuccessAmount)
        val tvPartyLabel = dialogView.findViewById<TextView>(R.id.tvPartyLabel)
        val tvSuccessParty = dialogView.findViewById<TextView>(R.id.tvSuccessParty)
        val tvSuccessTxnType = dialogView.findViewById<TextView>(R.id.tvSuccessTxnType)
        val tvSuccessRefId = dialogView.findViewById<TextView>(R.id.tvSuccessRefId)
        val tvSuccessDateTime = dialogView.findViewById<TextView>(R.id.tvSuccessDateTime)
        val btnCopyRef = dialogView.findViewById<MaterialButton>(R.id.btnCopyRef)
        val btnShareReceipt = dialogView.findViewById<MaterialButton>(R.id.btnShareReceipt)
        val btnDone = dialogView.findViewById<MaterialButton>(R.id.btnDone)

        tvSuccessTitle.text = "Money Sent Successfully!"
        tvSuccessAmount.text = "-$amountStr"
        tvPartyLabel.text = "Recipient"
        tvSuccessParty.text = recipientName
        tvSuccessTxnType.text = "DEBIT (Transfer)"
        tvSuccessRefId.text = refId

        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        tvSuccessDateTime.text = nowStr

        AnimationUtils.animateViewScalePunch(ivSuccessCheck)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCopyRef.setOnClickListener {
            val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
            val clip = ClipData.newPlainText("Transaction Reference ID", refId)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, "Reference ID copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnShareReceipt.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "PayEase Payment Receipt - $refId")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "PayEase Wallet Transfer Receipt\nAmount Sent: -$amountStr\nRecipient: $recipientName\nReference ID: $refId\nDate: $nowStr\nStatus: COMPLETED (Simulated)"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share Receipt Via"))
        }

        btnDone.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}
