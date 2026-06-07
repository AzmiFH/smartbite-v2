package com.capstone.smartbite.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.databinding.FragmentOnboardingHeightBinding
import com.kevalpatel2106.rulerpicker.RulerValuePickerListener

class OnboardingHeightFragment : Fragment() {

    private var _binding: FragmentOnboardingHeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingHeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext(), com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        val user = userPreference.getUser()

        setupRulerPicker(user.height)

        binding.btnNext.setOnClickListener {
            val currentUser = userPreference.getUser()
            currentUser.height = binding.heightRulerPicker.currentValue
            userPreference.setUser(currentUser)
            (activity as? OnboardingActivity)?.nextStep()
        }
    }

    private fun setupRulerPicker(currentHeight: Int) {
        binding.heightRulerPicker.apply {
            // Set initial value
            val initialValue = if (currentHeight > 100) currentHeight else 170
            selectValue(initialValue)
            binding.tvHeightValue.text = initialValue.toString()
            binding.tvRulerBadge.text = "$initialValue cm"

            setValuePickerListener(object : RulerValuePickerListener {
                override fun onValueChange(value: Int) {
                    binding.tvHeightValue.text = value.toString()
                    binding.tvRulerBadge.text = "$value cm"
                }

                override fun onIntermediateValueChange(selectedValue: Int) {
                    binding.tvHeightValue.text = selectedValue.toString()
                    binding.tvRulerBadge.text = "$selectedValue cm"
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}