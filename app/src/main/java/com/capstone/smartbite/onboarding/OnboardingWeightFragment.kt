package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingWeightBinding
import com.kevalpatel2106.rulerpicker.RulerValuePickerListener

class OnboardingWeightFragment : Fragment() {

    private var _binding: FragmentOnboardingWeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        val user = userPreference.getUser()

        setupRulerPicker(user.weight)

        binding.btnNext.setOnClickListener {
            val currentUser = userPreference.getUser()
            currentUser.weight = binding.weightRulerPicker.currentValue
            userPreference.setUser(currentUser)
            (activity as? OnboardingActivity)?.nextStep()
        }
    }

    private fun setupRulerPicker(currentWeight: Int) {
        binding.weightRulerPicker.apply {
            // Set initial value
            val initialValue = if (currentWeight > 30) currentWeight else 70
            selectValue(initialValue)
            binding.tvWeightValue.text = initialValue.toString()
            binding.tvRulerBadge.text = "$initialValue kg"

            setValuePickerListener(object : RulerValuePickerListener {
                override fun onValueChange(value: Int) {
                    binding.tvWeightValue.text = value.toString()
                    binding.tvRulerBadge.text = "$value kg"
                }

                override fun onIntermediateValueChange(selectedValue: Int) {
                    binding.tvWeightValue.text = selectedValue.toString()
                    binding.tvRulerBadge.text = "$selectedValue kg"
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}