package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.R
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingGoalBinding
import com.capstone.smartbite.utils.HealthMath

class OnboardingGoalFragment : Fragment() {

    private var _binding: FragmentOnboardingGoalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference
    private var selectedGoal: String? = null
    private var isSafe: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        val user = userPreference.getUser()

        // Pre-select if already exists
        if (!user.goal.isNullOrEmpty()) {
            updateUI(user.goal!!)
            if (user.targetWeight > 0) {
                if (user.goal == "Weight Loss Focus") {
                    binding.edtTargetWeightLose.setText(user.targetWeight.toString())
                } else if (user.goal == "Muscle Building") {
                    binding.edtTargetWeightGain.setText(user.targetWeight.toString())
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnMaintain.setOnClickListener { updateUI("Maintain Weight") }
        binding.btnLoseWeight.setOnClickListener { updateUI("Weight Loss Focus") }
        binding.btnGainMuscle.setOnClickListener { updateUI("Muscle Building") }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkWeightSafety()
                validateSelection()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.edtTargetWeightLose.addTextChangedListener(textWatcher)
        binding.edtTargetWeightGain.addTextChangedListener(textWatcher)

        // Add focus listener to scroll when keyboard appears
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.scrollView.postDelayed({
                    binding.scrollView.smoothScrollTo(0, v.bottom + 100)
                }, 200)
            }
        }
        binding.edtTargetWeightLose.onFocusChangeListener = focusListener
        binding.edtTargetWeightGain.onFocusChangeListener = focusListener

        binding.btnNext.setOnClickListener {
            if (selectedGoal != null && isSafe) {
                val currentUser = userPreference.getUser()
                currentUser.goal = selectedGoal
                
                currentUser.targetWeight = when (selectedGoal) {
                    "Weight Loss Focus" -> binding.edtTargetWeightLose.text.toString().toIntOrNull() ?: 0
                    "Muscle Building" -> binding.edtTargetWeightGain.text.toString().toIntOrNull() ?: 0
                    else -> currentUser.weight
                }

                userPreference.setUser(currentUser)
                (activity as? OnboardingNavigator)?.nextStep()
            }
        }
    }

    private fun checkWeightSafety() {
        val user = userPreference.getUser()
        val targetWeight = when (selectedGoal) {
            "Weight Loss Focus" -> binding.edtTargetWeightLose.text.toString().toIntOrNull() ?: 0
            "Muscle Building" -> binding.edtTargetWeightGain.text.toString().toIntOrNull() ?: 0
            else -> user.weight
        }

        var warningMessage: String? = null
        var shouldBlock = false

        // 1. Cek Logika Target vs Berat Sekarang
        if (selectedGoal == "Weight Loss Focus" && targetWeight >= user.weight && targetWeight > 0) {
            warningMessage = getString(R.string.onboarding_goal_lower_warning)
            shouldBlock = true
        } else if (selectedGoal == "Muscle Building" && targetWeight <= user.weight && targetWeight > 0) {
            warningMessage = getString(R.string.onboarding_goal_higher_warning)
            shouldBlock = true
        }

        // 2. Cek Kesehatan (BMI Categories - Kemenkes RI)
        if (targetWeight > 0) {
            val targetBMI = HealthMath.calculateBMI(targetWeight.toDouble(), user.height)
            
            if (selectedGoal == "Weight Loss Focus") {
                when {
                    targetBMI < 18.5 -> {
                        warningMessage = getString(R.string.onboarding_goal_bmi_low_warning)
                        shouldBlock = true
                    }
                    targetBMI > 27.0 -> {
                        warningMessage = getString(R.string.onboarding_goal_bmi_obese_warning)
                        shouldBlock = true
                    }
                }
            } else if (selectedGoal == "Muscle Building") {
                when {
                    targetBMI > 27.0 -> {
                        warningMessage = getString(R.string.onboarding_goal_bmi_obese_warning)
                        shouldBlock = true
                    }
                    targetBMI > 25.0 -> {
                        warningMessage = getString(R.string.onboarding_goal_overweight_warning)
                        // Warning only, don't block
                    }
                }
            }
        }

        // Reset visibility of both warnings
        binding.tvSafetyWarningLose.visibility = View.GONE
        binding.tvSafetyWarningGain.visibility = View.GONE

        if (warningMessage != null) {
            isSafe = !shouldBlock
            val warningView = if (selectedGoal == "Weight Loss Focus") {
                binding.tvSafetyWarningLose
            } else {
                binding.tvSafetyWarningGain
            }
            
            warningView.text = warningMessage
            warningView.visibility = View.VISIBLE
            warningView.setTextColor(if (shouldBlock) android.graphics.Color.RED else android.graphics.Color.parseColor("#FFA500")) // Orange for warning
        } else {
            isSafe = true
        }
    }

    private fun updateUI(goal: String) {
        selectedGoal = goal
        val user = userPreference.getUser()
        
        // Simpan perubahan secara instan agar logika skip di Activity sinkron
        user.goal = goal
        userPreference.setUser(user)
        
        resetCard(binding.btnMaintain, binding.ivCheckMaintain)
        resetCard(binding.btnLoseWeight, binding.ivCheckLose)
        resetCard(binding.btnGainMuscle, binding.ivCheckGain)
        
        binding.llTargetWeightLose.visibility = View.GONE
        binding.llTargetWeightGain.visibility = View.GONE
        binding.tvGoalInfoLose.visibility = View.GONE
        binding.tvGoalInfoGain.visibility = View.GONE

        when (goal) {
            "Maintain Weight" -> selectCard(binding.btnMaintain, binding.ivCheckMaintain)
            "Weight Loss Focus" -> {
                selectCard(binding.btnLoseWeight, binding.ivCheckLose)
                binding.llTargetWeightLose.visibility = View.VISIBLE
                
                val minSafeWeight = HealthMath.calculateWeightForBMI(18.5, user.height).toInt()
                val maxSafeWeight = user.weight - 1
                binding.tvGoalInfoLose.text = getString(R.string.target_weight_recommendation, minSafeWeight, maxSafeWeight)
                binding.tvGoalInfoLose.visibility = View.VISIBLE
            }
            "Muscle Building" -> {
                selectCard(binding.btnGainMuscle, binding.ivCheckGain)
                binding.llTargetWeightGain.visibility = View.VISIBLE
                
                val minSafeWeight = user.weight + 1
                val maxSafeWeight = HealthMath.calculateWeightForBMI(25.0, user.height).toInt()
                
                if (maxSafeWeight > minSafeWeight) {
                    binding.tvGoalInfoGain.text = getString(R.string.target_weight_recommendation, minSafeWeight, maxSafeWeight)
                } else {
                    binding.tvGoalInfoGain.text = getString(R.string.target_weight_recommendation_start, minSafeWeight)
                }
                binding.tvGoalInfoGain.visibility = View.VISIBLE
            }
        }

        checkWeightSafety()
        validateSelection()
    }

    private fun validateSelection() {
        val isInputValid = when (selectedGoal) {
            "Maintain Weight" -> true
            "Weight Loss Focus" -> binding.edtTargetWeightLose.text.isNotEmpty()
            "Muscle Building" -> binding.edtTargetWeightGain.text.isNotEmpty()
            else -> false
        }

        // Syarat Lanjut: Input valid DAN Aman (isSafe)
        val canProceed = isInputValid && (selectedGoal == "Maintain Weight" || isSafe)

        if (canProceed) {
            binding.btnNext.alpha = 1.0f
            binding.btnNext.isClickable = true
            binding.btnNext.isFocusable = true
        } else {
            binding.btnNext.alpha = 0.5f
            binding.btnNext.isClickable = false
            binding.btnNext.isFocusable = false
        }
    }

    private fun selectCard(card: View, check: android.widget.ImageView) {
        card.setBackgroundResource(R.drawable.bg_goal_card_selected)
        check.setImageResource(R.drawable.ic_check_circle_filled)
        check.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
        
        // Update icon background and tint for the selected card
        when (card.id) {
            binding.btnMaintain.id -> {
                binding.vIconBgMaintain.setBackgroundResource(R.drawable.bg_goal_icon_selected)
                binding.ivIconMaintain.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            }
            binding.btnLoseWeight.id -> {
                binding.vIconBgLose.setBackgroundResource(R.drawable.bg_goal_icon_selected)
                binding.ivIconLose.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            }
            binding.btnGainMuscle.id -> {
                binding.vIconBgGain.setBackgroundResource(R.drawable.bg_goal_icon_selected)
                binding.ivIconGain.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            }
        }
    }

    private fun resetCard(card: View, check: android.widget.ImageView) {
        card.setBackgroundResource(R.drawable.bg_goal_card_unselected)
        check.setImageResource(R.drawable.ic_check_circle_outline)
        check.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1D5DB"))

        // Reset icon background and tint
        when (card.id) {
            binding.btnMaintain.id -> {
                binding.vIconBgMaintain.setBackgroundResource(R.drawable.bg_goal_icon_unselected)
                binding.ivIconMaintain.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            }
            binding.btnLoseWeight.id -> {
                binding.vIconBgLose.setBackgroundResource(R.drawable.bg_goal_icon_unselected)
                binding.ivIconLose.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            }
            binding.btnGainMuscle.id -> {
                binding.vIconBgGain.setBackgroundResource(R.drawable.bg_goal_icon_unselected)
                binding.ivIconGain.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.brand_green, null))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
