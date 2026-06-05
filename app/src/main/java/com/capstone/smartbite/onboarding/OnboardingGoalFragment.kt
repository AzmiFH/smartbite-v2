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

class OnboardingGoalFragment : Fragment() {

    private var _binding: FragmentOnboardingGoalBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference
    private var selectedGoal: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext())
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

        binding.btnMaintain.setOnClickListener { updateUI("Maintain Weight") }
        binding.btnLoseWeight.setOnClickListener { updateUI("Weight Loss Focus") }
        binding.btnGainMuscle.setOnClickListener { updateUI("Muscle Building") }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateSelection()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.edtTargetWeightLose.addTextChangedListener(textWatcher)
        binding.edtTargetWeightGain.addTextChangedListener(textWatcher)

        binding.btnNext.setOnClickListener {
            if (selectedGoal != null) {
                user.goal = selectedGoal
                
                user.targetWeight = when (selectedGoal) {
                    "Weight Loss Focus" -> binding.edtTargetWeightLose.text.toString().toIntOrNull() ?: 0
                    "Muscle Building" -> binding.edtTargetWeightGain.text.toString().toIntOrNull() ?: 0
                    else -> user.weight // Maintain weight means target is current weight
                }

                userPreference.setUser(user)
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
    }

    private fun updateUI(goal: String) {
        selectedGoal = goal
        
        // Reset all
        resetCard(binding.btnMaintain, binding.ivCheckMaintain)
        resetCard(binding.btnLoseWeight, binding.ivCheckLose)
        resetCard(binding.btnGainMuscle, binding.ivCheckGain)
        
        binding.llTargetWeightLose.visibility = View.GONE
        binding.llTargetWeightGain.visibility = View.GONE

        // Select one
        when (goal) {
            "Maintain Weight" -> selectCard(binding.btnMaintain, binding.ivCheckMaintain)
            "Weight Loss Focus" -> {
                selectCard(binding.btnLoseWeight, binding.ivCheckLose)
                binding.llTargetWeightLose.visibility = View.VISIBLE
            }
            "Muscle Building" -> {
                selectCard(binding.btnGainMuscle, binding.ivCheckGain)
                binding.llTargetWeightGain.visibility = View.VISIBLE
            }
        }

        validateSelection()
    }

    private fun validateSelection() {
        val isValid = when (selectedGoal) {
            "Maintain Weight" -> true
            "Weight Loss Focus" -> binding.edtTargetWeightLose.text.isNotEmpty()
            "Muscle Building" -> binding.edtTargetWeightGain.text.isNotEmpty()
            else -> false
        }

        if (isValid) {
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