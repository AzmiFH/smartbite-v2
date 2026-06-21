package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingWeeklyGoalBinding

class OnboardingWeeklyGoalFragment : Fragment() {

    private var _binding: FragmentOnboardingWeeklyGoalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference
    private var selectedRate: Double = 0.5
    private var moderateRate: Double = 0.5
    private var maxSafeRate: Double = 1.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWeeklyGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        val user = userPreference.getUser()
        
        // Calculate dynamic rates based on goal
        if (user.goal == "Muscle Building") {
            moderateRate = 0.25
            maxSafeRate = com.capstone.smartbite.utils.HealthMath.getMaxWeightGainPerWeek()
        } else {
            moderateRate = 0.5
            maxSafeRate = com.capstone.smartbite.utils.HealthMath.getMaxWeightLossPerWeek(user)
        }
        
        // Initial selection if not already set
        if (selectedRate == 0.5 && moderateRate == 0.25) {
            selectedRate = moderateRate
        }

        binding.btnModerate.setOnClickListener {
            updateUI(moderateRate)
        }

        binding.btnChallenging.setOnClickListener {
            updateUI(maxSafeRate)
        }

        binding.btnNext.setOnClickListener {
            val updatedUser = userPreference.getUser()
            updatedUser.weeklyRate = selectedRate
            userPreference.setUser(updatedUser)
            (activity as? OnboardingNavigator)?.nextStep()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI(selectedRate)
    }

    private fun updateUI(rate: Double) {
        val user = userPreference.getUser()
        val weightDiff = if (user.goal == "Weight Loss Focus") {
            user.weight - user.targetWeight
        } else {
            user.targetWeight - user.weight
        }
        val weeks = Math.ceil(weightDiff / rate).toInt()

        selectedRate = rate
        if (rate == moderateRate) {
            binding.btnModerate.setBackgroundResource(R.drawable.bg_weekly_card_selected)
            binding.ivCheckModerate.setImageResource(R.drawable.ic_check_circle_filled)
            binding.ivCheckModerate.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            binding.tvModerateLabel.setTextColor(resources.getColor(R.color.brand_green, null))
            binding.tvModerateValue.text = getString(R.string.onboarding_weekly_rate_format, moderateRate)
            binding.tvModerateValue.setTextColor(resources.getColor(R.color.brand_green, null))
            binding.tvDateModerateValue.text = getString(R.string.onboarding_weekly_target_weeks, weeks)

            binding.btnChallenging.setBackgroundResource(R.drawable.bg_weekly_card_unselected)
            binding.ivCheckChallenging.setImageResource(R.drawable.ic_check_circle_outline)
            binding.ivCheckChallenging.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1D5DB"))
            binding.tvChallengingLabel.setTextColor(resources.getColor(R.color.text_secondary, null))
            binding.tvChallengingValue.text = getString(R.string.onboarding_weekly_rate_format, maxSafeRate)
            binding.tvChallengingValue.setTextColor(resources.getColor(R.color.text_primary, null))

            binding.llDateModerate.visibility = View.VISIBLE
            binding.llDateChallenging.visibility = View.GONE
        } else {
            binding.btnModerate.setBackgroundResource(R.drawable.bg_weekly_card_unselected)
            binding.ivCheckModerate.setImageResource(R.drawable.ic_check_circle_outline)
            binding.ivCheckModerate.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1D5DB"))
            binding.tvModerateLabel.setTextColor(resources.getColor(R.color.text_secondary, null))
            binding.tvModerateValue.text = getString(R.string.onboarding_weekly_rate_format, moderateRate)
            binding.tvModerateValue.setTextColor(resources.getColor(R.color.text_primary, null))

            binding.btnChallenging.setBackgroundResource(R.drawable.bg_weekly_card_selected)
            binding.ivCheckChallenging.setImageResource(R.drawable.ic_check_circle_filled)
            binding.ivCheckChallenging.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            binding.tvChallengingLabel.setTextColor(resources.getColor(R.color.brand_green, null))
            binding.tvChallengingValue.text = getString(R.string.onboarding_weekly_rate_format, maxSafeRate)
            binding.tvChallengingValue.setTextColor(resources.getColor(R.color.brand_green, null))
            binding.tvDateChallengingValue.text = getString(R.string.onboarding_weekly_target_weeks, weeks)

            binding.llDateModerate.visibility = View.GONE
            binding.llDateChallenging.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}