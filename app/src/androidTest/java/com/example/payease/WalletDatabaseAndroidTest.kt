package com.example.payease

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.payease.database.WalletDatabase
import com.example.payease.models.User
import com.example.payease.utils.ValidationUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletDatabaseAndroidTest {

    private companion object {
        private const val TEST_DB_NAME = "test_payease_wallet.db"
        private const val MIGRATION_TEST_DB_NAME = "test_migration_v1_to_v2.db"
    }

    private lateinit var db: WalletDatabase
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB_NAME)
        context.deleteDatabase(MIGRATION_TEST_DB_NAME)
        db = WalletDatabase(context, TEST_DB_NAME)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(TEST_DB_NAME)
        context.deleteDatabase(MIGRATION_TEST_DB_NAME)
    }

    @Test
    fun testUserRegistrationAndWelcomeBonus() {
        val user = User(
            fullName = "Instrumentation Tester",
            email = "test.android@example.com",
            mobile = "9876543210",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )

        val (success, message) = db.registerUser(user)
        assertTrue(success)

        val userId = message.toLong()
        val balance = db.getWalletBalancePaise(userId)
        assertEquals(50000L, balance) // ₹500 initial bonus

        val txns = db.getTransactionsForUser(userId)
        assertEquals(1, txns.size)
        assertEquals(50000L, txns[0].amountPaise)
    }

    @Test
    fun testDuplicateEmailRejection() {
        val user1 = User(
            fullName = "User One",
            email = "duplicate@example.com",
            mobile = "9876543210",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )
        val (success1, _) = db.registerUser(user1)
        assertTrue(success1)

        val user2 = User(
            fullName = "User Two",
            email = "duplicate@example.com",
            mobile = "9876543211",
            passwordHash = ValidationUtils.hashPassword("secret456")
        )
        val (success2, message2) = db.registerUser(user2)
        assertFalse(success2)
        assertEquals("An account with this email already exists.", message2)
    }

    @Test
    fun testAtomicAddMoneyCredit() {
        val user = User(
            fullName = "TopUp User",
            email = "topup@example.com",
            mobile = "9876543211",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )
        val (regSuccess, userIdStr) = db.registerUser(user)
        assertTrue(regSuccess)
        val userId = userIdStr.toLong()

        val (addSuccess, refId) = db.addMoney(userId, 100000L, "UPI") // +₹1000
        assertTrue(addSuccess)
        assertTrue(refId.startsWith("PETOP"))

        val balance = db.getWalletBalancePaise(userId)
        assertEquals(150000L, balance) // ₹500 + ₹1000 = ₹1500

        val txns = db.getTransactionsForUser(userId)
        assertEquals(2, txns.size)
    }

    @Test
    fun testAtomicSendMoneyDebitAndInsufficientBalanceRollback() {
        val user = User(
            fullName = "Transfer User",
            email = "transfer@example.com",
            mobile = "9876543212",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )
        val (regSuccess, userIdStr) = db.registerUser(user)
        assertTrue(regSuccess)
        val userId = userIdStr.toLong()

        // Balance is ₹500 (50000 paise). Try sending ₹1000 (100000 paise) -> Must fail and rollback!
        val (failResult, failMsg) = db.sendMoney(userId, "Alice", "9999999999", 100000L, "Rent")
        assertFalse(failResult)
        assertEquals("Insufficient wallet balance.", failMsg)

        // Balance must remain unchanged at ₹500
        val balanceAfterFail = db.getWalletBalancePaise(userId)
        assertEquals(50000L, balanceAfterFail)

        // Now send ₹200 (20000 paise) -> Must succeed
        val (sendSuccess, refId) = db.sendMoney(userId, "Alice", "9999999999", 20000L, "Rent")
        assertTrue(sendSuccess)
        assertTrue(refId.startsWith("PEPAY"))

        // Balance must be ₹300 (30000 paise)
        val balanceAfterSend = db.getWalletBalancePaise(userId)
        assertEquals(30000L, balanceAfterSend)
    }

    @Test
    fun testBalanceOverflowRejection() {
        val user = User(
            fullName = "Overflow User",
            email = "overflow@example.com",
            mobile = "9876543214",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )
        val (regSuccess, userIdStr) = db.registerUser(user)
        assertTrue(regSuccess)
        val userId = userIdStr.toLong()

        // Attempt to add Long.MAX_VALUE paise which overflows Math.addExact
        val (success, message) = db.addMoney(userId, Long.MAX_VALUE, "Card")
        assertFalse(success)
        assertEquals("Wallet balance overflow limit exceeded.", message)

        // Balance remains initial ₹500 (50000 paise)
        assertEquals(50000L, db.getWalletBalancePaise(userId))
    }

    @Test
    fun testInvalidAmountRejection() {
        val user = User(
            fullName = "Invalid User",
            email = "invalid@example.com",
            mobile = "9876543215",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )
        val (_, userIdStr) = db.registerUser(user)
        val userId = userIdStr.toLong()

        val (addFail, _) = db.addMoney(userId, 0L, "UPI")
        assertFalse(addFail)

        val (sendFail, _) = db.sendMoney(userId, "Bob", "9876543210", -500L, "Note")
        assertFalse(sendFail)
    }

    @Test
    fun testReopeningDatabasePreservesState() {
        val user = User(
            fullName = "Persist User",
            email = "persist@example.com",
            mobile = "9876543213",
            passwordHash = ValidationUtils.hashPassword("secret123")
        )
        val (_, userIdStr) = db.registerUser(user)
        val userId = userIdStr.toLong()

        db.addMoney(userId, 50000L, "UPI") // Total ₹1000

        // Close and reopen database instance
        db.close()
        val reopenedDb = WalletDatabase(context, TEST_DB_NAME)

        val balance = reopenedDb.getWalletBalancePaise(userId)
        assertEquals(100000L, balance)

        val fetchedUser = reopenedDb.getUserById(userId)
        assertNotNull(fetchedUser)
        assertEquals("Persist User", fetchedUser?.fullName)

        reopenedDb.close()
    }

    @Test
    fun testDatabaseMigrationFromV1ToV2() {
        // Step 1: Create a Version 1 database manually without indexes
        val dbFile = context.getDatabasePath(MIGRATION_TEST_DB_NAME)
        val v1Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v1Db.version = 1

        v1Db.execSQL("""
            CREATE TABLE users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                mobile TEXT NOT NULL,
                password_hash TEXT NOT NULL
            )
        """.trimIndent())

        v1Db.execSQL("""
            CREATE TABLE wallet (
                wallet_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL UNIQUE,
                balance_paise INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(user_id) REFERENCES users(user_id)
            )
        """.trimIndent())

        v1Db.execSQL("""
            CREATE TABLE transactions (
                transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                reference_id TEXT NOT NULL UNIQUE,
                user_id INTEGER NOT NULL,
                transaction_type TEXT NOT NULL,
                recipient_name TEXT NOT NULL,
                amount_paise INTEGER NOT NULL,
                description TEXT,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(user_id)
            )
        """.trimIndent())

        // Insert V1 test data
        val uValues = ContentValues().apply {
            put("full_name", "V1 Migration User")
            put("email", "v1migration@example.com")
            put("mobile", "9876543210")
            put("password_hash", "v1hash:12345")
        }
        val v1UserId = v1Db.insert("users", null, uValues)

        val wValues = ContentValues().apply {
            put("user_id", v1UserId)
            put("balance_paise", 75000L) // ₹750
        }
        v1Db.insert("wallet", null, wValues)

        val tValues = ContentValues().apply {
            put("reference_id", "PETOPV1FIX")
            put("user_id", v1UserId)
            put("transaction_type", "CREDIT")
            put("recipient_name", "V1 TopUp")
            put("amount_paise", 75000L)
            put("description", "V1 Initial Balance")
            put("timestamp", System.currentTimeMillis())
        }
        v1Db.insert("transactions", null, tValues)
        v1Db.close()

        // Step 2: Open with WalletDatabase (which is Version 2) -> Triggers onUpgrade(oldVersion=1, newVersion=2)
        val migratedDb = WalletDatabase(context, MIGRATION_TEST_DB_NAME)

        // Step 3: Verify Version 2 upgrade & complete data preservation
        val user = migratedDb.getUserById(v1UserId)
        assertNotNull(user)
        assertEquals("V1 Migration User", user?.fullName)
        assertEquals("v1migration@example.com", user?.email)
        assertEquals("v1hash:12345", user?.passwordHash)

        val balance = migratedDb.getWalletBalancePaise(v1UserId)
        assertEquals(75000L, balance)

        val txns = migratedDb.getTransactionsForUser(v1UserId)
        assertEquals(1, txns.size)
        assertEquals("PETOPV1FIX", txns[0].referenceId)
        assertEquals(75000L, txns[0].amountPaise)

        // Verify new indexes exist in sqlite_master
        val indexCursor = migratedDb.readableDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'",
            null
        )
        val indexNames = mutableListOf<String>()
        indexCursor.use {
            while (it.moveToNext()) {
                indexNames.add(it.getString(0))
            }
        }

        assertTrue(indexNames.contains("idx_users_email"))
        assertTrue(indexNames.contains("idx_wallet_user_id"))
        assertTrue(indexNames.contains("idx_transactions_user_id"))
        assertTrue(indexNames.contains("idx_transactions_timestamp"))

        migratedDb.close()
    }
}
