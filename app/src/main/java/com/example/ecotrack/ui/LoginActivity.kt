package com.example.ecotrack.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecotrack.MainActivity
import com.example.ecotrack.R
import com.example.ecotrack.data.AppDatabase
import com.example.ecotrack.data.User
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * LoginActivity handles user authentication, including logging in existing users
 * and registering new accounts.
 */
class LoginActivity : AppCompatActivity() {

    // UI components for user input
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI components
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        
        // Access the User Data Access Object (DAO) to interact with the database
        val userDao = AppDatabase.getDatabase(this).userDao()

        /**
         * Logic for the Login button.
         * Validates input, hashes the password, and checks the database for a matching user.
         */
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Run database check in a coroutine to avoid blocking the UI thread
            lifecycleScope.launch {
                val user = userDao.loginUser(email, hashPassword(password))
                if (user != null) {
                    Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to MainActivity and pass the unique User ID
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("USER_ID", user.id)
                    }
                    startActivity(intent)
                    finish() // Close login screen so user can't go back to it
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * Logic for the Signup button.
         * Checks if the email is already registered, then creates a new user account.
         */
        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Prevent duplicate registrations
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@LoginActivity, "User already exists!", Toast.LENGTH_SHORT).show()
                } else {
                    // Save new user with a hashed password for security
                    userDao.registerUser(User(email = email, passwordHash = hashPassword(password)))
                    Toast.makeText(this@LoginActivity, "Account created! You can login now.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Hashes a plain-text password using SHA-256 algorithm.
     * This ensures that actual passwords are never stored in plain text in the database.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
