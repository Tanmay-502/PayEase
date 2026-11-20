package com.example.payease

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

    private lateinit var db: WalletDatabase
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        context.deleteDatabase("test_payease_wallet.db")
        db = WalletDatabase(context)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase("test_payease_wallet.db")
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
        val reopenedDb = WalletDatabase(context)

        val balance = reopenedDb.getWalletBalancePaise(userId)
        assertEquals(100000L, balance)

        val fetchedUser = reopenedDb.getUserById(userId)
        assertNotNull(fetchedUser)
        assertEquals("Persist User", fetchedUser?.fullName)

        reopenedDb.close()
    }
}
