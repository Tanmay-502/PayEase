package com.example.payease.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.payease.MainActivity
import com.example.payease.R
import com.example.payease.database.WalletDatabase
import com.example.payease.models.User
import com.example.payease.utils.AnimationUtils
import com.example.payease.utils.SessionManager
import com.example.payease.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilMobile: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etMobile: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLoginLink: TextView

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = WalletDatabase(this)
        sessionManager = SessionManager(this)

        tilFullName = findViewById(R.id.tilFullName)
        tilEmail = findViewById(R.id.tilEmail)
        tilMobile = findViewById(R.id.tilMobile)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etMobile = findViewById(R.id.etMobile)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        AnimationUtils.applyTouchScale(btnRegister)

        btnRegister.setOnClickListener {
            handleRegister()
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun handleRegister() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        tilFullName.error = null
        tilEmail.error = null
        tilMobile.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (fullName.isEmpty()) {
            tilFullName.error = "Full name is required"
            return
        }

        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.error = "Enter a valid email address"
            return
        }

        if (!ValidationUtils.isValidMobile(mobile)) {
            tilMobile.error = "Enter a valid 10-digit mobile number"
            return
        }

        if (!ValidationUtils.isValidPassword(password)) {
            tilPassword.error = "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            return
        }

        val passwordHash = ValidationUtils.hashPassword(password)
        val newUser = User(
            fullName = fullName,
            email = email,
            mobile = mobile,
            passwordHash = passwordHash
        )

        val (success, message) = db.registerUser(newUser)

        if (success) {
            val userId = message.toLongOrNull() ?: 0L
            sessionManager.createLoginSession(
                userId = userId,
                fullName = fullName,
                email = email,
                mobile = mobile
            )
            Toast.makeText(this, "Registration successful! ₹500 welcome bonus added.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
