package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.capstone.smartbite.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager()

        binding.btnBack.setOnClickListener {
            if (binding.viewPagerOnboarding.currentItem > 0) {
                binding.viewPagerOnboarding.currentItem -= 1
            } else {
                finish()
            }
        }
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 8 // Nama, Gender, Age, Goal, Height, Weight, Activity, Finish

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> OnboardingNameFragment()
                    1 -> OnboardingGenderFragment()
                    2 -> OnboardingAgeFragment()
                    3 -> OnboardingHeightFragment()
                    4 -> OnboardingWeightFragment()
                    5 -> OnboardingGoalFragment()
                    6 -> OnboardingActivityLevelFragment()
                    7 -> OnboardingFinishFragment()
                    else -> OnboardingNameFragment()
                }
            }
        }
        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.isUserInputEnabled = false

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                if (position == 7) {
                    binding.btnBack.visibility = View.GONE
                    binding.tvStepIndicator.visibility = View.GONE
                    binding.clProgressContainer.visibility = View.GONE
                    return
                } else {
                    binding.btnBack.visibility = View.VISIBLE
                    binding.tvStepIndicator.visibility = View.VISIBLE
                    binding.clProgressContainer.visibility = View.VISIBLE
                }

                val step = position + 1
                val totalSteps = 7
                
                binding.tvStepIndicator.text = "Langkah $step dari $totalSteps"
                
                // Update Progress & Title
                when (position) {
                    0 -> {
                        binding.tvTitleStep.text = "Name"
                        updateProgress(14, "14%")
                    }
                    1 -> {
                        binding.tvTitleStep.text = "Gender"
                        updateProgress(28, "28%")
                    }
                    2 -> {
                        binding.tvTitleStep.text = "Age"
                        updateProgress(42, "42%")
                    }
                    3 -> {
                        binding.tvTitleStep.text = "Goal"
                        updateProgress(56, "56%")
                    }
                    4 -> {
                        binding.tvTitleStep.text = "Height"
                        updateProgress(70, "70%")
                    }
                    5 -> {
                        binding.tvTitleStep.text = "Weight"
                        updateProgress(84, "84%")
                    }
                    6 -> {
                        binding.tvTitleStep.text = "Activity"
                        updateProgress(100, "100%")
                    }
                }
            }
        })
    }

    private fun updateProgress(progress: Int, percentText: String) {
        binding.onboardingProgress.progress = progress
        binding.tvProgressPercent.text = percentText
    }

    fun nextStep() {
        if (binding.viewPagerOnboarding.currentItem < 10) {
            binding.viewPagerOnboarding.currentItem += 1
        } else {
            // Finish onboarding
        }
    }
}