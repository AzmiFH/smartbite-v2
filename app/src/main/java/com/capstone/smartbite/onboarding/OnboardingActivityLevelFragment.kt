package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingActivityLevelBinding

class OnboardingActivityLevelFragment : Fragment() {

    private var _binding: FragmentOnboardingActivityLevelBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference
    private var selectedLevel: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingActivityLevelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext())
        val user = userPreference.getUser()

        // Pre-select if already exists
        if (!user.activityLevel.isNullOrEmpty()) {
            updateUI(user.activityLevel!!)
        }

        binding.btnLow.setOnClickListener { updateUI("Low") }
        binding.btnMedium.setOnClickListener { updateUI("Medium") }
        binding.btnHigh.setOnClickListener { updateUI("High") }

        binding.btnNext.setOnClickListener {
            if (selectedLevel != null) {
                user.activityLevel = selectedLevel
                userPreference.setUser(user)
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
    }

    private fun updateUI(level: String) {
        selectedLevel = level
        
        // Reset all
        resetCard(binding.btnLow, binding.ivCheckLow)
        resetCard(binding.btnMedium, binding.ivCheckMedium)
        resetCard(binding.btnHigh, binding.ivCheckHigh)

        // Select one
        when (level) {
            "Low" -> selectCard(binding.btnLow, binding.ivCheckLow)
            "Medium" -> selectCard(binding.btnMedium, binding.ivCheckMedium)
            "High" -> selectCard(binding.btnHigh, binding.ivCheckHigh)
        }

        // Enable next button
        binding.btnNext.alpha = 1.0f
        binding.btnNext.isClickable = true
        binding.btnNext.isFocusable = true
    }

    private fun selectCard(card: View, check: android.widget.ImageView) {
        card.setBackgroundResource(R.drawable.bg_gender_card_selected)
        check.setImageResource(R.drawable.ic_check_circle_filled)
    }

    private fun resetCard(card: View, check: android.widget.ImageView) {
        card.setBackgroundResource(R.drawable.bg_gender_card_unselected)
        check.setImageResource(R.drawable.ic_check_circle_outline)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}