package com.example.payease.models

enum class TransactionType {
    CREDIT,
    DEBIT
}

data class Transaction(
    val transactionId: Long = 0,
    val referenceId: String,
    val userId: Long,
    val type: TransactionType,
    val recipientName: String,
    val amountPaise: Long,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
