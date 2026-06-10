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
            val current = binding.viewPagerOnboarding.currentItem
            if (current == 8 && !shouldShowWeeklyGoal()) {
                binding.viewPagerOnboarding.currentItem = 6
            } else if (current > 0) {
                binding.viewPagerOnboarding.currentItem -= 1
            } else {
                finish()
            }
        }
    }

    private fun shouldShowWeeklyGoal(): Boolean {
        val userPreference = com.capstone.smartbite.UserPreference(this, com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        val goal = userPreference.getUser().goal
        return goal == "Weight Loss Focus" || goal == "Muscle Building"
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 9 // Nama, Gender, Age, Goal, Height, Weight, Activity, Weekly Goal, Finish

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> OnboardingNameFragment()
                    1 -> OnboardingGenderFragment()
                    2 -> OnboardingAgeFragment()
                    3 -> OnboardingHeightFragment()
                    4 -> OnboardingWeightFragment()
                    5 -> OnboardingActivityLevelFragment()
                    6 -> OnboardingGoalFragment()
                    7 -> OnboardingWeeklyGoalFragment()
                    8 -> OnboardingFinishFragment()
                    else -> OnboardingNameFragment()
                }
            }
        }
        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.isUserInputEnabled = false

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                if (position == 8) {
                    binding.btnBack.visibility = View.GONE
                    binding.clProgressContainer.visibility = View.GONE
                    return
                } else {
                    binding.btnBack.visibility = View.VISIBLE
                    binding.clProgressContainer.visibility = View.VISIBLE
                }
                
                // Update Progress
                when (position) {
                    0 -> updateProgress(12)
                    1 -> updateProgress(25)
                    2 -> updateProgress(37)
                    3 -> updateProgress(50)
                    4 -> updateProgress(62)
                    5 -> updateProgress(75)
                    6 -> updateProgress(87)
                    7 -> updateProgress(100)
                }
            }
        })
    }

    private fun updateProgress(progress: Int) {
        binding.onboardingProgress.progress = progress
    }

    fun nextStep() {
        val current = binding.viewPagerOnboarding.currentItem
        if (current == 6 && !shouldShowWeeklyGoal()) {
            binding.viewPagerOnboarding.currentItem = 8
        } else if (current < 8) {
            binding.viewPagerOnboarding.currentItem += 1
        }
    }
}