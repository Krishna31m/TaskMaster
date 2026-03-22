package com.taskmaster.app.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.taskmaster.app.R
import com.taskmaster.app.databinding.ActivityForgotPasswordBinding
import com.taskmaster.app.utils.isValidEmail
import com.taskmaster.app.utils.showToast

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvBackToLogin.setOnClickListener { finish() }

        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            binding.tilEmail.error = null

            if (email.isEmpty()) { binding.tilEmail.error = getString(R.string.error_empty_email); return@setOnClickListener }
            if (!email.isValidEmail()) { binding.tilEmail.error = getString(R.string.error_invalid_email); return@setOnClickListener }

            showLoading(true)
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    showLoading(false)
                    showToast("Reset email sent! Check your inbox.")
                    finish()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showToast(e.message ?: "Failed to send reset email")
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnReset.isEnabled = !show
    }
}
