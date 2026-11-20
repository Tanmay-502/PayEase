package com.example.payease.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.payease.MainActivity
import com.example.payease.R
import com.example.payease.database.WalletDatabase
import com.example.payease.utils.AnimationUtils
import com.example.payease.utils.SessionManager
import com.example.payease.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegisterLink: TextView

    private lateinit var db: WalletDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        db = WalletDatabase(this)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        AnimationUtils.applyTouchScale(btnLogin)

        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tilEmail.error = null
        tilPassword.error = null

        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.error = "Enter a valid email address"
            return
        }

        if (password.isEmpty()) {
            tilPassword.error = "Password cannot be empty"
            return
        }

        val user = db.loginUser(email, password)

        if (user != null) {
            sessionManager.createLoginSession(
                userId = user.userId,
                fullName = user.fullName,
                email = user.email,
                mobile = user.mobile
            )
            Toast.makeText(this, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid email or password. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
}
