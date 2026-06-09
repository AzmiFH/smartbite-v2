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
                    5 -> OnboardingActivityLevelFragment()
                    6 -> OnboardingGoalFragment()
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
                    binding.clProgressContainer.visibility = View.GONE
                    return
                } else {
                    binding.btnBack.visibility = View.VISIBLE
                    binding.clProgressContainer.visibility = View.VISIBLE
                }
                
                // Update Progress
                when (position) {
                    0 -> {
                        updateProgress(14)
                    }
                    1 -> {
                        updateProgress(28)
                    }
                    2 -> {
                        updateProgress(42)
                    }
                    3 -> {
                        updateProgress(56)
                    }
                    4 -> {
                        updateProgress(70)
                    }
                    5 -> {
                        updateProgress(84)
                    }
                    6 -> {
                        updateProgress(100)
                    }
                }
            }
        })
    }

    private fun updateProgress(progress: Int) {
        binding.onboardingProgress.progress = progress
    }

    fun nextStep() {
        if (binding.viewPagerOnboarding.currentItem < 10) {
            binding.viewPagerOnboarding.currentItem += 1
        } else {
            // Finish onboarding
        }
    }
}