package com.example.ecotrack.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecotrack.MainActivity
import com.example.ecotrack.R
import com.example.ecotrack.data.AppDatabase
import com.example.ecotrack.data.LocalUser
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * LoginActivity handles user authentication using Firebase (Online)
 * and Room Database (Offline Fallback).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // 1. AUTO-LOGIN: If a user is already signed in, go to MainActivity immediately
        val currentUser = auth.currentUser
        if (currentUser != null) {
            proceedToMain(currentUser.uid)
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnForgotPassword = findViewById<Button>(R.id.btnForgotPassword)
        
        val localUserDao = AppDatabase.getDatabase(this).localUserDao()

        btnLogin.setOnClickListener {
            // Hide confirm password field during login
            tilConfirmPassword.visibility = android.view.View.GONE
            
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isOnline()) {
                // ONLINE LOGIN (Firebase)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            lifecycleScope.launch {
                                // Save/Update local copy for future offline access
                                localUserDao.saveUser(LocalUser(user!!.uid, email, hashPassword(password)))
                                proceedToMain(user.uid)
                            }
                        } else {
                            // ENHANCED ERROR HANDLING (Online)
                            val message = when (val exception = task.exception) {
                                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> 
                                    "This email is not registered. Please sign up first."
                                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> 
                                    "Incorrect password. Please try again."
                                else -> "Login Failed: ${exception?.localizedMessage}"
                            }
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                // OFFLINE LOGIN (Local Database)
                lifecycleScope.launch {
                    val localUser = localUserDao.getLocalUser(email)
                    if (localUser != null) {
                        if (localUser.passwordHash == hashPassword(password)) {
                            Toast.makeText(this@LoginActivity, "Offline Login Successful!", Toast.LENGTH_SHORT).show()
                            proceedToMain(localUser.firebaseUid)
                        } else {
                            Toast.makeText(this@LoginActivity, "Incorrect password for offline access.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "No local record for this user. Please log in online once first.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnSignup.setOnClickListener {
            // Show confirm password field if it's hidden
            if (tilConfirmPassword.visibility == android.view.View.GONE) {
                tilConfirmPassword.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Please confirm your password to sign up", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOnline()) {
                Toast.makeText(this, "Internet required for Sign Up", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        lifecycleScope.launch {
                            localUserDao.saveUser(LocalUser(user!!.uid, email, hashPassword(password)))
                            Toast.makeText(this@LoginActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                            proceedToMain(user.uid)
                        }
                    } else {
                        // ENHANCED ERROR HANDLING (Signup)
                        val message = when (task.exception) {
                            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> 
                                "This email is already in use. Please log in instead."
                            else -> "Signup Failed: ${task.exception?.localizedMessage}"
                        }
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (!isOnline()) {
                Toast.makeText(this, "Internet required for password reset", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset email sent to $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun proceedToMain(userId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
