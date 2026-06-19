package com.capstone.smartbite.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.capstone.smartbite.MainActivity
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.databinding.FragmentOnboardingFinishBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class OnboardingFinishFragment : Fragment() {

    private var _binding: FragmentOnboardingFinishBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingFinishBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load image from URL using Glide
        val imageUrl = "https://images.openai.com/static-rsc-4/nFdsTSoug99GLEA24dm6vjobLiYUwXTeFfM4oJD2skeqTeedONLR-Ock7qJTtqSMWEO_OAzobkUCJ3I2H73ry3yN1aJSJHDkARaCdzMKys7rwyWXa_tAm-wU1YF6NkdeUO1m1WQ7ydwOamn4Bgg_STYGNwNZ22FdLH-q2XxchszFyUw_nnxRzS_S-U-xA3yu?purpose=fullsize"
        Glide.with(this)
            .load(imageUrl)
            .placeholder(com.capstone.smartbite.R.drawable.bg_gender_card_unselected)
            .centerCrop()
            .into(binding.ivOnboardingSuccess)

        binding.btnGetStarted.setOnClickListener {
            val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            val userPreference = UserPreference(requireContext(), email)
            userPreference.setOnboardingFinished(true)
            
            // Get the absolute latest data before syncing to Cloud
            val finalUser = userPreference.getUser()
            
            lifecycleScope.launch {
                try {
                    FirebaseService().saveUserProfile(finalUser)
                    
                    // IF we are in UpdateBodyMetricsActivity, reset the daily log as requested
                    if (activity is com.capstone.smartbite.ui.profil.UpdateBodyMetricsActivity && email != null) {
                        FirebaseService().resetDailyLog(email)
                        // Mark today as the day of goal reset to show 0 on dashboard
                        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        userPreference.setLastGoalResetDate(dateString)
                    }
                } catch (e: Exception) {
                    // Log error or show toast if needed
                }

                if (activity is com.capstone.smartbite.ui.profil.UpdateBodyMetricsActivity) {
                    requireActivity().finish()
                } else {
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateGoalText()
    }

    private fun updateGoalText() {
        // Update Dynamic Text for Goal
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
        val userPreference = UserPreference(requireContext(), email)
        val user = userPreference.getUser()

        binding.tvFinalGoal.text = when (user.goal) {
            "Weight Loss Focus" -> "Menurunkan Berat Badan"
            "Muscle Building" -> "Meningkatkan Massa Otot"
            "Maintain Weight" -> "Menjaga Berat Badan"
            else -> "Target Berat Badan"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}