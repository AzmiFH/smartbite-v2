package com.capstone.smartbite.Login

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstone.smartbite.MainActivity
import com.capstone.smartbite.R
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.onboarding.OnboardingActivity
import com.capstone.smartbite.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Berikan padding atas dan bawah agar konten tidak tertutup status bar dan navigasi 3 button
            binding.main.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTextStyling()

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userPreference = UserPreference(this)
            val intent = if (userPreference.isOnboardingFinished()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, OnboardingActivity::class.java)
            }
            startActivity(intent)
            finish()
        }

        binding.buttonLogin.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        showLoading(true)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showLoading(false)
                // Error 12501 means the user cancelled the sign-in flow (e.g., pressed back)
                if (e.statusCode != 12501) {
                    Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val userPreference = UserPreference(this)
                        val currentModel = userPreference.getUser()
                        
                        // Only pre-fill if local name is empty
                        if (currentModel.name.isNullOrEmpty()) {
                            currentModel.name = firebaseUser.displayName
                            currentModel.email = firebaseUser.email
                            currentModel.profileImage = firebaseUser.photoUrl
                            userPreference.setUser(currentModel)
                        }
                    }
                    
                    val userPreference = UserPreference(this)
                    val intent = if (userPreference.isOnboardingFinished()) {
                        Intent(this, MainActivity::class.java)
                    } else {
                        Intent(this, OnboardingActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupTextStyling() {
        val titleText = "Track Your Nutrition,\nTransform Your Health"
        val spannable = SpannableString(titleText)

        // Color "Nutrition" blue (#4285F4 - Google Blue style or similar)
        val nutritionStart = titleText.indexOf("Nutrition")
        val nutritionEnd = nutritionStart + "Nutrition".length
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#4285F4")),
            nutritionStart,
            nutritionEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Color "Health" green (#34A853 - Google Green style)
        val healthStart = titleText.indexOf("Health")
        val healthEnd = healthStart + "Health".length
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#34A853")),
            healthStart,
            healthEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        //binding.tvTitle.text = spannable
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
    }
}
