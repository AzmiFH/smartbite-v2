package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.databinding.FragmentOnboardingWeeklyGoalBinding

class OnboardingWeeklyGoalFragment : Fragment() {

    private var _binding: FragmentOnboardingWeeklyGoalBinding? = null
    private val binding get() = _binding!!

    private var selectedRate: Double = 0.5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWeeklyGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnModerate.setOnClickListener {
            updateUI(0.5)
        }

        binding.btnChallenging.setOnClickListener {
            updateUI(1.0)
        }

        binding.btnNext.setOnClickListener {
            (activity as? OnboardingNavigator)?.nextStep()
        }
    }

    private fun updateUI(rate: Double) {
        selectedRate = rate
        if (rate == 0.5) {
            binding.btnModerate.setBackgroundResource(R.drawable.bg_weekly_card_selected)
            binding.ivCheckModerate.setImageResource(R.drawable.ic_check_circle_filled)
            binding.ivCheckModerate.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            binding.tvModerateLabel.setTextColor(resources.getColor(R.color.brand_green, null))

            binding.btnChallenging.setBackgroundResource(R.drawable.bg_weekly_card_unselected)
            binding.ivCheckChallenging.setImageResource(R.drawable.ic_check_circle_outline)
            binding.ivCheckChallenging.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1D5DB"))
            binding.tvChallengingLabel.setTextColor(resources.getColor(R.color.text_secondary, null))
        } else {
            binding.btnModerate.setBackgroundResource(R.drawable.bg_weekly_card_unselected)
            binding.ivCheckModerate.setImageResource(R.drawable.ic_check_circle_outline)
            binding.ivCheckModerate.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1D5DB"))
            binding.tvModerateLabel.setTextColor(resources.getColor(R.color.text_secondary, null))

            binding.btnChallenging.setBackgroundResource(R.drawable.bg_weekly_card_selected)
            binding.ivCheckChallenging.setImageResource(R.drawable.ic_check_circle_filled)
            binding.ivCheckChallenging.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            binding.tvChallengingLabel.setTextColor(resources.getColor(R.color.brand_green, null))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}