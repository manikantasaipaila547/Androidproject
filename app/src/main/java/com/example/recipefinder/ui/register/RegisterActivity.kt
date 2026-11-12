package com.example.recipefinder.ui.register

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recipefinder.databinding.ActivityRegisterBinding
import com.example.recipefinder.ui.home.HomeActivity
import com.example.recipefinder.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegistration() {
        val userName = binding.etName.text.toString().trim()
        val userEmail = binding.etRegEmail.text.toString().trim()
        val userPassword = binding.etRegPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (userName.isEmpty()) {
            binding.tilName.error = "Enter your full name"
            return
        } else {
            binding.tilName.error = null
        }

        if (userEmail.isEmpty()) {
            binding.tilRegEmail.error = "Enter your email address"
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            binding.tilRegEmail.error = "Enter a valid email address"
            return
        } else {
            binding.tilRegEmail.error = null
        }

        if (userPassword.isEmpty()) {
            binding.tilRegPassword.error = "Enter a password"
            return
        } else if (userPassword.length < 6) {
            binding.tilRegPassword.error = "Password must be at least 6 characters"
            return
        } else {
            binding.tilRegPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm your password"
            return
        } else if (userPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        } else {
            binding.tilConfirmPassword.error = null
        }

        binding.registerLoading.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(userName)
                        .build()

                    firebaseUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            binding.registerLoading.visibility = View.GONE
                            binding.btnRegister.isEnabled = true

                            Toast.makeText(
                                this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()

                            navigateToHome()
                        } ?: run {
                        binding.registerLoading.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        navigateToHome()
                    }
                } else {
                    binding.registerLoading.visibility = View.GONE
                    binding.btnRegister.isEnabled = true

                    val errorMessage = task.exception?.localizedMessage ?: "Registration failed. Please try again."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
