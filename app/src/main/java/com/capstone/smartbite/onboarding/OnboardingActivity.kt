package com.capstone.smartbite.onboarding

import android.os.Bundle
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
            override fun getItemCount(): Int = 5 // Nama, Gender, Age, Height, Weight

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> OnboardingNameFragment()
                    1 -> OnboardingGenderFragment()
                    2 -> OnboardingAgeFragment()
                    3 -> OnboardingHeightFragment()
                    4 -> OnboardingWeightFragment()
                    else -> OnboardingNameFragment()
                }
            }
        }
        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.isUserInputEnabled = false

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val step = position + 1
                val totalSteps = 11
                
                binding.tvStepIndicator.text = "$step/$totalSteps"
                
                // Update Progress & Title
                when (position) {
                    0 -> {
                        binding.tvTitleStep.text = "Name"
                        updateProgress(10, "10%")
                    }
                    1 -> {
                        binding.tvTitleStep.text = "Gender"
                        updateProgress(20, "20%")
                    }
                    2 -> {
                        binding.tvTitleStep.text = "Age"
                        updateProgress(30, "30%")
                    }
                    3 -> {
                        binding.tvTitleStep.text = "Height"
                        updateProgress(36, "36%")
                    }
                    4 -> {
                        binding.tvTitleStep.text = "Weight"
                        updateProgress(45, "45%")
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