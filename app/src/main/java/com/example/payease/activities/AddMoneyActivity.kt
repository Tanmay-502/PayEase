package com.example.payease.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class AddMoneyActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvCurrentBalance: TextView
    private lateinit var tilAmount: TextInputLayout
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnChip500: MaterialButton
    private lateinit var btnChip1000: MaterialButton
    private lateinit var btnChip2000: MaterialButton
    private lateinit var btnChip5000: MaterialButton
    private lateinit var rgPaymentSource: RadioGroup
    private lateinit var btnAddMoneySubmit: MaterialButton

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_money)

        db = WalletDatabase(this)
        sessionManager = SessionManager(this)

        btnBack = findViewById(R.id.btnBack)
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance)
        tilAmount = findViewById(R.id.tilAmount)
        etAmount = findViewById(R.id.etAmount)

        btnChip500 = findViewById(R.id.btnChip500)
        btnChip1000 = findViewById(R.id.btnChip1000)
        btnChip2000 = findViewById(R.id.btnChip2000)
        btnChip5000 = findViewById(R.id.btnChip5000)

        rgPaymentSource = findViewById(R.id.rgPaymentSource)
        btnAddMoneySubmit = findViewById(R.id.btnAddMoneySubmit)

        btnBack.setOnClickListener { finish() }

        loadBalance()

        AnimationUtils.applyTouchScale(btnChip500)
        AnimationUtils.applyTouchScale(btnChip1000)
        AnimationUtils.applyTouchScale(btnChip2000)
        AnimationUtils.applyTouchScale(btnChip5000)
        AnimationUtils.applyTouchScale(btnAddMoneySubmit)

        btnChip500.setOnClickListener { setAmount("500") }
        btnChip1000.setOnClickListener { setAmount("1000") }
        btnChip2000.setOnClickListener { setAmount("2000") }
        btnChip5000.setOnClickListener { setAmount("5000") }

        btnAddMoneySubmit.setOnClickListener {
            handleTopUp()
        }
    }

    private fun loadBalance() {
        val balancePaise = db.getWalletBalancePaise(sessionManager.userId)
        tvCurrentBalance.text = CurrencyUtils.formatPaiseToRupees(balancePaise)
    }

    private fun setAmount(valStr: String) {
        val currentStr = etAmount.text.toString().trim()
        val currentVal = currentStr.toDoubleOrNull() ?: 0.0
        val addedVal = valStr.toDoubleOrNull() ?: 0.0
        val newVal = currentVal + addedVal
        etAmount.setText(String.format(Locale.US, "%.2f", newVal))
        tilAmount.error = null
    }

    private fun handleTopUp() {
        val amountStr = etAmount.text.toString().trim()
        tilAmount.error = null

        val amountPaise = CurrencyUtils.rupeesToPaise(amountStr)
        if (amountPaise <= 0) {
            tilAmount.error = "Enter a valid amount greater than zero"
            return
        }

        val selectedRbId = rgPaymentSource.checkedRadioButtonId
        val selectedRb = findViewById<RadioButton>(selectedRbId)
        val source = selectedRb?.text?.toString() ?: "UPI"

        val formattedAmount = CurrencyUtils.formatPaiseToRupees(amountPaise)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Top-Up")
            .setMessage("Are you sure you want to add $formattedAmount to your PayEase wallet using $source?")
            .setPositiveButton("Confirm & Add") { _, _ ->
                processTopUp(amountPaise, source, formattedAmount)
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun processTopUp(amountPaise: Long, source: String, formattedAmount: String) {
        val (success, resultMessage) = db.addMoney(sessionManager.userId, amountPaise, source)

        if (success) {
            showSuccessDialog(formattedAmount, source, resultMessage)
        } else {
            Toast.makeText(this, "Failed: $resultMessage", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccessDialog(amountStr: String, source: String, refId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_success, null)
        val ivSuccessCheck = dialogView.findViewById<ImageView>(R.id.ivSuccessCheck)
        val tvSuccessTitle = dialogView.findViewById<TextView>(R.id.tvSuccessTitle)
        val tvSuccessAmount = dialogView.findViewById<TextView>(R.id.tvSuccessAmount)
        val tvSuccessParty = dialogView.findViewById<TextView>(R.id.tvSuccessParty)
        val tvSuccessRefId = dialogView.findViewById<TextView>(R.id.tvSuccessRefId)
        val btnCopyRef = dialogView.findViewById<MaterialButton>(R.id.btnCopyRef)
        val btnShareReceipt = dialogView.findViewById<MaterialButton>(R.id.btnShareReceipt)
        val btnDone = dialogView.findViewById<MaterialButton>(R.id.btnDone)

        tvSuccessTitle.text = "Money Added Successfully!"
        tvSuccessAmount.text = "+$amountStr"
        tvSuccessParty.text = source
        tvSuccessRefId.text = refId

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
                putExtra(Intent.EXTRA_SUBJECT, "PayEase Receipt - $refId")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "PayEase Wallet Receipt\nTop-Up Amount: +$amountStr\nSource: $source\nReference ID: $refId\nStatus: COMPLETED"
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
