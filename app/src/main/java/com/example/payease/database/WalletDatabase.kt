package com.example.payease.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.payease.models.Transaction
import com.example.payease.models.TransactionType
import com.example.payease.models.User
import com.example.payease.utils.ValidationUtils
import java.util.UUID

class WalletDatabase(
    context: Context,
    dbName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "payease_wallet.db"
        const val DATABASE_VERSION = 2

        // User table
        const val TABLE_USERS = "users"
        const val KEY_USER_ID = "user_id"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_EMAIL = "email"
        const val KEY_MOBILE = "mobile"
        const val KEY_PASSWORD_HASH = "password_hash"

        // Wallet table
        const val TABLE_WALLET = "wallet"
        const val KEY_WALLET_ID = "wallet_id"
        const val KEY_WALLET_USER_ID = "user_id"
        const val KEY_BALANCE_PAISE = "balance_paise"

        // Transactions table
        const val TABLE_TRANSACTIONS = "transactions"
        const val KEY_TXN_ID = "transaction_id"
        const val KEY_TXN_REF_ID = "reference_id"
        const val KEY_TXN_USER_ID = "user_id"
        const val KEY_TXN_TYPE = "transaction_type"
        const val KEY_RECIPIENT_NAME = "recipient_name"
        const val KEY_AMOUNT_PAISE = "amount_paise"
        const val KEY_DESCRIPTION = "description"
        const val KEY_TIMESTAMP = "timestamp"

        // Default initial bonus ₹500 (50000 paise) for new users
        const val INITIAL_WALLET_BALANCE_PAISE = 50000L
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $KEY_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_FULL_NAME TEXT NOT NULL,
                $KEY_EMAIL TEXT NOT NULL UNIQUE,
                $KEY_MOBILE TEXT NOT NULL,
                $KEY_PASSWORD_HASH TEXT NOT NULL
            )
        """.trimIndent()

        val createWalletTable = """
            CREATE TABLE $TABLE_WALLET (
                $KEY_WALLET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_WALLET_USER_ID INTEGER NOT NULL UNIQUE,
                $KEY_BALANCE_PAISE INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY($KEY_WALLET_USER_ID) REFERENCES $TABLE_USERS($KEY_USER_ID)
            )
        """.trimIndent()

        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $KEY_TXN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TXN_REF_ID TEXT NOT NULL UNIQUE,
                $KEY_TXN_USER_ID INTEGER NOT NULL,
                $KEY_TXN_TYPE TEXT NOT NULL,
                $KEY_RECIPIENT_NAME TEXT NOT NULL,
                $KEY_AMOUNT_PAISE INTEGER NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY($KEY_TXN_USER_ID) REFERENCES $TABLE_USERS($KEY_USER_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createWalletTable)
        db.execSQL(createTransactionsTable)

        // Create performance indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON $TABLE_USERS($KEY_EMAIL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_wallet_user_id ON $TABLE_WALLET($KEY_WALLET_USER_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON $TABLE_TRANSACTIONS($KEY_TXN_USER_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON $TABLE_TRANSACTIONS($KEY_TIMESTAMP DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Non-destructive database migrations (never drop tables!)
        if (oldVersion < 2) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON $TABLE_USERS($KEY_EMAIL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wallet_user_id ON $TABLE_WALLET($KEY_WALLET_USER_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON $TABLE_TRANSACTIONS($KEY_TXN_USER_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON $TABLE_TRANSACTIONS($KEY_TIMESTAMP DESC)")
        }
    }

    // Register User with initial wallet balance and welcome bonus transaction
    fun registerUser(user: User): Pair<Boolean, String> {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Check if email already exists
            val cursor = db.query(
                TABLE_USERS,
                arrayOf(KEY_USER_ID),
                "$KEY_EMAIL = ?",
                arrayOf(user.email.lowercase().trim()),
                null, null, null
            )
            val exists = cursor.use { it.count > 0 }
            if (exists) {
                return Pair(false, "An account with this email already exists.")
            }

            val userValues = ContentValues().apply {
                put(KEY_FULL_NAME, user.fullName.trim())
                put(KEY_EMAIL, user.email.lowercase().trim())
                put(KEY_MOBILE, user.mobile.trim())
                put(KEY_PASSWORD_HASH, user.passwordHash)
            }

            val userId = db.insert(TABLE_USERS, null, userValues)
            if (userId == -1L) {
                return Pair(false, "Failed to register user.")
            }

            // Create Wallet with initial bonus
            val walletValues = ContentValues().apply {
                put(KEY_WALLET_USER_ID, userId)
                put(KEY_BALANCE_PAISE, INITIAL_WALLET_BALANCE_PAISE)
            }
            val walletId = db.insert(TABLE_WALLET, null, walletValues)
            if (walletId == -1L) {
                return Pair(false, "Failed to initialize wallet.")
            }

            // Add Welcome Bonus Transaction
            val refId = generateRefId("BONUS")
            val txnValues = ContentValues().apply {
                put(KEY_TXN_REF_ID, refId)
                put(KEY_TXN_USER_ID, userId)
                put(KEY_TXN_TYPE, TransactionType.CREDIT.name)
                put(KEY_RECIPIENT_NAME, "PayEase Welcome Bonus")
                put(KEY_AMOUNT_PAISE, INITIAL_WALLET_BALANCE_PAISE)
                put(KEY_DESCRIPTION, "Sign-up reward credited to wallet")
                put(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val txnId = db.insert(TABLE_TRANSACTIONS, null, txnValues)
            if (txnId == -1L) {
                return Pair(false, "Failed to create welcome bonus transaction.")
            }

            db.setTransactionSuccessful()
            Pair(true, userId.toString())
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Database error occurred.")
        } finally {
            db.endTransaction()
        }
    }

    // Login user with PBKDF2 salt-hash verification
    fun loginUser(email: String, passwordRaw: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_USER_ID, KEY_FULL_NAME, KEY_EMAIL, KEY_MOBILE, KEY_PASSWORD_HASH),
            "$KEY_EMAIL = ?",
            arrayOf(email.lowercase().trim()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                val storedHash = it.getString(it.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
                if (ValidationUtils.verifyPassword(passwordRaw, storedHash)) {
                    User(
                        userId = it.getLong(it.getColumnIndexOrThrow(KEY_USER_ID)),
                        fullName = it.getString(it.getColumnIndexOrThrow(KEY_FULL_NAME)),
                        email = it.getString(it.getColumnIndexOrThrow(KEY_EMAIL)),
                        mobile = it.getString(it.getColumnIndexOrThrow(KEY_MOBILE)),
                        passwordHash = storedHash
                    )
                } else null
            } else null
        }
    }

    // Get User Details
    fun getUserById(userId: Long): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_USER_ID, KEY_FULL_NAME, KEY_EMAIL, KEY_MOBILE, KEY_PASSWORD_HASH),
            "$KEY_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                User(
                    userId = it.getLong(it.getColumnIndexOrThrow(KEY_USER_ID)),
                    fullName = it.getString(it.getColumnIndexOrThrow(KEY_FULL_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(KEY_EMAIL)),
                    mobile = it.getString(it.getColumnIndexOrThrow(KEY_MOBILE)),
                    passwordHash = it.getString(it.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
                )
            } else null
        }
    }

    // Get Wallet Balance in Paise
    fun getWalletBalancePaise(userId: Long): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WALLET,
            arrayOf(KEY_BALANCE_PAISE),
            "$KEY_WALLET_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(KEY_BALANCE_PAISE))
            } else 0L
        }
    }

    // Add Money to Wallet (Transaction-safe & Overflow-protected)
    fun addMoney(userId: Long, amountPaise: Long, paymentSource: String): Pair<Boolean, String> {
        if (amountPaise <= 0) return Pair(false, "Amount must be greater than zero.")

        val db = writableDatabase
        db.beginTransaction()
        return try {
            val currentBalance = getWalletBalancePaiseInternal(db, userId)
            val newBalance = try {
                Math.addExact(currentBalance, amountPaise)
            } catch (_: ArithmeticException) {
                return Pair(false, "Wallet balance overflow limit exceeded.")
            }

            // Update balance
            val contentValues = ContentValues().apply {
                put(KEY_BALANCE_PAISE, newBalance)
            }
            val rowsUpdated = db.update(
                TABLE_WALLET,
                contentValues,
                "$KEY_WALLET_USER_ID = ?",
                arrayOf(userId.toString())
            )

            if (rowsUpdated <= 0) {
                return Pair(false, "Failed to update wallet balance.")
            }

            // Insert Transaction record
            val refId = generateRefId("TOP")
            val txnValues = ContentValues().apply {
                put(KEY_TXN_REF_ID, refId)
                put(KEY_TXN_USER_ID, userId)
                put(KEY_TXN_TYPE, TransactionType.CREDIT.name)
                put(KEY_RECIPIENT_NAME, "Added via $paymentSource")
                put(KEY_AMOUNT_PAISE, amountPaise)
                put(KEY_DESCRIPTION, "Money added to wallet")
                put(KEY_TIMESTAMP, System.currentTimeMillis())
            }

            val txnId = db.insert(TABLE_TRANSACTIONS, null, txnValues)
            if (txnId == -1L) {
                return Pair(false, "Failed to record transaction.")
            }

            db.setTransactionSuccessful()
            Pair(true, refId)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Failed to complete transaction.")
        } finally {
            db.endTransaction()
        }
    }

    // Send Money (Transaction-safe, re-checks balance inside DB transaction, Overflow-protected)
    fun sendMoney(
        userId: Long,
        recipientName: String,
        recipientMobile: String,
        amountPaise: Long,
        description: String
    ): Pair<Boolean, String> {
        if (amountPaise <= 0) return Pair(false, "Amount must be greater than zero.")

        val db = writableDatabase
        db.beginTransaction()
        return try {
            val currentBalance = getWalletBalancePaiseInternal(db, userId)
            if (currentBalance < amountPaise) {
                return Pair(false, "Insufficient wallet balance.")
            }

            val newBalance = try {
                Math.subtractExact(currentBalance, amountPaise)
            } catch (_: ArithmeticException) {
                return Pair(false, "Transaction arithmetic error.")
            }

            // Update balance
            val contentValues = ContentValues().apply {
                put(KEY_BALANCE_PAISE, newBalance)
            }
            val rowsUpdated = db.update(
                TABLE_WALLET,
                contentValues,
                "$KEY_WALLET_USER_ID = ?",
                arrayOf(userId.toString())
            )

            if (rowsUpdated <= 0) {
                return Pair(false, "Failed to update wallet balance.")
            }

            // Insert Transaction record
            val refId = generateRefId("PAY")
            val desc = if (description.isBlank()) "Transfer to $recipientMobile" else description
            val txnValues = ContentValues().apply {
                put(KEY_TXN_REF_ID, refId)
                put(KEY_TXN_USER_ID, userId)
                put(KEY_TXN_TYPE, TransactionType.DEBIT.name)
                put(KEY_RECIPIENT_NAME, recipientName.trim())
                put(KEY_AMOUNT_PAISE, amountPaise)
                put(KEY_DESCRIPTION, desc.trim())
                put(KEY_TIMESTAMP, System.currentTimeMillis())
            }

            val txnId = db.insert(TABLE_TRANSACTIONS, null, txnValues)
            if (txnId == -1L) {
                return Pair(false, "Failed to record transaction.")
            }

            db.setTransactionSuccessful()
            Pair(true, refId)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Failed to process transfer.")
        } finally {
            db.endTransaction()
        }
    }

    // Get all transactions for a user (Newest first)
    fun getTransactionsForUser(userId: Long, limit: Int = 0): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val limitClause = if (limit > 0) limit.toString() else null

        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$KEY_TXN_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null,
            "$KEY_TIMESTAMP DESC",
            limitClause
        )

        cursor.use {
            while (it.moveToNext()) {
                val typeStr = it.getString(it.getColumnIndexOrThrow(KEY_TXN_TYPE))
                val type = if (typeStr == TransactionType.CREDIT.name) TransactionType.CREDIT else TransactionType.DEBIT
                list.add(
                    Transaction(
                        transactionId = it.getLong(it.getColumnIndexOrThrow(KEY_TXN_ID)),
                        referenceId = it.getString(it.getColumnIndexOrThrow(KEY_TXN_REF_ID)),
                        userId = it.getLong(it.getColumnIndexOrThrow(KEY_TXN_USER_ID)),
                        type = type,
                        recipientName = it.getString(it.getColumnIndexOrThrow(KEY_RECIPIENT_NAME)),
                        amountPaise = it.getLong(it.getColumnIndexOrThrow(KEY_AMOUNT_PAISE)),
                        description = it.getString(it.getColumnIndexOrThrow(KEY_DESCRIPTION)) ?: "",
                        timestamp = it.getLong(it.getColumnIndexOrThrow(KEY_TIMESTAMP))
                    )
                )
            }
        }
        return list
    }

    // Get total credit & total debit summary for profile screen
    fun getTransactionSummary(userId: Long): Pair<Long, Long> {
        var totalCredit = 0L
        var totalDebit = 0L
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT $KEY_TXN_TYPE, SUM($KEY_AMOUNT_PAISE) FROM $TABLE_TRANSACTIONS WHERE $KEY_TXN_USER_ID = ? GROUP BY $KEY_TXN_TYPE",
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                val type = it.getString(0)
                val sum = it.getLong(1)
                if (type == TransactionType.CREDIT.name) {
                    totalCredit = sum
                } else if (type == TransactionType.DEBIT.name) {
                    totalDebit = sum
                }
            }
        }
        return Pair(totalCredit, totalDebit)
    }

    private fun getWalletBalancePaiseInternal(db: SQLiteDatabase, userId: Long): Long {
        val cursor = db.query(
            TABLE_WALLET,
            arrayOf(KEY_BALANCE_PAISE),
            "$KEY_WALLET_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(KEY_BALANCE_PAISE))
            } else 0L
        }
    }

    private fun generateRefId(prefix: String): String {
        val random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).uppercase()
        val time = System.currentTimeMillis() % 1000000
        return "PE${prefix}${time}${random}"
    }
}
