
package com.example.recipefinder.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recipefinder.databinding.ActivityLoginBinding
import com.example.recipefinder.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check whether the user is already logged in
        if (auth.currentUser != null) {
            navigateToHome()
            return
        }

        binding.loading.visibility = View.GONE

        binding.login.setOnClickListener {
            performLogin()
        }

        binding.register.setOnClickListener {
            startActivity(Intent(this, com.example.recipefinder.ui.register.RegisterActivity::class.java))
            finish()
        }

        // Login when the user presses Done on the keyboard
        binding.password.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else {
                false
            }
        }
    }

    private fun performLogin() {

        val email = binding.username.text.toString().trim()
        val password = binding.password.text.toString()

        // Validate email
        if (email.isEmpty()) {
            binding.username.error = "Enter your email"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.username.error = "Enter a valid email address"
            return
        }

        // Validate password
        if (password.isEmpty()) {
            binding.password.error = "Enter your password"
            return
        }

        if (password.length < 6) {
            binding.password.error = "Password must be at least 6 characters"
            return
        }

        // Show loading indicator
        binding.loading.visibility = View.VISIBLE
        binding.login.isEnabled = false

        // Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                binding.loading.visibility = View.GONE
                binding.login.isEnabled = true

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToHome()

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "Login failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToHome() {

        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
