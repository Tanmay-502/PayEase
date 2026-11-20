package com.example.payease

import com.example.payease.models.TransactionType
import com.example.payease.utils.CurrencyUtils
import com.example.payease.utils.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayEaseUnitTest {

    @Test
    fun testCurrencyRupeesToPaiseConversion() {
        assertEquals(50000L, CurrencyUtils.rupeesToPaise("500"))
        assertEquals(50050L, CurrencyUtils.rupeesToPaise("500.50"))
        assertEquals(100000L, CurrencyUtils.rupeesToPaise("₹ 1,000"))
        assertEquals(0L, CurrencyUtils.rupeesToPaise("0"))
        assertEquals(0L, CurrencyUtils.rupeesToPaise("-500"))
        assertEquals(0L, CurrencyUtils.rupeesToPaise(""))
        assertEquals(0L, CurrencyUtils.rupeesToPaise("abc"))
        assertEquals(0L, CurrencyUtils.rupeesToPaise("999999999999999999999999999"))
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyUtils.formatPaiseToRupees(50000L)
        assertTrue(formatted.contains("500"))
    }

    @Test
    fun testEmailValidation() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"))
        assertTrue(ValidationUtils.isValidEmail("user.name+tag@domain.co.in"))
        assertFalse(ValidationUtils.isValidEmail("invalid-email"))
        assertFalse(ValidationUtils.isValidEmail(""))
    }

    @Test
    fun testMobileValidation() {
        assertTrue(ValidationUtils.isValidMobile("9876543210"))
        assertTrue(ValidationUtils.isValidMobile("98765-43210"))
        assertFalse(ValidationUtils.isValidMobile("12345"))
        assertFalse(ValidationUtils.isValidMobile("abcdefghij"))
    }

    @Test
    fun testPasswordValidation() {
        assertTrue(ValidationUtils.isValidPassword("secret123"))
        assertFalse(ValidationUtils.isValidPassword("12345"))
    }

    @Test
    fun testPbkdf2PasswordHashingAndVerification() {
        val rawPassword = "MySecurePassword123"
        val hash1 = ValidationUtils.hashPassword(rawPassword)
        val hash2 = ValidationUtils.hashPassword(rawPassword)

        // Unique random salt per hash -> hash strings must be different!
        assertNotEquals(hash1, hash2)

        // Verification must succeed for correct password
        assertTrue(ValidationUtils.verifyPassword(rawPassword, hash1))
        assertTrue(ValidationUtils.verifyPassword(rawPassword, hash2))

        // Verification must fail for wrong password
        assertFalse(ValidationUtils.verifyPassword("WrongPassword", hash1))
    }

    @Test
    fun testMonetaryCalculationsAndBalanceValidation() {
        val initialBalancePaise = 50000L // ₹500
        val topUpPaise = CurrencyUtils.rupeesToPaise("1000") // ₹1000
        val balanceAfterTopUp = initialBalancePaise + topUpPaise
        assertEquals(150000L, balanceAfterTopUp) // ₹1500

        val transferPaise = CurrencyUtils.rupeesToPaise("300") // ₹300
        val balanceAfterTransfer = balanceAfterTopUp - transferPaise
        assertEquals(120000L, balanceAfterTransfer) // ₹1200

        val invalidTransferPaise = CurrencyUtils.rupeesToPaise("5000") // ₹5000
        val canTransfer = balanceAfterTransfer >= invalidTransferPaise
        assertFalse(canTransfer) // Must block transfer!
    }

    @Test
    fun testTransactionTypeEnum() {
        assertEquals("CREDIT", TransactionType.CREDIT.name)
        assertEquals("DEBIT", TransactionType.DEBIT.name)
    }
}
