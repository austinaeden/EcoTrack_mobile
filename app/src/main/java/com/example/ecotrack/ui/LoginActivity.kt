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
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnForgotPassword = findViewById<Button>(R.id.btnForgotPassword)
        
        val localUserDao = AppDatabase.getDatabase(this).localUserDao()

        btnLogin.setOnClickListener {
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
                            Toast.makeText(this, "Online Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                // OFFLINE LOGIN (Local Database)
                lifecycleScope.launch {
                    val localUser = localUserDao.getLocalUser(email)
                    if (localUser != null && localUser.passwordHash == hashPassword(password)) {
                        Toast.makeText(this@LoginActivity, "Offline Login Successful!", Toast.LENGTH_SHORT).show()
                        proceedToMain(localUser.firebaseUid)
                    } else {
                        Toast.makeText(this@LoginActivity, "Offline Login Failed. Check credentials or go online.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

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
                        Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
