package com.taskmaster.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.taskmaster.app.R
import com.taskmaster.app.databinding.ActivitySignupBinding
import com.taskmaster.app.utils.isValidEmail
import com.taskmaster.app.utils.showToast

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object { private const val RC_SIGN_IN = 9001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        setupGoogleSignIn()

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.cardSignup.startAnimation(slideUp)

        binding.btnSignup.setOnClickListener { attemptSignUp() }
        binding.btnGoogleSignin.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun attemptSignUp() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (name.isEmpty()) { binding.tilName.error = getString(R.string.error_empty_name); return }
        if (email.isEmpty()) { binding.tilEmail.error = getString(R.string.error_empty_email); return }
        if (!email.isValidEmail()) { binding.tilEmail.error = getString(R.string.error_invalid_email); return }
        if (password.isEmpty()) { binding.tilPassword.error = getString(R.string.error_empty_password); return }
        if (password.length < 6) { binding.tilPassword.error = getString(R.string.error_short_password); return }
        if (confirm != password) { binding.tilConfirmPassword.error = getString(R.string.error_passwords_no_match); return }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user!!
                val profileUpdate = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user.updateProfile(profileUpdate)
                saveUserToFirestore(user.uid, name, email)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast(e.message ?: "Sign up failed")
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String) {
        val userData = hashMapOf("name" to name, "email" to email, "uid" to uid, "createdAt" to System.currentTimeMillis())
        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                showLoading(false)
                navigateToDashboard()
            }
            .addOnFailureListener {
                showLoading(false)
                navigateToDashboard() // still navigate, user is created
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                showLoading(true)
                val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
                auth.signInWithCredential(credential).addOnSuccessListener {
                    showLoading(false); navigateToDashboard()
                }.addOnFailureListener { e ->
                    showLoading(false); showToast(e.message ?: "Google sign-in failed")
                }
            } catch (e: ApiException) { showToast("Google sign-in failed") }
        }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !show
    }
}
