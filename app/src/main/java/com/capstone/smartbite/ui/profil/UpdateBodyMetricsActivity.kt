package com.capstone.smartbite.ui.profil

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.ActivityUpdateBodyMetricsBinding
import com.capstone.smartbite.onboarding.*
import com.google.firebase.auth.FirebaseAuth

class UpdateBodyMetricsActivity : AppCompatActivity(), OnboardingNavigator {

    private lateinit var binding: ActivityUpdateBodyMetricsBinding
    private lateinit var userPreference: UserPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBodyMetricsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreference = UserPreference(this, FirebaseAuth.getInstance().currentUser?.email)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager()

        binding.btnBack.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current == 6 && !shouldShowWeeklyGoal()) {
                binding.viewPager.currentItem = 4
            } else if (current > 0) {
                binding.viewPager.currentItem -= 1
            } else {
                finish()
            }
        }
    }

    private fun shouldShowWeeklyGoal(): Boolean {
        val goal = userPreference.getUser().goal
        return goal == "Weight Loss Focus" || goal == "Muscle Building"
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 7

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> OnboardingAgeFragment()
                    1 -> OnboardingHeightFragment()
                    2 -> OnboardingWeightFragment()
                    3 -> OnboardingActivityLevelFragment()
                    4 -> OnboardingGoalFragment()
                    5 -> OnboardingWeeklyGoalFragment()
                    6 -> OnboardingFinishFragment()
                    else -> OnboardingAgeFragment()
                }
            }
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateSegmentedProgress(position)
            }
        })
    }

    private fun updateSegmentedProgress(position: Int) {
        val segments = listOf(
            binding.progress1, binding.progress2, binding.progress3,
            binding.progress4, binding.progress5, binding.progress6,
            binding.progress7
        )

        segments.forEachIndexed { index, view ->
            if (index <= position) {
                view.setBackgroundResource(R.drawable.bg_progress_segment_active)
            } else {
                view.setBackgroundResource(R.drawable.bg_progress_segment_inactive)
            }
        }
    }

    override fun nextStep() {
        val current = binding.viewPager.currentItem
        if (current == 4 && !shouldShowWeeklyGoal()) {
            binding.viewPager.currentItem = 6
        } else if (current < 6) {
            binding.viewPager.currentItem += 1
        }
    }
}
