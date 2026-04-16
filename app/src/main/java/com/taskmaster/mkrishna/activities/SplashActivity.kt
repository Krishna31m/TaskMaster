package com.taskmaster.mkrishna.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.taskmaster.mkrishna.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playEntranceAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNext()
        }, 3000)
    }

    private fun playEntranceAnimation() {
        // Center content: scale + fade in
        binding.llCenter.apply {
            scaleX = 0.4f
            scaleY = 0.4f
            alpha = 0f
        }
        val scaleX = ObjectAnimator.ofFloat(binding.llCenter, "scaleX", 0.4f, 1f).apply {
            duration = 800; interpolator = OvershootInterpolator(1.2f)
        }
        val scaleY = ObjectAnimator.ofFloat(binding.llCenter, "scaleY", 0.4f, 1f).apply {
            duration = 800; interpolator = OvershootInterpolator(1.2f)
        }
        val fadeCenter = ObjectAnimator.ofFloat(binding.llCenter, "alpha", 0f, 1f).apply {
            duration = 600
        }

        // Bottom: slide up + fade
        binding.llBottom.translationY = 80f
        binding.llBottom.alpha = 0f
        val slideBottom = ObjectAnimator.ofFloat(binding.llBottom, "translationY", 80f, 0f).apply {
            duration = 700; startDelay = 400
        }
        val fadeBottom = ObjectAnimator.ofFloat(binding.llBottom, "alpha", 0f, 1f).apply {
            duration = 700; startDelay = 400
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, fadeCenter, slideBottom, fadeBottom)
            start()
        }
    }

    private fun navigateToNext() {
        val destination = if (auth.currentUser != null) {
            Intent(this, DashboardActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(destination)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
